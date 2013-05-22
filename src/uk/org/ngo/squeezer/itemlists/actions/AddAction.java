package uk.org.ngo.squeezer.itemlists.actions;

import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import android.os.RemoteException;
import android.util.Log;

public class AddAction extends PlayableItemAction {

	public AddAction(SqueezerItemListActivity activity) {
		super(activity);
	}

	@Override
	public void execute(SqueezerPlaylistItem item) throws RemoteException {
		Log.d(getTag(), "Adding song to playlist");
		activity.add(item);
	}
}
