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


import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import uk.org.ngo.squeezer.dialog.TipsDialog;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.ApplicationListActivity;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.FavoriteListActivity;
import uk.org.ngo.squeezer.itemlist.GenreListActivity;
import uk.org.ngo.squeezer.itemlist.MusicFolderListActivity;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;
import uk.org.ngo.squeezer.itemlist.RadioListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.itemlist.YearListActivity;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.service.IServiceHandshakeCallback;

public class HomeActivity extends BaseActivity {

    private final String TAG = "HomeActivity";

    private static final int ARTISTS = 0;

    private static final int ALBUMS = 1;

    private static final int SONGS = 2;

    private static final int GENRES = 3;

    private static final int YEARS = 4;

    private static final int NEW_MUSIC = 5;

    private static final int MUSIC_FOLDER = 6;

    private static final int RANDOM_MIX = 7;

    private static final int PLAYLISTS = 8;

    private static final int INTERNET_RADIO = 9;

    private static final int FAVORITES = 10;

    private static final int MY_APPS = 11;

    private boolean mCanFavorites = false;

    private boolean mCanMusicfolder = false;

    private boolean mCanMyApps = false;

    private boolean mCanRandomplay = false;

    private ListView listView;

    private GoogleAnalyticsTracker tracker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);

        // Enable Analytics if the option is on, and we're not running in debug
        // mode so that debug tests don't pollute the stats.
        if (preferences.getBoolean(Preferences.KEY_ANALYTICS_ENABLED, true)) {
            if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                Log.v("NowPlayingActivity", "Tracking page view 'HomeActivity");
                // Start the tracker in manual dispatch mode...
                tracker = GoogleAnalyticsTracker.getInstance();
                tracker.startNewSession("UA-26457780-1", this);
                tracker.trackPageView("HomeActivity");
            }
        }

        // Show the change log if necessary.
        ChangeLog changeLog = new ChangeLog(this);
        if (changeLog.isFirstRun()) {
            changeLog.getLogDialog().show();
        }
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        getService().registerHandshakeCallback(mCallback);
    }

    private final IServiceHandshakeCallback mCallback = new IServiceHandshakeCallback() {

        /**
         * Sets the menu after handshaking with the SqueezeServer has completed.
         * <p>
         * This is necessary because the service doesn't know whether the server
         * supports music folder browsing and random play ability until the
         * handshake completes, and the menu is adjusted depending on whether or
         * not those abilities exist.
         */
        @Override
        public void onHandshakeCompleted() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createListItems();

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
                            editor.commit();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // Nothing to do, don't crash.
                    }
                }
            });
        }

        @Override
        public Object getClient() {
            return HomeActivity.this;
        }
    };

    /**
     * Creates the list of items to show in the activity.
     * <p/>
     * Must be run on the UI thread.
     */
    private void createListItems() {
        int[] icons = new int[]{
                R.drawable.ic_artists,
                R.drawable.ic_albums, R.drawable.ic_songs,
                R.drawable.ic_genres, R.drawable.ic_years, R.drawable.ic_new_music,
                R.drawable.ic_music_folder, R.drawable.ic_random,
                R.drawable.ic_playlists, R.drawable.ic_internet_radio,
                R.drawable.ic_favorites, R.drawable.ic_my_apps
        };

        String[] items = getResources().getStringArray(R.array.home_items);

        if (getService() != null) {
            mCanFavorites = getService().canFavorites();
            mCanMusicfolder = getService().canMusicfolder();
            mCanMyApps = getService().canMyApps();
            mCanRandomplay = getService().canRandomplay();
        }

        List<IconRowAdapter.IconRow> rows = new ArrayList<IconRowAdapter.IconRow>();
        for (int i = ARTISTS; i <= MY_APPS; i++) {
            if (i == MUSIC_FOLDER && !mCanMusicfolder) {
                continue;
            }

            if (i == RANDOM_MIX && !mCanRandomplay) {
                continue;
            }

            if (i == FAVORITES && !mCanFavorites) {
                continue;
            }

            if (i == MY_APPS && !mCanMyApps) {
                continue;
            }

            rows.add(new IconRowAdapter.IconRow(i, items[i], icons[i]));
        }

        listView.setAdapter(new IconRowAdapter(this, rows));
        listView.setOnItemClickListener(onHomeItemClick);
    }

    private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch ((int) id) {
                case ARTISTS:
                    ArtistListActivity.show(HomeActivity.this);
                    break;
                case ALBUMS:
                    AlbumListActivity.show(HomeActivity.this);
                    break;
                case SONGS:
                    SongListActivity.show(HomeActivity.this);
                    break;
                case GENRES:
                    GenreListActivity.show(HomeActivity.this);
                    break;
                case YEARS:
                    YearListActivity.show(HomeActivity.this);
                    break;
                case NEW_MUSIC:
                    AlbumListActivity.show(HomeActivity.this,
                            AlbumViewDialog.AlbumsSortOrder.__new);
                    break;
                case MUSIC_FOLDER:
                    MusicFolderListActivity.show(HomeActivity.this);
                    break;
                case RANDOM_MIX:
                    RandomplayActivity.show(HomeActivity.this);
                    break;
                case PLAYLISTS:
                    PlaylistsActivity.show(HomeActivity.this);
                    break;
                case INTERNET_RADIO:
                    // Uncomment these next two lines as an easy way to check
                    // crash reporting functionality.

                    // String sCrashString = null;
                    // Log.e("MyApp", sCrashString.toString());
                    RadioListActivity.show(HomeActivity.this);
                    break;
                case FAVORITES:
                    FavoriteListActivity.show(HomeActivity.this);
                    break;
                case MY_APPS:
                    ApplicationListActivity.show(HomeActivity.this);
                    break;
            }
        }
    };

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
        context.startActivity(intent);
    }

}
