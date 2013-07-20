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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistDeleteDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistItemMoveDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerPlaylistRenameDialog;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import uk.org.ngo.squeezer.model.SqueezerSong;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class SqueezerPlaylistSongsActivity extends SqueezerAbstractSongListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playlist = extras.getParcelable("playlist");
        }
    }

	public static void show(Activity context, SqueezerPlaylist playlist) {
	    final Intent intent = new Intent(context, SqueezerPlaylistSongsActivity.class);
	    intent.putExtra("playlist", playlist);
	    context.startActivityForResult(intent, SqueezerPlaylistsActivity.PLAYLIST_SONGS_REQUEST_CODE);
	}

    private SqueezerPlaylist playlist;
    private String oldName;
    public SqueezerPlaylist getPlaylist() { return playlist; }

    public void playlistRename(String newName) {
        try {
            oldName = playlist.getName();
            getService().playlistsRename(playlist, newName);
            playlist.setName(newName);
            getIntent().putExtra("playlist", playlist);
            setResult(SqueezerPlaylistsActivity.PLAYLIST_RENAMED);
        } catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newName + "': " + e);
        }
    }

    public void playlistDelete() {
        try {
            getService().playlistsDelete(getPlaylist());
            setResult(SqueezerPlaylistsActivity.PLAYLIST_DELETED);
            finish();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error deleting playlist");
        }

    }

    @Override
    public SqueezerItemView<SqueezerSong> createItemView() {
        return new SqueezerSongView(this) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);

                menu.setGroupVisible(R.id.group_playlist, true);

                if (menuInfo.position == 0)
                    menu.findItem(R.id.playlist_move_up).setVisible(false);

                if (menuInfo.position == menuInfo.adapter.getCount() - 1)
                    menu.findItem(R.id.playlist_move_down).setVisible(false);
            }

            @Override
            public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem)
                    throws RemoteException {
                switch (menuItem.getItemId()) {
                    case R.id.play_now:
                        play(selectedItem);
                        return true;

                    case R.id.add_to_playlist:
                        add(selectedItem);
                        return true;

                    case R.id.play_next:
                        insert(selectedItem);
                        return true;

                    case R.id.remove_from_playlist:
                        getService().playlistsRemove(playlist, index);
                        orderItems();
                        return true;

                    case R.id.playlist_move_up:
                        getService().playlistsMove(playlist, index, index - 1);
                        orderItems();
                        return true;

                    case R.id.playlist_move_down:
                        getService().playlistsMove(playlist, index, index + 1);
                        orderItems();
                        return true;

                    case R.id.playlist_move:
                        SqueezerPlaylistItemMoveDialog.addTo(SqueezerPlaylistSongsActivity.this,
                                playlist, index);
                        return true;
                }

                return super.doItemContext(menuItem, index, selectedItem);
            }
        };
    }

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().playlistSongs(start, playlist);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		super.registerCallback();
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		super.unregisterCallback();
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);

	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistmenu, menu);
        getMenuInflater().inflate(R.menu.playmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
    		switch (item.getItemId()) {
    		case R.id.menu_item_playlists_delete:
    		    new SqueezerPlaylistDeleteDialog().show(getSupportFragmentManager(), SqueezerPlaylistDeleteDialog.class.getName());
    			return true;
    		case R.id.menu_item_playlists_rename:
    		    new SqueezerPlaylistRenameDialog().show(getSupportFragmentManager(), SqueezerPlaylistRenameDialog.class.getName());
    			return true;
            case R.id.play_now:
                play(playlist);
                return true;
            case R.id.add_to_playlist:
                add(playlist);
                return true;
    		}
        } catch (RemoteException e) {
            Log.e(getTag(), "Error executing menu action '" + item.getMenuInfo() + "': " + e);
        }
        return super.onOptionsItemSelected(item);
	}

    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			@Override
            public void run() {
				Toast.makeText(SqueezerPlaylistSongsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }

    private void setResult(String flagName) {
        Intent intent = new Intent();
        intent.putExtra(flagName, true);
        intent.putExtra(SqueezerPlaylistsActivity.CURRENT_PLAYLIST, playlist);
        setResult(RESULT_OK, intent);
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		@Override
        public void onRenameFailed(String msg) throws RemoteException {
			playlist.setName(oldName);
            getIntent().putExtra("playlist", playlist);
			showServiceMessage(msg);
		}

		@Override
        public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}

    };

}
