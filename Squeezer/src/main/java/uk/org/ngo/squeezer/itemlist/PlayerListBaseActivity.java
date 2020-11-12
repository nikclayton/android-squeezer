package uk.org.ngo.squeezer.itemlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;


public abstract class PlayerListBaseActivity extends ItemListActivity {
    private static final String TAG = PlayerListBaseActivity.class.getName();

    /**
     * Map from player IDs to Players synced to that player ID.
     */
    private final Multimap<String, Player> mPlayerSyncGroups = HashMultimap.create();
    protected boolean mTrackingTouch;
    /**
     * An update arrived while tracking touches. UI should be re-synced.
     */
    protected boolean mUpdateWhileTracking = false;
    PlayerListAdapter mResultsAdapter;
    private ExpandableListView mResultsExpandableListView;

    public static void show(Context context) {
        final Intent intent = new Intent(context, PlayerListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    /**
     * Updates the adapter with the current players, and ensures that the list view is
     * expanded.
     */
    protected void updateAndExpandPlayerList() {
        // Can't do anything if the adapter hasn't been set (pre-handshake).
        if (mResultsExpandableListView.getAdapter() == null) {
            return;
        }

        updateSyncGroups(getService().getPlayers());
        mResultsAdapter.setSyncGroups(mPlayerSyncGroups);

        for (int i = 0; i < mResultsAdapter.getGroupCount(); i++) {
            mResultsExpandableListView.expandGroup(i);
        }
    }

    @Override
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        super.onServiceConnected(service);
        Log.d(TAG, "onServiceConnected: service.isConnected=" + service.isConnected());

        if (!service.isConnected()) {
            service.startConnect();
        }
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        // Do nothing -- the service has been tracking players from the time it
        // initially connected to the server.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultsAdapter = new PlayerListAdapter(this);

        setIgnoreVolumeChange(true);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(layoutResId);
        // TODO rcv
//        mResultsExpandableListView = (ExpandableListView) listView;
//        mResultsExpandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
//            mResultsAdapter.onGroupClick(v, groupPosition);
//            return true;
//        });
//        mResultsExpandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
//            mResultsAdapter.onChildClick(v, groupPosition, childPosition);
//            return true;
//        });
//
//        mResultsExpandableListView.setOnScrollListener(new ScrollListener());
    }

    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);
        if (mResultsExpandableListView.getExpandableListAdapter() == null)
            mResultsExpandableListView.setAdapter(mResultsAdapter);
        updateAndExpandPlayerList();
    }


    public void onEventMainThread(PlayerStateChanged event) {
        if (!mTrackingTouch) {
            updateAndExpandPlayerList();
        } else {
            mUpdateWhileTracking = true;
        }
    }

    public void setTrackingTouch(boolean trackingTouch) {
        mTrackingTouch = trackingTouch;
        if (!mTrackingTouch) {
            if (mUpdateWhileTracking) {
                mUpdateWhileTracking = false;
                updateAndExpandPlayerList();
            }
        }
    }

    /**
     * Builds the list of lists that is a sync group.
     *
     * @param players List of players.
     */
    public void updateSyncGroups(Collection<Player> players) {
        Map<String, Player> connectedPlayers = new HashMap<>();

        // Make a copy of the players we know about, ignoring unconnected ones.
        for (Player player : players) {
            if (!player.getConnected())
                continue;

            connectedPlayers.put(player.getId(), player);
        }

        mPlayerSyncGroups.clear();

        // Iterate over all the connected players to build the list of master players.
        for (Player player : connectedPlayers.values()) {
            String playerId = player.getId();
            String name = player.getName();
            PlayerState playerState = player.getPlayerState();
            String syncMaster = playerState.getSyncMaster();

            Log.d(TAG, "player discovered: id=" + playerId + ", syncMaster=" + syncMaster + ", name=" + name);
            // If a player doesn't have a sync master then it's in a group of its own.
            if (syncMaster == null) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // If the master is this player then add itself and all the slaves.
            if (playerId.equals(syncMaster)) {
                mPlayerSyncGroups.put(playerId, player);
                continue;
            }

            // Must be a slave. Add it under the master. This might have already
            // happened (in the block above), but might not. For example, it's possible
            // to have a player that's a syncslave of an player that is not connected.
            mPlayerSyncGroups.put(syncMaster, player);
        }
    }

    @NonNull
    public Multimap<String, Player> getPlayerSyncGroups() {
        return mPlayerSyncGroups;
    }

    @Override
    protected void clearItemAdapter() {
        mResultsAdapter.clear();
    }

    public abstract PlayerBaseView createPlayerView(View view);
}
