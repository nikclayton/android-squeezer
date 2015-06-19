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

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.SpinnerItemAdapter;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class GenreSpinner {

    private final GenreSpinnerCallback callback;

    private final ItemListActivity activity;

    private final Spinner spinner;

    public GenreSpinner(GenreSpinnerCallback callback, ItemListActivity activity, Spinner spinner) {
        this.callback = callback;
        this.activity = activity;
        this.spinner = spinner;
        orderItems();
    }

    private void orderItems() {
        if (callback.getService() != null) {
            callback.getService().genres(-1, null, genreListCallback);
        }
    }

    private final IServiceItemListCallback<Genre> genreListCallback
            = new IServiceItemListCallback<Genre>() {
        private ItemAdapter<Genre> adapter;

        @Override
        public void onItemsReceived(final int count, final int start, Map<String, String> parameters, final List<Genre> list, Class<Genre> dataType) {
            callback.getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        GenreView itemView = new GenreView(activity) {
                            @Override
                            public View getAdapterView(View convertView, ViewGroup parent,
                                    Genre item) {
                                return Util.getSpinnerItemView(getActivity(), convertView, parent,
                                        item.getName());
                            }

                            @Override
                            public View getAdapterView(View convertView, ViewGroup parent,
                                    String label) {
                                return Util.getSpinnerItemView(getActivity(), convertView, parent,
                                        label);
                            }

                        };
                        adapter = new SpinnerItemAdapter<Genre>(itemView, true);
                        spinner.setAdapter(adapter);
                    }
                    adapter.update(count, start, list);
                    spinner.setSelection(adapter.findItem(callback.getGenre()));
                }
            });
        }

        @Override
        public Object getClient() {
            return activity;
        }
    };

    public interface GenreSpinnerCallback {

        ISqueezeService getService();

        Handler getUIThreadHandler();

        Genre getGenre();

        void setGenre(Genre genre);
    }

}
