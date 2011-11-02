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

package com.danga.squeezer.framework;


import android.app.Activity;
import android.content.res.Resources;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.Adapter;


/**
 * Defines view logic for a {@link SqueezerItem}
 *
 * @author Kurt Aaholst
 *
 * @param <T>
 *            Denotes the class of the item this class implements view logic for
 */
public interface SqueezerItemView<T extends SqueezerItem> {

	/**
	 * @return The activity associated with this view logic
	 */
	SqueezerItemListActivity getActivity();

	/**
	 * @return {@link Resources#getQuantityString(int, int)}
	 */
	String getQuantityString(int quantity);

	/**
	 * <p>Called by {@link Adapter#getView(int, View, ViewGroup)}
	 *
	 * @param convertView
	 * @param item
	 * @return
	 */
	View getAdapterView(View convertView, T item);

	/**
	 * <p>Called by {@link Adapter#getView(int, View, ViewGroup)}
	 *
	 * @param convertView
	 * @param item
	 * @return
	 */
	View getAdapterView(View convertView, String label);

	/**
	 * @return The generic argument of the implementation
	 */
    Class<T> getItemClass();

    /**
     * @return the creator for the current {@link SqueezerItem} implementation
     */
	Creator<T> getCreator();

	/**
	 * Set the adapter which uses this view logic
	 * @param adapter
	 */
	void setAdapter(SqueezerItemAdapter<T> adapter);

	/**
	 * Implement the action to be taken when an item is selected.
	 * @param index Position in the list of the selected item.
	 * @param item The selected item. This may be null if
	 * @throws RemoteException
	 */
	void onItemSelected(int index, T item) throws RemoteException;

	/**
	 * <p>Setup the context menu for the current {@link SqueezerItem} implementation
	 * <p>Leave this empty if there shall be no context menu.
	 * @see OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, android.view.ContextMenu.ContextMenuInfo)
	 */
	void setupContextMenu(ContextMenu menu, int index, T item);

	/**
	 * Perform the selected action from the context menu for the selected item.
	 *
	 * @param selectedItem The item the context menu was generated for
	 * @param menuItem The selected menu action
	 * @return True if the action was consumed
	 * @throws RemoteException
	 * @see {@link Activity#onContextItemSelected(MenuItem)}
	 */
	public boolean doItemContext(MenuItem menuItem, int index, T selectedItems) throws RemoteException;

}
