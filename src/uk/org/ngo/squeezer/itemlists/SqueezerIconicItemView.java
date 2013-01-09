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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;

/**
 * Represents the view hierarchy for a single {@link SqueezerItem} subclass,
 * where the item has an icon associated with it.
 * 
 * @param <T>
 */
public abstract class SqueezerIconicItemView<T extends SqueezerItem> extends SqueezerBaseItemView<T> {
    /** The icon to display when there's no artwork for this item */
    protected static final int ICON_NO_ARTWORK = R.drawable.icon_album_noart;

    /** The icon to display when artwork exists, but has not been loaded yet */
    protected static final int ICON_PENDING_ARTWORK = R.drawable.icon_album_noart;

    public SqueezerIconicItemView(SqueezerItemListActivity activity) {
        super(activity);
    }
}