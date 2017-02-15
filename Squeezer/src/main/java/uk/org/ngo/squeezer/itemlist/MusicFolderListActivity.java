/*
 * Copyright (c) 2012 Google Inc.
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
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.MusicFolderItem;

/**
 * Display a list of Squeezebox music folders.
 * <p/>
 * If the <code>extras</code> bundle contains a key that matches <code>MusicFolder.class.getName()</code>
 * the value is assumed to be an instance of that class, and that folder will be displayed.
 * <p/>
 * Otherwise the root music folder is shown.
 * <p/>
 * The activity's content views scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 *
 * @author nik
 */
public class MusicFolderListActivity extends BaseListActivity<MusicFolderItem> {

    /**
     * The folder to view. The root folder if null.
     */
    MusicFolderItem mFolder;

    @Override
    public ItemView<MusicFolderItem> createItemView() {
        return new MusicFolderView(this);
    }

    @Override
    protected ItemAdapter<MusicFolderItem> createItemListAdapter(
            ItemView<MusicFolderItem> itemView) {
        return new ItemAdapter<MusicFolderItem>(itemView);
    }

    /**
     * Extract the folder to view (if provided).
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mFolder = extras.getParcelable(MusicFolderItem.class.getName());
            TextView header = (TextView) findViewById(R.id.header);
            header.setText(mFolder.getName());
            header.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mFolder != null) {
            getMenuInflater().inflate(R.menu.playmenu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play_now:
                play(mFolder);
                return true;
            case R.id.add_to_playlist:
                add(mFolder);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fetch the contents of a folder. Fetches the contents of <code>mFolder</code> if non-null, the
     * root folder otherwise.
     *
     * @param start Where in the list of folders to start fetching.
     */
    @Override
    protected void orderPage(int start) {
        getService().musicFolders(start, mFolder, this);
    }

    /**
     * Show this activity, showing the contents of the root folder.
     *
     * @param activity
     */
    public static void show(Activity activity) {
        final Intent intent = new Intent(activity, MusicFolderListActivity.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Show this activity, showing the contents of the given folder.
     *
     * @param activity
     * @param folder The folder whose contents will be shown.
     */
    public static void show(Activity activity, MusicFolderItem folder) {
        final Intent intent = new Intent(activity, MusicFolderListActivity.class);
        intent.putExtra(folder.getClass().getName(), folder);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

}
