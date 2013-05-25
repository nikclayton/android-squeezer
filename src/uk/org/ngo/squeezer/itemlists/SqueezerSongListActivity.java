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

package uk.org.ngo.squeezer.itemlists;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.YearSpinner.YearSpinnerCallback;
import uk.org.ngo.squeezer.itemlists.actions.PlayableItemAction;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongFilterDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongOrderDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongOrderDialog.SongsSortOrder;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerFilterMenuItemFragment;
import uk.org.ngo.squeezer.menu.SqueezerOrderMenuItemFragment;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Spinner;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MenuFragment.add(this, SqueezerFilterMenuItemFragment.class);
        MenuFragment.add(this, SqueezerOrderMenuItemFragment.class);

        Bundle extras = getIntent().getExtras();
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
    protected PlayableItemAction getOnSelectAction() {
    	String actionType = preferences.getString(Preferences.KEY_ON_SELECT_SONG_ACTION, PlayableItemAction.Type.PLAY.name());
    	return PlayableItemAction.createAction(this, actionType);
    }
    
    
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
        getService().songs(start, sortOrder.name(), getSearchString(), album, artist, getYear(),
                genre);
    }

    public SongsSortOrder getSortOrder() {
        return sortOrder;
    }

	public void setSortOrder(SongsSortOrder sortOrder) {
		this.sortOrder = sortOrder;
		orderItems();
	}

    public void showFilterDialog() {
        new SqueezerSongFilterDialog().show(getSupportFragmentManager(), "SongFilterDialog");
    }

    public void showOrderDialog() {
        new SqueezerSongOrderDialog().show(this.getSupportFragmentManager(), "OrderDialog");
    }
}
