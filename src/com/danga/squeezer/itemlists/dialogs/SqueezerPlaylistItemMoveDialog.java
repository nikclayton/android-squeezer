package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

import com.danga.squeezer.R;
import com.danga.squeezer.Util;
import com.danga.squeezer.itemlists.SqueezerAbstractSongListActivity;
import com.danga.squeezer.model.SqueezerPlaylist;

public class SqueezerPlaylistItemMoveDialog extends SqueezerBaseEditTextDialog {
    private final SqueezerAbstractSongListActivity activity;
    private int fromIndex;
    private SqueezerPlaylist playlist;
    
    private SqueezerPlaylistItemMoveDialog(SqueezerAbstractSongListActivity activity, SqueezerPlaylist playlist, int fromIndex) {
        this.activity = activity;
        this.playlist = playlist;
        this.fromIndex = fromIndex + 1;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(R.string.move_to_index_hint);

        return dialog;
    }
    
    @Override
    protected boolean commit(String targetString) {
        int targetIndex = Util.parseDecimalInt(targetString, -1);
        if (targetIndex > 0 && targetIndex <= activity.getItemAdapter().getCount()) {
            try {
                if (playlist == null)
                    activity.getService().playlistMove(fromIndex-1, targetIndex-1);
                else
                    activity.getService().playlistsMove(playlist, fromIndex-1, targetIndex-1);
                activity.orderItems();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error moving song from '"+ fromIndex + "' to '" +targetIndex + "': " + e);
            }
            return true;
        }
        return false;
    }

    public static void addTo(SqueezerAbstractSongListActivity activity, int fromIndex) {
        SqueezerPlaylistItemMoveDialog dialog = new SqueezerPlaylistItemMoveDialog(activity, null, fromIndex);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

    public static void addTo(SqueezerAbstractSongListActivity activity, SqueezerPlaylist playlist, int fromIndex) {
        SqueezerPlaylistItemMoveDialog dialog = new SqueezerPlaylistItemMoveDialog(activity, playlist, fromIndex);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

}
