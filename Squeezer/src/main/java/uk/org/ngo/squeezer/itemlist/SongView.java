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


import android.view.ContextMenu;
import android.view.View;

import java.util.EnumSet;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItemView;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.util.ImageFetcher;

import static android.text.format.DateUtils.formatElapsedTime;

/**
 * A view that shows a single song with its artwork, and a context menu.
 */
public class SongView extends PlaylistItemView<Song> {

    /**
     * Which details to show in the second line of text.
     */
    public enum Details {
        /**
         * Show the artist name.  Mutually exclusive with ARTIST_IF_COMPILATION.
         */
        ARTIST,

        /**
         * Show the artist name only if the song is part of a compilation.  Mutually exclusive with
         * ARTIST.
         */
        ARTIST_IF_COMPILATION,

        /**
         * Show the album name.
         */
        ALBUM,

        /**
         * Show the year (if known).
         */
        YEAR,

        /**
         * Show the genre (if known).
         */
        GENRE,

        /**
         * Track number.
         */
        TRACK_NO,

        /**
         * Duration.
         */
        DURATION
    }

    private EnumSet<Details> mDetails = EnumSet.noneOf(Details.class);

    private boolean browseByAlbum;

    public void setBrowseByAlbum(boolean browseByAlbum) {
        this.browseByAlbum = browseByAlbum;
    }

    private boolean browseByArtist;

    public void setBrowseByArtist(boolean browseByArtist) {
        this.browseByArtist = browseByArtist;
    }

    public SongView(ItemListActivity activity) {
        super(activity);

        setViewParams(EnumSet.of(ViewParams.TWO_LINE, ViewParams.CONTEXT_BUTTON));
    }

    public void setDetails(EnumSet<Details> details) {
        if (details.contains(Details.ARTIST) && details.contains(Details.ARTIST_IF_COMPILATION)) {
            throw new IllegalArgumentException(
                    "ARTIST and ARTIST_IF_COMPILATION are mutually exclusive");
        }
        mDetails = details;
    }

    @Override
    public void bindView(View view, Song item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        viewHolder.text2.setText(mJoiner.join(
                mDetails.contains(Details.TRACK_NO) ? item.getTrackNum() : null,
                mDetails.contains(Details.DURATION) ? formatElapsedTime(item.getDuration()) : null,
                mDetails.contains(Details.ARTIST) ? item.getArtist() : null,
                mDetails.contains(Details.ARTIST_IF_COMPILATION) && item.getCompilation() ? item
                        .getArtist() : null,
                mDetails.contains(Details.ALBUM) ? item.getAlbumName() : null,
                mDetails.contains(Details.YEAR) ? item.getYear() : null
        ));
    }

    /**
     * Binds the label to {@link ViewHolder#text1}. Hides the {@link ViewHolder#btnContextMenu} and
     * clears {@link ViewHolder#text2}.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param label The text to bind to {@link ViewHolder#text1}
     */
    @Override
    public void bindView(View view, String label) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(label);
        viewHolder.text2.setText("");
    }

    /**
     * Returns the URL to download the specified album artwork, or null if the artwork does not
     * exist, or there was a problem with the service.
     *
     * @param artwork_track_id
     *
     * @return
     */
    protected String getAlbumArtUrl(String artwork_track_id) {
        if (artwork_track_id == null) {
            return null;
        }

        ISqueezeService service = getActivity().getService();
        if (service == null) {
            return null;
        }
        return service.getAlbumArtUrl(artwork_track_id);
    }

    @Override
    protected PlayableItemAction getOnSelectAction() {
        String actionType = preferences.getString(Preferences.KEY_ON_SELECT_SONG_ACTION,
                PlayableItemAction.Type.NONE.name());
        return PlayableItemAction.createAction(getActivity(), actionType);
    }

    /**
     * Creates the context menu for a song by inflating R.menu.songcontextmenu.
     * <p/>
     * Subclasses that show songs in playlists should call through to this first, then adjust the
     * visibility of R.id.group_playlist.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menuInfo.menuInflater.inflate(R.menu.songcontextmenu, menu);

        if (((Song) menuInfo.item).getAlbumId().equals("") && !browseByAlbum) {
            menu.findItem(R.id.view_this_album).setVisible(true);
        }

        if (((Song) menuInfo.item).getArtistId().equals("")) {
            menu.findItem(R.id.view_albums_by_song).setVisible(true);
        }

        if (((Song) menuInfo.item).getArtistId().equals("") && !browseByArtist) {
            menu.findItem(R.id.view_songs_by_artist).setVisible(true);
        }
    }

    @Override
    public boolean doItemContext(android.view.MenuItem menuItem, int index, Song selectedItem) {
        switch (menuItem.getItemId()) {
            case R.id.view_this_album:
                SongListActivity.show(getActivity(), selectedItem.getAlbum());
                return true;

            // XXX: Is this actually "view albums by artist"?
            case R.id.view_albums_by_song:
                AlbumListActivity.show(getActivity(),
                        new Artist(selectedItem.getArtistId(), selectedItem.getArtist()));
                return true;

            case R.id.view_songs_by_artist:
                SongListActivity.show(getActivity(),
                        new Artist(selectedItem.getArtistId(), selectedItem.getArtist()));
                return true;
        }

        return super.doItemContext(menuItem, index, selectedItem);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.song, quantity);
    }
}
