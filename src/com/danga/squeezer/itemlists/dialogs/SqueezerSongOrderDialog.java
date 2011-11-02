package com.danga.squeezer.itemlists.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerSongListActivity;

public class SqueezerSongOrderDialog extends DialogFragment {
    private final SqueezerSongListActivity activity;
    
    private SqueezerSongOrderDialog(SqueezerSongListActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] sortOrderStrings = new String[SongsSortOrder.values().length];
        sortOrderStrings[SongsSortOrder.title.ordinal()] = getString(R.string.songs_sort_order_title);
        sortOrderStrings[SongsSortOrder.tracknum.ordinal()] = getString(R.string.songs_sort_order_tracknum);
        int checkedItem = activity.getSortOrder().ordinal();
        builder.setTitle(getString(R.string.choose_sort_order, activity.getItemAdapter().getQuantityString(2)));
        builder.setSingleChoiceItems(sortOrderStrings, checkedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int indexSelected) {
                    activity.setSortOrder(SongsSortOrder.values()[indexSelected]);
                    dialog.dismiss();
                }
            });
        return builder.create();
    }
    

    public static void addTo(SqueezerSongListActivity activity) {
        SqueezerSongOrderDialog dialog = new SqueezerSongOrderDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "OrderDialog");
    }

    public enum SongsSortOrder {
        title,
        tracknum;
    }

}
