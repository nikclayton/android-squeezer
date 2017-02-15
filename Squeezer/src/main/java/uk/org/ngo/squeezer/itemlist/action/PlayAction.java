package uk.org.ngo.squeezer.itemlist.action;

import android.util.Log;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public class PlayAction extends PlayableItemAction {

    public PlayAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        Log.d(getTag(), "Playing song");
        activity.play(item);
    }

}
