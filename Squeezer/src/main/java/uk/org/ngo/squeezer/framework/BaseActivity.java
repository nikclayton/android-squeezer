/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.framework;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.VolumePanel;
import uk.org.ngo.squeezer.menu.BaseMenuFragment;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.PlayerVolume;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.SqueezePlayer;
import uk.org.ngo.squeezer.util.ThemeManager;

/**
 * Common base class for all activities in Squeezer.
 *
 * @author Kurt Aaholst
 */
public abstract class BaseActivity extends ActionBarActivity implements HasUiThread {

    @Nullable
    private ISqueezeService mService = null;

    private final ThemeManager mTheme = new ThemeManager();

    /** Records whether the activity has registered on the service's event bus. */
    private boolean mRegisteredOnEventBus;

    private final Handler uiThreadHandler = new Handler() {
    };

    private SqueezePlayer squeezePlayer;

    /** Option menu volume control entry. */
    @Nullable
    private MenuItem mMenuItemVolume;

    /** Whether volume changes should be ignored. */
    private boolean mIgnoreVolumeChange;

    /** Volume control panel. */
    @Nullable
    private VolumePanel mVolumePanel;

    protected String getTag() {
        return getClass().getSimpleName();
    }

    /**
     * @return The squeezeservice, or null if not bound
     */
    @Nullable
    public ISqueezeService getService() {
        return mService;
    }

    /**
     * Use this to post Runnables to work off thread
     */
    @Override
    public Handler getUIThreadHandler() {
        return uiThreadHandler;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = (ISqueezeService) binder;
            BaseActivity.this.onServiceConnected(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTheme.onCreate(this);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setIcon(R.drawable.ic_launcher);
        actionBar.setHomeButtonEnabled(true);
        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());

        BaseMenuFragment.add(this, MenuFragment.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTheme.onResume(this);

        if (mService != null) {
            maybeRegisterOnEventBus(mService);
        }

        mVolumePanel = new VolumePanel(this);

        // If SqueezePlayer is installed, start it
        // TODO Only when connected (or at least serveraddress is saved)
        if (SqueezePlayer.hasSqueezePlayer(this) && new Preferences(this).controlSqueezePlayer()) {
            squeezePlayer = new SqueezePlayer(this);
        }

        // Ensure that any image fetching tasks started by this activity do not finish prematurely.
        ImageFetcher.getInstance(this).setExitTasksEarly(false);
    }

    @Override
    public void onPause() {
        mVolumePanel.dismiss();
        mVolumePanel = null;

        if (squeezePlayer != null) {
            squeezePlayer.stopControllingSqueezePlayer();
            squeezePlayer = null;
        }
        if (mRegisteredOnEventBus) {
            // If we are not bound to the service, it's process is no longer
            // running, so the callbacks are already cleaned up.
            if (mService != null) {
                mService.getEventBus().unregister(this);
                mService.cancelItemListRequests(this);
                mService.cancelSubscriptions(this);
            }
            mRegisteredOnEventBus = false;
        }

        // Ensure that any pending image fetching tasks are unpaused, and finish quickly.
        ImageFetcher imageFetcher = ImageFetcher.getInstance(this);
        imageFetcher.setExitTasksEarly(true);
        imageFetcher.setPauseWork(false);

        super.onPause();
    }

    /**
     * Clear the image memory cache if memory gets low.
     */
    @Override
    public void onLowMemory() {
        ImageFetcher.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    /** Fix for https://code.google.com/p/android/issues/detail?id=63570. */
    private boolean mIsRestoredToTop;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            mIsRestoredToTop = true;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !isTaskRoot()
                && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectedly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }

    /**
     * Performs any actions necessary after the service has been connected. Derived classes
     * should call through to the base class.
     * <ul>
     *     <li>Invalidates the options menu so that menu items can be adjusted based on
     *     the state of the service connection.</li>
     *     <li>Ensures that callbacks are registered.</li>
     * </ul>
     *
     * @param service The connection to the bound service.
     */
    @CallSuper
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        supportInvalidateOptionsMenu();
        maybeRegisterOnEventBus(service);
    }

