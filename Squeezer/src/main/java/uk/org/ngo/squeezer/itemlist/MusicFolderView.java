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

package uk.org.ngo.squeezer.itemlist;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItemView;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;
import uk.org.ngo.squeezer.model.MusicFolderItem;

/**
 * View for one entry in a {@link MusicFolderListActivity}.
 * <p/>
 * Shows an entry with an icon indicating the type of the music folder item, and the name of the
 * item.
 *
 * @author nik
 */
public class MusicFolderView extends PlaylistItemView<MusicFolderItem> {

    @SuppressWarnings("unused")
    private final static String TAG = "MusicFolderView";

    public MusicFolderView(ItemListActivity activity) {
        super(activity);

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON);
    }

    @Override
    public void bindView(View view, MusicFolderItem item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        String type = item.getType();
        int icon_resource = R.drawable.ic_unknown;

        if ("folder".equals(type)) {
            icon_resource = R.drawable.ic_music_folder;
        }
        if ("track".equals(type)) {
            icon_resource = R.drawable.ic_songs;
        }
        if ("playlist".equals(type)) {
            icon_resource = R.drawable.ic_playlists;
        }

        viewHolder.icon.setImageResource(icon_resource);
    }

    @Override
    protected PlayableItemAction getOnSelectAction() {
        String actionType = preferences.getString(Preferences.KEY_ON_SELECT_SONG_ACTION,
                PlayableItemAction.Type.NONE.name());
        return PlayableItemAction.createAction(getActivity(), actionType);
    }

    @Override
    public boolean isSelectable(MusicFolderItem item) {
        if ("track".equals(item.getType())) {
            return super.isSelectable(item);
        } else
        if ("folder".equals(item.getType())) {
            return true;
        }
        return false;
    }

    @Override
    public void onItemSelected(int index, MusicFolderItem item) {
        if ("track".equals(item.getType())) {
            super.onItemSelected(index, item);
        } else
        if ("folder".equals(item.getType())) {
            MusicFolderListActivity.show(getActivity(), item);
        }
    }

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MusicFolderItem item = (MusicFolderItem) menuInfo.item;
        if ("folder".equals(item.getType())) {
            menu.add(Menu.NONE, R.id.browse_songs, Menu.NONE, R.string.BROWSE_SONGS);
        }
        menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
        menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
        menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);

        // Support downloading tracks and folders, but only if the server provided a URL for
        // them. At least one server seen in the wild does not, see the crash at
        // https://www.crashlytics.com/squeezer/android/apps/uk.org.ngo.squeezer/issues/5480783765f8dfea153f1e34/sessions/5480787e0060000179ac2a75b3a3f1e2
        // for an example.
        if (("track".equals(item.getType()) || "folder".equals(item.getType()))
                && (item.getUrl() != null)) {
            menu.add(Menu.NONE, R.id.download, Menu.NONE, R.string.DOWNLOAD_ITEM);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, MusicFolderItem selectedItem) {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                MusicFolderListActivity.show(getActivity(), selectedItem);
                return true;
            case R.id.download:
                if ("track".equals(selectedItem.getType()) || "folder".equals(selectedItem.getType())) {
                    getActivity().downloadItem(selectedItem);
                }
                return true;
        }
        return super.doItemContext(menuItem, index, selectedItem);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.musicfolder, quantity);
    }
}
