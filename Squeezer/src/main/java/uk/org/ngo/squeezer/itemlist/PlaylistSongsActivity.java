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

package uk.org.ngo.squeezer.itemlist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.itemlist.dialog.PlaylistDeleteDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlaylistItemMoveDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlaylistRenameDialog;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Song;

public class PlaylistSongsActivity extends BaseListActivity<Song> {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playlist = extras.getParcelable("playlist");
        }
    }

    public static void show(Activity context, Playlist playlist) {
        final Intent intent = new Intent(context, PlaylistSongsActivity.class);
        intent.putExtra("playlist", playlist);
        context.startActivityForResult(intent, PlaylistsActivity.PLAYLIST_SONGS_REQUEST_CODE);
    }

    private Playlist playlist;

    private String oldName;

    public Playlist getPlaylist() {
        return playlist;
    }

    public void playlistRename(String newName) {
        oldName = playlist.getName();
        getService().playlistsRename(playlist, newName);
        playlist.setName(newName);
        getIntent().putExtra("playlist", playlist);
        setResult(PlaylistsActivity.PLAYLIST_RENAMED);
    }

    public void playlistDelete() {
        getService().playlistsDelete(getPlaylist());
        setResult(PlaylistsActivity.PLAYLIST_DELETED);
        finish();
    }

    @Override
    public ItemView<Song> createItemView() {
        return new SongView(this) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);

                menu.setGroupVisible(R.id.group_playlist, true);

                if (menuInfo.position == 0) {
                    menu.findItem(R.id.playlist_move_up).setVisible(false);
                }

                if (menuInfo.position == menuInfo.adapter.getCount() - 1) {
                    menu.findItem(R.id.playlist_move_down).setVisible(false);
                }
            }

            @Override
            public boolean doItemContext(MenuItem menuItem, int index, Song selectedItem) {
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
                        clearAndReOrderItems();
                        return true;

                    case R.id.playlist_move_up:
                        getService().playlistsMove(playlist, index, index - 1);
                        clearAndReOrderItems();
                        return true;

                    case R.id.playlist_move_down:
                        getService().playlistsMove(playlist, index, index + 1);
                        clearAndReOrderItems();
                        return true;

                    case R.id.playlist_move:
                        PlaylistItemMoveDialog.addTo(PlaylistSongsActivity.this,
                                playlist, index);
                        return true;
                }

                return super.doItemContext(menuItem, index, selectedItem);
            }
        };
    }

    @Override
    protected void orderPage(int start) {
        getService().playlistSongs(start, playlist, this);
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistmenu, menu);
        getMenuInflater().inflate(R.menu.playmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_playlists_delete:
                new PlaylistDeleteDialog().show(getSupportFragmentManager(),
                        PlaylistDeleteDialog.class.getName());
                return true;
            case R.id.menu_item_playlists_rename:
                new PlaylistRenameDialog().show(getSupportFragmentManager(),
                        PlaylistRenameDialog.class.getName());
                return true;
            case R.id.play_now:
                play(playlist);
                return true;
            case R.id.add_to_playlist:
                add(playlist);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showServiceMessage(final String msg) {
        getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PlaylistSongsActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setResult(String flagName) {
        Intent intent = new Intent();
        intent.putExtra(flagName, true);
        intent.putExtra(PlaylistsActivity.CURRENT_PLAYLIST, playlist);
        setResult(RESULT_OK, intent);
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback
            = new IServicePlaylistMaintenanceCallback() {

        @Override
        public void onRenameFailed(String msg) {
            playlist.setName(oldName);
            getIntent().putExtra("playlist", playlist);
            showServiceMessage(msg);
        }

        @Override
        public void onCreateFailed(String msg) {
            showServiceMessage(msg);
        }

        @Override
        public Object getClient() {
            return PlaylistSongsActivity.this;
        }
    };

}
