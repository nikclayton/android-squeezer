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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.itemlist.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlist.YearSpinner.YearSpinnerCallback;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumFilterDialog;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.menu.BaseMenuFragment;
import uk.org.ngo.squeezer.menu.FilterMenuFragment;
import uk.org.ngo.squeezer.menu.FilterMenuFragment.FilterableListActivity;
import uk.org.ngo.squeezer.menu.ViewMenuItemFragment;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.ISqueezeService;

/**
 * Lists albums, optionally filtered to match specific criteria.
 */
public class AlbumListActivity extends BaseListActivity<Album>
        implements GenreSpinnerCallback, YearSpinnerCallback,
        FilterableListActivity,
        ViewMenuItemFragment.ListActivityWithViewMenu<Album, AlbumViewDialog.AlbumListLayout, AlbumViewDialog.AlbumsSortOrder> {

    private AlbumViewDialog.AlbumsSortOrder sortOrder = null;

    private AlbumViewDialog.AlbumListLayout listLayout = null;

    private String searchString = null;

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private Song song;

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    private Artist artist;

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    private Year year;

    /** Name to use for intent data for the item sort order. */
    private static final String SORT_ORDER_KEY = AlbumViewDialog.AlbumsSortOrder.class.getName();

    /** Name to use for intent data for filtering the view. */
    private static final String FILTER_KEY = AlbumListActivity.class.getName();

    @Override
    public Year getYear() {
        return year;
    }

    @Override
    public void setYear(Year year) {
        this.year = year;
    }

    private Genre genre;

    @Override
    public Genre getGenre() {
        return genre;
    }

    @Override
    public void setGenre(Genre genre) {
        this.genre = genre;
    }


    @Override
    public ItemView<Album> createItemView() {
        return (listLayout == AlbumViewDialog.AlbumListLayout.grid) ? new AlbumGridView(this) : new AlbumView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setListLayout();
        super.onCreate(savedInstanceState);

        BaseMenuFragment.add(this, FilterMenuFragment.class);
        BaseMenuFragment.add(this, ViewMenuItemFragment.class);

        Intent intent = getIntent();
        Parcelable item = intent.getParcelableExtra(FILTER_KEY);
        if (item != null) {
            if (item instanceof Artist) {
                artist = (Artist) item;
            } else if (item instanceof Year) {
                year = (Year) item;
            } else if (item instanceof Genre) {
                genre = (Genre) item;
            } else if (item instanceof Song) {
                song = (Song) item;
            } else {
                Log.e(getTag(), "Unexpected class as filter key: " + item.getClass());
            }
        }

        String sortOrderString = intent.getStringExtra(SORT_ORDER_KEY);
        if (sortOrderString != null) {
            sortOrder = AlbumViewDialog.AlbumsSortOrder.valueOf(sortOrderString);
        }

        TextView header = (TextView) findViewById(R.id.header);
        @AlbumView.SecondLineDetails int details = AlbumView.DETAILS_ALL;
        if (artist != null) {
            details &= ~AlbumView.DETAILS_ARTIST;
            header.setText(getString(R.string.albums_by_artist_header, artist.name()));
            header.setVisibility(View.VISIBLE);
            ((AlbumView) getItemView()).setArtist(artist);
        }
        if (genre != null) {
            details &= ~AlbumView.DETAILS_GENRE;
            header.setText(getString(R.string.albums_by_genre_header, genre.name()));
            header.setVisibility(View.VISIBLE);
        }
        if (year != null) {
            details &= ~AlbumView.DETAILS_YEAR;
            header.setText(getString(R.string.albums_by_year_header, year.name()));
            header.setVisibility(View.VISIBLE);
        }
        ((AlbumView) getItemView()).setDetails(details);
    }

    @Override
    protected int getContentView() {
        return (listLayout == AlbumViewDialog.AlbumListLayout.grid) ? R.layout.item_grid
                : R.layout.item_list_albums;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (sortOrder == null) {
            try {
                sortOrder = AlbumViewDialog.AlbumsSortOrder.valueOf(service.preferredAlbumSort());
            } catch (IllegalArgumentException e) {
                Log.w(getTag(), "Unknown preferred album sort: " + e);
                sortOrder = AlbumViewDialog.AlbumsSortOrder.album;
            }
        }

        service.albums(this, start, sortOrder.name().replace("__", ""), searchString,
                artist, year, genre, song);
    }

    @Override
    public AlbumViewDialog.AlbumsSortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(AlbumViewDialog.AlbumsSortOrder sortOrder) {
        ISqueezeService service = getService();
        if (service == null) {
            return;
        }

        this.sortOrder = sortOrder;
        service.setPreferredAlbumSort(sortOrder.name());
        getIntent().putExtra(AlbumViewDialog.AlbumsSortOrder.class.getName(), sortOrder.name());
        clearAndReOrderItems();
    }

    @Override
    public AlbumViewDialog.AlbumListLayout getListLayout() {
        return listLayout;
    }

    /**
     * Set the preferred album list layout.
     * <p>
     * If the list layout is not selected, a default one is chosen, based on the current screen
     * size, on the assumption that the artwork grid is preferred on larger screens.
     */
    private void setListLayout() {
        listLayout = new Preferences(this).getAlbumListLayout();
    }

    @Override
    public void setListLayout(AlbumViewDialog.AlbumListLayout listLayout) {
        new Preferences(this).setAlbumListLayout(listLayout);
        startActivity(getIntent());
        finish();
    }

    @Override
    public boolean onSearchRequested() {
        showFilterDialog();
        return false;
    }

    @Override
    public void showFilterDialog() {
        new AlbumFilterDialog().show(getSupportFragmentManager(), "AlbumFilterDialog");
    }

    @Override
    public void showViewDialog() {
        new AlbumViewDialog().show(getSupportFragmentManager(), "AlbumOrderDialog");
    }

    /**
     * Start the activity to show all albums, using the default sort order.
     *
     * @param context the context to use to start the activity.
     */
    public static void show(Context context) {
        show(context, null, null);
    }

    /**
     * Start the activity to show all albums, using the given sort order.
     *
     * @param context the context to use to start the activity.
     * @param sortOrder ordering for items.
     */
    public static void show(Context context, AlbumViewDialog.AlbumsSortOrder sortOrder) {
        show(context, sortOrder, null);
    }

    /**
     * Start the activity to show all albums that match the given item.
     *
     * @param context the context to use to start the activity.
     * @param item the item to filter by. If it's an {@link Artist} then albums with songs by
     * by that artist are shown. If it's a {@link Year} then albums from that year are shown. If
     * it's a {@link Genre} then albums with that genre are shown. If it's a {@link Song} then
     * albums that contain that song are shown.
     */
    public static void show(Context context, Item item) {
        show(context, null, item);
    }

    public static void show(Context context, @Nullable AlbumViewDialog.AlbumsSortOrder sortOrder,
                            @Nullable Item item) {
        final Intent intent = new Intent(context, AlbumListActivity.class);

        if (sortOrder != null) {
            intent.putExtra(SORT_ORDER_KEY, sortOrder.name());
        }

        if (item != null) {
            intent.putExtra(FILTER_KEY, item);
        }

        context.startActivity(intent);
    }
}
