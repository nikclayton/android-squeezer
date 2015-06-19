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
import android.support.annotation.IntDef;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Joiner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.util.Reflection;
import uk.org.ngo.squeezer.widget.ListItemImageButton;
import uk.org.ngo.squeezer.widget.SquareImageView;

/**
 * Represents the view hierarchy for a single {@link Item} subclass, suitable for displaying in a
 * {@link ItemListActivity}.
 * <p/>
 * This class supports views that have a {@link TextView} to display the primary information about
 * the {@link Item} and can optionally enable additional views.  The layout is defined in {@code
 * res/layout/list_item.xml}. <ul> <li>A {@link SquareImageView} suitable for displaying icons</li>
 * <li>A second, smaller {@link TextView} for additional item information</li> <li>A {@link
 * ListItemImageButton} that shows a disclosure triangle for a context menu</li> </ul> The view can
 * display an item in one of two states.  The primary state is when the data to be inserted in to
 * the view is known, and represented by a complete {@link Item} subclass. The loading state is when
 * the data type is known, but has not been fetched from the server yet.
 * <p/>
 * To customise the view's display create an int of {@link ViewParam} and pass it to
 * {@link #setViewParams(int)} or {@link #setLoadingViewParams(int)} depending on whether
 * you want to change the layout of the view in its primary state or the loading state. For example,
 * if the primary state should show a context button you may not want to show that button while
 * waiting for data to arrive.
 * <p/>
 * Override {@link #bindView(View, Item)} and {@link #bindView(View, String)} to
 * control how data from the item is inserted in to the view.
 * <p/>
 * If you need a completely custom view hierarchy then override {@link #getAdapterView(View,
 * ViewGroup, int)} and {@link #getAdapterView(View, ViewGroup, String)}.
 *
 * @param <T> the Item subclass this view represents.
 */
public abstract class BaseItemView<T extends Item> implements ItemView<T> {

    protected static final int BROWSE_ALBUMS = 1;

    private final ItemListActivity mActivity;

    private final LayoutInflater mLayoutInflater;

    private Class<T> mItemClass;

    private Creator<T> mCreator;

    @IntDef(flag=true, value={
            VIEW_PARAM_ICON, VIEW_PARAM_TWO_LINE, VIEW_PARAM_CONTEXT_BUTTON
    })
    @Retention(RetentionPolicy.SOURCE)
    /** Parameters that control which additional views will be enabled in the item view. */
    public @interface ViewParam {}
    /** Adds a {@link SquareImageView} for displaying artwork or other iconography. */
    public static final int VIEW_PARAM_ICON = 1;
    /** Adds a second line for detail information ({@code R.id.text2}). */
    public static final int VIEW_PARAM_TWO_LINE = 1 << 1;
    /** Adds a button, with click handler, to display the context menu. */
    public static final int VIEW_PARAM_CONTEXT_BUTTON = 1 << 2;

    /**
     * View parameters for a filled-in view.  One primary line with context button.
     */
    @ViewParam private int mViewParams = VIEW_PARAM_CONTEXT_BUTTON;

    /**
     * View parameters for a view that is loading data.  Primary line only.
     */
    @ViewParam private int mLoadingViewParams = 0;

    /** Width of the icon, if VIEW_PARAM_ICON is used. */
    protected int mIconWidth;

    /** Height of the icon, if VIEW_PARAM_ICON is used. */
    protected int mIconHeight;

    /**
     * A ViewHolder for the views that make up a complete list item.
     */
    public static class ViewHolder {

        public ImageView icon;

        public TextView text1;

        public TextView text2;

        public ImageButton btnContextMenu;

        public @ViewParam int viewParams;
    }

    /**
     * Joins elements together with ' - ', skipping nulls.
     */
    protected static final Joiner mJoiner = Joiner.on(" - ").skipNulls();

