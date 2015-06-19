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

import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;

import java.util.List;

import uk.org.ngo.squeezer.R;


/**
 * A generic class for an adapter to list items of a particular SqueezeServer data type. The data
 * type is defined by the generic type argument, and must be an extension of {@link Item}.
 * <p/>
 * Normally there is no need to extend this, as we delegate all type dependent stuff to
 * {@link ItemView}.
 *
 * @param <T> Denotes the class of the items this class should list
 *
 * @author Kurt Aaholst
 * @see ItemView
 */
public class ItemAdapter<T extends Item> extends BaseAdapter implements
        OnCreateContextMenuListener {

    private static final String TAG = ItemAdapter.class.getSimpleName();

    /**
     * View logic for this adapter
     */
    private ItemView<T> mItemView;

    /**
     * List of items, possibly headed with an empty item.
     * <p/>
     * As the items are received from SqueezeServer they will be inserted in the list.
     */
    private int count;

    private final SparseArray<T[]> pages = new SparseArray<T[]>();

    /**
     * This is set if the list shall start with an empty item.
     */
    protected final boolean mEmptyItem;

    /**
     * Text to display before the items are received from SqueezeServer
     */
    protected final String loadingText;

    /**
     * Number of elements to by fetched at a time
     */
    private final int pageSize;

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Creates a new adapter. Initially the item list is populated with items displaying the
     * localized "loading" text. Call {@link #update(int, int, List)} as items arrives from
     * SqueezeServer.
     *
     * @param itemView The {@link ItemView} to use with this adapter
     * @param emptyItem If set the list of items shall start with an empty item
     */
    public ItemAdapter(ItemView<T> itemView, boolean emptyItem) {
        mItemView = itemView;
        mEmptyItem = emptyItem;
        loadingText = itemView.getActivity().getString(R.string.loading_text);
        pageSize = itemView.getActivity().getResources().getInteger(R.integer.PageSize);
        pages.clear();
    }

    /**
     * Calls {@link #ItemAdapter(ItemView, boolean)}, with emptyItem = false
     */
    public ItemAdapter(ItemView<T> itemView) {
        this(itemView, false);
    }

    private int pageNumber(int position) {
        return position / pageSize;
    }

    /**
     * Removes all items from this adapter leaving it empty.
     */
    public void clear() {
        this.count = (mEmptyItem ? 1 : 0);
        pages.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        if (item != null) {
            return mItemView.getAdapterView(convertView, parent, item);
        }

        return mItemView.getAdapterView(convertView, parent,
                (position == 0 && mEmptyItem ? "" : loadingText));
    }

    public String getQuantityString(int size) {
        return mItemView.getQuantityString(size);
    }

    public ItemListActivity getActivity() {
        return mItemView.getActivity();
    }

    public void onItemSelected(int position) {
        T item = getItem(position);
        if (item != null && item.getId() != null) {
            mItemView.onItemSelected(position, item);
        }
    }

    /**
     * Creates the context menu for the selected item by calling {@link
     * ItemView#onCreateContextMenu} which the subclass should have specialised.
     * <p/>
     * Unpacks the {@link ContextMenu.ContextMenuInfo} passed to this method, and creates a {@link
     * ItemView.ContextMenuInfo} suitable for passing to subclasses of {@link BaseItemView}.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        final T selectedItem = getItem(adapterMenuInfo.position);

        ItemView.ContextMenuInfo c = new ItemView.ContextMenuInfo(
                adapterMenuInfo.position, selectedItem, this,
                getActivity().getMenuInflater());

        if (selectedItem != null && selectedItem.getId() != null) {
            mItemView.onCreateContextMenu(menu, v, c);
        }
    }

    public boolean doItemContext(MenuItem menuItem, int position) {
        return mItemView.doItemContext(menuItem, position, getItem(position));
    }

    public boolean doItemContext(MenuItem menuItem) {
        return mItemView.doItemContext(menuItem);
    }

    public ItemView<T> getItemView() {
        return mItemView;
    }

    public void setItemView(ItemView<T> itemView) {
        mItemView = itemView;
    }

    @Override
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
        for (T item : items) {
            if (offset >= pageSize) {
                start += offset;
                page = getPage(start);
                offset = 0;
            }
            page[offset++] = item;
        }
    }

    @Override
    public T getItem(int position) {
        T item = getPage(position)[position % pageSize];
        if (item == null) {
            if (mEmptyItem) {
                position--;
            }
            getActivity().maybeOrderPage(pageNumber(position) * pageSize);
        }
        return item;
    }

    public void setItem(int position, T item) {
        getPage(position)[position % pageSize] = item;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        T item = getItem(position);
        return item != null && item.getId() != null && mItemView.isSelectable(item);
    }

    /**
     * Generates a string suitable for use as the list's title.
     *
     * @return the title.
     */
    public String getHeader() {
        String item_text = getQuantityString(getCount());
        return getActivity().getString(R.string.browse_items_text, item_text, getCount());
    }

    /**
     * Called when the number of items in the list changes. The default implementation is empty.
     */
    protected void onCountUpdated() {
    }

    /**
     * Update the contents of the items in this list.
     * <p/>
     * The size of the list of items is automatically adjusted if necessary, to obey the given
     * parameters.
     *
     * @param count Number of items as reported by squeezeserver.
     * @param start The start position of items in this update.
     * @param items New items to insert in the main list
     */
    public void update(int count, int start, List<T> items) {
        int offset = (mEmptyItem ? 1 : 0);
        count += offset;
        start += offset;
        if (count == 0 || count != getCount()) {
            this.count = count;
            onCountUpdated();
        }
        setItems(start, items);

        notifyDataSetChanged();
    }

    /**
     * @param item
     *
     * @return The position of the given item in this adapter or 0 if not found
     */
    public int findItem(T item) {
        for (int pos = 0; pos < getCount(); pos++) {
            if (getItem(pos) == null) {
                if (item == null) {
                    return pos;
                }
            } else if (getItem(pos).equals(item)) {
                return pos;
            }
        }
        return 0;
    }

    protected T[] arrayInstance(int size) {
        return mItemView.getCreator().newArray(size);
    }

}
