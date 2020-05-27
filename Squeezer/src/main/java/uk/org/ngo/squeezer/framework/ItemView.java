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

package uk.org.ngo.squeezer.framework;

import android.os.Parcelable.Creator;
import android.view.View;
import android.view.ViewGroup;


/**
 * Defines view logic for a {@link Item}
 * <p>
 * We keep this here because we don't want to pollute the model with view related stuff.
 * <p>
 * Currently this is the only logic class you have to implement for each SqueezeServer data type, so
 * it contains a few methods, which are not strictly view related.
 * <p>
 * {@link BaseItemView} implements all the common functionality, an some sensible defaults.
 *
 * @param <T> Denotes the class of the item this class implements view logic for
 *
 * @author Kurt Aaholst
 */
public interface ItemView<T extends Item> {

    /**
     * @return The activity associated with this view logic
     */
    ItemListActivity getActivity();

    /**
     * Gets a {@link android.view.View} that displays the data at the specified position in the data
     * set. See {@link ItemAdapter#getView(int, View, android.view.ViewGroup)}
     *
     * @param convertView the old view to reuse, per {@link android.widget.Adapter#getView(int, View,
     * android.view.ViewGroup)}
     * @param position Position of item in adapter
     * @param item the item to display.
     *
     * @return the view to display.
     */
    View getAdapterView(View convertView, ViewGroup parent, int position, T item);

    /**
     * Gets a {@link android.view.View} suitable for displaying the supplied (static) text. See
     * {@link ItemAdapter#getView(int, View, android.view.ViewGroup)}
     *
     * @param convertView The old view to reuse, per {@link android.widget.Adapter#getView(int,
     * View, android.view.ViewGroup)}
     * @param text text to display
     *
     * @return the view to display.
     */
    View getAdapterView(View convertView, ViewGroup parent, String text);

    /**
     * @return The generic argument of the implementation
     */
    Class<T> getItemClass();

    /**
     * @return the creator for the current {@link Item} implementation
     */
    Creator<T> getCreator();

    /**
     * Return whether the supplied item shall be selectable in a list
     *
     * @param item Item to check
     * @return True if the item is selectable
     * @see android.widget.ListAdapter#isEnabled(int)
     */
    boolean isSelectable(T item);

    /**
     * Implement the action to be taken when an item is selected.
     *
     * @param view The view currently showing the item.
     * @param index Position in the list of the selected item.
     * @param item The selected item. This may be null if
     */
    void onItemSelected(View view, int index, T item);
    
    /**
     * Implement the action to be taken when an item is selected.
     *
     * @param view The view currently showing the item.
     */
    void onGroupSelected(View view, T[] items);

    /**
     * Creates the context menu.
     * <p>
     * The default implementation is empty.
     * <p>
     * Subclasses with a context menu should override this method, create a
     * {@link android.widget.PopupMenu} or a {@link android.app.Dialog} then
     * inflate their context menu and show it.
     *
     */
    void showContextMenu(BaseItemView.ViewHolder v, T item);
}
