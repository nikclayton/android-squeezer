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
import android.view.View;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.SpinnerItemView;
import uk.org.ngo.squeezer.model.Year;

public class YearView extends SpinnerItemView<Year> {

    public YearView(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.year, quantity);
    }

    @Override
    public void onItemSelected(int index, Year item) {
        AlbumListActivity.show(getActivity(), item);
    }

    // XXX: Make this a menu resource.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, R.id.browse_songs, 0, R.string.BROWSE_SONGS);
        menu.add(Menu.NONE, BROWSE_ALBUMS, 1, R.string.BROWSE_ALBUMS);
        menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
        menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
        menu.add(Menu.NONE, R.id.download, 3, R.string.DOWNLOAD_ITEM);
    }
}
