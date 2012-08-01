package uk.org.ngo.squeezer.itemlists.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;

public class SqueezerPlaylistItemMoveDialog extends SqueezerBaseEditTextDialog {
    private SqueezerBaseListActivity<?> activity;
    private int fromIndex;
    private SqueezerPlaylist playlist;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (SqueezerBaseListActivity<?>) getActivity();
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

    public static void addTo(SqueezerBaseListActivity<?> activity, int fromIndex) {
        SqueezerPlaylistItemMoveDialog dialog = new SqueezerPlaylistItemMoveDialog();
        Bundle args = new Bundle();
        args.putInt("fromIndex", fromIndex+1);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

    public static void addTo(SqueezerBaseListActivity<?> activity, SqueezerPlaylist playlist, int fromIndex) {
        SqueezerPlaylistItemMoveDialog dialog = new SqueezerPlaylistItemMoveDialog();
        Bundle args = new Bundle();
        args.putInt("fromIndex", fromIndex+1);
        args.putParcelable("playlist", playlist);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "MoveDialog");
    }

}
