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


import android.view.View;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ArtworkItem;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItemView;
import uk.org.ngo.squeezer.service.ISqueezeService;

/**
 * Represents the view hierarchy for a single {@link uk.org.ngo.squeezer.framework.Item} subclass.
 * where the item has track artwork associated with it.
 *
 * @param <T>
 */
public abstract class AlbumArtView<T extends ArtworkItem> extends
        PlaylistItemView<T> {

    public AlbumArtView(ItemListActivity activity) {
        super(activity);

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON | VIEW_PARAM_TWO_LINE);
    }

    /**
     * Binds the label to {@link ViewHolder#text1}. Sets {@link ViewHolder#icon} to the generic
     * pending icon, and clears {@link ViewHolder#text2}.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param text The text to bind to {@link ViewHolder#text1}
     */
    @Override
    public void bindView(View view, String text) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.icon.setImageResource(R.drawable.icon_pending_artwork);
        viewHolder.text1.setText(text);
        viewHolder.text2.setText("");
    }
}
