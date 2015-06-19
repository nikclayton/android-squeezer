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


import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.RetainFragment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A generic base class for an activity to list items of a particular SqueezeServer data type. The
 * data type is defined by the generic type argument, and must be an extension of {@link Item}. You
 * must provide an {@link ItemView} to provide the view logic used by this activity. This is done by
 * implementing {@link #createItemView()}.
 * <p/>
 * When the activity is first created ({@link #onCreate(Bundle)}), an empty {@link ItemAdapter}
 * is created using the provided {@link ItemView}. See {@link ItemListActivity} for see details of
 * ordering and receiving of list items from SqueezeServer, and handling of item selection.
 *
 * @param <T> Denotes the class of the items this class should list
 *
 * @author Kurt Aaholst
 */
public abstract class BaseListActivity<T extends Item> extends ItemListActivity implements IServiceItemListCallback<T> {

    private static final String TAG = BaseListActivity.class.getName();

    /**
     * Tag for first visible position in mRetainFragment.
     */
    private static final String TAG_POSITION = "position";


    /**
     * Tag for itemAdapter in mRetainFragment.
     */
    public static final String TAG_ADAPTER = "adapter";

    private AbsListView mListView;

    private ItemAdapter<T> itemAdapter;

    /**
     * Progress bar (spinning) while items are loading.
     */
    private ProgressBar loadingProgress;

    /**
     * Fragment to retain information across the activity lifecycle.
     */
    private RetainFragment mRetainFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRetainFragment = RetainFragment.getInstance(TAG, getSupportFragmentManager());

        setContentView(getContentView());
        mListView = checkNotNull((AbsListView) findViewById(R.id.item_list),
                "getContentView() did not return a view containing R.id.item_list");

        loadingProgress = checkNotNull((ProgressBar) findViewById(R.id.loading_progress),
                "getContentView() did not return a view containing R.id.loading_progress");

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getItemAdapter().onItemSelected(position);
            }
        });

        mListView.setOnScrollListener(new ScrollListener());

        mListView.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                if (imageView != null) {
                    imageView.setImageBitmap(null);
                }
            }
        });

        // Delegate context menu creation to the adapter.
        mListView.setOnCreateContextMenuListener(getItemAdapter());
    }

    public void onEventMainThread(HandshakeComplete event) {
        maybeOrderVisiblePages(mListView);
        setAdapter();
    }

    /**
     * Returns the ID of a content view to be used by this list activity.
     * <p/>
     * The content view must contain a {@link AbsListView} with the id {@literal item_list} and a
     * {@link ProgressBar} with the id {@literal loading_progress} in order to be valid.
     *
     * @return The ID
     */
    protected int getContentView() {
        return R.layout.item_list;
    }

    /**
     * @return A new view logic to be used by this activity
     */
    abstract protected ItemView<T> createItemView();

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        // If menuInfo is null we have a sub menu, we expect the adapter to have stored the position
        if (menuInfo == null)
            return itemAdapter.doItemContext(menuItem);
        else
            return itemAdapter.doItemContext(menuItem, menuInfo.position);
    }

    /**
     * Set our adapter on the list view.
     * <p/>
     * This can't be done in {@link #onCreate(android.os.Bundle)} because getView might be called
     * before the handshake is complete, so we need to delay it.
     * <p/>
     * However when we set the adapter after onCreate the list is scrolled to top, so we retain the
     * visible position.
     * <p/>
     * Call this method after the handshake is complete.
     */
    private void setAdapter() {
        // setAdapter is not defined for AbsListView before API level 11, but
        // it is for concrete implementations, so we call it by reflection
        try {
            Method method = mListView.getClass().getMethod("setAdapter", ListAdapter.class);
            method.invoke(mListView, getItemAdapter());
        } catch (Exception e) {
            Log.e(getTag(), "Error calling 'setAdapter'", e);
        }

        Integer position = (Integer) mRetainFragment.get(TAG_POSITION);
        if (position != null) {
            if (mListView instanceof ListView) {
                ((ListView) mListView).setSelectionFromTop(position, 0);
            } else {
                mListView.setSelection(position);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveVisiblePosition();
    }

    /**
     * Store the first visible position of {@link #mListView}, in the {@link #mRetainFragment}, so
     * we can later retrieve it.
     *
     * @see android.widget.AbsListView#getFirstVisiblePosition()
     */
    private void saveVisiblePosition() {
        mRetainFragment.put(TAG_POSITION, mListView.getFirstVisiblePosition());
    }

    /**
     * @return The current {@link ItemAdapter}'s {@link ItemView}
     */
    public ItemView<T> getItemView() {
        return getItemAdapter().getItemView();
    }

    /**
     * @return The current {@link ItemAdapter}, creating it if necessary.
     */
    public ItemAdapter<T> getItemAdapter() {
        if (itemAdapter == null) {
            //noinspection unchecked
            itemAdapter = (ItemAdapter<T>) mRetainFragment.get(TAG_ADAPTER);
            if (itemAdapter == null) {
                itemAdapter = createItemListAdapter(createItemView());
                mRetainFragment.put(TAG_ADAPTER, itemAdapter);
            } else {
                // We have just retained the item adapter, we need to create a new
                // item view logic, cause it holds a reference to the old activity
                itemAdapter.setItemView(createItemView());
                // Update views with the count from the retained item adapter
                itemAdapter.onCountUpdated();
            }
        }

        return itemAdapter;
    }

    @Override
    protected void clearItemAdapter() {
        // TODO: This should be removed in favour of showing a progress spinner in the actionbar.
        mListView.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);

        getItemAdapter().clear();
    }

    /**
     * @return The {@link AbsListView} used by this activity
     */
    public AbsListView getListView() {
        return mListView;
    }

    protected ItemAdapter<T> createItemListAdapter(ItemView<T> itemView) {
        return new ItemAdapter<T>(itemView);
    }

    public void onItemsReceived(final int count, final int start, final List<T> items) {
        super.onItemsReceived(count, start, items.size());

        getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mListView.setVisibility(View.VISIBLE);
                loadingProgress.setVisibility(View.GONE);
                getItemAdapter().update(count, start, items);
            }
        });
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, String> parameters, List<T> items, Class<T> dataType) {
        onItemsReceived(count, start, items);
    }

    @Override
    public Object getClient() {
        return this;
    }

    protected class ScrollListener extends ItemListActivity.ScrollListener {

        ScrollListener() {
            super();
        }

        /**
         * Pauses cache disk fetches if the user is flinging the list, or if their finger is still
         * on the screen.
         */
        @Override
        public void onScrollStateChanged(AbsListView listView, int scrollState) {
            super.onScrollStateChanged(listView, scrollState);

            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                    scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(true);
            } else {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(false);
            }
        }
    }
}
