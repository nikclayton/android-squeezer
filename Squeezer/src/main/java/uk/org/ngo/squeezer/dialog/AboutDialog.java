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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import de.cketti.library.changelog.ChangeLog;
import uk.org.ngo.squeezer.R;

public class AboutDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint({"InflateParams"}) // OK, as view is passed to AlertDialog.Builder.setView()
        final View view = getActivity().getLayoutInflater().inflate(R.layout.about_dialog, null);
        final TextView titleText = (TextView) view.findViewById(R.id.about_title);
        final TextView versionText = (TextView) view.findViewById(R.id.version_text);

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo info;
        try {
            info = pm.getPackageInfo(getActivity().getPackageName(), 0);
            versionText.setText(info.versionName);
        } catch (NameNotFoundException e) {
            titleText.setText(getString(R.string.app_name));
        }

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.changelog_full_title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ChangeLog changeLog = new ChangeLog(getActivity());
                changeLog.getFullLogDialog().show();
            }
        });
        builder.setNegativeButton(R.string.dialog_license, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new LicenseDialog()
                        .show(getActivity().getSupportFragmentManager(), "LicenseDialog");
            }
        });
        return builder.create();
    }
}
