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

import android.content.Context;
import android.content.Intent;

import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Genre;

public class GenreListActivity extends BaseListActivity<Genre> {

    @Override
    public ItemView<Genre> createItemView() {
        return new GenreView(this);
    }

    @Override
    protected void orderPage(int start) {
        getService().genres(start, null, this);
    }


    public static void show(Context context) {
        final Intent intent = new Intent(context, GenreListActivity.class);
        context.startActivity(intent);
    }

}
