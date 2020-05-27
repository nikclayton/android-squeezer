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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

import uk.org.ngo.squeezer.R;


/**
 * A generic class for an adapter to list items of a particular SqueezeServer data type. The data
 * type is defined by the generic type argument, and must be an extension of {@link Item}.
 * <p>
 * Normally there is no need to extend this, as we delegate all type dependent stuff to
 * {@link ItemView}.
 *
 * @param <T> Denotes the class of the items this class should list
 *
 * @author Kurt Aaholst
 * @see ItemView
 */
public class ItemAdapter<T extends Item> extends BaseAdapter {

    /**
     * View logic for this adapter
     */
    private ItemView<T> mItemView;

    /**
     * List of items, possibly headed with an empty item.
     * <p>
     * As the items are received from SqueezeServer they will be inserted in the list.
     */
    private int count;

    private final SparseArray<T[]> pages = new SparseArray<>();

    /**
     * This is set if the list shall start with an empty item.
     */
    private final boolean mEmptyItem;

    /**
     * Text to display before the items are received from SqueezeServer
     */
    private final String loadingText;

    /**
     * Number of elements to by fetched at a time
     */
    private final int pageSize;

    /**
     * Index of the latest selected item see {@link #onItemSelected(View, int)}
     */
    private int selectedIndex;

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
     * Calls {@link #(ItemView, boolean)}, with emptyItem = false
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
        count = (mEmptyItem ? 1 : 0);
        pages.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        T item = getItem(position);
        if (item != null) {
            if (item.radio != null) {
                item.radio = (position == selectedIndex);
            }
            return mItemView.getAdapterView(convertView, parent, position, item);
        }

        return mItemView.getAdapterView(convertView, parent,
                (position == 0 && mEmptyItem ? "" : loadingText));
    }

    public ItemListActivity getActivity() {
        return mItemView.getActivity();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    public void onItemSelected(View view, int position) {
        T item = getItem(position);
        if (item != null) {
            selectedIndex = position;
            mItemView.onItemSelected(view, position, item);
            if (item.radio != null) {
                notifyDataSetChanged();
            }
        }
    }
    public void onSelected(View view) {
        mItemView.onGroupSelected(view, getPage(0));
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
            if (item.radio != null && item.radio) {
                selectedIndex = start + offset;
            }
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
        if (item.radio != null && item.radio) {
            selectedIndex = position;
        }
        getPage(position)[position % pageSize] = item;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        T item = getItem(position);
        return item != null && mItemView.isSelectable(item);
    }

    /**
     * Called when the number of items in the list changes. The default implementation is empty.
     */
    protected void onCountUpdated() {
    }

    /**
     * Update the contents of the items in this list.
     * <p>
     * The size of the list of items is automatically adjusted if necessary, to obey the given
     * parameters.
     *
     * @param count Number of items as reported by SqueezeServer.
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

    /**
     * Remove the item at the specified position, update the count and notify the change.
     */
    public void removeItem(int position) {
        T[] page = getPage(position);
        int offset = position % pageSize;
        while (position++ <= count) {
            if (offset == pageSize - 1) {
                T[] nextPage = getPage(position);
                page[offset] = nextPage[0];
                offset = 0;
                page = nextPage;
            } else {
                page[offset] = page[offset+1];
                offset++;
            }
        }

        count--;
        onCountUpdated();
        notifyDataSetChanged();
    }

    public void insertItem(int position, T item) {
        int n = count;
        T[] page = getPage(n);
        int offset = n % pageSize;
        while (n-- > position) {
            if (offset == 0) {
                T[] nextPage = getPage(n);
                offset = pageSize - 1;
                page[0] = nextPage[offset];
                page = nextPage;
            } else {
                page[offset] = page[offset-1];
                offset--;
            }
        }
        page[offset] = item;

        count++;
        onCountUpdated();
        notifyDataSetChanged();
    }

    private T[] arrayInstance(int size) {
        return mItemView.getCreator().newArray(size);
    }

}
