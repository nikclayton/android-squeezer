
package com.danga.squeezer.itemlists;

import java.util.List;

import android.os.RemoteException;

import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.service.SqueezerServerState;

public abstract class SqueezerAbstractSongListActivity extends
        SqueezerBaseListActivity<SqueezerSong> {

    @Override
    protected void registerCallback() throws RemoteException {
        getService().registerSongListCallback(songListCallback);
    }

    @Override
    protected void unregisterCallback() throws RemoteException {
        getService().unregisterSongListCallback(songListCallback);
    }

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        public void onSongsReceived(int count, int start, List<SqueezerSong> items)
                throws RemoteException {
            onItemsReceived(count, start, items);
        }

        public void onItemsFinished() {
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            // TODO Auto-generated method stub

        }
    };

}
