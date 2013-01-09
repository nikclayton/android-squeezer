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

package uk.org.ngo.squeezer.dialogs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.acra.ErrorReporter;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.util.UIUtils;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Shows a preference dialog that allows the user to scan the local network
 * for servers, choose a server from the results of the scan, or enter the
 * name/address of a server directly.
 */
public class ServerAddressPreference extends DialogPreference {
    private EditText mServerAddressEditText;
    private Button mScanBtn;
    private Spinner mServersSpinner;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;

    private ArrayAdapter<String> mServersAdapter;

    private final Context mContext;
    private ProgressDialog mProgressDialog;

    public ServerAddressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setDialogLayoutResource(R.layout.server_address_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mServerAddressEditText = (EditText) view.findViewById(R.id.server_address);
        mScanBtn = (Button) view.findViewById(R.id.scan_btn);
        mServersSpinner = (Spinner) view.findViewById(R.id.found_servers);

        // If there's no server address configured then set the default text
        // in the edit box to our IP address, trimmed of the last octet.
        String serveraddr = getPersistedString("");

        if (serveraddr.length() == 0) {
            WifiManager mWifiManager =
                    (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();

            String ipstr = Formatter.formatIpAddress(mWifiInfo.getIpAddress());
            int i = ipstr.lastIndexOf(".");
            serveraddr = ipstr.substring(0, ++i);
        }

        // Move the cursor to the end of the address.
        mServerAddressEditText.setText(serveraddr);
        mServerAddressEditText.setSelection(serveraddr.length());

        // Set up the servers spinner.
        mServersAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item);
        mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        // Only support network scanning on WiFi.
        //
        // Seen a crash in previous versions of this code that suggests that
        // getActiveNetworkInfo() can return null, so play safe here.
        ConnectivityManager cm = (ConnectivityManager) Squeezer.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
            mScanBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    startNetworkScan();
                }
            });
        } else {
            TextView scan_msg = (TextView) view.findViewById(R.id.scan_msg);
            scan_msg.setText(mContext.getText(R.string.settings_server_scanning_disabled_msg));
            mScanBtn.setEnabled(false);
        }
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan() {
        mServersSpinner.setVisibility(View.GONE);
        createProgressDialog();
        mScanNetworkTask = new ScanNetworkTask(mContext, this);
        mScanNetworkTask.execute();
    }

    /**
     * Creates and shows a ProgressDialog while network scanning is happening.
     */
    @TargetApi(11)
    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(5); // See MAX_DISCOVERY_ATTEMPTS
        mProgressDialog.setSecondaryProgress(0);
        mProgressDialog.setMessage(mContext.getString(R.string.settings_server_scan_progress));

        mProgressDialog.show();
        if (UIUtils.hasHoneycomb()) {
            mProgressDialog.setProgressNumberFormat(null);
            mProgressDialog.setProgressPercentFormat(null);
        }
    }

    /**
     * Updates the progress bar.
     */
    private void updateProgress(int progress, int secondaryProgress) {
        mProgressDialog.setProgress(progress);
        mProgressDialog.setSecondaryProgress(secondaryProgress);
    }

    /**
     * Called when server scanning has finished.
     */
    void onScanFinished() {
        mProgressDialog.dismiss();
        mDiscoveredServers = mScanNetworkTask.getDiscoveredServers();
        mScanNetworkTask = null;

        switch (mDiscoveredServers.size()) {
            case 0:
                // Do nothing, no servers found.
                break;

            case 1:
                // Populate the edit text widget with the address found.
                mServerAddressEditText
                        .setText(mDiscoveredServers.get(mDiscoveredServers.firstKey()));
                break;

            default:
                // Show the spinner so the user can choose a server.
                mServersAdapter.clear();
                for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    mServersAdapter.add(e.getKey());
                }
                mServersSpinner.setVisibility(View.VISIBLE);
                mServersSpinner.setAdapter(mServersAdapter);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel(true);
            mProgressDialog.dismiss();
        }

        if (positiveResult) {
            String addr = mServerAddressEditText.getText().toString();
            persistString(addr);
            callChangeListener(addr);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /**
         * The preference dialog is being destroyed, probably because the user
         * has rotated the device, so hide the progress dialog and cancel the
         * scanning task, if necessary.
         */
        if (mScanNetworkTask != null) {
            mProgressDialog.dismiss();
            mScanNetworkTask.cancel(true);
        }

        return super.onSaveInstanceState();
    }

    /**
     * Inserts the selected address in to the edittext widget.
     */
    public class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent,
                View view, int pos, long id) {
            mServerAddressEditText.setText(mDiscoveredServers.get(parent.getItemAtPosition(pos)
                    .toString()));
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    /**
     * Scans the local network for servers.
     */
    static class ScanNetworkTask extends AsyncTask<Void, Integer, Void> {
        private final String TAG = "scanNetworkTask";

        private final Context mContext;
        private final ServerAddressPreference mPref;

        /** Map server names to IP addresses. */
        private final TreeMap<String, String> mServerMap = new TreeMap<String, String>();

        /** UDP port to broadcast discovery requests to. */
        private final int DISCOVERY_PORT = 3483;

        /** Maximum number of discovery attempts. */
        public final int MAX_DISCOVERY_ATTEMPTS = 5;

        /** Maximum time to wait between discovery attempts (ms). */
        private final int DISCOVERY_ATTEMPT_TIMEOUT = 1000;

        ScanNetworkTask(Context context, ServerAddressPreference pref) {
            mContext = context;
            mPref = pref;
        }

        /**
         * Discover Squeezerservers on the local network.
         * <p>
         * Do this by sending MAX_DISCOVERY_ATTEMPT UDP broadcasts to port 3483
         * at approximately DISCOVERY_ATTEMPT_TIMEOUT intervals. Squeezeservers
         * are supposed to listen for this, and respond with a packet that
         * starts 'E' and some information about the server, including its name.
         * <p>
         * Map the name to an IP address and store in mDiscoveredServers for
         * later use.
         * <p>
         * See the Slim::Networking::Discovery module in Squeezeserver for more
         * details.
         */
        @Override
        protected Void doInBackground(Void... unused) {
            WifiManager.WifiLock wifiLock = null;
            DatagramSocket socket = null;

            // UDP broadcast data that causes Squeezeservers to reply. The
            // format is 'e', followed by null-terminated tags that indicate the
            // data to return.
            //
            // The Squeezeserver uses the size of the request packet to
            // determine the size of the response packet.

            byte[] request = {
                    'e', // 'existence' ?
                    'I', 'P', 'A', 'D', 0, // Include IP address
                    'N', 'A', 'M', 'E', 0, // Include server name
                    'J', 'S', 'O', 'N', 0, // Include server port
            };
            byte[] data = new byte[512];
            System.arraycopy(request, 0, data, 0, request.length);

            WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            wifiLock = wm.createWifiLock(TAG);

            // mServerMap.put("Dummy", "127.0.0.1");

            Log.v(TAG, "Locking WiFi while scanning");
            wifiLock.acquire();

            try {
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                boolean timedOut;
                socket = new DatagramSocket();
                DatagramPacket discoveryPacket = new DatagramPacket(data, data.length,
                        broadcastAddr, DISCOVERY_PORT);

                byte[] buf = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);

                socket.setSoTimeout(DISCOVERY_ATTEMPT_TIMEOUT);

                for (int attempt = 0; attempt < MAX_DISCOVERY_ATTEMPTS; attempt++) {
                    if (isCancelled()) {
                        break;
                    }

                    timedOut = false;

                    socket.send(discoveryPacket);

                    try {
                        socket.receive(responsePacket);
                    } catch (IOException e) {
                        timedOut = true;
                    }

                    if (!timedOut) {
                        if (buf[0] == (byte) 'E') {
                            String serverAddr = responsePacket.getAddress().getHostAddress();

                            // Blocks of data are TAG/LENGTH/VALUE, where TAG is
                            // a 4 byte string identifying the item, LENGTH is
                            // the length of the VALUE (e.g., reading \t means the
                            // value is 9 bytes, and VALUE is the actual value.

                            // Find the 'NAME' block
                            int i = 1;
                            while (i < buf.length && buf[i] != 'N') {
                                i += 4;
                                i += buf[i] + 2;
                            }

                            // Now at first block that starts 'N', should be
                            // NAME.
                            i += 4;

                            // i now pointing at the length of the NAME value.
                            String name = new String(buf, i + 1, buf[i]);

                            mServerMap.put(name, serverAddr);
                        }
                    }

                    publishProgress(attempt);
                }
            } catch (SocketException e) {
                // new DatagramSocket(3483)
                ErrorReporter.getInstance().handleException(e);
            } catch (UnknownHostException e) {
                // InetAddress.getByName()
                ErrorReporter.getInstance().handleException(e);
            } catch (IOException e) {
                // socket.send()
                ErrorReporter.getInstance().handleException(e);
            }

            if (socket != null)
                socket.close();

            Log.v(TAG, "Scanning complete, unlocking WiFi");
            if (wifiLock != null)
                wifiLock.release();

            // For testing that multiple servers are handled correctly.
            // mServerMap.put("Dummy 2", "127.0.0.2");
            return null;
        }

        /**
         * Update the progress bar. The main progress value corresponds to how
         * many servers have been discovered, the secondary progress value
         * corresponds to how far through the discovery process we are.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            mPref.updateProgress(Math.min(mServerMap.size(), 5), values[0].intValue());
        }

        @Override
        protected void onCancelled(Void result) {
            mPref.onScanFinished();
        }

        @Override
        protected void onPostExecute(Void result) {
            mPref.onScanFinished();
        }

        private TreeMap<String, String> getDiscoveredServers() {
            return mServerMap;
        }
    }
}
