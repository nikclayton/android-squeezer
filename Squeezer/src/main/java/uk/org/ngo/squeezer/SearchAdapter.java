/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.AlbumView;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.itemlist.GenreView;
import uk.org.ngo.squeezer.itemlist.SongView;
import uk.org.ngo.squeezer.itemlist.SongViewWithArt;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Song;

public class SearchAdapter extends BaseExpandableListAdapter implements
        OnCreateContextMenuListener {

    private final int[] groupIcons = {
            R.drawable.ic_songs, R.drawable.ic_albums, R.drawable.ic_artists, R.drawable.ic_genres
    };

    private final SearchActivity activity;

    private final ItemAdapter<? extends Item>[] childAdapters;

    private final Map<Class<? extends Item>, ItemAdapter<? extends Item>> childAdapterMap
            = new HashMap<Class<? extends Item>, ItemAdapter<? extends Item>>();

    public SearchAdapter(SearchActivity activity) {
        this.activity = activity;

        ItemAdapter<?>[] adapters = {
                new ItemAdapter<Song>(new SongViewWithArt(activity)),
                new ItemAdapter<Album>(new AlbumView(activity)),
                new ItemAdapter<Artist>(new ArtistView(activity)),
                new ItemAdapter<Genre>(new GenreView(activity)),
        };

        ((SongViewWithArt) adapters[0].getItemView()).setDetails(
                SongView.DETAILS_DURATION | SongView.DETAILS_ALBUM | SongView.DETAILS_ARTIST);

        ((AlbumView) adapters[1].getItemView()).setDetails(
                AlbumView.DETAILS_ARTIST | AlbumView.DETAILS_YEAR);

        childAdapters = adapters;
        for (ItemAdapter<? extends Item> itemAdapter : childAdapters) {
            childAdapterMap.put(itemAdapter.getItemView().getItemClass(), itemAdapter);
        }
    }

    public void clear() {
        for (ItemAdapter<? extends Item> itemAdapter : childAdapters) {
            itemAdapter.clear();
        }
    }

    public <T extends Item> void updateItems(int count, int start, List<T> items, Class<T> dataType) {
        @SuppressWarnings("unchecked")
        ItemAdapter<T> adapter = (ItemAdapter<T>) childAdapterMap.get(dataType);
        adapter.update(count, start, items);
        notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) menuInfo;
        long packedPosition = contextMenuInfo.packedPosition;
        if (ExpandableListView.getPackedPositionType(packedPosition)
                == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);

            AdapterContextMenuInfo adapterContextMenuInfo = new AdapterContextMenuInfo(
                    contextMenuInfo.targetView, childPosition, contextMenuInfo.id);

            childAdapters[groupPosition].onCreateContextMenu(menu, v, adapterContextMenuInfo);
        }
    }

    public void onChildClick(int groupPosition, int childPosition) {
        childAdapters[groupPosition].onItemSelected(childPosition);
    }

    public boolean doItemContext(MenuItem menuItem, int groupPosition, int childPosition) {
        return childAdapters[groupPosition].doItemContext(menuItem, childPosition);
    }

    @Override
    public PlaylistItem getChild(int groupPosition, int childPosition) {
        return (PlaylistItem) childAdapters[groupPosition].getItem(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        return childAdapters[groupPosition].getView(childPosition, convertView, parent);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return childAdapters[groupPosition].getCount();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return childAdapters[groupPosition];
    }

    @Override
    public int getGroupCount() {
        return childAdapters.length;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        View row = activity.getLayoutInflater().inflate(R.layout.group_item, parent, false);

        TextView label = (TextView) row.findViewById(R.id.label);
        label.setText(childAdapters[groupPosition].getHeader());

        // Build the icon to display next to the text.
        //
        // Take the normal icon (at 48dp) and scale it to 75% of its
        // original size. Then set it as the left-most compound drawable.

        Drawable icon = Squeezer.getContext().getResources().getDrawable(groupIcons[groupPosition]);
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        icon.setBounds(0, 0, (int) Math.ceil(w * 0.75), (int) Math.ceil(h * 0.75));

        label.setCompoundDrawables(icon, null, null, null);

        return (row);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
