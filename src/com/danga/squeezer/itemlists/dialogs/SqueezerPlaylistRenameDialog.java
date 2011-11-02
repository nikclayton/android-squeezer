package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerPlaylistSongsActivity;

public class SqueezerPlaylistRenameDialog extends SqueezerBaseEditTextDialog {
    private final SqueezerPlaylistSongsActivity activity;
    
    private SqueezerPlaylistRenameDialog(SqueezerPlaylistSongsActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(getString(R.string.rename_title, activity.getPlaylist().getName()));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(activity.getPlaylist().getName());
        
        return dialog;
    }

    @Override
    protected boolean commit(String newname) {
        try {
            activity.setOldname(activity.getPlaylist().getName());
            activity.getService().playlistsRename(activity.getPlaylist(), newname);
            activity.getPlaylist().setName(newname);
            activity.getItemAdapter().notifyDataSetChanged();
        } catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
        }
        return true;
    }

    
    public static void addTo(SqueezerPlaylistSongsActivity activity) {
        SqueezerPlaylistRenameDialog dialog = new SqueezerPlaylistRenameDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "RenameDialog");
    }

}
