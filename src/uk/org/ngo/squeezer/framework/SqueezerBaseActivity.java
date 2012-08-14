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

import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.UIUtils;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Common base class for all activities in the squeezer
 * @author Kurt Aaholst
 *
 */
public abstract class SqueezerBaseActivity extends FragmentActivity {
	private ISqueezeService service = null;
	private final Handler uiThreadHandler = new Handler() {};

	protected abstract void onServiceConnected() throws RemoteException;

    protected String getTag() {
    	return getClass().getSimpleName();
	}

	/**
	 * @return The squeezeservice, or null if not bound
	 */
	public ISqueezeService getService() {
		return service;
	}

	private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
   			try {
   				SqueezerBaseActivity.this.onServiceConnected();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error in onServiceConnected: " + e);
            }
        }
		public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX: Hack to work around NetworkOnMainThreadException. Hack
        // not necessary on the actionbar branch, and should be removed
        // after the actionbar branch is merged in to master.
        if (UIUtils.hasHoneycomb()) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork()
                    .build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());
    }

	@Override
    public void onPause() {
        super.onPause();
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }

	/**
	 * Use this to post Runnables to work off thread
	 */
	public Handler getUIThreadHandler() {
		return uiThreadHandler;
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


	// This section is just an easier way to call squeeze service

    public boolean play(SqueezerPlaylistItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.load, item);
	}

    public boolean add(SqueezerPlaylistItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.add, item);
	}

    public boolean insert(SqueezerPlaylistItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.insert, item);
	}

    private boolean playlistControl(PlaylistControlCmd cmd, SqueezerPlaylistItem item)
            throws RemoteException {
        if (service == null) {
            return false;
        }
        service.playlistControl(cmd.name(), item.getPlaylistTag(), item.getId());
        return true;
    }

    /**
     * Attempts to download the song given by songId.
     * 
     * @param songId ID of the song to download
     */
    public void downloadSong(String songId) {
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
    	insert;
    }

}
