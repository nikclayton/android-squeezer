package uk.org.ngo.squeezer.itemlist.action;

import android.util.Log;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public class AddAction extends PlayableItemAction {

    public AddAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        Log.d(getTag(), "Adding song to playlist");
        activity.add(item);
    }
}
