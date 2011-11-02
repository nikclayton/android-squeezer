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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.danga.squeezer.R;


/**
 * A generic class for an adapter to list items of a particular
 * SqueezeServer data type. The data type is defined by the generic type
 * argument, and must be an extension of {@link SqueezerItem}.
 * <p>
 * If you need an adapter for a {@link SqueezerBaseListActivity}, then use
 * {@link SqueezerItemListAdapter} instead.
 *
 * @param <T>
 *            Denotes the class of the items this class should list
 * @see SqueezerItemView
 * @author Kurt Aaholst
 */
public class SqueezerItemAdapter<T extends SqueezerItem> extends BaseAdapter {

	/**
	 * View logic for this adapter
	 */
	private final SqueezerItemView<T> itemView;

	/**
	 * List of items, possibly headed with an empty item.
	 * <p>
	 * As the items are received from SqueezeServer they will be inserted in the
	 * list.
	 */
	private int count;
	private final Map<Integer, T[]> pages = new HashMap<Integer, T[]>();

	/**
	 *  This is set if the list shall start with an empty item.
	 */
	private final boolean emptyItem;

	/**
	 * Text to display before the items are received from SqueezeServer
	 */
	private final String loadingText;

	/*
	 * Number of elements to by fetched at a time
	 */
	private int pageSize;
	public int getPageSize() { return pageSize; }

	/**
	 * Creates a new adapter. Initially the item list is populated with items displaying the
	 * localized "loading" text. Call {@link #update(int, int, int, List)} as items arrives
	 * from SqueezeServer.
	 *
	 * @param itemView The {@link SqueezerItemView} to use with this adapter
	 * @param count The number of items this adapter initially contains.
	 * @param emptyItem If set the list of items shall start with an empty item
	 */
	public SqueezerItemAdapter(SqueezerItemView<T> itemView, boolean emptyItem) {
		this.itemView = itemView;
		itemView.setAdapter(this);
		this.emptyItem = emptyItem;
		loadingText = itemView.getActivity().getString(R.string.loading_text);
		pageSize = itemView.getActivity().getResources().getInteger(R.integer.PageSize);
		pages.clear();
	}

	/**
	 * Calls {@link #SqueezerBaseAdapter(SqueezerItemView, int, boolean)}, with emptyItem = false
	 */
	public SqueezerItemAdapter(SqueezerItemView<T> itemView) {
		this(itemView, false);
	}

	private int pageNumber(int position) {
		return position / pageSize;
	}

	/**
	 * Removes all items from this adapter leaving it empty.
	 */
	public void clear() {
		this.count = (emptyItem ? 1 : 0);
		pages.clear();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		T item = getItem(position);
		if (item != null)
			return itemView.getAdapterView(convertView, item);
		return itemView.getAdapterView(convertView,
				(position == 0 && emptyItem ? "" : loadingText));
	}

	public String getQuantityString(int size) {
		return itemView.getQuantityString(size);
	}

	public SqueezerItemListActivity getActivity() {
		return itemView.getActivity();
	}

	public void setupContextMenu(ContextMenu menu, int position) {
		final T selectedItem = getItem(position);
		if (selectedItem != null && selectedItem.getId() != null) {
			itemView.setupContextMenu(menu, position, selectedItem);
		}
	}

	public boolean doItemContext(MenuItem menuItem, int position) throws RemoteException {
		return itemView.doItemContext(menuItem, position, getItem(position));
	}

	public SqueezerItemView<T> getItemView() {
		return itemView;
	}

	public int getCount() {
		return count;
	}

	private T[] getPage(int position) {
		int pageNumber = pageNumber(position);
		T[] page = pages.get(pageNumber);
		if (page == null) {
			pages.put(pageNumber, page = arrayInstance(pageSize));
		}
		return page;
	}

	private void setItems(int start, List<T> items) {
		T[] page = getPage(start);
		int offset = start % pageSize;
		Iterator<T> it = items.iterator();
		while (it.hasNext()) {
			if (offset >= pageSize) {
				start += offset;
				page = getPage(start);
				offset = 0;
			}
			page[offset++] = it.next();
		}
	}

	public T getItem(int position) {
		T item = getPage(position)[position % pageSize];
		if (item == null) {
			if (emptyItem) position--;
			getActivity().maybeOrderPage(pageNumber(position) * pageSize);
		}
		return item;
	}

	public long getItemId(int position) {
		return position;
	}

	public String getHeader() {
		String item_text = getQuantityString(getCount());
		String header = getActivity().getString(R.string.browse_items_text, item_text, getCount());
		return header;
	}


	/**
	 * Called when the number of items in the list changes.
	 * The default implementation is empty.
	 */
	protected void updateHeader() {
	}

	/**
	 * Update the contents of the items in this list.
	 * <p>
	 * The size of the list of items is automatically adjusted if necessary, to obey the given
	 * parameters.
	 *
	 * @param count
	 *            Number of items as reported by squeezeserver.
	 * @param start
	 *            The start position of items in this update.
	 * @param items
	 *            New items to insert in the main list
	 */
	public void update(int count, int start, List<T> items) {
		int offset = (emptyItem ? 1 : 0);
		count += offset;
		start += offset;
		if (count == 0 || count != getCount()) {
			this.count = count;
			updateHeader();
		}
		setItems(start, items);

		notifyDataSetChanged();
	}

	/**
	 * @param item
	 * @return The position of the given item in this adapter or 0 if not found
	 */
	public int findItem(T item) {
		for (int pos = 0; pos < getCount(); pos++)
			if (getItem(pos) == null) {
				if (item == null) return pos;
			} else
				if (getItem(pos).equals(item)) return pos;
		return 0;
	}

	protected T[] arrayInstance(int size) {
		return itemView.getCreator().newArray(size);
	}

}