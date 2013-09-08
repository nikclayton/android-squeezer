/*
 * Copyright (c) 2012 Google Inc.
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

import java.util.EnumSet;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerMusicFolderItem;
import uk.org.ngo.squeezer.util.ImageFetcher;

import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * View for one entry in a {@link SqueezerMusicFolderListActivity}.
 * <p>
 * Shows an entry with an icon indicating the type of the music folder item, and
 * the name of the item.
 *
 * @author nik
 */
public class SqueezerMusicFolderView extends SqueezerBaseItemView<SqueezerMusicFolderItem> {
    @SuppressWarnings("unused")
    private final static String TAG = "SqueezerMusicFolderView";

    public SqueezerMusicFolderView(SqueezerItemListActivity activity) {
        super(activity);

        setViewParams(EnumSet.of(ViewParams.ICON, ViewParams.CONTEXT_BUTTON));
        setLoadingViewParams(EnumSet.of(ViewParams.ICON));
    }

    public void bindView(View view, SqueezerMusicFolderItem item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        String type = item.getType();
        int icon_resource = R.drawable.ic_unknown;

        if (type.equals("folder"))
            icon_resource = R.drawable.ic_music_folder;
        if (type.equals("track"))
            icon_resource = R.drawable.ic_songs;
        if (type.equals("playlist"))
            icon_resource = R.drawable.ic_playlists;

        viewHolder.icon.setImageResource(icon_resource);
    }

    @Override
    public void onItemSelected(int index, SqueezerMusicFolderItem item) throws RemoteException {
        if (item.getType().equals("folder")) {
            SqueezerMusicFolderListActivity.show(getActivity(), item);
        }
    }

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        SqueezerMusicFolderItem item = (SqueezerMusicFolderItem) menuInfo.item;
        if (item.getType().equals("folder")) {
            menu.add(Menu.NONE, R.id.browse_songs, Menu.NONE, R.string.BROWSE_SONGS);
        }
        menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
        menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
        menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);
        if (item.getType().equals("track")) {
            menu.add(Menu.NONE, R.id.download, Menu.NONE, R.string.DOWNLOAD_ITEM);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, SqueezerMusicFolderItem selectedItem)
            throws RemoteException {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                SqueezerMusicFolderListActivity.show(getActivity(), selectedItem);
                return true;
            case R.id.download:
                getActivity().downloadSong(selectedItem.getId());
                return true;
        }
        return super.doItemContext(menuItem, index, selectedItem);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.musicfolder, quantity);
    }
}
