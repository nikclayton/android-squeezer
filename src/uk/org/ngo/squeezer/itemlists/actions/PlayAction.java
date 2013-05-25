package uk.org.ngo.squeezer.itemlists.actions;

import android.os.RemoteException;
import android.util.Log;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;

public class PlayAction extends PlayableItemAction {

	public PlayAction(SqueezerItemListActivity activity) {
		super(activity);
	}

	@Override
	public void execute(SqueezerPlaylistItem item) throws RemoteException {
		Log.d(getTag(), "Playing song");
		activity.play(item);
	}

}
