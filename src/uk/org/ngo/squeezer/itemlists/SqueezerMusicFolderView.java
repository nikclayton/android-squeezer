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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerMusicFolderItem;
import uk.org.ngo.squeezer.util.ImageFetcher;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * View for one entry in a {@link SqueezerMusicFolderListActivity}.
 * <p>
 * Shows an entry with an icon indicating the type of the music folder item, and
 * the name of the item.
 *
 * @author nik
 */
public class SqueezerMusicFolderView extends SqueezerBaseItemView<SqueezerMusicFolderItem> {
    // Note: Does not derive from SqueezerIconicItemView because the icons that
    // this class displays are packaged with the app, not downloaded from the
    // server.

    private final LayoutInflater mLayoutInflater;

    SqueezerItemListActivity mContext;

    public SqueezerMusicFolderView(SqueezerItemListActivity activity) {
        super(activity);
        mContext = activity;
        mLayoutInflater = activity.getLayoutInflater();
    }

    @Override
    public View getAdapterView(View convertView, SqueezerMusicFolderItem item,
            ImageFetcher imageFetcher) {
        ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(R.layout.icon_large_row_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.label);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.label.setText(item.getName());

        String type = item.getType();
        int icon_resource = R.drawable.icon_help;

        if (type.equals("folder"))
            icon_resource = R.drawable.ic_music_folder;
        if (type.equals("track"))
            icon_resource = R.drawable.ic_songs;
        if (type.equals("playlist"))
            icon_resource = R.drawable.ic_playlists;

        viewHolder.icon.setImageResource(icon_resource);

        return convertView;
    }

    public void onItemSelected(int index, SqueezerMusicFolderItem item) throws RemoteException {
        if (item.getType().equals("folder")) {
            SqueezerMusicFolderListActivity.show(getActivity(), item);
            return;
        }

        // XXX: This duplicates code in SqueezerBaseItemView::doItemContext()
        mContext.play(item);
        Toast.makeText(mContext, mContext.getString(R.string.ITEM_PLAYING, item.getName()),
                Toast.LENGTH_SHORT).show();
    };

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.CONTEXTMENU_PLAY_ITEM);
        menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.CONTEXTMENU_ADD_ITEM);
        menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.CONTEXTMENU_INSERT_ITEM);

        if (((SqueezerMusicFolderItem) menuInfo.item).getType().equals("track")) {
            menu.add(Menu.NONE, R.id.download, Menu.NONE, R.string.CONTEXTMENU_DOWNLOAD_ITEM);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, SqueezerMusicFolderItem selectedItem)
            throws RemoteException {
        switch (menuItem.getItemId()) {
            case R.id.download:
                getActivity().downloadSong(selectedItem.getId());
                return true;
        }
        return super.doItemContext(menuItem, index, selectedItem);
    }

    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.musicfolder, quantity);
    }

    private static class ViewHolder {
        TextView label;
        ImageView icon;
    }
}
