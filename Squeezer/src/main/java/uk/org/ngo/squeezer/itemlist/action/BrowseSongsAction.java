package uk.org.ngo.squeezer.itemlist.action;

import android.util.Log;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.model.Artist;

public class BrowseSongsAction extends PlayableItemAction {

    public BrowseSongsAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        Log.d(getTag(), "Browsing songs of " + item);
        Artist artist = null;
        if (activity instanceof AlbumListActivity) {
           artist = ((AlbumListActivity)activity).getArtist();
        }
        if (artist != null)
            SongListActivity.show(activity, item, artist);
        else
            SongListActivity.show(activity, item);
    }

}
