package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlayerListActivity;

public class PlayerRenameDialog extends BaseEditTextDialog {

    private PlayerListActivity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (PlayerListActivity) getActivity();
        dialog.setTitle(getString(R.string.rename_title, activity.getCurrentPlayer().getName()));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(activity.getCurrentPlayer().getName());

        return dialog;
    }

    @Override
    protected boolean commit(String newName) {
        activity.playerRename(newName);
        return true;
    }

}
