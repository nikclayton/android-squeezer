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


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.itemlist.action.PlayableItemAction;

/**
 * Represents the view hierarchy for a single {@link PlaylistItem} subclass. with a configurable on
 * select action.
 *
 * @param <T>
 */
public abstract class PlaylistItemView<T extends PlaylistItem> extends
        BaseItemView<T> implements OnSharedPreferenceChangeListener {

    protected final SharedPreferences preferences;

    protected PlayableItemAction onSelectAction;

    public PlaylistItemView(ItemListActivity activity) {
        super(activity);
        preferences = activity.getSharedPreferences(Preferences.NAME, 0);
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSelectAction = getOnSelectAction();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        onSelectAction = getOnSelectAction();
    }

    abstract protected PlayableItemAction getOnSelectAction();

    @Override
    public void onItemSelected(int index, T item) {
        Log.d(getTag(), "Executing on select action");
        if (onSelectAction != null) {
            onSelectAction.execute(item);
        }
    }

}
