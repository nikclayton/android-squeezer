/*
 * Copyright (c) 2019 Kurt Aaholst <kaaholst@gmail.com>
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

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseConfirmDialog;
import uk.org.ngo.squeezer.model.JiveItem;

public class DownloadDialog extends BaseConfirmDialog {
    private static final String TAG = DownloadDialog.class.getSimpleName();
    private static final String TITLE_KEY = "TITLE_KEY";

    public DownloadDialog(ConfirmDialogListener callback) {
        super(callback);
    }

    @Override
    protected String title() {
        return getString(R.string.download_item, getArguments().getString(TITLE_KEY));
    }

    @Override
    protected String okText() {
        return getString(R.string.DOWNLOAD);
    }

    @Override
    protected void onPersistChecked(boolean persist) {
        getDialog().getButton(DialogInterface.BUTTON_NEGATIVE).setText(persist ? R.string.disable_downloads : android.R.string.cancel);
    }

    public static DownloadDialog show(FragmentManager fragmentManager, JiveItem item, ConfirmDialogListener callback) {
        DownloadDialog dialog = new DownloadDialog(callback);

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, item.getName());
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
        return dialog;
    }
}
