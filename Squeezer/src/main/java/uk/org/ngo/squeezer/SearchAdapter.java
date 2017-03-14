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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.AlbumGridView;
import uk.org.ngo.squeezer.itemlist.AlbumView;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.itemlist.GenreView;
import uk.org.ngo.squeezer.itemlist.SongGridView;
import uk.org.ngo.squeezer.itemlist.SongView;
import uk.org.ngo.squeezer.itemlist.SongViewWithArt;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SongViewDialog;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Song;

public class SearchAdapter extends BaseExpandableListAdapter implements
        OnCreateContextMenuListener {

    private final SearchActivity activity;
    private final Group[] groups;
    private final Map<Class<? extends Item>, Group> groupMap
            = new HashMap<Class<? extends Item>, Group>();
    private final View.OnClickListener onItemClickListener;
    private final View.OnLongClickListener onItemLongClickListener;

    private int currentGroup;
    private int currentChild;

    public SearchAdapter(SearchActivity activity) {
        this.activity = activity;
        final Preferences preferences = new Preferences(activity);
        final boolean songGrid= preferences.getSongListLayout() == SongViewDialog.SongListLayout.grid;
        final boolean albumGrid= preferences.getAlbumListLayout() == AlbumViewDialog.AlbumListLayout.grid;

        groups = new Group[]{
                new Group(songGrid, R.drawable.ic_songs, new ItemAdapter<Song>(songGrid ? new SongGridView(activity) : new SongViewWithArt(activity))),
                new Group(albumGrid, R.drawable.ic_albums, new ItemAdapter<Album>(albumGrid ? new AlbumGridView(activity) : new AlbumView(activity))),
                new Group(false, R.drawable.ic_artists, new ItemAdapter<Artist>(new ArtistView(activity))),
                new Group(false, R.drawable.ic_genres, new ItemAdapter<Genre>(new GenreView(activity))),
        };

        ((SongViewWithArt) groups[0].adapter.getItemView()).setDetails(
                SongView.DETAILS_DURATION | SongView.DETAILS_ALBUM | SongView.DETAILS_ARTIST);

        ((AlbumView) groups[1].adapter.getItemView()).setDetails(
                AlbumView.DETAILS_ARTIST | AlbumView.DETAILS_YEAR);

        for (Group group : groups) {
            groupMap.put(group.adapter.getItemView().getItemClass(), group);
        }

        onItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int groupPosition = (int) v.getTag(R.id.group_position_tag);
                int childPosition = (int) v.getTag(R.id.child_position_tag);
                onChildClick(groupPosition, childPosition);
            }
        };

        onItemLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.showContextMenu();
                return true;
            }
        };
    }

    public void clear() {
        for (Group group : groups) {
            group.adapter.clear();
        }
    }

    public <T extends Item> void updateItems(int count, int start, List<T> items, Class<T> dataType) {
        @SuppressWarnings("unchecked")
        ItemAdapter<T> adapter = (ItemAdapter<T>) groupMap.get(dataType).adapter;
        adapter.update(count, start, items);
        notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        currentGroup = (int) v.getTag(R.id.group_position_tag);
        currentChild = (int) v.getTag(R.id.child_position_tag);
        groups[currentGroup].adapter.createContextMenu(menu, v, currentChild);
    }

    public void onChildClick(int groupPosition, int childPosition) {
        groups[groupPosition].adapter.onItemSelected(childPosition);
    }

    public boolean doItemContext(MenuItem menuItem) {
        return groups[currentGroup].adapter.doItemContext(menuItem, currentChild);
    }

    public boolean doItemContext(MenuItem menuItem, int groupPosition, int childPosition) {
        return groups[groupPosition].adapter.doItemContext(menuItem, childPosition);
    }

    @Override
    public PlaylistItem getChild(int groupPosition, int childPosition) {
        return (PlaylistItem) groups[groupPosition].adapter.getItem(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (groups[groupPosition].isGrid) {
            return getViewGroupChild(convertView, groupPosition, childPosition, parent);
        } else {
            return getChildView(groupPosition, childPosition, convertView, parent);
        }
    }

    private View getChildView(int groupPosition, int childPosition, View convertView, ViewGroup parent) {
        if (convertView != null && (int)convertView.getTag(R.id.group_position_tag) != groupPosition)
            convertView = null;
        final View view = groups[groupPosition].adapter.getView(childPosition, convertView, parent);
        prepareItemView(view, groupPosition, childPosition);

        return view;
    }

    private View getViewGroupChild(View convertView, int groupPosition, int childPosition, ViewGroup parent) {
        final Group group = groups[groupPosition];
        ViewGroup row;

        if (convertView != null && convertView instanceof ViewGroup && (int)convertView.getTag(R.id.group_position_tag) == groupPosition) {
            row = (ViewGroup) convertView;
        } else {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            row = (ViewGroup) inflater.inflate(R.layout.expandable_list_row, parent, false);
            row.setTag(R.id.group_position_tag, groupPosition);
        }

        if (group.colCount == null) {
            group.colWidth = parent.getResources().getDimensionPixelSize(R.dimen.grid_column_width);
            group.colCount = (int) Math.floor(parent.getWidth() / group.colWidth);
            notifyDataSetChanged();
        }

        childPosition *= group.colCount;
        for (int i = 0; i < group.colCount; i++) {
            int position = childPosition + i;
            final View view = row.getChildAt(i);
            if (position < group.adapter.getCount()) {
                final View itemView = group.adapter.getView(position, view, row);
                prepareItemView(itemView, groupPosition, position);

                if (view == null) {
                    final ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                    layoutParams.width = group.colWidth;
                    row.addView(itemView);
                }
            } else if (view != null) {
                row.removeViewAt(i);
            }
        }

        return row;
    }

    private void prepareItemView(View view, int groupPosition, int childPosition) {
        view.setTag(R.id.group_position_tag, groupPosition);
        view.setTag(R.id.child_position_tag, childPosition);
        view.setOnClickListener(onItemClickListener);
        view.setOnLongClickListener(onItemLongClickListener);
        view.setOnCreateContextMenuListener(this);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        final Group group = groups[groupPosition];
        final int itemCount = group.adapter.getCount();
        final Integer colCount = group.colCount;
        return (colCount == null ? itemCount : (itemCount + colCount -1) / colCount);
    }

    @Override
    public Group getGroup(int groupPosition) {
        return groups[groupPosition];
    }

    @Override
    public int getGroupCount() {
        return groups.length;
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
        label.setText(groups[groupPosition].adapter.getHeader());

        // Build the icon to display next to the text.
        //
        // Take the normal icon (at 48dp) and scale it to 75% of its
        // original size. Then set it as the left-most compound drawable.

        Drawable icon = Squeezer.getContext().getResources().getDrawable(groups[groupPosition].icon);
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
        return false;
    }

    public static class Group {
        public Group(boolean isGrid, int icon, ItemAdapter<? extends Item> adapter) {
            this.isGrid = isGrid;
            this.icon = icon;
            this.adapter = adapter;
        }

        public int getColCount() {
            return (colCount == null ? 1 : colCount);
        }

        boolean isGrid;
        int icon;
        Integer colWidth;
        Integer colCount;
        ItemAdapter<? extends Item> adapter;
    }
}
