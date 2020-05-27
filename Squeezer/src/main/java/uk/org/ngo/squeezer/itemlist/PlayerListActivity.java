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

import android.os.Bundle;

import androidx.annotation.NonNull;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.dialog.DefeatDestructiveTouchToPlayDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayTrackAlbumDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerSyncDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PlayerVolume;


public class PlayerListActivity extends PlayerListBaseActivity implements
        PlayerSyncDialog.PlayerSyncDialogHost,
        PlayTrackAlbumDialog.PlayTrackAlbumDialogHost,
        DefeatDestructiveTouchToPlayDialog.DefeatDestructiveTouchToPlayDialogHost {
    private static final String CURRENT_PLAYER = "currentPlayer";

    private Player currentPlayer;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_PLAYER, currentPlayer);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected boolean needPlayer() {
        return false;
    }

    public void onEventMainThread(PlayerStateChanged event) {
        if (!mTrackingTouch) {
            updateAndExpandPlayerList();
        } else {
            mUpdateWhileTracking = true;
        }
    }

    public void onEventMainThread(PlayerVolume event) {
        if (!mTrackingTouch) {
            mResultsAdapter.notifyDataSetChanged();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.item_list_players);

        if (savedInstanceState != null)
            currentPlayer = savedInstanceState.getParcelable(PlayerListActivity.CURRENT_PLAYER);
    }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void playerRename(String newName) {
        ISqueezeService service = getService();
        if (service == null) {
            return;
        }

        service.playerRename(currentPlayer, newName);
        this.currentPlayer.setName(newName);
        mResultsAdapter.notifyDataSetChanged();
    }

    public PlayerBaseView createPlayerView() {
        return new PlayerView(this);
    }

    /**
     * Synchronises the slave player to the player with masterId.
     *
     * @param slave    the player to sync.
     * @param masterId ID of the player to sync to.
     */
    @Override
    public void syncPlayerToPlayer(@NonNull Player slave, @NonNull String masterId) {
        getService().syncPlayerToPlayer(slave, masterId);
    }

    /**
     * Removes the player from any sync groups.
     *
     * @param player the player to be removed from sync groups.
     */
    @Override
    public void unsyncPlayer(@NonNull Player player) {
        getService().unsyncPlayer(player);
    }

    @Override
    public String getPlayTrackAlbum() {
        return currentPlayer.getPlayerState().prefs.get(Player.Pref.PLAY_TRACK_ALBUM);
    }

    @Override
    public void setPlayTrackAlbum(@NonNull String option) {
        getService().playerPref(currentPlayer, Player.Pref.PLAY_TRACK_ALBUM, option);
    }

    @Override
    public String getDefeatDestructiveTTP() {
        return currentPlayer.getPlayerState().prefs.get(Player.Pref.DEFEAT_DESTRUCTIVE_TTP);
    }

    @Override
    public void setDefeatDestructiveTTP(@NonNull String option) {
        getService().playerPref(currentPlayer, Player.Pref.DEFEAT_DESTRUCTIVE_TTP, option);
    }
}
