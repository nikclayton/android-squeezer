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

import java.util.List;

import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.os.RemoteException;

public abstract class SqueezerAbstractSongListActivity extends SqueezerBaseListActivity<SqueezerSong> {

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerSongListCallback(songListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterSongListCallback(songListCallback);
	}

	private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
		public void onSongsReceived(int count, int start, List<SqueezerSong> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

}
