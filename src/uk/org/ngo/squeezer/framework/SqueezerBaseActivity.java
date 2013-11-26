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

import org.acra.ErrorReporter;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.actionbarcompat.ActionBarActivity;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerMenuFragment;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.SqueezerServerString;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Common base class for all activities in the squeezer
 * @author Kurt Aaholst
 *
 */
public abstract class SqueezerBaseActivity extends ActionBarActivity implements HasUiThread {
	private ISqueezeService service = null;
	private final Handler uiThreadHandler = new Handler() {};

	protected abstract void onServiceConnected();

    protected String getTag() {
    	return getClass().getSimpleName();
	}

	/**
	 * @return The squeezeservice, or null if not bound
	 */
	public ISqueezeService getService() {
		return service;
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
            service = ISqueezeService.Stub.asInterface(binder);
            SqueezerBaseActivity.this.onServiceConnected();
        }
		@Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBarHelper().setIcon(R.drawable.ic_launcher);
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());

        MenuFragment.add(this, SqueezerMenuFragment.class);
    }

	@Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }

    /**
     * Block searches, when we are not connected.
     */
    @Override
    public boolean onSearchRequested() {
        if (!isConnected()) return false;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
            changeVolumeBy(+5);
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            changeVolumeBy(-5);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private boolean changeVolumeBy(int delta) {
        if (getService() == null) {
            return false;
        }
        Log.v(getTag(), "Adjust volume by: " + delta);
        try {
            getService().adjustVolumeBy(delta);
            return true;
        } catch (RemoteException e) {
            Log.e(getTag(), "Error from service.adjustVolumeBy: " + e);
        }
        return false;
    }

    // Safe accessors

    public boolean isConnected() {
        if (service == null) {
            return false;
        }
        try {
            return service.isConnected();
        } catch (RemoteException e) {
            Log.e(getTag(), "Service exception in isConnected(): " + e);
        }
        return false;
    }

    public String getIconUrl(String icon) {
        if (service == null || icon == null) {
            return null;
        }
        try {
            return service.getIconUrl(icon);
        } catch (RemoteException e) {
            Log.e(getClass().getSimpleName(), "Error requesting icon url '" + icon + "': " + e);
            return null;
        }
    }

    public String getServerString(SqueezerServerString stringToken) {
        return SqueezerServerString.values()[stringToken.ordinal()].getLocalizedString();
    }

    // This section is just an easier way to call squeeze service

    public void play(SqueezerPlaylistItem item) throws RemoteException {
        playlistControl(PlaylistControlCmd.load, item, R.string.ITEM_PLAYING);
    }

    public void add(SqueezerPlaylistItem item) throws RemoteException {
        playlistControl(PlaylistControlCmd.add, item, R.string.ITEM_ADDED);
    }

    public void insert(SqueezerPlaylistItem item) throws RemoteException {
        playlistControl(PlaylistControlCmd.insert, item, R.string.ITEM_INSERTED);
    }

    private void playlistControl(PlaylistControlCmd cmd, SqueezerPlaylistItem item, int resId)
            throws RemoteException {
        if (service == null)
            return;

        service.playlistControl(cmd.name(), item.getPlaylistTag(), item.getId());
        Toast.makeText(this, getString(resId, item.getName()), Toast.LENGTH_SHORT).show(); 
    }

    /**
     * Attempts to download the supplied song.
     * <p>This method will silently refuse to download if song is null or is remote.
     *
     * @param song song to download
     */
    public void downloadSong(SqueezerSong song) {
        if (song != null && !song.isRemote())
            downloadSong(song.getId());
    }

    /**
     * Attempts to download the song given by songId.
     *
     * @param songId ID of the song to download
     */
    public void downloadSong(String songId) {
        if (songId == null) return;

        /*
         * Quick-and-dirty version. Use ACTION_VIEW to have something try and
         * download the song (probably the browser).
         *
         * TODO: If running on Gingerbread or greater use the Download Manager
         * APIs to have more control over the download.
         */
        try {
            String url = getService().getSongDownloadUrl(songId);

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (RemoteException e) {
            ErrorReporter.getInstance().handleException(e);
            e.printStackTrace();
        }
    }

    private enum PlaylistControlCmd {
    	load,
    	add,
        insert
    }

}
