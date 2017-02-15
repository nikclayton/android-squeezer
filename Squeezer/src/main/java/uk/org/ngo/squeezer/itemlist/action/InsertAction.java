package uk.org.ngo.squeezer.itemlist.action;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public class InsertAction extends PlayableItemAction {

    public InsertAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        activity.insert(item);
    }

}
