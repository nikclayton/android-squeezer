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

package uk.org.ngo.squeezer.itemlist;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import java.util.EnumSet;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.Slider;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.itemlist.dialog.ArtworkListLayout;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginView extends BaseItemView<Plugin> {
    private final PluginViewLogic logicDelegate;
    private Window.WindowStyle windowStyle;

    /** Width of the icon, if VIEW_PARAM_ICON is used. */
    private int mIconWidth;

    /** Height of the icon, if VIEW_PARAM_ICON is used. */
    private int mIconHeight;

    PluginView(BaseListActivity<Plugin> activity, Window.WindowStyle windowStyle) {
        super(activity);
        setWindowStyle(windowStyle);
        this.logicDelegate = new PluginViewLogic(activity);
        setLoadingViewParams(viewParamIcon() | VIEW_PARAM_TWO_LINE );
    }

    PluginViewLogic getLogicDelegate() {
        return logicDelegate;
    }

    void setWindowStyle(Window.WindowStyle windowStyle) {
        this.windowStyle = windowStyle;
        if (listLayout() == ArtworkListLayout.grid) {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_height);
        } else {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_height);
        }
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, final Plugin item) {
        if (item.hasSlider()) {
            return sliderView(parent, item);
        } else {
            @ViewParam int viewParams = (viewParamIcon() | VIEW_PARAM_TWO_LINE | viewParamContext(item));
            View view = getAdapterView(convertView, parent, viewParams);
            bindView(view, item);
            return view;
        }
    }

    private View sliderView(ViewGroup parent, final Plugin item) {
        View view = getLayoutInflater().inflate(R.layout.slider_item, parent, false);
        final TextView sliderValue = view.findViewById(R.id.slider_value);
        SeekBar seekBar = view.findViewById(R.id.slider);
        final int thumbWidth = seekBar.getThumb().getIntrinsicWidth();
        final int thumbOffset = seekBar.getThumbOffset();
        final Slider slider = item.slider;
        final int max = seekBar.getMax();
        seekBar.setProgress((slider.initial - slider.min) * max / (slider.max - slider.min));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sliderValue.setText(String.valueOf(getValue(progress)));

                int pos = progress * (seekBar.getWidth() - 2 * thumbWidth) / seekBar.getMax();
                sliderValue.setX(seekBar.getX() + pos + thumbOffset + thumbWidth);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                sliderValue.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (item.goAction != null) {
                    sliderValue.setVisibility(View.INVISIBLE);
                    item.inputValue = String.valueOf(getValue(seekBar.getProgress()));
                    getActivity().action(item, item.goAction);
                }
            }

            private int getValue(int progress) {
                return slider.min + (slider.max - slider.min) * progress / max;
            }
        });
        return view;
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return getAdapterView(convertView, parent, viewParams, layoutResource());
    }

    @LayoutRes private int layoutResource() {
        return (listLayout() == ArtworkListLayout.grid) ? R.layout.grid_item : R.layout.list_item;
    }

    ArtworkListLayout listLayout() {
        return listLayout(getActivity(), windowStyle);
    }

    static ArtworkListLayout listLayout(ItemListActivity activity, Window.WindowStyle windowStyle) {
        if (canChangeListLayout(windowStyle)) {
            return activity.getPreferredListLayout();
        }
        return ArtworkListLayout.list;
    }

    static boolean canChangeListLayout(Window.WindowStyle windowStyle) {
        return EnumSet.of(Window.WindowStyle.HOME_MENU, Window.WindowStyle.ICON_LIST).contains(windowStyle);
    }

    private int viewParamIcon() {
        return windowStyle == Window.WindowStyle.TEXT_ONLY ? 0 : VIEW_PARAM_ICON;
    }

    private int viewParamContext(Plugin item) {
        return item.hasContextMenu() ? VIEW_PARAM_CONTEXT_BUTTON : 0;
    }

    @Override
    public void bindView(View view, Plugin item) {
        super.bindView(view, item);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text2.setText(item.text2);

        // If the item has an image, then fetch and display it
        if (item.hasArtwork()) {
            ImageFetcher.getInstance(getActivity()).loadImage(item.getIcon(), viewHolder.icon,
                    mIconWidth, mIconHeight);
        } else {
            viewHolder.icon.setImageDrawable(item.getIconDrawable(getActivity()));
        }

        if (item.hasContextMenu()) {
            viewHolder.contextMenuButton.setVisibility(item.checkbox == null && item.radio == null ? View.VISIBLE : View.GONE);
            viewHolder.contextMenuCheckbox.setVisibility(item.checkbox != null ? View.VISIBLE : View.GONE);
            viewHolder.contextMenuRadio.setVisibility(item.radio != null ? View.VISIBLE : View.GONE);
            if (item.checkbox != null) {
                viewHolder.contextMenuCheckbox.setChecked(item.checkbox);
            } else if (item.radio != null) {
                viewHolder.contextMenuRadio.setChecked(item.radio);
            }
        }
    }

    @Override
    public boolean isSelectable(Plugin item) {
        return item.isSelectable();
    }

    @Override
    public void onItemSelected(View view, int index, Plugin item) {
        Action.JsonAction action = (item.goAction != null && item.goAction.action != null) ? item.goAction.action : null;
        Action.NextWindow nextWindow = (action != null ? action.nextWindow : item.nextWindow);
        if (item.checkbox != null) {
            item.checkbox = !item.checkbox;
            Action checkboxAction = item.checkboxActions.get(item.checkbox);
            if (checkboxAction != null) {
                getActivity().action(item, checkboxAction);
            }
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.contextMenuCheckbox.setChecked(item.checkbox);
        } else if (nextWindow != null && !item.hasInput()) {
            getActivity().action(item, item.goAction);
        } else {
            if (item.goAction != null)
                logicDelegate.execGoAction((ViewHolder) view.getTag(), item, 0);
            else if (item.hasSubItems())
                PluginListActivity.show(getActivity(), item);
            else if (item.getNode() != null) {
                HomeMenuActivity.show(getActivity(), item);
            }
        }
   }
    public void onGroupSelected(View view, Plugin[] items) {
    
    }
    @Override
    public void showContextMenu(ViewHolder viewHolder, Plugin item) {
        logicDelegate.showContextMenu(viewHolder, item);
    }
}
