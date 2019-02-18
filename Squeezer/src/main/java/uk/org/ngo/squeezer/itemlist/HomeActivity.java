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

package uk.org.ngo.squeezer.itemlist;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.v7.app.ActionBar;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import uk.org.ngo.squeezer.BuildConfig;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.ChangeLogDialog;
import uk.org.ngo.squeezer.dialog.TipsDialog;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

public class HomeActivity extends HomeMenuActivity {
    private final String TAG = HomeActivity.class.getSimpleName();

    private GoogleAnalyticsTracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn off the home icon.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);

        // Enable Analytics if the option is on, and we're not running in debug
        // mode so that debug tests don't pollute the stats.
        if ((!BuildConfig.DEBUG) && preferences.getBoolean(Preferences.KEY_ANALYTICS_ENABLED, true)) {
            Log.v("TAG", "Tracking page view '" + TAG + "'");
            // Start the tracker in manual dispatch mode...
            tracker = GoogleAnalyticsTracker.getInstance();
            tracker.startNewSession("UA-26457780-1", this);
            tracker.trackPageView(TAG);
        }

        // Show the change log if necessary.
        ChangeLogDialog changeLog = new ChangeLogDialog(this);
        if (changeLog.isFirstRun()) {
            if (changeLog.isFirstRunEver()) {
                changeLog.skipLogDialog();
            } else {
                changeLog.getThemedLogDialog().show();
            }
        }
    }

    @MainThread
    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);

        // Show a tip about volume controls, if this is the first time this app
        // has run. TODO: Add more robust and general 'tips' functionality.
        PackageInfo pInfo;
        try {
            final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                    0);

            pInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            if (preferences.getLong("lastRunVersionCode", 0) == 0) {
                new TipsDialog().show(getSupportFragmentManager(), "TipsDialog");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("lastRunVersionCode", pInfo.versionCode);
                editor.apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do, don't crash.
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Send analytics stats (if enabled).
        if (tracker != null) {
            tracker.dispatch();
            tracker.stopSession();
        }
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("node", "home");
        context.startActivity(intent);
    }

}