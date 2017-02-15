package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;

public class PlaylistsNewDialog extends BaseEditTextDialog {

    private PlaylistsActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (PlaylistsActivity) getActivity();
        dialog.setTitle(R.string.new_playlist_title);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(R.string.new_playlist_hint);

        return dialog;
    }

    @Override
    protected boolean commit(String name) {
        activity.getService().playlistsNew(name);
        activity.clearAndReOrderItems();
        return true;
    }

}
