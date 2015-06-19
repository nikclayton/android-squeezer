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
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


/**
 * Defines view logic for a {@link Item}
 * <p/>
 * We keep this here because we don't want to pollute the model with view related stuff.
 * <p/>
 * Currently this is the only logic class you have to implement for each SqueezeServer data type, so
 * it contains a few methods, which are not strictly view related.
 * <p/>
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
     * @return {@link android.content.res.Resources#getQuantityString(int, int)}
     */
    String getQuantityString(int quantity);

    /**
     * Gets a {@link android.view.View} that displays the data at the specified position in the data
     * set. See {@link ItemAdapter#getView(int, View, android.view.ViewGroup)}
     *
     * @param convertView the old view to reuse, per {@link android.widget.Adapter#getView(int, View,
     * android.view.ViewGroup)}
     * @param item the item to display.
     *
     * @return the view to display.
     */
    View getAdapterView(View convertView, ViewGroup parent, T item);

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
     * @param index Position in the list of the selected item.
     * @param item The selected item. This may be null if
     */
    void onItemSelected(int index, T item);

    /**
     * Creates the context menu, and sets the menu's title to the name of the item that it is the
     * context menu.
     * <p/>
     * Subclasses with no context menu should override this method and do nothing.
     * <p/>
     * Subclasses with a context menu should call this method, then inflate their context menu and
     * perform any adjustments to it before returning.
     *
     * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View,
     * android.view.ContextMenu.ContextMenuInfo)
     */
    void onCreateContextMenu(ContextMenu menu, View v,
            ItemView.ContextMenuInfo menuInfo);

    /**
     * Perform the selected action from the context menu for the selected item.
     *
     * @param menuItem The selected menu action
     * @param index Position in the list of the selected item.
     * @param selectedItem The item the context menu was generated for
     *
     * @return True if the action was consumed
     *
     * @see android.app.Activity#onContextItemSelected(MenuItem)
     */
    boolean doItemContext(MenuItem menuItem, int index, T selectedItem);

    /**
     * Perform the selected action from the context sub-menu.
     *
     * @param menuItem The selected menu action
     *
     * @return True if the action was consumed
     *
     * @see android.app.Activity#onContextItemSelected(MenuItem)
     */
    boolean doItemContext(MenuItem menuItem);

    /**
     * Extra menu information provided to the {@link android.view.View.OnCreateContextMenuListener#onCreateContextMenu(ContextMenu,
     * View, ContextMenu.ContextMenuInfo) } callback when a context menu is brought up for this ItemView.
     */
    class ContextMenuInfo implements ContextMenu.ContextMenuInfo {

        /**
         * The position in the adapter for which the context menu is being displayed.
         */
        public final int position;

        /**
         * The {@link Item} for which the context menu is being displayed.
         */
        public final Item item;

        /**
         * The {@link ItemAdapter} that is bridging the content to the list view.
         */
        public final ItemAdapter<?> adapter;

        /**
         * A {@link android.view.MenuInflater} that can be used to inflate a menu resource.
         */
        public final MenuInflater menuInflater;

        public ContextMenuInfo(int position, Item item, ItemAdapter<?> adapter,
                MenuInflater menuInflater) {
            this.position = position;
            this.item = item;
            this.adapter = adapter;
            this.menuInflater = menuInflater;
        }
    }
}
