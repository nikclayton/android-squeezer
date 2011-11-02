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

package com.danga.squeezer;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.danga.squeezer.framework.SqueezerBaseActivity;
import com.danga.squeezer.itemlists.SqueezerRadioListActivity;
import com.danga.squeezer.menu.SqueezerMenuFragment;

public class SqueezerHomeActivity extends SqueezerBaseActivity {
    private ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);
        SqueezerMenuFragment.addTo(this);
        setHomeMenu();
	}

    @Override
    protected void onServiceConnected() throws RemoteException {
    }

	private void setHomeMenu() {
		int[] icons = new int[] { R.drawable.icon_nowplaying,
				R.drawable.icon_mymusic, R.drawable.icon_internet_radio,
				R.drawable.icon_my_apps, R.drawable.icon_favorites };
		listView.setAdapter(new IconRowAdapter(this, getResources().getStringArray(R.array.home_items), icons));
		listView.setOnItemClickListener(onHomeItemClick);
	}

	private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
		private static final int NOW_PLAYING = 0;
		private static final int MUSIC = 1;
		private static final int INTERNET_RADIO = 2;
		private static final int APPS = 3;
		private static final int FAVORITES = 4;

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			switch (position) {
			case NOW_PLAYING:
				SqueezerActivity.show(SqueezerHomeActivity.this);
				break;
			case MUSIC:
				SqueezerMusicActivity.show(SqueezerHomeActivity.this);
				break;
            case INTERNET_RADIO:
                // Uncomment these next two lines as an easy way to check crash
                // reporting functionality.
                // String sCrashString = null;
                // Log.e("MyApp", sCrashString.toString());
                SqueezerRadioListActivity.show(SqueezerHomeActivity.this);
                break;
            case APPS:
                // TODO (kaa) implement
                // Currently hidden, by commenting out the entry in strings.xml.
                // SqueezerApplicationListActivity.show(SqueezerHomeActivity.this);
                break;
            case FAVORITES:
                // Currently hidden, by commenting out the entry in strings.xml.
                // TODO: Implement
				break;
			}
		}
	};

	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerHomeActivity.class)
        		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        		.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
