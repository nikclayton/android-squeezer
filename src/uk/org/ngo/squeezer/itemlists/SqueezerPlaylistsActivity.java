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

package uk.org.ngo.squeezer.itemlists;

import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistsNewDialog;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SqueezerPlaylistsActivity extends SqueezerBaseListActivity<SqueezerPlaylist>{
    public final static int PLAYLIST_SONGS_REQUEST_CODE = 1;
    public static final String PLAYLIST_RENAMED = "playlist_renamed";
    public static final String PLAYLIST_DELETED = "playlist_deleted";

    public static final String CURRENT_PLAYLIST = "currentPlaylist";
    private static final String CURRENT_INDEX = "currentIndex";

    private int currentIndex = -1;
    private SqueezerPlaylist currentPlaylist;
    private String oldName;
    public SqueezerPlaylist getCurrentPlaylist() { return currentPlaylist; }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentIndex = savedInstanceState.getInt(CURRENT_INDEX);
        currentPlaylist = savedInstanceState.getParcelable(CURRENT_PLAYLIST);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_INDEX, currentIndex);
        outState.putParcelable(CURRENT_PLAYLIST, currentPlaylist);
        super.onSaveInstanceState(outState);
    }

    /**
     * Set the playlist to be used as context
     */
    public void setCurrentPlaylist(int index, SqueezerPlaylist playlist) {
        this.currentIndex = index;
	    this.currentPlaylist = playlist;
	}

    /**
     * Rename the playlist previously set as context.
     */
    public void playlistRename(String newName) {
        try {
            getService().playlistsRename(currentPlaylist, newName);
            oldName = currentPlaylist.getName();
            currentPlaylist.setName(newName);
            getItemAdapter().notifyDataSetChanged();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newName + "': " + e);
        }
    }

	@Override
	public SqueezerItemView<SqueezerPlaylist> createItemView() {
		return new SqueezerPlaylistView(this);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPlaylistsCallback(playlistsCallback);
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPlaylistsCallback(playlistsCallback);
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().playlists(start);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(getTag(), "onActivityResult(" + requestCode  + "," + resultCode + ",'" + data + "')");
        if (requestCode == PLAYLIST_SONGS_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(PLAYLIST_RENAMED, false)) {
                currentPlaylist = data.getParcelableExtra(CURRENT_PLAYLIST);
                getItemAdapter().setItem(currentIndex, currentPlaylist);
                getItemAdapter().notifyDataSetChanged();
            }
            if (data.getBooleanExtra(PLAYLIST_DELETED, false)) {
                orderItems();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistsmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlists_new:
		    new SqueezerPlaylistsNewDialog().show(getSupportFragmentManager(), SqueezerPlaylistsNewDialog.class.getName());
		    return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlaylistsActivity.class);
        context.startActivity(intent);
    }

    private final IServicePlaylistsCallback playlistsCallback = new IServicePlaylistsCallback.Stub() {
		@Override
        public void onPlaylistsReceived(int count, int start, List<SqueezerPlaylist> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			@Override
            public void run() {
				getItemAdapter().notifyDataSetChanged();
				Toast.makeText(SqueezerPlaylistsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		@Override
        public void onRenameFailed(String msg) throws RemoteException {
            if (currentIndex != -1)
		        currentPlaylist.setName(oldName);
			showServiceMessage(msg);
		}

		@Override
        public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}

    };

}
