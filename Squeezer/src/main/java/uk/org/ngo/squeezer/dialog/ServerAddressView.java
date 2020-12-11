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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.util.ScanNetworkTask;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends LinearLayout implements ScanNetworkTask.ScanNetworkCallback {
    private Preferences preferences;
    private Preferences.ServerAddress serverAddress;

    private RadioButton squeezeNetworkButton;
    private RadioButton localServerButton;
    private EditText serverAddressEditText;
    private TextView serverName;
    private Spinner serversSpinner;
    private EditText userNameEditText;
    private EditText passwordEditText;
    private MaterialCheckBox wakeOnLan;
    private TextInputLayout macLayout;
    private boolean macDirty;
    private EditText macEditText;
    private View scanResults;
    private View scanProgress;

    private ScanNetworkTask scanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> discoveredServers;

    private ArrayAdapter<String> serversAdapter;

    public ServerAddressView(final Context context) {
        super(context);
        initialize(context);
    }

    public ServerAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(final Context context) {
        inflate(context, R.layout.server_address_view, this);
        if (!isInEditMode()) {
            preferences = new Preferences(context);
            serverAddress = preferences.getServerAddress();
            if (serverAddress.localAddress() == null) {
                Preferences.ServerAddress cliServerAddress = preferences.getCliServerAddress();
                if (cliServerAddress.localAddress() != null) {
                    serverAddress.setAddress(cliServerAddress.localHost());
                }
            }

            squeezeNetworkButton = findViewById(R.id.squeezeNetwork);
            localServerButton = findViewById(R.id.squeezeServer);

            serverAddressEditText = findViewById(R.id.server_address);
            userNameEditText = findViewById(R.id.username);
            passwordEditText = findViewById(R.id.password);

            wakeOnLan = findViewById(R.id.wol);
            wakeOnLan.setOnCheckedChangeListener((compoundButton, b) -> macLayout.setVisibility(b ? VISIBLE : GONE));
            macLayout = findViewById(R.id.mac_til);
            macEditText = findViewById(R.id.mac);
            macLayout.setEndIconOnClickListener(view -> {
                FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                InfoDialog.show(fragmentManager, R.string.settings_MAC_label, R.string.settings_MAC_info);
            });
            macLayout.setErrorIconOnClickListener(view -> {
                FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                InfoDialog.show(fragmentManager, R.string.settings_MAC_label, R.string.settings_MAC_info);
            });
            macEditText.setOnFocusChangeListener((view, b) -> {
                if (!b) {
                    checkMac();
                }
            });
            macEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (macDirty) {
                        macLayout.setError(Util.validateMac(editable.toString()) ? null : getResources().getString(R.string.settings_invalid_MAC));
                    }
                }
            });

            final OnClickListener onNetworkSelected = view -> setSqueezeNetwork(view.getId() == R.id.squeezeNetwork);
            squeezeNetworkButton.setOnClickListener(onNetworkSelected);
            localServerButton.setOnClickListener(onNetworkSelected);

            // Set up the servers spinner.
            serversAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
            serversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            serverName = findViewById(R.id.server_name);
            serversSpinner = findViewById(R.id.found_servers);
            serversSpinner.setAdapter(serversAdapter);

            scanResults = findViewById(R.id.scan_results);
            scanProgress = findViewById(R.id.scan_progress);
            scanProgress.setVisibility(GONE);
            TextView scanDisabledMessage = findViewById(R.id.scan_disabled_msg);

            setSqueezeNetwork(serverAddress.squeezeNetwork);
            setServerAddress(serverAddress.localAddress());

            // Only support network scanning on WiFi.
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
            if (isWifi) {
                scanDisabledMessage.setVisibility(GONE);
                startNetworkScan(context);
                Button scanButton = findViewById(R.id.scan_button);
                scanButton.setOnClickListener(v -> startNetworkScan(context));
            } else {
                scanResults.setVisibility(GONE);
            }
        }
    }

    private boolean checkMac() {
        macDirty = true;
        String mac = macEditText.getText().toString();
        boolean macOk = Util.validateMac(mac);
        macLayout.setError(macOk ? null : "Invalid MAC address");
        return macOk;
    }

    public boolean savePreferences() {
        if (wakeOnLan.isChecked() && !checkMac()) {
            return false;
        }

        serverAddress.squeezeNetwork = squeezeNetworkButton.isChecked();
        String address = serverAddressEditText.getText().toString();
        serverAddress.setAddress(address);
        serverAddress.setServerName(getServerName(address));
        serverAddress.userName = userNameEditText.getText().toString();
        serverAddress.password = passwordEditText.getText().toString();
        serverAddress.wakeOnLan = wakeOnLan.isChecked();
        serverAddress.mac = Util.parseMac(macEditText.getText().toString());
        preferences.saveServerAddress(serverAddress);

        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        // Stop scanning
        if (scanNetworkTask != null) {
            scanNetworkTask.cancel();
        }

        super.onDetachedFromWindow();
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan(Context context) {
        scanResults.setVisibility(GONE);
        scanProgress.setVisibility(VISIBLE);
        scanNetworkTask = new ScanNetworkTask(context, this);
        new Thread(scanNetworkTask).start();
    }

    /**
     * Called when server scanning has finished.
     * @param serverMap Discovered servers, key is the server name, value is the IP address.
     */
    public void onScanFinished(TreeMap<String, String> serverMap) {
        scanResults.setVisibility(VISIBLE);
        serverName.setVisibility(GONE);
        serversSpinner.setVisibility(GONE);
        scanProgress.setVisibility(GONE);
        serversAdapter.clear();

        if (scanNetworkTask == null) {
            return;
        }

        discoveredServers = serverMap;

        scanNetworkTask = null;

        if (discoveredServers.size() == 0) {
            // No servers found, manually enter address
            // Populate the edit text widget with current address stored in preferences.
            setServerAddress(serverAddress.localAddress());
            serverAddressEditText.setEnabled(true);
            serverName.setVisibility(VISIBLE);
        } else {
            // Show the spinner so the user can choose a server or to manually enter address.
            // Don't fire onItemSelected by calling notifyDataSetChanged and
            // setSelection(pos, false) before setting OnItemSelectedListener
            serversSpinner.setOnItemSelectedListener(null);

            for (Entry<String, String> e : discoveredServers.entrySet()) {
                serversAdapter.add(e.getKey());
            }
            serversAdapter.add(getContext().getString(R.string.settings_manual_server_addr));
            serversAdapter.notifyDataSetChanged();

            // First look the stored server name in the list of found servers
            String addressOfStoredServerName = discoveredServers.get(serverAddress.serverName());
            int position = getServerPosition(addressOfStoredServerName);

            // If that fails, look for the stored server address in the list of found servers
            if (position < 0) {
                position = getServerPosition(serverAddress.localAddress());
            }

            serversSpinner.setSelection((position < 0 ? serversAdapter.getCount() - 1 : position), false);
            serverAddressEditText.setEnabled(position < 0 && !serverAddress.squeezeNetwork);

            serversSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    String serverAddress = discoveredServers.get(serversAdapter.getItem(pos));
                    setSqueezeNetwork(false);
                    setServerAddress(serverAddress);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing.
                }
            });
            serversSpinner.setVisibility(VISIBLE);
        }
    }

    private void setSqueezeNetwork(boolean isSqueezeNetwork) {
        squeezeNetworkButton.setChecked(isSqueezeNetwork);
        localServerButton.setChecked(!isSqueezeNetwork);
        setEditServerAddressAvailability(isSqueezeNetwork);
        userNameEditText.setEnabled(!isSqueezeNetwork);
        passwordEditText.setEnabled(!isSqueezeNetwork);
        wakeOnLan.setEnabled(!isSqueezeNetwork);
        macEditText.setEnabled(!isSqueezeNetwork);
    }

    private void setServerAddress(String address) {
        serverAddress = preferences.getServerAddress(address);

        serverAddressEditText.setText(serverAddress.localAddress());
        userNameEditText.setText(serverAddress.userName);
        passwordEditText.setText(serverAddress.password);
        wakeOnLan.setChecked(serverAddress.wakeOnLan);
        macLayout.setVisibility(serverAddress.wakeOnLan ? VISIBLE : GONE);
        macEditText.setText(Util.formatMac(serverAddress.mac));
    }

    private void setEditServerAddressAvailability(boolean isSqueezeNetwork) {
        if (isSqueezeNetwork) {
            serverAddressEditText.setEnabled(false);
        } else if (serversAdapter.getCount() == 0) {
            serverAddressEditText.setEnabled(true);
        } else {
            serverAddressEditText.setEnabled(serversSpinner.getSelectedItemPosition() == serversSpinner.getCount() - 1);
        }
    }

    private String getServerName(String ipPort) {
        if (discoveredServers != null)
            for (Entry<String, String> entry : discoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }

    private int getServerPosition(String host) {
        if (host != null && discoveredServers != null) {
            int position = 0;
            for (Entry<String, String> entry : discoveredServers.entrySet()) {
                if (host.equals(entry.getValue()))
                    return position;
                position++;
            }
        }
        return -1;
    }

}
