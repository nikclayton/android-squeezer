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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

import com.danga.squeezer.framework.SqueezerFilterableListActivity;
import com.danga.squeezer.framework.SqueezerItem;
import com.danga.squeezer.framework.SqueezerItemView;
import com.danga.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import com.danga.squeezer.itemlists.dialogs.SqueezerArtistFilterDialog;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerGenre;

public class SqueezerArtistListActivity extends SqueezerFilterableListActivity<SqueezerArtist> implements GenreSpinnerCallback{

    private String searchString = null;
    public String getSearchString() { return searchString; }
    public void setSearchString(String searchString) { this.searchString = searchString; }

	private SqueezerAlbum album;
    public SqueezerAlbum getAlbum() { return album; }
    public void setAlbum(SqueezerAlbum album) { this.album = album; }

	SqueezerGenre genre;
    public SqueezerGenre getGenre() { return genre; }
    public void setGenre(SqueezerGenre genre) { this.genre = genre; }

	private GenreSpinner genreSpinner;
	public void setGenreSpinner(Spinner spinner) {
	    genreSpinner = new GenreSpinner(this, this, spinner);
	}

	
    @Override
	public SqueezerItemView<SqueezerArtist> createItemView() {
		return new SqueezerArtistView(this);
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerAlbum.class.getName().equals(key)) {
					album = extras.getParcelable(key);
				} else if (SqueezerGenre.class.getName().equals(key)) {
					genre = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerArtistListCallback(artistsListCallback);
		if (genreSpinner != null) genreSpinner.registerCallback();
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterArtistListCallback(artistsListCallback);
		if (genreSpinner != null) genreSpinner.unregisterCallback();
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().artists(start, getSearchString(), album, genre);
	}

    public void showFilterDialog() {
        SqueezerArtistFilterDialog.addTo(this);
    }

	public static void show(Context context, SqueezerItem... items) {
        final Intent intent = new Intent(context, SqueezerArtistListActivity.class);
        for (SqueezerItem item: items)
        	intent.putExtra(item.getClass().getName(), item);
        context.startActivity(intent);
    }

    private final IServiceArtistListCallback artistsListCallback = new IServiceArtistListCallback.Stub() {
		public void onArtistsReceived(int count, int start, List<SqueezerArtist> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
