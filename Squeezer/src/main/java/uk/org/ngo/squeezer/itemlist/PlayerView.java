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

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerRenameDialog;
import uk.org.ngo.squeezer.itemlist.dialog.PlayerSyncDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;

public class PlayerView extends BaseItemView<Player> {
    private static final Map<String, Integer> modelIcons = initializeModelIcons();

    private final PlayerListActivity activity;

    public PlayerView(PlayerListActivity activity) {
        super(activity);
        this.activity = activity;

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE);
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return getAdapterView(convertView, parent, viewParams, R.layout.list_item_player);
    }

    @Override
    public ViewHolder createViewHolder() {
        return new PlayerViewHolder();
    }

    @Override
    public void bindView(View view, Player item) {
        final PlayerListActivity activity = (PlayerListActivity) getActivity();
        PlayerState playerState = activity.getPlayerState(item.getId());
        PlayerViewHolder viewHolder = (PlayerViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        viewHolder.icon.setImageResource(getModelIcon(item.getModel()));

        if (viewHolder.volumeBar == null) {
            viewHolder.volumeBar = (SeekBar) view.findViewById(R.id.volume_slider);
            viewHolder.volumeBar.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(item, viewHolder.volumeValue));
        }

        viewHolder.volumeBar.setVisibility(playerState != null ? View.VISIBLE : View.GONE);

        if (playerState != null) {
            if (playerState.isPoweredOn()) {
                Util.setAlpha(viewHolder.text1, 1.0f);
            } else {
                Util.setAlpha(viewHolder.text1, 0.25f);
            }

            viewHolder.volumeBar.setProgress(playerState.getCurrentVolume());

            viewHolder.text2.setVisibility(playerState.getSleepDuration() > 0 ? View.VISIBLE : View.INVISIBLE);
            viewHolder.text2.setText(activity.getServerString(ServerString.SLEEPING_IN)
                    + " " + Util.formatElapsedTime(playerState.getSleep()));
        }
    }

    @Override
    public void onItemSelected(int index, Player item) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menuInfo.menuInflater.inflate(R.menu.playercontextmenu, menu);

        menu.findItem(R.id.sleep).setTitle(activity.getServerString(ServerString.SLEEP));
        String xMinutes = activity.getServerString(ServerString.X_MINUTES);
        menu.findItem(R.id.in_15_minutes).setTitle(String.format(xMinutes, "15"));
        menu.findItem(R.id.in_30_minutes).setTitle(String.format(xMinutes, "30"));
        menu.findItem(R.id.in_45_minutes).setTitle(String.format(xMinutes, "45"));
        menu.findItem(R.id.in_60_minutes).setTitle(String.format(xMinutes, "60"));
        menu.findItem(R.id.in_90_minutes).setTitle(String.format(xMinutes, "90"));

        PlayerState playerState = activity.getPlayerState(menuInfo.item.getId());
        if (playerState != null) {
            if (playerState.getSleepDuration() != 0) {
                MenuItem cancelSleepItem = menu.findItem(R.id.cancel_sleep);
                cancelSleepItem.setTitle(activity.getServerString(ServerString.SLEEP_CANCEL));
                cancelSleepItem.setVisible(true);
            }

            Song currentSong = playerState.getCurrentSong();
            boolean isPlaying = (playerState.isPlaying() && currentSong != null);
            if (isPlaying && !currentSong.isRemote()) {
                MenuItem sleepAtEndOfSongItem = menu.findItem(R.id.end_of_song);
                sleepAtEndOfSongItem.setTitle(activity.getServerString(ServerString.SLEEP_AT_END_OF_SONG));
                sleepAtEndOfSongItem.setVisible(true);
            }

            MenuItem togglePowerItem = menu.findItem(R.id.toggle_power);
            togglePowerItem.setTitle(
                    activity.getString(playerState.isPoweredOn() ? R.string.menu_item_power_off
                            : R.string.menu_item_power_on));
            togglePowerItem.setVisible(true);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, Player selectedItem) {
        activity.setCurrentPlayer(selectedItem);
        ISqueezeService service = activity.getService();
        if (service == null) {
            return super.doItemContext(menuItem, index, selectedItem);
        }

        switch (menuItem.getItemId()) {
            case R.id.sleep:
                // This is the start of a context menu.
                // Just return, as we have set the current player.
                return true;
            case R.id.cancel_sleep:
                service.sleep(selectedItem, 0);
                return true;
            case R.id.rename:
                new PlayerRenameDialog().show(activity.getSupportFragmentManager(),
                        PlayerRenameDialog.class.getName());
                return true;
            case R.id.toggle_power:
                service.togglePower(selectedItem);
                return true;
            case R.id.player_sync:
                new PlayerSyncDialog().show(activity.getSupportFragmentManager(),
                        PlayerSyncDialog.class.getName());
                return true;
        }
        return super.doItemContext(menuItem, index, selectedItem);
    }

    @Override
    public boolean doItemContext(MenuItem menuItem) {
        ISqueezeService service = activity.getService();
        if (service == null) {
            return super.doItemContext(menuItem);
        }

        Player currentPlayer = activity.getCurrentPlayer();
        switch (menuItem.getItemId()) {
            case R.id.end_of_song:
                PlayerState playerState = activity.getPlayerState(currentPlayer.getId());
                if (playerState != null) {
                    Song currentSong = playerState.getCurrentSong();
                    boolean isPlaying = (playerState.isPlaying() && currentSong != null);
                    if (isPlaying && !currentSong.isRemote()) {
                        int sleep = playerState.getCurrentSongDuration() - playerState.getCurrentTimeSecond() + 1;
                        if (sleep >= 0)
                            service.sleep(currentPlayer, sleep);
                    }

                }
                return true;
            case R.id.in_15_minutes:
                service.sleep(currentPlayer, 15*60);
                return true;
            case R.id.in_30_minutes:
                service.sleep(currentPlayer, 30*60);
                return true;
            case R.id.in_45_minutes:
                service.sleep(currentPlayer, 45*60);
                return true;
            case R.id.in_60_minutes:
                service.sleep(currentPlayer, 60*60);
                return true;
            case R.id.in_90_minutes:
                service.sleep(currentPlayer, 90*60);
                return true;
        }
        return super.doItemContext(menuItem);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.player, quantity);
    }

    private static Map<String, Integer> initializeModelIcons() {
        Map<String, Integer> modelIcons = new HashMap<String, Integer>();
        modelIcons.put("baby", R.drawable.ic_baby);
        modelIcons.put("boom", R.drawable.ic_boom);
        modelIcons.put("fab4", R.drawable.ic_fab4);
        modelIcons.put("receiver", R.drawable.ic_receiver);
        modelIcons.put("controller", R.drawable.ic_controller);
        modelIcons.put("sb1n2", R.drawable.ic_sb1n2);
        modelIcons.put("sb3", R.drawable.ic_sb3);
        modelIcons.put("slimp3", R.drawable.ic_slimp3);
        modelIcons.put("softsqueeze", R.drawable.ic_softsqueeze);
        modelIcons.put("squeezeplay", R.drawable.ic_squeezeplay);
        modelIcons.put("transporter", R.drawable.ic_transporter);
        modelIcons.put("squeezeplayer", R.drawable.ic_squeezeplayer);
        return modelIcons;
    }

    private static int getModelIcon(String model) {
        Integer icon = modelIcons.get(model);
        return (icon != null ? icon : R.drawable.ic_blank);
    }

    private class VolumeSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final Player player;
        private final TextView valueView;

        public VolumeSeekBarChangeListener(Player player, TextView valueView) {
            this.player = player;
            this.valueView = valueView;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                ISqueezeService service = activity.getService();
                if (service == null) {
                    return;
                }
                service.adjustVolumeTo(player, progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            activity.setTrackingTouch(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            activity.setTrackingTouch(false);
        }
    }

    private class PowerButtonClickListener implements View.OnClickListener {
        private final Player player;

        private PowerButtonClickListener(Player player) {
            this.player = player;
        }

        @Override
        public void onClick(View v) {
            ISqueezeService service = activity.getService();
            if (service == null) {
                return;
            }
            service.togglePower(player);
        }
    }

    private static class PlayerViewHolder extends ViewHolder {
        SeekBar volumeBar;
        TextView volumeValue;
    }
}
