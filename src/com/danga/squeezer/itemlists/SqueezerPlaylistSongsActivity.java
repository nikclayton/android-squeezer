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

package com.danga.squeezer.itemlists;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.itemlists.dialogs.SqueezerPlaylistDeleteDialog;
import com.danga.squeezer.itemlists.dialogs.SqueezerPlaylistItemMoveDialog;
import com.danga.squeezer.itemlists.dialogs.SqueezerPlaylistRenameDialog;
import com.danga.squeezer.model.SqueezerPlaylist;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerPlaylistSongsActivity extends SqueezerAbstractSongListActivity {
	private static final int PLAYLIST_CONTEXTMENU_PLAY_ITEM = 0;
	private static final int PLAYLIST_CONTEXTMENU_ADD_ITEM = 1;
	private static final int PLAYLIST_CONTEXTMENU_INSERT_ITEM = 2;
	private static final int PLAYLIST_CONTEXTMENU_REMOVE_ITEM = 3;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_UP = 4;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_DOWN = 5;
	private static final int PLAYLIST_CONTEXTMENU_MOVE = 6;

	public static void show(Context context, SqueezerPlaylist playlist) {
	    final Intent intent = new Intent(context, SqueezerPlaylistSongsActivity.class);
	    intent.putExtra(playlist.getClass().getName(), playlist);
	    context.startActivity(intent);
	}

    private SqueezerPlaylist playlist;
    public SqueezerPlaylist getPlaylist() { return playlist; }
	
    private String oldname;
    public String getOldname() { return oldname; }
    public void setOldname(String oldname) { this.oldname = oldname; }

	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {
			@Override
			public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_PLAY_ITEM, 1, R.string.CONTEXTMENU_PLAY_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_ADD_ITEM, 2, R.string.CONTEXTMENU_ADD_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_INSERT_ITEM, 3, R.string.CONTEXTMENU_INSERT_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_REMOVE_ITEM, 4, R.string.PLAYLIST_CONTEXTMENU_REMOVE_ITEM);
				if (index > 0)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_UP, 5, R.string.PLAYLIST_CONTEXTMENU_MOVE_UP);
				if (index < getAdapter().getCount()-1)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_DOWN, 6, R.string.PLAYLIST_CONTEXTMENU_MOVE_DOWN);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE, 7, R.string.PLAYLIST_CONTEXTMENU_MOVE);
			}

			@Override
			public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
				switch (menuItem.getItemId()) {
				case PLAYLIST_CONTEXTMENU_PLAY_ITEM:
					play(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_ADD_ITEM:
					add(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_INSERT_ITEM:
					insert(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_REMOVE_ITEM:
					getService().playlistsRemove(playlist, index);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_UP:
					getService().playlistsMove(playlist, index, index-1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_DOWN:
					getService().playlistsMove(playlist, index, index+1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE:
				    SqueezerPlaylistItemMoveDialog.addTo(SqueezerPlaylistSongsActivity.this, playlist, index);
					return true;
				}
				return false;
			};
		};
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerPlaylist.class.getName().equals(key)) {
					playlist = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().playlistSongs(start, playlist);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		super.registerCallback();
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	};

	@Override
	protected void unregisterCallback() throws RemoteException {
		super.unregisterCallback();
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);

	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlists_delete:
		    SqueezerPlaylistDeleteDialog.addTo(this);
			return true;
		case R.id.menu_item_playlists_rename:
		    SqueezerPlaylistRenameDialog.addTo(this);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				Toast.makeText(SqueezerPlaylistSongsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		public void onRenameFailed(String msg) throws RemoteException {
			playlist.setName(oldname);
			showServiceMessage(msg);
		}

		public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}

    };

}