    /**
     * Conditionally registers with the service's EventBus.
     * <p/>
     * Registration can happen in {@link #onResume()} and {@link
     * #onServiceConnected(uk.org.ngo.squeezer.service.ISqueezeService)}, this ensures that it only
     * happens once.
     *
     * @param service The connection to the bound service.
     */
    private void maybeRegisterOnEventBus(@NonNull ISqueezeService service) {
        if (!mRegisteredOnEventBus) {
            service.getEventBus().registerSticky(this);
            mRegisteredOnEventBus = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.base_activity, menu);

        mMenuItemVolume = menu.findItem(R.id.menu_item_volume);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean connected = isConnected();

        if (mMenuItemVolume != null) {
            mMenuItemVolume.setEnabled(connected);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_volume:
                // Show the volume dialog.
                if (mService != null) {
                    PlayerState playerState = mService.getPlayerState();
                    Player player = mService.getActivePlayer();

                    if (playerState != null  && mVolumePanel != null) {
                        mVolumePanel.postVolumeChanged(playerState.getCurrentVolume(),
                                player == null ? "" : player.getName());
                    }

                    return true;
                }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Block searches, when we are not connected.
     */
    @Override
    public boolean onSearchRequested() {
        if (!isConnected()) {
            return false;
        }
        return super.onSearchRequested();
    }

    /*
     * Intercept hardware volume control keys to control Squeezeserver
     * volume.
     *
     * Change the volume when the key is depressed.  Suppress the keyUp
     * event, otherwise you get a notification beep as well as the volume
     * changing.
     */
    @Override
    @CallSuper
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return changeVolumeBy(+5);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return changeVolumeBy(-5);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    @CallSuper
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private boolean changeVolumeBy(int delta) {
        ISqueezeService service = getService();
        if (service == null) {
            return false;
        }
        Log.v(getTag(), "Adjust volume by: " + delta);
        service.adjustVolumeBy(delta);
        return true;
    }

    public void onEvent(PlayerVolume event) {
        if (!mIgnoreVolumeChange && mVolumePanel != null && event.player == mService.getActivePlayer()) {
            mVolumePanel.postVolumeChanged(event.volume, event.player.getName());
        }
    }

    public void setIgnoreVolumeChange(boolean ignoreVolumeChange) {
        mIgnoreVolumeChange = ignoreVolumeChange;
    }

    // Safe accessors

    public boolean canDownload() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
    }

    public boolean isConnected() {
        return mService != null && mService.isConnected();
    }

    public String getServerString(ServerString stringToken) {
        return ServerString.values()[stringToken.ordinal()].getLocalizedString();
    }

    // This section is just an easier way to call squeeze service

    public void play(PlaylistItem item) {
        playlistControl(PLAYLIST_PLAY_NOW, item, R.string.ITEM_PLAYING);
    }

    public void add(PlaylistItem item) {
        playlistControl(PLAYLIST_ADD_TO_END, item, R.string.ITEM_ADDED);
    }

    public void insert(PlaylistItem item) {
        playlistControl(PLAYLIST_PLAY_AFTER_CURRENT, item, R.string.ITEM_INSERTED);
    }

    private void playlistControl(@PlaylistControlCmd String cmd, PlaylistItem item, int resId)
            {
        if (mService == null) {
            return;
        }

        mService.playlistControl(cmd, item);
        Toast.makeText(this, getString(resId, item.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Initiate download of songs for the supplied item.
     *
     * @param item Song or item with songs to download
     * @see ISqueezeService#downloadItem(FilterItem)
     */
    public void downloadItem(FilterItem item) {
        if (canDownload())
            mService.downloadItem(item);
        else
            Toast.makeText(this, R.string.DOWNLOAD_MANAGER_NEEDED, Toast.LENGTH_LONG).show();
    }

    @StringDef({PLAYLIST_PLAY_NOW, PLAYLIST_ADD_TO_END, PLAYLIST_PLAY_AFTER_CURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaylistControlCmd {}
    public static final String PLAYLIST_PLAY_NOW = "load";
    public static final String PLAYLIST_ADD_TO_END = "add";
    public static final String PLAYLIST_PLAY_AFTER_CURRENT = "insert";

    /**
     * Look up an attribute resource styled for the current theme.
     *
     * @param attribute Attribute identifier to look up.
     * @return The resource identifier for the given attribute.
     */
    public int getAttributeValue(int attribute) {
        TypedValue v = new TypedValue();
        getTheme().resolveAttribute(attribute, v, true);
        return v.resourceId;
    }
}