    public BaseItemView(ItemListActivity activity) {
        mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
        mIconWidth = mActivity.getResources().getDimensionPixelSize(R.dimen.album_art_icon_width);
        mIconHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.album_art_icon_height);
    }

    @Override
    public ItemListActivity getActivity() {
        return mActivity;
    }

    public LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }

    /**
     * Set the view parameters to use for the view when data is loaded.
     */
    protected void setViewParams(@ViewParam int viewParams) {
        mViewParams = viewParams;
    }

    /**
     * Set the view parameters to use for the view while data is being loaded.
     */
    protected void setLoadingViewParams(@ViewParam int viewParams) {
        mLoadingViewParams = viewParams;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getItemClass() {
        if (mItemClass == null) {
            mItemClass = (Class<T>) Reflection.getGenericClass(getClass(), ItemView.class,
                    0);
            if (mItemClass == null) {
                throw new RuntimeException("Could not read generic argument for: " + getClass());
            }
        }
        return mItemClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Creator<T> getCreator() {
        if (mCreator == null) {
            Field field;
            try {
                field = getItemClass().getField("CREATOR");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                mCreator = (Creator<T>) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mCreator;
    }

    protected String getTag() {
        return getClass().getSimpleName();
    }

    /**
     * Returns a view suitable for displaying the data of item in a list. Item may not be null.
     * <p/>
     * Override this method and {@link #getAdapterView(View, ViewGroup, String)} if your subclass
     * uses a different layout.
     */
    @Override
    public View getAdapterView(View convertView, ViewGroup parent, T item) {
        View view = getAdapterView(convertView, parent, mViewParams);
        bindView(view, item);
        return view;
    }

    /**
     * Binds the item's name to {@link ViewHolder#text1}.
     * <p/>
     * OVerride this instead of {@link #getAdapterView(View, ViewGroup, Item)} if the
     * default layouts are sufficient.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param item The item to be bound
     */
    public void bindView(View view, T item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
    }

    /**
     * Returns a view suitable for displaying the "Loading..." text.
     * <p/>
     * Override this method and {@link #getAdapterView(View, ViewGroup, Item)} if your
     * extension uses a different layout.
     */
    @Override
    public View getAdapterView(View convertView, ViewGroup parent, String text) {
        View view = getAdapterView(convertView, parent, mLoadingViewParams);
        bindView(view, text);
        return view;
    }

    /**
     * Binds the text to {@link ViewHolder#text1}.
     * <p/>
     * Override this instead of {@link #getAdapterView(View, ViewGroup, String)} if the default
     * layout is sufficient.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param text The text to set in the view.
     */
    public void bindView(View view, String text) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(text);
    }

    /**
     * Creates a view from {@code convertView} and the {@code viewParams} using the default layout
     * {@link R.layout#list_item}
     *
     * @param convertView View to reuse if possible.
     * @param parent The {@link ViewGroup} to inherit properties from.
     * @param viewParams A set of 0 or more {@link ViewParam} to customise the view.
     *
     * @return convertView if it can be reused, or a new view
     */
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return getAdapterView(convertView, parent, viewParams, R.layout.list_item);
    }

    /**
     * Creates a view from {@code convertView} and the {@code viewParams}.
     *
     * @param convertView View to reuse if possible.
     * @param parent The {@link ViewGroup} to inherit properties from.
     * @param viewParams A set of 0 or more {@link ViewParam} to customise the view.
     * @param layoutResource The layout resource defining the item view
     *
     * @return convertView if it can be reused, or a new view
     */
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams,
            int layoutResource) {
        ViewHolder viewHolder =
                (convertView != null && convertView.getTag() instanceof ViewHolder)
                        ? (ViewHolder) convertView.getTag()
                        : null;

        if (viewHolder == null) {
            convertView = getLayoutInflater().inflate(layoutResource, parent, false);
            viewHolder = createViewHolder();
            viewHolder.text1 = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.text2 = (TextView) convertView.findViewById(R.id.text2);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.btnContextMenu = (ImageButton) convertView.findViewById(R.id.context_menu);
            convertView.setTag(viewHolder);
        }

        // If the view parameters are different then reset the visibility of child views and hook
        // up any standard behaviours.
        if (viewParams != viewHolder.viewParams) {
            viewHolder.icon.setVisibility(
                    (viewParams & VIEW_PARAM_ICON) != 0 ? View.VISIBLE : View.GONE);
            viewHolder.text2.setVisibility(
                    (viewParams & VIEW_PARAM_TWO_LINE) != 0 ? View.VISIBLE : View.GONE);

            if ((viewParams & VIEW_PARAM_CONTEXT_BUTTON) != 0) {
                viewHolder.btnContextMenu.setVisibility(View.VISIBLE);
                viewHolder.btnContextMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.showContextMenu();
                    }
                });
            } else {
                viewHolder.btnContextMenu.setVisibility(View.GONE);
            }

            viewHolder.viewParams = viewParams;
        }

        return convertView;
    }

    public ViewHolder createViewHolder() {
        return new ViewHolder();
    }

    @Override
    public boolean isSelectable(T item) {
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ItemView.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(menuInfo.item.getName());
    }

    /**
     * The default context menu handler handles some common actions.
     */
    @Override
    public boolean doItemContext(MenuItem menuItem, int index, T selectedItem) {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                SongListActivity.show(mActivity, selectedItem);
                return true;

            case BROWSE_ALBUMS:
                AlbumListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.browse_artists:
                ArtistListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.play_now:
                mActivity.play((PlaylistItem) selectedItem);
                return true;

            case R.id.add_to_playlist:
                mActivity.add((PlaylistItem) selectedItem);
                return true;

            case R.id.play_next:
                mActivity.insert((PlaylistItem) selectedItem);
                return true;

            case R.id.download:
                if (selectedItem instanceof FilterItem)
                    mActivity.downloadItem((FilterItem) selectedItem);
                return true;
        }
        return false;
    }

    /** Empty default context-sub-menu implementation, as most context menus doesn't have subs */
    @Override
    public boolean doItemContext(MenuItem menuItem) {
        return false;
    }
}
