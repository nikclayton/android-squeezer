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
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.itemlists.dialogs.SqueezerPlaylistItemMoveDialog;
import com.danga.squeezer.itemlists.dialogs.SqueezerPlaylistSaveDialog;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerCurrentPlaylistActivity extends SqueezerAbstractSongListActivity {
	private static final int PLAYLIST_CONTEXTMENU_PLAY_ITEM = 0;
	private static final int PLAYLIST_CONTEXTMENU_REMOVE_ITEM = 1;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_UP = 2;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_DOWN = 3;
	private static final int PLAYLIST_CONTEXTMENU_MOVE = 4;

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerCurrentPlaylistActivity.class);
	    context.startActivity(intent);
	}

	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {

			@Override
			public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
				getActivity().getService().playlistIndex(index);
				getActivity().finish();
			}

			@Override
			public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_PLAY_ITEM, 1, R.string.CONTEXTMENU_PLAY_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_REMOVE_ITEM, 2, R.string.PLAYLIST_CONTEXTMENU_REMOVE_ITEM);
				if (index > 0)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_UP, 3, R.string.PLAYLIST_CONTEXTMENU_MOVE_UP);
				if (index < getAdapter().getCount()-1)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_DOWN, 4, R.string.PLAYLIST_CONTEXTMENU_MOVE_DOWN);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE, 5, R.string.PLAYLIST_CONTEXTMENU_MOVE);
			}

			@Override
			public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
				switch (menuItem.getItemId()) {
				case PLAYLIST_CONTEXTMENU_PLAY_ITEM:
					getService().playlistIndex(index);
					return true;
				case PLAYLIST_CONTEXTMENU_REMOVE_ITEM:
					getService().playlistRemove(index);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_UP:
					getService().playlistMove(index, index-1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_DOWN:
					getService().playlistMove(index, index+1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE:
					SqueezerPlaylistItemMoveDialog.addTo(SqueezerCurrentPlaylistActivity.this, index);
					return true;
				}
				return false;
			};
		};
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().currentPlaylist(start);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.currentplaylistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlist_clear:
			if (getService() != null)
				try {
					getService().playlistClear();
					finish();
				} catch (RemoteException e) {
					Log.e(getTag(), "Error trying to clear playlist: " + e);
				}
			return true;
		case R.id.menu_item_playlist_save:
		    SqueezerPlaylistSaveDialog.addTo(this);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

}
