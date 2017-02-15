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
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class PluginItemListActivity extends BaseListActivity<PluginItem>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener {

    private Plugin plugin;

    private PluginItem parent;

    private String search;

    @Override
    public ItemView<PluginItem> createItemView() {
        return new PluginItemView(this);
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
                public void onClick(View v) {
                    if (getService() != null) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                    }
                }
            });
        }
    }

    private void updateHeader(String headerText) {
        TextView header = (TextView) findViewById(R.id.header);
        header.setText(headerText);
        header.setVisibility(View.VISIBLE);
    }

    public Plugin getPlugin() {
        return plugin;
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
    protected void orderPage(int start) {
        getService().pluginItems(start, plugin, parent, search, this);
    }


    public void show(PluginItem pluginItem) {
        final Intent intent = new Intent(this, PluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        intent.putExtra(pluginItem.getClass().getName(), pluginItem);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void show(Activity activity, Plugin plugin) {
        final Intent intent = new Intent(activity, PluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, String> parameters, List<PluginItem> items, Class<PluginItem> dataType) {
        if (parameters.containsKey("title")) {
            runOnUiThread(new Runnable() {
                public void run() {
                    updateHeader(parameters.get("title"));
                }
            });
        }

        // The documentation says "Returned with value 1 if there was a network error accessing
        // the content source.". In practice (with at least the Napster and Pandora plugins) the
        // value is an error message suitable for displaying to the user.
        if (parameters.containsKey("networkerror")) {
            Resources resources = getResources();
            String playerName = getService().getActivePlayer().getName();
            String errorMsg = parameters.get("networkerror");

            String errorMessage = String.format(resources.getString(R.string.server_error),
                    playerName, errorMsg);
            NetworkErrorDialogFragment networkErrorDialogFragment =
                    NetworkErrorDialogFragment.newInstance(errorMessage);
            networkErrorDialogFragment.show(getSupportFragmentManager(), "networkerror");
        }

        // Automatically fetch subitems, if this is the only item.
        // TODO: Seen an NPE here (before adding size() > 0) check. Find out
        // why count == 1 might be true, but items.size might be 0.
        if (count == 1 && items.size() > 0 && items.get(0).isHasitems()) {
            parent = items.get(0);
            getUIThreadHandler().post(new Runnable() {
                public void run() {
                    clearAndReOrderItems();
                }
            });
            return;
        }
        super.onItemsReceived(count, start, parameters, items, dataType);
    }

    /**
     * The user dismissed the network error dialog box. There's nothing more to do, so finish
     * the activity.
     */
    public void onDialogDismissed(DialogInterface dialog) {
        runOnUiThread(new Runnable() {
            public void run() {
                finish();
            }
        });
    }

    // Shortcuts for operations for plugin items

    public boolean play(PluginItem item) {
        return pluginPlaylistControl(PluginPlaylistControlCmd.play, item);
    }

    public boolean load(PluginItem item) {
        return pluginPlaylistControl(PluginPlaylistControlCmd.load, item);
    }

    public boolean insert(PluginItem item) {
        return pluginPlaylistControl(PluginPlaylistControlCmd.insert, item);
    }

    public boolean add(PluginItem item) {
        return pluginPlaylistControl(PluginPlaylistControlCmd.add, item);
    }

    private boolean pluginPlaylistControl(PluginPlaylistControlCmd cmd, PluginItem item) {
        if (getService() == null) {
            return false;
        }
        getService().pluginPlaylistControl(plugin, cmd.name(), item.getId());
        return true;
    }

    private enum PluginPlaylistControlCmd {
        play,
        load,
        add,
        insert
    }

}
