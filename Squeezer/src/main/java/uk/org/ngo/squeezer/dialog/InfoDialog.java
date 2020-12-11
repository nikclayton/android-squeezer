/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class InfoDialog extends DialogFragment {
    private static final String TAG = DialogFragment.class.getSimpleName();
    private static final String TITLE_RESOURCE_KEY = "TITLE_RESOURCE_KEY";
    private static final String TEXT_RESOURCE_KEY = "TEXT_RESOURCE_KEY";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        if (getArguments().getInt(TITLE_RESOURCE_KEY) != 0) {
            builder.setTitle(getArguments().getInt(TITLE_RESOURCE_KEY));
        }
        builder.setMessage(Html.fromHtml(getString(getArguments().getInt(TEXT_RESOURCE_KEY))));
        builder.setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }

    public static InfoDialog show(FragmentManager fragmentManager, @StringRes int textResourceId) {
        return show(fragmentManager, 0, textResourceId);
    }

    public static InfoDialog show(FragmentManager fragmentManager, @StringRes int titleResourceId, @StringRes int textResourceId) {
        // Remove any currently showing dialog
        Fragment prev = fragmentManager.findFragmentByTag(TAG);
        if (prev != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(prev);
            fragmentTransaction.commit();
        }

        // Create and show the dialog
        InfoDialog dialog = new InfoDialog();

        Bundle args = new Bundle();
        args.putInt(TITLE_RESOURCE_KEY, titleResourceId);
        args.putInt(TEXT_RESOURCE_KEY, textResourceId);
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
        return dialog;
    }
}
