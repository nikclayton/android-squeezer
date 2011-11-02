package com.danga.squeezer.itemlists.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

import com.danga.squeezer.R;

public abstract class SqueezerBaseFilterDialog extends DialogFragment {
    protected View filterForm;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        filterForm = getActivity().getLayoutInflater().inflate(R.layout.filter_dialog, null);
        builder.setTitle(R.string.menu_item_filter);
        builder.setView(filterForm);

        EditText editText = (EditText) filterForm.findViewById(R.id.search_string);
        editText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    filter();
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                filter();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
    
    protected abstract void filter();

}
