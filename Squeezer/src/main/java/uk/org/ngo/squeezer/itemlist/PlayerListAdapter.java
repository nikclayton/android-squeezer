/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Song;

class PlayerListAdapter extends BaseExpandableListAdapter implements View.OnCreateContextMenuListener {
    private final PlayerListActivity mActivity;

    private final List<SyncGroup> mChildAdapters = new ArrayList();

    /**
     * A list adapter for a synchronization group, containing players.
     * This class is comparable and it has a name for the synchronization group.
     */
    private class SyncGroup extends ItemAdapter<Player> implements Comparable {

        public String syncGroupName; // the name of the synchronization group as displayed in the players screen

        public SyncGroup(PlayerView playerView) {
            super(playerView);
        }

        @Override
        public int compareTo(Object otherSyncGroup) {
            // compare this syncgroup name with the other one, alphabetically
            return this.syncGroupName.compareToIgnoreCase(((SyncGroup)otherSyncGroup).syncGroupName);
        }

        @Override
        public void update(int count, int start, List<Player> syncedPlayersList) {
            Collections.sort(syncedPlayersList); // first order players in syncgroup alphabetically

            // add the list
            super.update(count, start, syncedPlayersList);

            // determine and set synchronization group name (player names divided by commas)
            List<String> playerNames = new ArrayList<String>();
            for (int i = 0; i < this.getCount(); i++) {
                Player p = this.getItem(i);
                playerNames.add(p.getName());
            }
            syncGroupName = Joiner.on(", ").join(playerNames);
        }

    }
    /** The last set of player sync groups that were provided. */
    private Multimap<String, Player> prevPlayerSyncGroups;

    /** Indicates if the list of players has changed. */
    private boolean mPlayersChanged;

    /** The group position of the item that was most recently selected. */
    private int mLastGroupPosition;

    /** Joins elements together with ' - ', skipping nulls. */
    private static final Joiner mJoiner = Joiner.on(" - ").skipNulls();

    /** Count of how many players are in the adapter. */
    private int mPlayerCount;

    public PlayerListAdapter(PlayerListActivity activity) {
        mActivity = activity;
    }

    public void onChildClick(int groupPosition, int childPosition) {
        mChildAdapters.get(groupPosition).onItemSelected(childPosition);
    }

    public void clear() {
        mPlayersChanged = true;
        mChildAdapters.clear();
        mPlayerCount = 0;
        notifyDataSetChanged();
    }

    /**
     * Sets the players in to the adapter.
     *
     * @param playerSyncGroups Multimap, mapping from the player ID of the syncmaster to the
     *     Players synced to that master. See
     *     {@link PlayerListActivity#updateSyncGroups(List, Player)} for how this map is
     *     generated.
     */
    public void setSyncGroups(Multimap<String, Player> playerSyncGroups) {
        // The players might not have changed (so there's no need to reset the contents of the
        // adapter) but information about an individual player might have done.
        if (prevPlayerSyncGroups != null && prevPlayerSyncGroups.equals(playerSyncGroups)) {
            notifyDataSetChanged();
            return;
        }

        prevPlayerSyncGroups = HashMultimap.create(playerSyncGroups);
        clear();

        // Get a list of slaves for every synchronization group
        for (Collection<Player> slaves: playerSyncGroups.asMap().values()) {
            // create a new synchronization group
            SyncGroup syncGroup = new SyncGroup(new PlayerView(mActivity));
            mPlayerCount += slaves.size();
            // add the slaves (the players) to the synchronization group
            syncGroup.update(slaves.size(), 0, new ArrayList<>(slaves));
            // add synchronization group to the child adapters
            mChildAdapters.add(syncGroup);
        }
        Collections.sort(mChildAdapters); // sort syncgroup list alphabetically by syncgroup name
        notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        mPlayersChanged = false;
        ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        long packedPosition = contextMenuInfo.packedPosition;
        if (ExpandableListView.getPackedPositionType(packedPosition)
                == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);

            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo
                    = new AdapterView.AdapterContextMenuInfo(
                    contextMenuInfo.targetView, childPosition, contextMenuInfo.id);

            mChildAdapters.get(groupPosition).onCreateContextMenu(menu, v, adapterContextMenuInfo);

            // Enable player sync menu options if there's more than one player.
            if (mPlayerCount > 1) {
                menu.findItem(R.id.player_sync).setVisible(true);
            }
        }
    }

    public boolean doItemContext(MenuItem menuItem, int groupPosition, int childPosition) {
        if (mPlayersChanged) {
            Toast.makeText(mActivity, mActivity.getText(R.string.player_list_changed),
                    Toast.LENGTH_LONG).show();
            return true;
        }

        mLastGroupPosition = groupPosition;
        return mChildAdapters.get(groupPosition).doItemContext(menuItem, childPosition);
    }

    /**
     * Handle sub menu items of context menus.
     *
     * @param menuItem
     * @return
     */
    public boolean doItemContext(MenuItem menuItem) {
        if (mPlayersChanged) {
            Toast.makeText(mActivity, mActivity.getText(R.string.player_list_changed),
                    Toast.LENGTH_LONG).show();
            return true;
        }

        return mChildAdapters.get(mLastGroupPosition).doItemContext(menuItem);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true; // Should be false, but then there is no divider
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return mChildAdapters.get(groupPosition).getView(childPosition, convertView, parent);
    }

    @Override
    public int getGroupCount() {
        return mChildAdapters.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildAdapters.get(groupPosition).getCount();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mChildAdapters.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildAdapters.get(groupPosition).getItem(childPosition);
    }

    /**
     * Use the ID of the first player in the group as the identifier for the group.
     * <p>
     * {@inheritDoc}
     * @param groupPosition
     * @return
     */
    @Override
    public long getGroupId(int groupPosition) {
        return mChildAdapters.get(groupPosition).getItem(0).getIdAsLong();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return mChildAdapters.get(groupPosition).getItem(childPosition).getIdAsLong();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View row = mActivity.getLayoutInflater().inflate(R.layout.group_player, parent, false);

        TextView text1 = (TextView) row.findViewById(R.id.text1);
        TextView text2 = (TextView) row.findViewById(R.id.text2);

        SyncGroup syncGroup = mChildAdapters.get(groupPosition);
        String header = syncGroup.syncGroupName;
        text1.setText(mActivity.getString(R.string.player_group_header, header));

        Song groupSong = syncGroup.getItem(0).getPlayerState().getCurrentSong();

        if (groupSong != null) {
            text2.setText(mJoiner.join(groupSong.getName(), groupSong.getArtist(),
                    groupSong.getAlbumName()));
        }
        return row;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
