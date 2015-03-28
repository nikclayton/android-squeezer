/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public final class Preferences {

    public static final String NAME = "Squeezer";

    // e.g. "10.0.0.81:9090"
    public static final String KEY_SERVER_ADDRESS = "squeezer.serveraddr";

    // Optional Squeezebox Server name
    private static final String KEY_SERVER_NAME = "squeezer.server_name";

    // Optional Squeezebox Server user name
    private static final String KEY_USERNAME = "squeezer.username";

    // Optional Squeezebox Server password
    private static final String KEY_PASSWORD = "squeezer.password";

    // The playerId that we were last connected to. e.g. "00:04:20:17:04:7f"
    public static final String KEY_LAST_PLAYER = "squeezer.lastplayer";

    // Do we automatically try and connect on WiFi availability?
    public static final String KEY_AUTO_CONNECT = "squeezer.autoconnect";

    // Do we keep the notification going at top, even when we're not connected?
    public static final String KEY_NOTIFY_OF_CONNECTION = "squeezer.notifyofconnection";

    // Do we scrobble track information?
    // Deprecated, retained for compatibility when upgrading. Was an int, of
    // either 0 == No scrobbling, 1 == use ScrobbleDroid API, 2 == use SLS API
    public static final String KEY_SCROBBLE = "squeezer.scrobble";

    // Do we scrobble track information (if a scrobble service is available)?
    //
    // Type of underlying preference is bool / CheckBox
    public static final String KEY_SCROBBLE_ENABLED = "squeezer.scrobble.enabled";

    // Do we send anonymous usage statistics?
    public static final String KEY_ANALYTICS_ENABLED = "squeezer.analytics.enabled";

    // Fade-in period? (0 = disable fade-in)
    public static final String KEY_FADE_IN_SECS = "squeezer.fadeInSecs";

    // Are we having a party?
    public static final String KEY_PARTYMODE_ENABLED = "squeezer.partymode.enabled";

    // Pin to disable the party mode
    public static final String KEY_PARTYMODE_PIN = "squeezer.partymode.pin";

    // What do to when an album is selected in the list view
    public static final String KEY_ON_SELECT_ALBUM_ACTION = "squeezer.action.onselect.album";

    // What do to when a song is selected in the list view
    public static final String KEY_ON_SELECT_SONG_ACTION = "squeezer.action.onselect.song";

    // Preferred album list layout.
    public static final String KEY_ALBUM_LIST_LAYOUT = "squeezer.album.list.layout";

    // Preferred song list layout.
    public static final String KEY_SONG_LIST_LAYOUT = "squeezer.song.list.layout";

    // Start SqueezePlayer automatically if installed.
    public static final String KEY_SQUEEZEPLAYER_ENABLED = "squeezer.squeezeplayer.enabled";

    // Preferred UI theme.
    public static final String KEY_ON_THEME_SELECT_ACTION = "squeezer.theme";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public Preferences(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
    }

    public Boolean getPartyModeStatus() {
        return sharedPreferences.getBoolean(Preferences.KEY_PARTYMODE_ENABLED, true);
    }

    public void setPartyModeStatus(Boolean status){
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(Preferences.KEY_PARTYMODE_ENABLED, status);
        editor.commit();
    }

    public String getPartyModePin() {
        return getPartyModePin(null);
    }

    public String getPartyModePin(String defaultValue) {
        return getStringPreference(Preferences.KEY_PARTYMODE_PIN, defaultValue);
    }

    public void setPartyModePin (String password){
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(Preferences.KEY_PARTYMODE_PIN, password);
        editor.commit();
    }

    private String getStringPreference(String preference, String defaultValue) {
        final String pref = sharedPreferences.getString(preference, null);
        if (pref == null || pref.length() == 0) {
            return defaultValue;
        }
        return pref;
    }

    public ServerAddress getServerAddress() {
        ServerAddress serverAddress = new ServerAddress();

        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        serverAddress.bssId = (connectionInfo != null ? connectionInfo.getBSSID() : null);
        if (serverAddress.bssId != null)
            serverAddress.address = getStringPreference(KEY_SERVER_ADDRESS + "_" + serverAddress.bssId, null);
        if (serverAddress.address == null)
            serverAddress.address = getStringPreference(KEY_SERVER_ADDRESS, null);

        return serverAddress;
    }

    public static class ServerAddress {
        public String bssId;
        public String address; // <host name or ip>:<port>

        @Override
        public String toString() {
            return (bssId != null ? bssId + "_ " : "") + address + "_";
        }
    }

    public ServerAddress saveServerAddress(String address) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        String bssId = (connectionInfo != null ? connectionInfo.getBSSID() : null);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(bssId != null ? KEY_SERVER_ADDRESS + "_" + bssId : KEY_SERVER_ADDRESS, address);
        editor.commit();

        ServerAddress serverAddress = new ServerAddress();
        serverAddress.bssId = bssId;
        serverAddress.address = address;

        return serverAddress;
    }

    public String getServerName() {
        return getServerName(getServerAddress());
    }

    public String getServerName(ServerAddress serverAddress) {
        String serverName = getStringPreference(serverAddress + KEY_SERVER_NAME, null);
        return serverName != null ? serverName : serverAddress.address;
    }

    public void saveServerName(ServerAddress serverAddress, String serverName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(serverAddress + KEY_SERVER_NAME, serverName);
        editor.commit();
    }

    public String getUserName(ServerAddress serverAddress) {
        return getUserName(serverAddress, null);
    }

    public String getUserName(ServerAddress serverAddress, String defaultValue) {
        return getStringPreference(serverAddress + KEY_USERNAME, defaultValue);
    }

    public String getPassword(ServerAddress serverAddress) {
        return getPassword(serverAddress, null);
    }

    public String getPassword(ServerAddress serverAddress, String defaultValue) {
        return getStringPreference(serverAddress + KEY_PASSWORD, defaultValue);
    }

    public void saveUserCredentials(ServerAddress serverAddress, String userName, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(serverAddress + KEY_USERNAME, userName);
        editor.putString(serverAddress + KEY_PASSWORD, password);
        editor.commit();
    }

    public String getTheme() {
        return getStringPreference(KEY_ON_THEME_SELECT_ACTION, null);
    }

    public boolean isAutoConnect() {
        return sharedPreferences.getBoolean(KEY_AUTO_CONNECT, true);
    }

    public boolean controlSqueezePlayer() {
        return sharedPreferences.getBoolean(KEY_SQUEEZEPLAYER_ENABLED, true);
    }

}
