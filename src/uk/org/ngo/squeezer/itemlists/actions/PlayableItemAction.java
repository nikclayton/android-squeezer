package uk.org.ngo.squeezer.itemlists.actions;

import android.os.RemoteException;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;

public abstract class PlayableItemAction {
	public static enum Type {
		/** PLay song immediately */
		PLAY(R.string.playable_item_action_play),
		/**Add the song to the playlist */
		ADD(R.string.playable_item_action_add),
		/** Play the song after the current song*/
		INSERT(R.string.playable_item_action_insert),
		/** Browse contents. */
		BROWSE(R.string.playable_item_action_browse);
		public final int labelId;
		private Type(int label) {
			this.labelId = label;
		}
	}

	protected final SqueezerItemListActivity activity;

	public PlayableItemAction(SqueezerItemListActivity activity) {
		super();
		this.activity = activity;
	}

	protected String getTag() {
		return getClass().getSimpleName();
	}

	public abstract void execute(SqueezerPlaylistItem item)
			throws RemoteException;

	public static PlayableItemAction createAction(
			SqueezerItemListActivity activity, String actionType) {
		if (actionType == null || actionType.equals("")) {
			return new PlayAction(activity);
		}
		Type type = Type.valueOf(actionType);
		switch (type) {
		case BROWSE:
			return new BrowseSongsAction(activity);
		case ADD:
			return new AddAction(activity);
		case INSERT:
			return new InsertAction(activity);
		case PLAY:
		default:
			return new PlayAction(activity);
		}

	}
}