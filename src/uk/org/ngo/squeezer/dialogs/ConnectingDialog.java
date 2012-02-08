package uk.org.ngo.squeezer.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SqueezerActivity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class ConnectingDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String connectingTo = args.getString("connectingTo");
        ProgressDialog connectingDialog = new ProgressDialog(getActivity());
        connectingDialog.setTitle(getText(R.string.connecting_text));
        connectingDialog.setIndeterminate(true);
        connectingDialog.setMessage(getString(R.string.connecting_to_text, connectingTo));
        return connectingDialog;
    }
    
    @Override
    public void onDismiss(android.content.DialogInterface dialog) {
        FragmentActivity activity = getActivity();
        if (activity != null)
            ((SqueezerActivity)activity).clearConnectingDialog();
    };

    public static ConnectingDialog addTo(FragmentActivity activity, String connectingTo) {
        ConnectingDialog dialog = new ConnectingDialog();
        Bundle args = new Bundle();
        args.putString("connectingTo", connectingTo);
        dialog.setArguments(args);
        try {
            dialog.show(activity.getSupportFragmentManager(), "ConnectingDialog");
        } catch (IllegalStateException e) {
            // Apparently we are not allowed to show the dialog at this point.
            Log.i("ConnectingDialog", "show() was not allowed: " + e);
            return null;
        }
        return dialog;
    }

}