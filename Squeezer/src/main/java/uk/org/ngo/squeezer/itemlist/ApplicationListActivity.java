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
import android.content.Intent;

import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;

public class ApplicationListActivity extends PluginListActivity {

    @Override
    public ItemView<Plugin> createItemView() {
        return new RadioView(this);
    }

    public static void show(Activity activity) {
        show(activity, Plugin.MY_APPS);
    }

    public static void show(Activity activity, Plugin plugin) {
        final Intent intent = new Intent(activity, ApplicationListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        activity.startActivity(intent);
    }
}
