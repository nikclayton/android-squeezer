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

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.itemlists.IServiceGenreListCallback;
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.itemlists.SqueezerAlbumListActivity;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerSearchActivity extends SqueezerItemListActivity {
	private TextView loadingLabel;
	private ExpandableListView resultsExpandableListView;
	private SqueezerSearchAdapter searchResultsAdapter;
	private String searchString;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_layout);

		ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
		loadingLabel = (TextView) findViewById(R.id.loading_label);
		final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

		searchResultsAdapter = new SqueezerSearchAdapter(this);
		resultsExpandableListView = (ExpandableListView) findViewById(R.id.search_expandable_list);
		resultsExpandableListView.setAdapter( searchResultsAdapter );

		searchCriteriaText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					doSearch(searchCriteriaText.getText().toString());
					return true;
				}
				return false;
			}
		});

        searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
            	if (getService() != null) {
            		doSearch(searchCriteriaText.getText().toString());
            	}
			}
		});

        resultsExpandableListView.setOnChildClickListener( new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
	    		SqueezerItem item = searchResultsAdapter.getChild(groupPosition, childPosition);
				if (item != null && item.getId() != null) {
					try {
						if (item instanceof SqueezerAlbum) {
							play(item);
							SqueezerActivity.show(SqueezerSearchActivity.this);
						} else
							SqueezerAlbumListActivity.show(SqueezerSearchActivity.this, item);
					} catch (RemoteException e) {
						Log.e(getTag(), "Error from default action for search result '" + item
								+ "': " + e);
					}
				}
				return true;
			}
		});

        resultsExpandableListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) menuInfo;
				long packedPosition = contextMenuInfo.packedPosition;
				if (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
					int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
					int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
					searchResultsAdapter.setupContextMenu(menu, groupPosition, childPosition);
				}
			}
		});

        resultsExpandableListView.setOnScrollListener(this);
	};

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerArtistListCallback(artistsCallback);
		getService().registerAlbumListCallback(albumsCallback);
		getService().registerGenreListCallback(genresCallback);
		getService().registerSongListCallback(songsCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterArtistListCallback(artistsCallback);
		getService().unregisterAlbumListCallback(albumsCallback);
		getService().unregisterGenreListCallback(genresCallback);
		getService().unregisterSongListCallback(songsCallback);
	}

	@Override
	public final boolean onContextItemSelected(MenuItem menuItem) {
		if (getService() != null) {
			ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) menuItem.getMenuInfo();
			long packedPosition = contextMenuInfo.packedPosition;
			int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
			int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
			if (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				try {
					searchResultsAdapter.doItemContext(menuItem, groupPosition, childPosition);
				} catch (RemoteException e) {
					SqueezerItem item = searchResultsAdapter.getChild(groupPosition, childPosition);
					Log.e(getTag(), "Error executing context menu action '" + contextMenuInfo + "' for '"	+ item + "': " + e);
				}
			}
		}
		return false;
	}

	@Override
	protected void onServiceConnected() throws RemoteException {
		registerCallback();
		doSearch();
	}

	@Override
    public void onPause() {
        if (getService() != null) {
        	try {
        		unregisterCallback();
			} catch (RemoteException e) {
                Log.e(getTag(), "Error unregistering list callback: " + e);
			}
        }
        super.onPause();
    }

	static void show(Context context) {
		final Intent intent = new Intent(context, SqueezerSearchActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }


	@Override
	protected void orderPage(int start) {
		try {
			getService().search(start, searchString);
		} catch (RemoteException e) {
			Log.e(getTag(), "Error performing search: " + e);
		}
	}

	private void doSearch(String searchString) {
		if (searchString != null && searchString.length() > 0 && getService() != null) {
			this.searchString = searchString;
			reorderItems();
			resultsExpandableListView.setVisibility(View.GONE);
			loadingLabel.setVisibility(View.VISIBLE);
			searchResultsAdapter.clear();
		}
	}

	private void doSearch() {
		doSearch(searchString);
	}

	private <T extends SqueezerItem> void onItemsReceived(final int count, final int start, final List<T> items) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				searchResultsAdapter.updateItems(count, start, items);
				loadingLabel.setVisibility(View.GONE);
				resultsExpandableListView.setVisibility(View.VISIBLE);
			}
		});
	}


    private final IServiceArtistListCallback artistsCallback = new IServiceArtistListCallback.Stub() {
		public void onArtistsReceived(int count, int start, List<SqueezerArtist> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
	};

	private final IServiceAlbumListCallback albumsCallback = new IServiceAlbumListCallback.Stub() {
		public void onAlbumsReceived(int count, int start, List<SqueezerAlbum> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
	};

	private final IServiceGenreListCallback genresCallback = new IServiceGenreListCallback.Stub() {
		public void onGenresReceived(int count, int start, List<SqueezerGenre> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
	};

	private final IServiceSongListCallback songsCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
	};

}
