package uk.org.ngo.squeezer.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.EditText;

import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;

public class AuthenticationDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final Preferences preferences = new Preferences(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        @SuppressLint({"InflateParams"}) // OK, as view is passed to AlertDialog.Builder.setView()
        View form = activity.getLayoutInflater().inflate(R.layout.authentication_dialog, null);
        builder.setView(form);

        final EditText userNameEditText = (EditText) form.findViewById(R.id.username);
        userNameEditText.setText(preferences.getUserName());

        final EditText passwordEditText = (EditText) form.findViewById(R.id.password);
        passwordEditText.setText(preferences.getPassword());

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                preferences.saveUserCredentials(userNameEditText.getText().toString(), passwordEditText.getText().toString());

                ((NowPlayingFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.now_playing_fragment)).startVisibleConnection();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

}
