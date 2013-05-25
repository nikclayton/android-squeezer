package uk.org.ngo.squeezer.itemlists.actions;

import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import android.os.RemoteException;
import android.util.Log;

public class BrowseSongsAction extends PlayableItemAction {

	public BrowseSongsAction(SqueezerItemListActivity activity) {
		super(activity);
	}

	@Override
	public void execute(SqueezerPlaylistItem item) throws RemoteException {
		Log.d(getTag(), "Browsing songs of " + item);
		 SqueezerSongListActivity.show(activity, item);
	}

}
