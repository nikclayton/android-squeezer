package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerPlaylistsActivity;

public class SqueezerPlaylistsRenameDialog extends SqueezerBaseEditTextDialog {
    private final SqueezerPlaylistsActivity activity;
    
    private SqueezerPlaylistsRenameDialog(SqueezerPlaylistsActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(getString(R.string.rename_title, activity.getCurrentPlaylist().getName()));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(activity.getCurrentPlaylist().getName());
        
        return dialog;
    }

    @Override
    protected boolean commit(String newname) {
        try {
            activity.setOldname(activity.getCurrentPlaylist().getName());
            activity.getService().playlistsRename(activity.getCurrentPlaylist(), newname);
            activity.getCurrentPlaylist().setName(newname);
            activity.getItemAdapter().notifyDataSetChanged();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
        }
        return true;
    }

    
    public static void addTo(SqueezerPlaylistsActivity activity) {
        SqueezerPlaylistsRenameDialog dialog = new SqueezerPlaylistsRenameDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "RenameDialog");
    }

}
