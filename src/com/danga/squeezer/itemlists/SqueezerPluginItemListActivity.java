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

package com.danga.squeezer.itemlists;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerBaseListActivity;
import com.danga.squeezer.framework.SqueezerItemAdapter;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.model.SqueezerPlugin;
import com.danga.squeezer.model.SqueezerPluginItem;

public class SqueezerPluginItemListActivity extends SqueezerBaseListActivity<SqueezerPluginItem>{
	private SqueezerPlugin plugin;
	private SqueezerPluginItem parent;
	private String search;

	@Override
	public SqueezerItemView<SqueezerPluginItem> createItemView() {
		return new SqueezerPluginItemView(this);
	}

	@Override
	protected SqueezerItemAdapter<SqueezerPluginItem> createItemListAdapter(SqueezerItemView<SqueezerPluginItem> itemView) {
		return new SqueezerItemAdapter<SqueezerPluginItem>(itemView);
	};

	@Override
	public void prepareActivity(Bundle extras) {
		plugin = extras.getParcelable(SqueezerPlugin.class.getName());
		parent = extras.getParcelable(SqueezerPluginItem.class.getName());
		findViewById(R.id.search_view).setVisibility(plugin.isSearchable() ? View.VISIBLE : View.GONE);

		ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
		final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

		searchCriteriaText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					orderItems(searchCriteriaText.getText().toString());
					return true;
				}
				return false;
			}
		});

        searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
            	if (getService() != null) {
					orderItems(searchCriteriaText.getText().toString());
            	}
			}
		});

	}

	private void orderItems(String searchString) {
		if (getService() != null && !(plugin.isSearchable() && (searchString == null || searchString.length() == 0))) {
			search = searchString;
			super.orderItems();
		}

	}

	@Override
	public void orderItems() {
		orderItems(search);
	};

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPluginItemListCallback(pluginItemListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPluginItemListCallback(pluginItemListCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().pluginItems(start, plugin, parent, search);
	}


	public void show(SqueezerPluginItem pluginItem) {
        final Intent intent = new Intent(this, SqueezerPluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        intent.putExtra(pluginItem.getClass().getName(), pluginItem);
        startActivity(intent);
    }

	public static void show(Context context, SqueezerPlugin plugin) {
        final Intent intent = new Intent(context, SqueezerPluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        context.startActivity(intent);
    }

    private final IServicePluginItemListCallback pluginItemListCallback = new IServicePluginItemListCallback.Stub() {
		public void onPluginItemsReceived(int count, int start, @SuppressWarnings("rawtypes") final Map parameters, List<SqueezerPluginItem> items) throws RemoteException {
			if (parameters.containsKey("title")) {
				getUIThreadHandler().post(new Runnable() {
					public void run() {
						setTitle((String) parameters.get("title"));
					}
				});
			}
			if (count == 1 && items.get(0).isHasitems()) {
				// Automatically fetch subitems, if this is the only item
				SqueezerPluginItemListActivity.this.parent = items.get(0);
				getUIThreadHandler().post(new Runnable() {
					public void run() {
						orderItems();
					}
				});
				return;
			}
			onItemsReceived(count, start, items);
		}
    };


    // Shortcuts for operations for plugin items

    public boolean play(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.play, item);
    }

    public boolean load(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.load, item);
    }

    public boolean insert(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.insert, item);
    }

    public boolean add(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.add, item);
    }

    private boolean pluginPlaylistControl(PluginPlaylistControlCmd cmd, SqueezerPluginItem item) throws RemoteException {
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
    	insert;
    }

}
