/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * A dialog for displaying networking error messages received from the server.
 * <p/>
 * Activities that host this dialog must implement
 * {@link NetworkErrorDialogFragment.NetworkErrorDialogListener} to
 * be notified when the user dismisses the dialog.
 * <p/>
 * To easily create the dialog displaying a given message call {@link #newInstance(String)} with
 * the message to display.
 */
public class NetworkErrorDialogFragment extends DialogFragment {
    /** Key used to store the message in the arguments bundle. */
    private static final String MESSAGE_KEY = "message";

    /** The activity that hosts this dialog. */
    private NetworkErrorDialogListener mListener;

    /**
     * Activities hosting this dialog must implement this interface in order to receive
     * notifications when the user dismisses the dialog.
     */
    public interface NetworkErrorDialogListener {

        /**
         * The user has dismissed the dialog. Either by clicking the OK button, or by pressing
         * the "Back" button.
         *
         * @param dialog The dialog that has been dismissed.
         */
        public void onDialogDismissed(DialogInterface dialog);
    }

    /**
     * Static factory method for creating an instance that will display the given message.
     *
     * @param message The message to display in the dialog.
     * @return The created dialog fragment.
     */
    @NonNull
    public static NetworkErrorDialogFragment newInstance(@NonNull String message) {
        NetworkErrorDialogFragment fragment = new NetworkErrorDialogFragment();

        Bundle args = new Bundle();
        args.putString(MESSAGE_KEY, message);
        fragment.setArguments(args);

        return fragment;
    }

    // Ensure that the containing activity implements NetworkErrorDialogListener.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (NetworkErrorDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement NetworkErrorDialogListener");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString(MESSAGE_KEY);
        if (message == null) {
            message = "No message provided.";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message).setPositiveButton(android.R.string.ok, null);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mListener.onDialogDismissed(dialog);
    }
}
