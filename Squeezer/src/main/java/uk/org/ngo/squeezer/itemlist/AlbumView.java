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
import android.view.MenuItem;
import android.view.View;

import java.util.EnumSet;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.util.ImageFetcher;

/**
 * Shows a single album with its artwork, and a context menu.
 */
public class AlbumView extends AlbumArtView<Album> {

    /**
     * The details to show in the second line of text.
     */
    public enum Details {
        /**
         * Show the artist name.
         */
        ARTIST,

        /**
         * Show the year (if known).
         */
        YEAR,

        /**
         * Show the genre (if known).
         */
        GENRE
    }

    private EnumSet<Details> mDetails = EnumSet.noneOf(Details.class);

    public AlbumView(ItemListActivity activity) {
        super(activity);
    }

    public void setDetails(EnumSet<Details> details) {
        mDetails = details;
    }

    Artist mArtist;

    public void setArtist(Artist artist) {
        mArtist = artist;
    }

    @Override
    public void bindView(View view, Album item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        String text2 = "";
        if (item.getId() != null) {
            text2 = mJoiner.join(
                    mDetails.contains(Details.ARTIST) ? item.getArtist() : null,
                    mDetails.contains(Details.YEAR) && item.getYear() != 0 ? item.getYear() : null
            );
        }
        viewHolder.text2.setText(text2);

        String artworkUrl = getAlbumArtUrl(item.getArtwork_track_id());
        if (artworkUrl == null) {
            viewHolder.icon.setImageResource(R.drawable.icon_album_noart);
        } else {
            imageFetcher.loadImage(artworkUrl, viewHolder.icon);
        }
    }

    @Override
    protected PlayableItemAction getOnSelectAction() {
        String actionType = preferences.getString(Preferences.KEY_ON_SELECT_ALBUM_ACTION,
                PlayableItemAction.Type.BROWSE.name());
        return PlayableItemAction.createAction(getActivity(), actionType);
    }

    /**
     * Creates the context menu for an album by inflating R.menu.albumcontextmenu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menuInfo.menuInflater.inflate(R.menu.albumcontextmenu, menu);
        if (mArtist != null) {
            MenuItem item = menu.findItem(R.id.browse_songs_by_artist);
            item.setVisible(true);
            item.setTitle(getActivity().getString(R.string.BROWSE_SONGS_BY_ITEM, mArtist.getName()));
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, Album selectedItem) {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs_by_artist:
                SongListActivity.show(getActivity(), selectedItem, mArtist);
                return true;
        }
        return super.doItemContext(menuItem, index, selectedItem);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.album, quantity);
    }
}
