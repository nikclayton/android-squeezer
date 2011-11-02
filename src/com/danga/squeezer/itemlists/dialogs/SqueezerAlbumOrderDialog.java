package com.danga.squeezer.itemlists.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerAlbumListActivity;

public class SqueezerAlbumOrderDialog extends DialogFragment {
    private final SqueezerAlbumListActivity activity;
    
    private SqueezerAlbumOrderDialog(SqueezerAlbumListActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] sortOrderStrings = new String[AlbumsSortOrder.values().length];
        sortOrderStrings[AlbumsSortOrder.album.ordinal()] = getString(R.string.albums_sort_order_album);
        sortOrderStrings[AlbumsSortOrder.artflow.ordinal()] = getString(R.string.albums_sort_order_artflow);
        sortOrderStrings[AlbumsSortOrder.__new.ordinal()] = getString(R.string.albums_sort_order_new);
        int checkedItem = activity.getSortOrder().ordinal();
        builder.setTitle(getString(R.string.choose_sort_order, activity.getItemAdapter().getQuantityString(2)));
        builder.setSingleChoiceItems(sortOrderStrings, checkedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int indexSelected) {
                    activity.setSortOrder(AlbumsSortOrder.values()[indexSelected]);
                    dialog.dismiss();
                }
            });
        return builder.create();
    }
    

    public static void addTo(SqueezerAlbumListActivity activity) {
        SqueezerAlbumOrderDialog dialog = new SqueezerAlbumOrderDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "OrderDialog");
    }


    public enum AlbumsSortOrder {
        album,
        artflow,
        __new;
    }

}
