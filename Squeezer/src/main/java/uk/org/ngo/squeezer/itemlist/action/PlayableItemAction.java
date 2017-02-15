package uk.org.ngo.squeezer.itemlist.action;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public abstract class PlayableItemAction {

    public static enum Type {
        /**
         * Do nothing
         */
        NONE(R.string.NO_ACTION),
        /**
         * PLay song immediately
         */
        PLAY(R.string.PLAY_NOW),
        /**
         * Add the song to the playlist
         */
        ADD(R.string.ADD_TO_END),
        /**
         * Play the song after the current song
         */
        INSERT(R.string.PLAY_NEXT),
        /**
         * Browse contents.
         */
        BROWSE(R.string.BROWSE_SONGS);

        public final int labelId;

        private Type(int label) {
            this.labelId = label;
        }
    }

    protected final ItemListActivity activity;

    public PlayableItemAction(ItemListActivity activity) {
        super();
        this.activity = activity;
    }

    protected String getTag() {
        return getClass().getSimpleName();
    }

    public abstract void execute(PlaylistItem item);

    public static PlayableItemAction createAction(
            ItemListActivity activity, String actionType) {
        if (actionType == null || actionType.equals("")) {
            return new PlayAction(activity);
        }

        Type type = Type.valueOf(actionType);
        switch (type) {
            case NONE:
                return null;
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
