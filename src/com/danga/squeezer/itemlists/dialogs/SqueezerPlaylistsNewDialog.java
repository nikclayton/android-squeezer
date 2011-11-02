package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerPlaylistsActivity;

public class SqueezerPlaylistsNewDialog extends SqueezerBaseEditTextDialog {
    private final SqueezerPlaylistsActivity activity;
    
    private SqueezerPlaylistsNewDialog(SqueezerPlaylistsActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(R.string.new_playlist_title);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(R.string.new_playlist_hint);

        return dialog;
    }

    @Override
    protected boolean commit(String name) {
        try {
            activity.getService().playlistsNew(name);
            activity.orderItems();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error saving playlist as '"+ name + "': " + e);
        }
        return true;
    }

    
    public static void addTo(SqueezerPlaylistsActivity activity) {
        SqueezerPlaylistsNewDialog dialog = new SqueezerPlaylistsNewDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "NewPlaylistDialog");
    }

}
