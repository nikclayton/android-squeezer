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


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.Arrays;

import uk.org.ngo.squeezer.framework.BaseActivity;

public class RandomplayActivity extends BaseActivity {

    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);
        setRandomplayMenu();
    }


    private void setRandomplayMenu() {
        String[] values = getResources().getStringArray(R.array.randomplay_items);
        int[] icons = new int[values.length];
        Arrays.fill(icons, R.drawable.ic_random);

        // XXX: Implement the "Choose the genres that the random mix will be
        // drawn from" functionality.
        // icons[icons.length - 1] = R.drawable.ic_genres;
        listView.setAdapter(new IconRowAdapter(this, values, icons));
        listView.setOnItemClickListener(onRandomplayItemClick);
    }

    private final OnItemClickListener onRandomplayItemClick = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position < RandomPlayType.values().length) {
                getService().randomPlay(RandomPlayType.values()[position].toString());
                NowPlayingActivity.show(RandomplayActivity.this);
            }
        }
    };

    static void show(Context context) {
        final Intent intent = new Intent(context, RandomplayActivity.class);
        context.startActivity(intent);
    }

    public enum RandomPlayType {
        tracks,
        albums,
        contributors,
        year
    }

}
