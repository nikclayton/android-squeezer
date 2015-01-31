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

import android.content.Context;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;

/**
 * Shows a preference dialog that allows the user to enable party mode, and choose an
 * optional pin to disable party mode.
 */
public class PartyModePin extends DialogPreference {

    private EditText passwordEditText;
    private boolean previousPartyModeEnabled;
    private Preferences preferences;

    public PartyModePin(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.party_mode_pin);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        preferences = new Preferences(getContext());
        passwordEditText = (EditText) view.findViewById(R.id.partypin);

        previousPartyModeEnabled = preferences.getPartyModeStatus();

        TextView statusText = (TextView) view.findViewById(R.id.PartyStatus);
        if (previousPartyModeEnabled) {
            statusText.setText("Disable Party Mode:");
        } else {
            statusText.setText("Enable Party Mode:");
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        // If the user has pressed ok
        if (positiveResult) {

            // If party mode wasn't enabled before
            if (!previousPartyModeEnabled) {
                String pin = passwordEditText.getText().toString();
                preferences.setPartyModePin(pin);
                preferences.setPartyModeStatus(true);

            } else {
                String storedPin = preferences.getPartyModePin(null);
                String pin = passwordEditText.getText().toString();

                if (storedPin == null || pin.equals(preferences.getPartyModePin())) {
                    preferences.setPartyModeStatus(false);
                }
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }
}
