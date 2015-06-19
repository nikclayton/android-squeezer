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


import android.support.annotation.IntDef;
import android.view.ContextMenu;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItemView;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ISqueezeService;

import static android.text.format.DateUtils.formatElapsedTime;

/**
 * A view that shows a single song with its artwork, and a context menu.
 */
public class SongView extends PlaylistItemView<Song> {


    /**
     * Which details to show in the second line of text.
     */
    @IntDef(flag=true, value={
            DETAILS_NONE, DETAILS_ARTIST, DETAILS_ARTIST_IF_COMPILATION, DETAILS_ALBUM,
            DETAILS_YEAR, DETAILS_GENRE, DETAILS_TRACK_NO, DETAILS_DURATION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SecondLineDetails {}
    public static final int DETAILS_NONE = 0;
    /** Show the artist name.  Mutually exclusive with ARTIST_IF_COMPILATION. */
    public static final int DETAILS_ARTIST = 1;
    /**
     * Show the artist name only if the song is part of a compilation.  Mutually exclusive with
     * ARTIST.
     */
    public static final int DETAILS_ARTIST_IF_COMPILATION = 1 << 1;
    /** Show the album name. */
    public static final int DETAILS_ALBUM = 1 << 2;
    /** Show the year (if known). */
    public static final int DETAILS_YEAR = 1 << 3;
    /** Show the genre (if known). */
    public static final int DETAILS_GENRE = 1 << 4;
    /** Show the track number. */
    public static final int DETAILS_TRACK_NO = 1 << 5;
    /** Show the duration. */
    public static final int DETAILS_DURATION = 1 << 6;

    @SecondLineDetails private int mDetails = DETAILS_NONE;

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

        setViewParams(VIEW_PARAM_TWO_LINE | VIEW_PARAM_CONTEXT_BUTTON);
    }

    public void setDetails(@SecondLineDetails int details) {
        if ((details & DETAILS_ARTIST) != 0 && (details & DETAILS_ARTIST_IF_COMPILATION) != 0) {
            throw new IllegalArgumentException(
                    "ARTIST and ARTIST_IF_COMPILATION are mutually exclusive");
        }
        mDetails = details;
    }

    @Override
    public void bindView(View view, Song item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        viewHolder.text2.setText(mJoiner.join(
                (mDetails & DETAILS_TRACK_NO) > 0 ? item.getTrackNum() : null,
                (mDetails & DETAILS_DURATION) > 0 ? formatElapsedTime(item.getDuration()) : null,
                (mDetails & DETAILS_ARTIST) > 0 ? item.getArtist() : null,
                (mDetails & DETAILS_ARTIST_IF_COMPILATION) > 0 && item.getCompilation() ? item
                        .getArtist() : null,
                (mDetails & DETAILS_ALBUM) > 0 ? item.getAlbumName() : null,
                (mDetails & DETAILS_YEAR) > 0 ? item.getYear() : null
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

        Song song = (Song) menuInfo.item;

        if (!"".equals(song.getAlbumId()) && !browseByAlbum) {
            menu.findItem(R.id.view_this_album).setVisible(true);
        }

        if (!"".equals(song.getArtistId())) {
            menu.findItem(R.id.view_albums_by_song).setVisible(true);
        }

        if (!"".equals(song.getArtistId()) && !browseByArtist) {
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
