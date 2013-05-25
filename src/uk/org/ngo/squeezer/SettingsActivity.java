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

import uk.org.ngo.squeezer.dialogs.ServerAddressPreference;
import uk.org.ngo.squeezer.itemlists.actions.PlayableItemAction;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.Scrobble;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener, OnSharedPreferenceChangeListener {
	private final String TAG = "SettingsActivity";

    private static final int DIALOG_SCROBBLE_APPS = 0;

    private ISqueezeService serviceStub = null;
    private ServerAddressPreference addrPref;
    private IntEditTextPreference fadeInPref;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceStub = ISqueezeService.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName name) {
            serviceStub = null;
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + serviceStub);

        getPreferenceManager().setSharedPreferencesName(Preferences.NAME);
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);

        addrPref = (ServerAddressPreference) findPreference(Preferences.KEY_SERVERADDR);
        addrPref.setOnPreferenceChangeListener(this);
        updateAddressSummary(preferences.getString(Preferences.KEY_SERVERADDR, ""));

        fadeInPref = (IntEditTextPreference) findPreference(Preferences.KEY_FADE_IN_SECS);
        fadeInPref.setOnPreferenceChangeListener(this);
        updateFadeInSecondsSummary(preferences.getInt(Preferences.KEY_FADE_IN_SECS, 0));

        CheckBoxPreference autoConnectPref = (CheckBoxPreference) findPreference(Preferences.KEY_AUTO_CONNECT);
        autoConnectPref.setChecked(preferences.getBoolean(Preferences.KEY_AUTO_CONNECT, true));

        // Scrobbling
        CheckBoxPreference scrobblePref = (CheckBoxPreference) findPreference(Preferences.KEY_SCROBBLE_ENABLED);
        scrobblePref.setOnPreferenceChangeListener(this);

        if (!Scrobble.canScrobble()) {
            scrobblePref.setSummaryOff(getString(R.string.settings_scrobble_noapp));
            scrobblePref.setChecked(false);
        } else {
            scrobblePref.setSummaryOff(getString(R.string.settings_scrobble_off));

            scrobblePref
                    .setChecked(preferences.getBoolean(Preferences.KEY_SCROBBLE_ENABLED, false));

            // If an old KEY_SCROBBLE preference exists, use it, delete it, and
            // upgrade it to the new KEY_SCROBBLE_ENABLED preference.
            if (preferences.contains(Preferences.KEY_SCROBBLE)) {
                boolean enabled = (Integer.parseInt(
                        preferences.getString(Preferences.KEY_SCROBBLE, "0")) > 0);
                scrobblePref.setChecked(enabled);
                Editor editor = preferences.edit();
                editor.putBoolean(Preferences.KEY_SCROBBLE_ENABLED, enabled);
                editor.remove(Preferences.KEY_SCROBBLE);
                editor.commit();
            }
		}
		fillPlayableItemSelectionPreferences();
	}

	private void fillPlayableItemSelectionPreferences() {
		String addLabel = getString(PlayableItemAction.Type.ADD.labelId);
		String playLabel = getString(PlayableItemAction.Type.PLAY.labelId);
		String insertLabel = getString(PlayableItemAction.Type.INSERT.labelId);
		String browseLabel = getString(PlayableItemAction.Type.BROWSE.labelId);

		ListPreference onSelectAlbumPref = (ListPreference) findPreference(Preferences.KEY_ON_SELECT_ALBUM_ACTION);
		onSelectAlbumPref.setEntryValues(new String[] {
				PlayableItemAction.Type.PLAY.name(),
				PlayableItemAction.Type.INSERT.name(),
				PlayableItemAction.Type.ADD.name(),
				PlayableItemAction.Type.BROWSE.name() });
		onSelectAlbumPref.setEntries(new String[] { playLabel, insertLabel,
				addLabel, browseLabel });
		onSelectAlbumPref.setDefaultValue(PlayableItemAction.Type.BROWSE.name());
		if (onSelectAlbumPref.getValue() == null) {
			onSelectAlbumPref.setValue(PlayableItemAction.Type.BROWSE.name());
		}

		ListPreference onSelectSongPref = (ListPreference) findPreference(Preferences.KEY_ON_SELECT_SONG_ACTION);
		onSelectSongPref.setEntryValues(new String[] {
				PlayableItemAction.Type.PLAY.name(),
				PlayableItemAction.Type.INSERT.name(),
				PlayableItemAction.Type.ADD.name() });
		onSelectSongPref.setEntries(new String[] { playLabel, insertLabel,
				addLabel });
		onSelectSongPref.setDefaultValue(PlayableItemAction.Type.ADD.name());
		if (onSelectSongPref.getValue() == null) {
			onSelectSongPref.setValue(PlayableItemAction.Type.ADD.name());
		}
	}
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private void updateAddressSummary(String addr) {
        if (addr.length() > 0) {
            addrPref.setSummary(addr);
        } else {
            addrPref.setSummary(R.string.settings_serveraddr_summary);
        }
    }

    private void updateFadeInSecondsSummary(int fadeInSeconds) {
        if (fadeInSeconds == 0) {
            fadeInPref.setSummary(R.string.disabled);
        } else {
            fadeInPref.setSummary(fadeInSeconds + " " + getResources().getQuantityString(R.plurals.seconds, fadeInSeconds));
        }
    }

    /**
     * A preference has been changed by the user, but has not yet been
     * persisted.
     *
     * @param preference
     * @param newValue
     * @return
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		final String key = preference.getKey();
		Log.v(TAG, "preference change for: " + key);

        if (Preferences.KEY_SERVERADDR.equals(key)) {
            final String ipPort = newValue.toString();
            // TODO: check that it looks valid?
            updateAddressSummary(ipPort);
            return true;
        }

        if (Preferences.KEY_FADE_IN_SECS.equals(key)) {
            updateFadeInSecondsSummary(Util.parseDecimalIntOrZero(newValue.toString()));
            return true;
        }

        // If the user has enabled Scrobbling but we don't think it will work
        // pop up a dialog with links to Google Play for apps to install.
        if (Preferences.KEY_SCROBBLE_ENABLED.equals(key)) {
            if (newValue.equals(true) && !Scrobble.canScrobble()) {
                showDialog(DIALOG_SCROBBLE_APPS);

                // User hit back, or similar (or maybe went to install an
                // app). Check again to see if scrobbling will work.
                if (!Scrobble.canScrobble()) {
                    return false;
                }
            }
            return true;
        }

		return false;
	}

    /**
     * A preference has been changed by the user and is going to be persisted.
     *
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(TAG, "Preference changed: " + key);
        if (serviceStub != null) {
            try {
                serviceStub.preferenceChanged(key);
            } catch (RemoteException e) {
                Log.v(TAG, "serviceStub.preferenceChanged() failed: " + e.toString());
            }
        } else {
            Log.v(TAG, "serviceStub is null!");
        }
    }

    @Override
    @Deprecated
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_SCROBBLE_APPS:
                final CharSequence[] apps = {
                        "Last.fm", "ScrobbleDroid", "SLS"
                };
                final CharSequence[] urls = {
                        "fm.last.android", "net.jjc1138.android.scrobbler",
                        "com.adam.aslfms"
                };

                final int[] icons = {
                        R.drawable.ic_launcher_lastfm,
                        R.drawable.ic_launcher_scrobbledroid, R.drawable.ic_launcher_sls
                };

                dialog = new Dialog(this);
                dialog.setContentView(R.layout.scrobbler_choice_dialog);
                dialog.setTitle("Scrobbling applications");

                ListView appList = (ListView) dialog.findViewById(R.id.scrobble_apps);
                appList.setAdapter(new IconRowAdapter(this, apps, icons));

                appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + urls[position]));
                        startActivity(intent);
                    }
                });
        }

        return dialog;
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}
