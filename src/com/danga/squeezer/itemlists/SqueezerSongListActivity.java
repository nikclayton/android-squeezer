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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import com.danga.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import com.danga.squeezer.itemlists.dialogs.SqueezerSongFilterDialog;
import com.danga.squeezer.itemlists.dialogs.SqueezerSongOrderDialog;
import com.danga.squeezer.itemlists.dialogs.SqueezerSongOrderDialog.SongsSortOrder;
import com.danga.squeezer.menu.SqueezerFilterMenuItemFragment;
import com.danga.squeezer.menu.SqueezerOrderMenuItemFragment;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerSongListActivity extends SqueezerAbstractSongListActivity 
        implements GenreSpinnerCallback, YearSpinnerCallback,
        SqueezerFilterMenuItemFragment.SqueezerFilterableListActivity,
        SqueezerOrderMenuItemFragment.SqueezerOrderableListActivity {
    
    private SongsSortOrder sortOrder = SongsSortOrder.title;

    private String searchString;
    public String getSearchString() { return searchString; }
    public void setSearchString(String searchString) { this.searchString = searchString; }

    private SqueezerAlbum album;
    public SqueezerAlbum getAlbum() { return album; }
    public void setAlbum(SqueezerAlbum album) { this.album = album; }

    private SqueezerArtist artist;
    public SqueezerArtist getArtist() { return artist; }
    public void setArtist(SqueezerArtist artist) { this.artist = artist; }
	
	private SqueezerYear year;
    public SqueezerYear getYear() { return year; }
    public void setYear(SqueezerYear year) { this.year = year; }
	
	private SqueezerGenre genre;
    public SqueezerGenre getGenre() { return genre; }
    public void setGenre(SqueezerGenre genre) { this.genre = genre; }

	private GenreSpinner genreSpinner;
	public void setGenreSpinner(Spinner spinner) {
	    genreSpinner = new GenreSpinner(this, this, spinner);
	}
	
	private YearSpinner yearSpinner;
	public void setYearSpinner(Spinner spinner) {
	    yearSpinner = new YearSpinner(this, this, spinner);
	}
	
	private SqueezerSongView songViewLogic;

	
    public static void show(Context context, SqueezerItem... items) {
	    final Intent intent = new Intent(context, SqueezerSongListActivity.class);
        for (SqueezerItem item: items)
        	intent.putExtra(item.getClass().getName(), item);
	    context.startActivity(intent);
	}


	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		songViewLogic = new SqueezerSongView(this);
		return songViewLogic;
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerAlbum.class.getName().equals(key)) {
					album = extras.getParcelable(key);
					sortOrder = SongsSortOrder.tracknum;
				} else if (SqueezerArtist.class.getName().equals(key)) {
					artist = extras.getParcelable(key);
				} else if (SqueezerYear.class.getName().equals(key)) {
					year = extras.getParcelable(key);
				} else if (SqueezerGenre.class.getName().equals(key)) {
					genre = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
		songViewLogic.setBrowseByAlbum(album != null);
		songViewLogic.setBrowseByArtist(artist != null);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.registerCallback();
		if (yearSpinner != null) yearSpinner.registerCallback();
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		super.registerCallback();
		if (genreSpinner != null) genreSpinner.unregisterCallback();
		if (yearSpinner != null) yearSpinner.unregisterCallback();
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().songs(start, sortOrder.name(), getSearchString(), album, artist, getYear(), genre);
	}

	public SongsSortOrder getSortOrder() {
	    return sortOrder;
	}
	
	public void setSortOrder(SongsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}

	@Override 
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        SqueezerFilterMenuItemFragment.addTo(this);
        SqueezerOrderMenuItemFragment.addTo(this);
	};

	@Override
	public boolean onSearchRequested() {
		showFilterDialog();
		return false;
	}

    public void showFilterDialog() {
        SqueezerSongFilterDialog.addTo(this);
    }

    public void showOrderDialog() {
        SqueezerSongOrderDialog.addTo(this);
    }

}