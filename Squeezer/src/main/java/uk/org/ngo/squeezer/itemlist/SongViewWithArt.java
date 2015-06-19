/*
 * Copyright (c) 2011 Google Inc.  All Rights Reserved.
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


import android.view.View;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.util.ImageFetcher;

/**
 * A view that shows a single song with its artwork, and a context menu.
 */
public class SongViewWithArt extends SongView {

    @SuppressWarnings("unused")
    private static final String TAG = "SongView";

    public SongViewWithArt(ItemListActivity activity) {
        super(activity);

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE);
    }

    @Override
    public void bindView(View view, Song item) {
        super.bindView(view, item);

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String artworkUrl = item.getArtworkUrl();
        if ("".equals(artworkUrl)) {
            viewHolder.icon.setImageResource(
                    item.isRemote() ? R.drawable.icon_iradio_noart : R.drawable.icon_album_noart);
        } else {
            ImageFetcher.getInstance(getActivity()).loadImage(artworkUrl, viewHolder.icon,
                    mIconWidth, mIconHeight);
        }
    }

    /**
     * Binds the label to {@link ViewHolder#text1}. Sets {@link ViewHolder#icon} to the generic
     * pending icon, and clears {@link ViewHolder#text2}.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param label The text to bind to {@link ViewHolder#text1}
     */
    @Override
    public void bindView(View view, String label) {
        super.bindView(view, label);

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.icon.setImageResource(R.drawable.icon_pending_artwork);
    }
}
