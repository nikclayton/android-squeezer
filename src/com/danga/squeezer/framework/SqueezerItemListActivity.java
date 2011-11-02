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

package com.danga.squeezer.framework;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import com.danga.squeezer.R;
import com.danga.squeezer.menu.SqueezerMenuFragment;
import com.danga.squeezer.service.SqueezeService;

/**
 * <p>
 * This class defines the common minimum, which any activity browsing the SqueezeServer's database
 * must implement.
 * </p>
 * @author Kurt Aaholst
 */
public abstract class SqueezerItemListActivity extends SqueezerBaseActivity implements OnScrollListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SqueezerMenuFragment.addTo(this);
    };
    
    /**
     * This is called when the service is connected.
     * <p>
     * You must register a callback for {@link SqueezeService} to call when the ordered items
     * from {@link #orderPage(int)} are received from SqueezeServer. This callback must pass
     * these items on to {@link SqueezerItemListAdapter#update(int, int, int, List)}.
     *
     * @throws RemoteException
     */
	protected abstract void registerCallback() throws RemoteException;

	/**
	 * This is called when the service is disconnected.
	 * @throws RemoteException
	 */
	protected abstract void unregisterCallback() throws RemoteException;

    /**
     * Implementations must start an asynchronous fetch of items, when this is called.
     * @throws RemoteException
     * @param start Position in list to start the fetch. Pass this on to {@link SqueezeService}
     */
	protected abstract void orderPage(int start) throws RemoteException;


	private final Set<Integer> orderedPages = new HashSet<Integer>();

	/**
	 * Order page at specified position, if it has not already been ordered.
	 * @param pagePosition
	 */
	public void maybeOrderPage(int pagePosition) {
		if (!listBusy && !orderedPages.contains(pagePosition)) {
			orderedPages.add(pagePosition);
			try {
				orderPage(pagePosition);
			} catch (RemoteException e) {
				Log.e(getTag(), "Error ordering items (" + pagePosition + "): " + e);
			}
		}
	}

	/**
	 * Clear all information about which pages has been ordered, and reorder the first page
	 */
	public void reorderItems() {
		orderedPages.clear();
		maybeOrderPage(0);
	}



	private boolean listBusy;
	public boolean isListBusy() { return listBusy; }

	public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
        case OnScrollListener.SCROLL_STATE_IDLE:
        	listBusy = false;

        	int pageSize = getResources().getInteger(R.integer.PageSize);
        	int pos = (view.getFirstVisiblePosition() / pageSize) * pageSize;
        	int end = view.getFirstVisiblePosition() + view.getChildCount();
        	while (pos < end) {
        		maybeOrderPage(pos);
        		pos += pageSize;
        	}
        	break;
        case OnScrollListener.SCROLL_STATE_FLING:
        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
        	listBusy = true;
        	break;
        }
 	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

}