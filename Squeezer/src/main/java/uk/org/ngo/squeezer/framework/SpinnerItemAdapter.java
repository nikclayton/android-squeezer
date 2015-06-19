/*
 * Copyright (c) 2015 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.framework;

import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import uk.org.ngo.squeezer.util.ImageFetcher;

/**
 * An item adapater suitable for listing items in a spinner. ItemViews that are used with this
 * adapter should implement {@link SpinnerItemView#getDropDownAdapterView(View, ViewGroup, int, Item, ImageFetcher)}
 * (or inherit from {@link SpinnerItemView} to get the default behaviour) in order to provide a
 * view for the dropdown.
 *
 * @param <T> The class of the items to show in the spinner.
 */
public class SpinnerItemAdapter<T extends Item> extends ItemAdapter<T> implements SpinnerAdapter {
    private SpinnerItemView<T> mItemView;

    public SpinnerItemAdapter(SpinnerItemView itemView, boolean emptyItem) {
        super(itemView, emptyItem);
        mItemView = itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        if (item != null) {
            // XXX: This is ugly -- not all adapters need an ImageFetcher.
            // We should really have subclasses of types in the model classes,
            // with the hierarchy probably being:
            //
            // [basic item] -> [item with artwork] -> [artwork is downloaded]
            //
            // instead of special-casing whether or not mImageFetcher is null
            // in getAdapterView().
            return mItemView.getDropDownAdapterView(convertView, parent, position, item);
        }

        return mItemView.getDropDownAdapterView(convertView, parent,
                (position == 0 && mEmptyItem ? "" : loadingText));
    }
}
