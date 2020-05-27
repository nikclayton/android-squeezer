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
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.Reflection;
import uk.org.ngo.squeezer.widget.SquareImageView;

/**
 * Represents the view hierarchy for a single {@link Item} subclass, suitable for displaying in a
 * {@link ItemListActivity}.
 * <p>
 * This class supports views that have a {@link TextView} to display the primary information about
 * the {@link Item} and can optionally enable additional views.  The layout is defined in {@code
 * res/layout/list_item.xml}. <ul> <li>A {@link SquareImageView} suitable for displaying icons</li>
 * <li>A second, smaller {@link TextView} for additional item information</li> <li>A {@link
 * Button} that shows a disclosure triangle for a context menu</li> </ul> The view can
 * display an item in one of two states.  The primary state is when the data to be inserted in to
 * the view is known, and represented by a complete {@link Item} subclass. The loading state is when
 * the data type is known, but has not been fetched from the server yet.
 * <p>
 * To customise the view's display create an int of {@link ViewParam} and pass it to
 * {@link #setViewParams(int)} or {@link #setLoadingViewParams(int)} depending on whether
 * you want to change the layout of the view in its primary state or the loading state. For example,
 * if the primary state should show a context button you may not want to show that button while
 * waiting for data to arrive.
 * <p>
 * Override {@link #bindView(View, Item)} and {@link #bindView(View, String)} to
 * control how data from the item is inserted in to the view.
 * <p>
 * If you need a completely custom view hierarchy then override {@link #getAdapterView(View,
 * ViewGroup, int)} and {@link #getAdapterView(View, ViewGroup, String)}.
 *
 * @param <T> the Item subclass this view represents.
 */
public abstract class BaseItemView<T extends Item> implements ItemView<T> {

    private final ItemListActivity mActivity;

    private final LayoutInflater mLayoutInflater;

    private Class<T> mItemClass;

    private Creator<T> mCreator;

    @IntDef(flag=true, value={
            VIEW_PARAM_ICON, VIEW_PARAM_TWO_LINE, VIEW_PARAM_CONTEXT_BUTTON
    })
    @Retention(RetentionPolicy.SOURCE)
    /* Parameters that control which additional views will be enabled in the item view. */
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

    /**
     * A ViewHolder for the views that make up a complete list item.
     */
    public static class ViewHolder {
        public View itemView;

        public ImageView icon;

        public TextView text1;

        public TextView text2;

        public View contextMenuButtonHolder;
        public Button contextMenuButton;
        public ProgressBar contextMenuLoading;
        public CheckBox contextMenuCheckbox;
        public RadioButton contextMenuRadio;

        public @ViewParam int viewParams;

        public void setView(View view) {
            itemView = view;
            text1 = view.findViewById(R.id.text1);
            text2 = view.findViewById(R.id.text2);
            icon = view.findViewById(R.id.icon);
            setContextMenu(view);
        }

        public void setContextMenu(View view) {
            contextMenuButtonHolder = view.findViewById(R.id.context_menu);
            if (contextMenuButtonHolder!= null) {
                contextMenuButton = contextMenuButtonHolder.findViewById(R.id.context_menu_button);
                contextMenuLoading = contextMenuButtonHolder.findViewById(R.id.loading_progress);
                contextMenuCheckbox = contextMenuButtonHolder.findViewById(R.id.checkbox);
                contextMenuRadio = contextMenuButtonHolder.findViewById(R.id.radio);
            }
        }
    }

    public BaseItemView(ItemListActivity activity) {
        mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
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
                mItemClass = (Class<T>) Reflection.getGenericClass(getClass().getSuperclass(), ItemView.class,
                    0);
                if (mItemClass == null) {
                    
                    throw new RuntimeException("Could not read generic argument for: " + getClass());
                }
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

    /**
     * Returns a view suitable for displaying the data of item in a list. Item may not be null.
     * <p>
     * Override this method and {@link #getAdapterView(View, ViewGroup, String)} if your subclass
     * uses a different layout.
     */
    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, T item) {
        View view = getAdapterView(convertView, parent, mViewParams);
        bindView(view, item);
        return view;
    }

    /**
     * Binds the item's name to {@link ViewHolder#text1}.
     * <p>
     * OVerride this instead of {@link #getAdapterView(View, ViewGroup, int, Item)} if the
     * default layouts are sufficient.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param item The item to be bound
     */
    public void bindView(final View view, final T item) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        
        if (viewHolder.contextMenuButton!= null) {
            viewHolder.contextMenuButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showContextMenu(viewHolder, item);
                }
            });
        }
    }

    /**
     * Returns a view suitable for displaying the "Loading..." text.
     * <p>
     * Override this method and {@link #getAdapterView(View, ViewGroup, int, Item)} if your
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
     * <p>
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
    protected View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams, @LayoutRes int layoutResource) {
        ViewHolder viewHolder =
                (convertView != null && convertView.getTag() instanceof ViewHolder)
                        ? (ViewHolder) convertView.getTag()
                        : null;

        if (viewHolder == null) {
            convertView = getLayoutInflater().inflate(layoutResource, parent, false);
            viewHolder = createViewHolder();
            viewHolder.setView(convertView);
            setViewParams(viewParams, viewHolder);
            convertView.setTag(viewHolder);
        }

        // If the view parameters are different then reset the visibility of child views and hook
        // up any standard behaviours.
        if (viewParams != viewHolder.viewParams) {
            setViewParams(viewParams, viewHolder);
        }

        return convertView;
    }

    private void setViewParams(@ViewParam int viewParams, ViewHolder viewHolder) {
        viewHolder.icon.setVisibility(
                (viewParams & VIEW_PARAM_ICON) != 0 ? View.VISIBLE : View.GONE);
        viewHolder.text2.setVisibility(
                (viewParams & VIEW_PARAM_TWO_LINE) != 0 ? View.VISIBLE : View.GONE);
        
        if (viewHolder.contextMenuButtonHolder != null) {
            viewHolder.contextMenuButtonHolder.setVisibility(
                    (viewParams & VIEW_PARAM_CONTEXT_BUTTON) != 0 ? View.VISIBLE : View.GONE);
        }
        viewHolder.viewParams = viewParams;
    }

    public ViewHolder createViewHolder() {
        return new ViewHolder();
    }

    @Override
    public boolean isSelectable(T item) {
        return (item.getId() != null);
    }

    @Override
    public void showContextMenu(ViewHolder v, T item) {
    }
}
