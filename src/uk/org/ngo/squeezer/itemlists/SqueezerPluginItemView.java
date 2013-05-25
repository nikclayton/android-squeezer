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

package uk.org.ngo.squeezer.itemlists;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseItemView;
import uk.org.ngo.squeezer.model.SqueezerPluginItem;
import uk.org.ngo.squeezer.util.ImageFetcher;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SqueezerPluginItemView extends SqueezerBaseItemView<SqueezerPluginItem> {
	private final SqueezerPluginItemListActivity activity;

	public SqueezerPluginItemView(SqueezerPluginItemListActivity activity) {
		super(activity);
		this.activity = activity;
	}

    @Override
    public View getAdapterView(View convertView, SqueezerPluginItem item, ImageFetcher imageFetcher) {
        View view = getAdapterView(convertView);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

		viewHolder.label.setText(item.getName());
        if (!item.isHasitems()) {
            viewHolder.btnContextMenu.setVisibility(View.VISIBLE);
            viewHolder.btnContextMenu.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    v.showContextMenu();
                }
            });
        } else
            viewHolder.btnContextMenu.setVisibility(View.GONE);

        // If the item has an image, then fetch and display it
        if (item.getImage() != null) {
            imageFetcher.loadImage(item.getImage(), viewHolder.icon);
        } else {
            // Otherwise we will revert to some other icon.
            // This is not an exact approach, more like a best effort.

            if (item.isHasitems()) {
                // If this item has sub-items we use the icon of the parent and if that fails, the current plugin
                if (activity.getPlugin().getIconResource() != 0)
                    viewHolder.icon.setImageResource(activity.getPlugin().getIconResource());
                else
                    imageFetcher.loadImage(activity.getIconUrl(activity.getPlugin().getIcon()), viewHolder.icon);
            } else {
                // Finally we assume it is an item that can be played. This is consistent with onItemSelected and onCreateContextMenu
                viewHolder.icon.setImageResource(R.drawable.ic_songs);
            }
        }

        return view;
    }

    @Override
    public View getAdapterView(View convertView, String label) {
        View view = getAdapterView(convertView);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.label.setText(label);
        viewHolder.icon.setImageResource(R.drawable.icon_pending_artwork);
        viewHolder.btnContextMenu.setVisibility(View.GONE);

        return view;
    }

    private View getAdapterView(View convertView) {
        ViewHolder viewHolder = (convertView != null && convertView.getTag().getClass() == ViewHolder.class)
                ? (ViewHolder) convertView.getTag()
                : null;

        if (viewHolder == null) {
            convertView = getLayoutInflater().inflate(R.layout.icon_one_line, null);

            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.btnContextMenu = (ImageButton) convertView.findViewById(R.id.context_menu);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }

	private static class ViewHolder {
		TextView label;
		ImageView icon;
        ImageButton btnContextMenu;
	}

    @Override
    public String getQuantityString(int quantity) {
		return null;
	}

    @Override
    public void onItemSelected(int index, SqueezerPluginItem item) throws RemoteException {
		if (item.isHasitems())
			activity.show(item);
		else {
			activity.play(item);
			NowPlayingActivity.show(getActivity());
		}
	}

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (!((SqueezerPluginItem) menuInfo.item).isHasitems()) {
            super.onCreateContextMenu(menu, v, menuInfo);

            menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
            menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
            menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);
        }
    }

	@Override
	public boolean doItemContext(MenuItem menuItem, int index, SqueezerPluginItem selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
            case R.id.play_now:
                if (activity.play(selectedItem))
                    Toast.makeText(activity,
                            activity.getString(R.string.ITEM_PLAYING, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;

            case R.id.add_to_playlist:
                if (activity.add(selectedItem))
                    Toast.makeText(activity,
                            activity.getString(R.string.ITEM_ADDED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;

            case R.id.play_next:
                if (activity.insert(selectedItem))
                    Toast.makeText(activity,
                            activity.getString(R.string.ITEM_INSERTED, selectedItem.getName()),
                            Toast.LENGTH_SHORT).show();
                return true;
		}
		return false;
	}

}
