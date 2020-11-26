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
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.itemlist.dialog.AlarmSettingsDialog;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.ActivePlayerChanged;
import uk.org.ngo.squeezer.service.event.PlayerPrefReceived;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;
import uk.org.ngo.squeezer.widget.UndoBarController;

public class AlarmsActivity extends BaseListActivity<AlarmView, Alarm> implements AlarmSettingsDialog.HostActivity {
    /** The most recent active player. */
    private Player mActivePlayer;

    /** Toggle/Switch that controls whether all alarms are enabled or disabled. */
    private CompoundButtonWrapper mAlarmsEnabledButton;

    /** View that contains all_alarms_{on,off}_hint text. */
    private TextView mAllAlarmsHintView;

    /** Settings button. */
    private Button mSettingsButton;

    /** Have player preference values been requested from the server? */
    private boolean mPrefsOrdered = false;

    /** Maps from a @Player.Pref.Name to its value. */
    private final Map<String, String> mPlayerPrefs = new HashMap<>();

    private final List<AlarmPlaylist> alarmPlaylists = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((TextView)findViewById(R.id.all_alarms_text)).setText(R.string.ALARM_ALL_ALARMS);
        mAllAlarmsHintView = findViewById(R.id.all_alarms_hint);

        mAlarmsEnabledButton = new CompoundButtonWrapper(findViewById(R.id.alarms_enabled));
        findViewById(R.id.add_alarm).setOnClickListener(v -> TimePickerFragment.show(getSupportFragmentManager(), DateFormat.is24HourFormat(AlarmsActivity.this), getThemeId() == R.style.AppTheme));

        mSettingsButton = findViewById(R.id.settings);
        mSettingsButton.setOnClickListener(view -> new AlarmSettingsDialog().show(getSupportFragmentManager(), "AlarmSettingsDialog"));

        if (savedInstanceState != null) {
            mActivePlayer = savedInstanceState.getParcelable("activePlayer");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mAlarmsEnabledButton.setOncheckedChangeListener((buttonView, isChecked) -> {
            mAllAlarmsHintView.setText(isChecked ? R.string.all_alarms_on_hint : R.string.all_alarms_off_hint);
            if (getService() != null) {
                getService().playerPref(Player.Pref.ALARMS_ENABLED, isChecked ? "1" : "0");
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("activePlayer", mActivePlayer);
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
    protected ItemAdapter<AlarmView, Alarm> createItemListAdapter() {
        return new AlarmsAdapter(this);
    }

    private static class AlarmsAdapter extends ItemAdapter<AlarmView, Alarm> {

        public AlarmsAdapter(AlarmsActivity activity) {
            super(activity);
        }

        @Override
        public AlarmView createViewHolder(View view) {
            return new AlarmView((AlarmsActivity) getActivity(), view);
        }

        @Override
        protected int getItemViewType(Alarm item) {
            return R.layout.list_item_alarm;
        }
    }

    @Override
    protected boolean needPlayer() {
        return true;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.alarms(start, this);
        if (start == 0) {
            mActivePlayer = service.getActivePlayer();
            service.alarmPlaylists(alarmPlaylistsCallback);

            mAlarmsEnabledButton.setEnabled(false);
            service.playerPref(Player.Pref.ALARMS_ENABLED);
        }
    }

    private final IServiceItemListCallback<AlarmPlaylist> alarmPlaylistsCallback = new IServiceItemListCallback<AlarmPlaylist>() {
        private final List<AlarmPlaylist> alarmPlaylists = new ArrayList<>();

        @Override
        public void onItemsReceived(final int count, final int start, Map<String, Object> parameters, final List<AlarmPlaylist> items, Class<AlarmPlaylist> dataType) {
            runOnUiThread(() -> {
                if (start == 0) {
                    alarmPlaylists.clear();
                }

                alarmPlaylists.addAll(items);
                if (start + items.size() >= count) {
                    setAlarmPlaylists(alarmPlaylists);
                    getItemAdapter().notifyDataSetChanged();

                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    public void setAlarmPlaylists(List<AlarmPlaylist> alarmPlaylists) {
        String currentCategory = null;

        this.alarmPlaylists.clear();
        for (AlarmPlaylist alarmPlaylist : alarmPlaylists) {
            if (!alarmPlaylist.getCategory().equals(currentCategory)) {
                AlarmPlaylist categoryAlarmPlaylist = new AlarmPlaylist();
                categoryAlarmPlaylist.setCategory(alarmPlaylist.getCategory());
                this.alarmPlaylists.add(categoryAlarmPlaylist);
            }
            this.alarmPlaylists.add(alarmPlaylist);
            currentCategory = alarmPlaylist.getCategory();
        }
    }

    public void onEventMainThread(PlayerPrefReceived event) {
        if (!event.player.equals(getService().getActivePlayer())) {
            return;
        }

        mPlayerPrefs.put(event.pref, event.value);

        if (Player.Pref.ALARMS_ENABLED.equals(event.pref)) {
            boolean checked = Integer.parseInt(event.value) > 0;
            mAlarmsEnabledButton.setEnabled(true);
            mAlarmsEnabledButton.setChecked(checked);
            mAllAlarmsHintView.setText(checked ? R.string.all_alarms_on_hint : R.string.all_alarms_off_hint);
        }

        // The settings dialog can only be shown after all 4 prefs have been received, so
        // that it can show their values.
        if (mSettingsButton.getVisibility() == View.INVISIBLE) {
            if (mPlayerPrefs.containsKey(Player.Pref.ALARM_DEFAULT_VOLUME) &&
                    mPlayerPrefs.containsKey(Player.Pref.ALARM_SNOOZE_SECONDS) &&
                    mPlayerPrefs.containsKey(Player.Pref.ALARM_TIMEOUT_SECONDS) &&
                    mPlayerPrefs.containsKey(Player.Pref.ALARM_FADE_SECONDS)) {
                mSettingsButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @MainThread
    public void onEventMainThread(ActivePlayerChanged event) {
        super.onEventMainThread(event);
        mActivePlayer = event.player;
    }

    @Override
    @NonNull
    public Player getPlayer() {
        return mActivePlayer;
    }

    @Override
    @NonNull
    public String getPlayerPref(@NonNull @Player.Pref.Name String playerPref, @NonNull String def) {
        String ret = mPlayerPrefs.get(playerPref);
        if (ret == null) {
            ret = def;
        }
        return ret;
    }

    public List<AlarmPlaylist> getAlarmPlaylists() {
        return alarmPlaylists;
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
