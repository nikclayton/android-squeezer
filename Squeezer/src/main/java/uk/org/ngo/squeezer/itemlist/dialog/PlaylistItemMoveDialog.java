package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.model.Playlist;

public class PlaylistItemMoveDialog extends BaseEditTextDialog {

    private BaseListActivity<?> activity;

    private int fromIndex;

    private Playlist playlist;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (BaseListActivity<?>) getActivity();
        Bundle args = getArguments();
        fromIndex = args.getInt("fromIndex");
        playlist = args.getParcelable("playlist");
        dialog.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(R.string.move_to_index_hint);

        return dialog;
    }

    @Override
    protected boolean commit(String targetString) {
        int targetIndex = Util.parseDecimalInt(targetString, -1);
        if (targetIndex > 0 && targetIndex <= activity.getItemAdapter().getCount()) {
            if (playlist == null) {
                activity.getService().playlistMove(fromIndex - 1, targetIndex - 1);
            } else {
                activity.getService().playlistsMove(playlist, fromIndex - 1, targetIndex - 1);
            }
            activity.clearAndReOrderItems();
            return true;
        }
        return false;
    }

    public static void addTo(BaseListActivity<?> activity, int fromIndex) {
        PlaylistItemMoveDialog dialog = new PlaylistItemMoveDialog();
        Bundle args = new Bundle();
        args.putInt("fromIndex", fromIndex + 1);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

    public static void addTo(BaseListActivity<?> activity, Playlist playlist, int fromIndex) {
        PlaylistItemMoveDialog dialog = new PlaylistItemMoveDialog();
        Bundle args = new Bundle();
        args.putInt("fromIndex", fromIndex + 1);
        args.putParcelable("playlist", playlist);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

}
