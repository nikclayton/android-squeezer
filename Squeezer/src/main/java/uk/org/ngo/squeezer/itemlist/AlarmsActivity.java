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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.AlarmSettingsDialog;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.event.PlayerPrefReceived;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;
import uk.org.ngo.squeezer.widget.UndoBarController;

public class AlarmsActivity extends BaseListActivity<Alarm> implements AlarmSettingsDialog.HostActivity {
    /** The most recent active player. */
    private Player mActivePlayer;

    private AlarmView mAlarmView;

    /** Toggle/Switch that controls whether all alarms are enabled or disabled. */
    private CompoundButtonWrapper mAlarmsEnabledButton;

    /** View to display when no players are connected. */
    private View mEmptyView;

    /** View to display when at least one player is connected. */
    private View mNonEmptyView;

    /** View that contains all_alarms_{on,off}_hint text. */
    private TextView mAllAlarmsHintView;

    /** Have player preference values been requested from the server? */
    private boolean mPrefsOrdered = false;

    /** Maps from a @Player.Pref.Name to its value. */
    private final Map<String, String> mPlayerPrefs = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNonEmptyView = findViewById(R.id.alarm_manager);
        mEmptyView = findViewById(android.R.id.empty);

        ((TextView)findViewById(R.id.all_alarms_text)).setText(ServerString.ALARM_ALL_ALARMS.getLocalizedString());
        mAllAlarmsHintView = (TextView) findViewById(R.id.all_alarms_hint);

        mAlarmsEnabledButton = new CompoundButtonWrapper((CompoundButton) findViewById(R.id.alarms_enabled));
        findViewById(R.id.add_alarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment.show(getSupportFragmentManager(), DateFormat.is24HourFormat(AlarmsActivity.this), getThemeId() == R.style.AppTheme);
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlarmSettingsDialog().show(getSupportFragmentManager(), "AlarmSettingsDialog");
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mAlarmsEnabledButton.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAllAlarmsHintView.setText(isChecked ? R.string.all_alarms_on_hint : R.string.all_alarms_off_hint);
                if (getService() != null) {
                    getService().playerPref(Player.Pref.ALARMS_ENABLED, isChecked ? "1" : "0");
                }
            }
        });
    }

    @Override
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        super.onServiceConnected(service);
        maybeOrderPrefs(service);
    }

    @Override
    public void onResume() {
        super.onResume();
        ISqueezeService service = getService();
        if (service != null) {
            maybeOrderPrefs(service);
        }
    }

    private void maybeOrderPrefs(ISqueezeService service) {
        if (!mPrefsOrdered) {
            mPrefsOrdered = true;

            service.playerPref(Player.Pref.ALARM_FADE_SECONDS);
            service.playerPref(Player.Pref.ALARM_DEFAULT_VOLUME);
            service.playerPref(Player.Pref.ALARM_SNOOZE_SECONDS);
            service.playerPref(Player.Pref.ALARM_TIMEOUT_SECONDS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        UndoBarController.hide(this);
    }

    public static void show(Activity context) {
        final Intent intent = new Intent(context, AlarmsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    protected int getContentView() {
        return R.layout.item_list_player_alarms;
    }

    @Override
    public ItemView<Alarm> createItemView() {
        mAlarmView = new AlarmView(this);
        return mAlarmView;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.alarms(start, this);
        if (start == 0) {
            mActivePlayer = service.getActivePlayer();
            service.alarmPlaylists(mAlarmPlaylistsCallback);

            mAlarmsEnabledButton.setEnabled(false);
            service.playerPref(Player.Pref.ALARMS_ENABLED);
        }
    }

    private final IServiceItemListCallback<AlarmPlaylist> mAlarmPlaylistsCallback = new IServiceItemListCallback<AlarmPlaylist>() {
        private final List<AlarmPlaylist> mAlarmPlaylists = new ArrayList<>();

        @Override
        public void onItemsReceived(final int count, final int start, Map<String, String> parameters, final List<AlarmPlaylist> items, Class<AlarmPlaylist> dataType) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (start == 0) {
                        mAlarmPlaylists.clear();
                    }

                    mAlarmPlaylists.addAll(items);
                    if (start + items.size() >= count) {
                        mAlarmView.setAlarmPlaylists(mAlarmPlaylists);
                        getItemAdapter().notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    public void onEventMainThread(PlayerPrefReceived event) {
        if (!event.player.equals(getService().getActivePlayer())) {
            return;
        }

        mPlayerPrefs.put(event.pref, event.value);

        if (Player.Pref.ALARMS_ENABLED.equals(event.pref)) {
            boolean checked = Integer.valueOf(event.value) > 0;
            mAlarmsEnabledButton.setEnabled(true);
            mAlarmsEnabledButton.setChecked(checked);
            mAllAlarmsHintView.setText(checked ? R.string.all_alarms_on_hint : R.string.all_alarms_off_hint);
        }
    }

    public void onEventMainThread(PlayersChanged event) {
        // Only include players that are connected to the server.
        ArrayList<Player> connectedPlayers = new ArrayList<>();
        for (Player player : event.players.values()) {
            if (player.getConnected()) {
                connectedPlayers.add(player);
            }
        }

        if (connectedPlayers.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mNonEmptyView.setVisibility(View.GONE);
            mActivePlayer = null;
            return;
        }

        Player newActivePlayer = getService().getActivePlayer();
        if (newActivePlayer != null && newActivePlayer.equals(mActivePlayer)
                && mActivePlayer.getConnected() == newActivePlayer.getConnected()) {
            return;
        }

        mActivePlayer = newActivePlayer;
        mEmptyView.setVisibility(View.GONE);
        mNonEmptyView.setVisibility(View.VISIBLE);
        clearAndReOrderItems();
    }

    @Override
    @NonNull
    public Player getPlayer() {
        return mActivePlayer;
    }

    @Override
    @Nullable
    public String getPlayerPref(@Player.Pref.Name String playerPref) {
        return mPlayerPrefs.get(playerPref);
    }

    @Override
    public void onPositiveClick(int volume, int snooze, int timeout, boolean fade) {
        ISqueezeService service = getService();
        if (service != null) {
            service.playerPref(Player.Pref.ALARM_DEFAULT_VOLUME, String.valueOf(volume));
            service.playerPref(Player.Pref.ALARM_SNOOZE_SECONDS, String.valueOf(snooze));
            service.playerPref(Player.Pref.ALARM_TIMEOUT_SECONDS, String.valueOf(timeout));
            service.playerPref(Player.Pref.ALARM_FADE_SECONDS, fade ? "1" : "0");
        }
    }

    public static class TimePickerFragment extends TimePickerDialog implements TimePickerDialog.OnTimeSetListener {
        BaseListActivity activity;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (BaseListActivity) getActivity();
            setOnTimeSetListener(this);
            return super.onCreateDialog(savedInstanceState);
        }

        public static void show(FragmentManager manager, boolean is24HourMode, boolean dark) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerFragment fragment = new TimePickerFragment();
            fragment.initialize(fragment, hour, minute, is24HourMode);
            fragment.setThemeDark(dark);
            fragment.show(manager, TimePickerFragment.class.getSimpleName());
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            if (activity.getService() != null) {
                activity.getService().alarmAdd((hourOfDay * 60 + minute) * 60);
                // TODO add to list and animate the new alarm in
                activity.clearAndReOrderItems();
            }
        }
    }

}
