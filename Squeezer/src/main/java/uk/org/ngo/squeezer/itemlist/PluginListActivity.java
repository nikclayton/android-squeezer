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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.service.ISqueezeService;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class PluginListActivity extends BaseListActivity<Plugin>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener {

    private Plugin plugin;

    private PluginItem parent;

    private String search;

    @Override
    public ItemView<Plugin> createItemView() {
        return new RadioView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            plugin = extras.getParcelable(Plugin.class.getName());
            parent = extras.getParcelable(PluginItem.class.getName());
            findViewById(R.id.search_view).setVisibility(
                    plugin.isSearchable() ? View.VISIBLE : View.GONE);

            ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
            final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

            searchCriteriaText.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN)
                            && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                        return true;
                    }
                    return false;
                }
            });

            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getService() != null) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                    }
                }
            });
        }
    }

    private void clearAndReOrderItems(String searchString) {
        if (getService() != null && !(plugin.isSearchable() && (searchString == null
                || searchString.length() == 0))) {
            search = searchString;
            super.clearAndReOrderItems();
        }
    }

    @Override
    public void clearAndReOrderItems() {
        clearAndReOrderItems(search);
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.apps(start, this);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    /**
     * The user dismissed the network error dialog box. There's nothing more to do, so finish
     * the activity.
     */
    @Override
    public void onDialogDismissed(DialogInterface dialog) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    // Shortcuts for operations for plugin items

    public boolean play(PluginItem item) {
        return pluginPlaylistControl(PLUGIN_PLAYLIST_PLAY, item);
    }

    public boolean load(PluginItem item) {
        return pluginPlaylistControl(PLUGIN_PLAYLIST_PLAY_NOW, item);
    }

    public boolean insert(PluginItem item) {
        return pluginPlaylistControl(PLUGIN_PLAYLIST_PLAY_AFTER_CURRENT, item);
    }

    public boolean add(PluginItem item) {
        return pluginPlaylistControl(PLUGIN_PLAYLIST_ADD_TO_END, item);
    }

    private boolean pluginPlaylistControl(@PluginPlaylistControlCmd String cmd, PluginItem item) {
        if (getService() == null) {
            return false;
        }
        getService().pluginPlaylistControl(plugin, cmd, item.getId());
        return true;
    }

    @StringDef({PLUGIN_PLAYLIST_PLAY, PLUGIN_PLAYLIST_PLAY_NOW, PLUGIN_PLAYLIST_ADD_TO_END,
            PLUGIN_PLAYLIST_PLAY_AFTER_CURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PluginPlaylistControlCmd {}
    public static final String PLUGIN_PLAYLIST_PLAY = "play";
    public static final String PLUGIN_PLAYLIST_PLAY_NOW = "load";
    public static final String PLUGIN_PLAYLIST_ADD_TO_END = "add";
    public static final String PLUGIN_PLAYLIST_PLAY_AFTER_CURRENT = "insert";
}
