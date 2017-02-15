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

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.EnumSet;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginItemView extends BaseItemView<PluginItem> {

    private final PluginItemListActivity mActivity;

    public PluginItemView(PluginItemListActivity activity) {
        super(activity);
        mActivity = activity;

        setViewParams(EnumSet.of(ViewParams.ICON, ViewParams.CONTEXT_BUTTON));
        setLoadingViewParams(EnumSet.of(ViewParams.ICON));
    }

    public void bindView(View view, PluginItem item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());

        // Disable the context menu if this item has sub-items.
        if (item.isHasitems()) {
            viewHolder.btnContextMenu.setVisibility(View.GONE);
        }

        // If the item has an image, then fetch and display it
        if (item.getImage() != null) {
            imageFetcher.loadImage(item.getImage(), viewHolder.icon);
        } else {
            // Otherwise we will revert to some other icon. This is not an exact approach, more
            // like a best effort.
            if (item.isHasitems()) {
                // If this item has sub-items we use the icon of the parent and if that fails,
                // the current plugin.
                if (mActivity.getPlugin().getIconResource() != 0) {
                    viewHolder.icon.setImageResource(mActivity.getPlugin().getIconResource());
                } else {
                    imageFetcher.loadImage(mActivity.getIconUrl(mActivity.getPlugin().getIcon()),
                            viewHolder.icon);
                }
            } else {
                // Finally we assume it is an item that can be played. This is consistent with
                // onItemSelected and onCreateContextMenu.
                viewHolder.icon.setImageResource(R.drawable.ic_songs);
            }
        }
    }

    @Override
    public String getQuantityString(int quantity) {
        return null;
    }

    @Override
    public void onItemSelected(int index, PluginItem item) {
        if (item.isHasitems()) {
            mActivity.show(item);
        }
    }

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (!((PluginItem) menuInfo.item).isHasitems()) {
            super.onCreateContextMenu(menu, v, menuInfo);

            menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
            menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
            menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, PluginItem selectedItem) {
        switch (menuItem.getItemId()) {
            case R.id.play_now:
                if (mActivity.play(selectedItem)) {
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_PLAYING, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.add_to_playlist:
                if (mActivity.add(selectedItem)) {
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_ADDED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.play_next:
                if (mActivity.insert(selectedItem)) {
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.ITEM_INSERTED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return false;
    }

}
