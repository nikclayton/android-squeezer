package uk.org.ngo.squeezer.itemlists.actions;

import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import android.os.RemoteException;

public class InsertAction extends PlayableItemAction {

	public InsertAction(SqueezerItemListActivity activity) {
		super(activity);
	}

	@Override
	public void execute(SqueezerPlaylistItem item) throws RemoteException {
		activity.insert(item);
	}

}
