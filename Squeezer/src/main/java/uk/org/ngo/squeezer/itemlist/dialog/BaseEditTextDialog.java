package uk.org.ngo.squeezer.itemlist.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

import uk.org.ngo.squeezer.R;

public abstract class BaseEditTextDialog extends DialogFragment {

    protected EditText editText;

    abstract protected boolean commit(String string);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint({"InflateParams"}) // OK, as view is passed to AlertDialog.Builder.setView()
        View form = getActivity().getLayoutInflater().inflate(R.layout.edittext_dialog, null);
        builder.setView(form);
        editText = (EditText) form.findViewById(R.id.edittext);

        editText.setText("");
        editText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode
                        == KeyEvent.KEYCODE_ENTER)) {
                    if (commit(editText.getText().toString())) {
                        dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                commit(editText.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

}
