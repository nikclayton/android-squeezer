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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.model.CurrentPlaylistItem;
import uk.org.ngo.squeezer.model.Player;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerGroupViewHolder> {
    private final PlayerListActivity mActivity;

    private final List<SyncGroup> mChildAdapters = new ArrayList<>();

    /**
     * A list adapter for a synchronization group, containing players.
     * This class is comparable and it has a name for the synchronization group.
     */
    private class SyncGroup extends ItemAdapter<PlayerView, Player> implements Comparable<SyncGroup> {

        public String syncGroupName; // the name of the synchronization group as displayed in the players screen

        public SyncGroup() {
            super(mActivity);
        }

        @Override
        public PlayerView createViewHolder(View view) {
            return new PlayerView((PlayerListActivity) getActivity(), view);
        }

        @Override
        protected int getItemViewType(Player item) {
            return R.layout.list_item_player;
        }

        @Override
        public int compareTo(SyncGroup otherSyncGroup) {
            // compare this sync group name with the other one, alphabetically
            return this.syncGroupName.compareToIgnoreCase((otherSyncGroup).syncGroupName);
        }

        @Override
        public void update(int count, int start, List<Player> syncedPlayersList) {
            Collections.sort(syncedPlayersList); // first order players in sync group alphabetically

            // add the list
            super.update(count, start, syncedPlayersList);

            // determine and set synchronization group name (player names divided by commas)
            List<String> playerNames = new ArrayList<>();
            for (int i = 0; i < this.getItemCount(); i++) {
                Player p = this.getItem(i);
                playerNames.add(p.getName());
            }
            syncGroupName = Joiner.on(", ").join(playerNames);
        }

    }
    /** The last set of player sync groups that were provided. */
    private Multimap<String, Player> prevPlayerSyncGroups;

    /** Indicates if the list of players has changed. */
    boolean mPlayersChanged;

    /** Joins elements together with ' - ', skipping nulls. */
    private static final Joiner mJoiner = Joiner.on(" - ").skipNulls();

    /** Count of how many players are in the adapter. */
    int mPlayerCount;

    public PlayerListAdapter(PlayerListActivity activity) {
        mActivity = activity;
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
     * @param playerSyncGroups Multimap, mapping from the player ID of the sync master to the
     *     Players synced to that master. See
     *     {@link PlayerListActivity#updateSyncGroups(Collection)} for how this map is
     *     generated.
     */
    void setSyncGroups(Multimap<String, Player> playerSyncGroups) {
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
            SyncGroup syncGroup = new SyncGroup();
            mPlayerCount += slaves.size();
            // add the slaves (the players) to the synchronization group
            syncGroup.update(slaves.size(), 0, new ArrayList<>(slaves));
            // add synchronization group to the child adapters
            mChildAdapters.add(syncGroup);
        }
        Collections.sort(mChildAdapters); // sort sync group list alphabetically by sync group name
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mChildAdapters.size();
    }

    @NonNull
    @Override
    public PlayerGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlayerGroupViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.player_group_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerGroupViewHolder holder, int position) {
        SyncGroup syncGroup = mChildAdapters.get(position);
        holder.text1.setText(mActivity.getString(R.string.player_group_header, syncGroup.syncGroupName));

        CurrentPlaylistItem groupSong = syncGroup.getItem(0).getPlayerState().getCurrentSong();
        if (groupSong != null) {
            holder.text2.setText(mJoiner.join(groupSong.getName(), groupSong.getArtist(),
                    groupSong.getAlbum()));
        }

        holder.players.setAdapter(syncGroup);
        holder.players.addItemDecoration(new DividerItemDecoration(holder.players.getContext(), LinearLayoutManager.VERTICAL));
        holder.players.setLayoutManager(new LinearLayoutManager(holder.players.getContext()));
    }

    public static class PlayerGroupViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;
        RecyclerView players;

        public PlayerGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(R.id.text1);
            text2 = itemView.findViewById(R.id.text2);
            players = itemView.findViewById(R.id.players_container);
        }
    }
}
