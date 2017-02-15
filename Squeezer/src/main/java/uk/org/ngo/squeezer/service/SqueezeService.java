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

package uk.org.ngo.squeezer.service;

import org.acra.ACRA;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.FilterItem;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.IServiceCurrentPlaylistCallback;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SongViewDialog;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.PlayerState.PlayStatus;
import uk.org.ngo.squeezer.model.PlayerState.RepeatStatus;
import uk.org.ngo.squeezer.model.PlayerState.ShuffleStatus;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.util.Scrobble;


public class SqueezeService extends Service implements ServiceCallbackList.ServicePublisher {

    private static final String TAG = "SqueezeService";

    private static final int PLAYBACKSERVICE_STATUS = 1;

    private static final String ALBUMTAGS = "alyj";

    /**
     * Information that will be requested about songs.
     * <p/>
     * a: artist name<br/>
     * C: compilation (1 if true, missing otherwise)<br/>
     * d: duration, in seconds<br/>
     * e: album ID<br/>
     * j: coverart (1 if available, missing otherwise)<br/>
     * J: artwork_track_id (if available, missing otherwise)<br/>
     * K: URL to remote artwork<br/>
     * l: album name<br/>
     * s: artist id<br/>
     * t: tracknum, if known<br/>
     * x: 1, if this is a remote track<br/>
     * y: song year<br/>
     * u: Song file url
     */
    // This should probably be a field in Song.
    private static final String SONGTAGS = "aCdejJKlstxyu";

    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    Thread mainThread;

    private boolean mHandshakeComplete = false;

    /** Keeps track of all subscriptions, so we can cancel all subscriptions for a client at once */
    final Map<ServiceCallback, ServiceCallbackList> callbacks = new ConcurrentHashMap<ServiceCallback, ServiceCallbackList>();

    @Override
    public void addClient(ServiceCallbackList callbackList, ServiceCallback item) {
        callbacks.put(item, callbackList);
    }

    @Override
    public void removeClient(ServiceCallback item) {
        callbacks.remove(item);
    }

    final ServiceCallbackList<IServiceCallback> mServiceCallbacks
            = new ServiceCallbackList<IServiceCallback>(this);

    final ServiceCallbackList<IServiceConnectionCallback> mConnectionCallbacks
            = new ServiceCallbackList<IServiceConnectionCallback>(this);

    final ServiceCallbackList<IServicePlayersCallback> mPlayersCallbacks
            = new ServiceCallbackList<IServicePlayersCallback>(this);

    final ServiceCallbackList<IServiceVolumeCallback> mVolumeCallbacks
            = new ServiceCallbackList<IServiceVolumeCallback>(this);

    final ServiceCallbackList<IServiceCurrentPlaylistCallback> mCurrentPlaylistCallbacks
            = new ServiceCallbackList<IServiceCurrentPlaylistCallback>(this);

    final ServiceCallbackList<IServiceMusicChangedCallback> mMusicChangedCallbacks
            = new ServiceCallbackList<IServiceMusicChangedCallback>(this);

    final ServiceCallbackList<IServiceHandshakeCallback> mHandshakeCallbacks
            = new ServiceCallbackList<IServiceHandshakeCallback>(this);

    final ServiceCallbackList<IServicePlaylistMaintenanceCallback> playlistMaintenanceCallbacks
            = new ServiceCallbackList<IServicePlaylistMaintenanceCallback>(this);

    final ServiceCallbackList<IServicePlayerStateCallback> mPlayerStateCallbacks
            = new ServiceCallbackList<IServicePlayerStateCallback>(this);


    final ConnectionState connectionState = new ConnectionState();

    PlayerState playerState = new PlayerState();

    final CliClient cli = new CliClient(this);

    /**
     * Is scrobbling enabled?
     */
    private boolean scrobblingEnabled;

    /**
     * Was scrobbling enabled?
     */
    private boolean scrobblingPreviouslyEnabled;

    boolean mUpdateOngoingNotification;

    int mFadeInSecs;

    @Override
    public void onCreate() {
        super.onCreate();

        // Get the main thread
        mainThread = Thread.currentThread();

        // Clear leftover notification in case this service previously got killed while playing
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
        connectionState
                .setWifiLock(((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(
                        WifiManager.WIFI_MODE_FULL, "Squeezer_WifiLock"));

        getPreferences();

        cli.initialize();
    }

    private void getPreferences() {
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        scrobblingEnabled = preferences.getBoolean(Preferences.KEY_SCROBBLE_ENABLED, false);
        mFadeInSecs = preferences.getInt(Preferences.KEY_FADE_IN_SECS, 0);
        mUpdateOngoingNotification = preferences
                .getBoolean(Preferences.KEY_NOTIFY_OF_CONNECTION, false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (IBinder) squeezeService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    void disconnect() {
        disconnect(false);
    }

    void disconnect(boolean isServerDisconnect) {
        connectionState.disconnect(this, isServerDisconnect && !mHandshakeComplete);
        mHandshakeComplete = false;
        clearOngoingNotification();
        playerState = new PlayerState();
    }


    private interface CmdHandler {

        public void handle(List<String> tokens);
    }

    private Map<String, CmdHandler> initializeGlobalHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HandlerList.GLOBAL) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }
        handlers.put("playlists", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if ("delete".equals(tokens.get(1))) {
                    ;
                } else if ("edit".equals(tokens.get(1))) {
                    ;
                } else if ("new".equals(tokens.get(1))) {
                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    if (tokenMap.get("overwritten_playlist_id") != null) {
                        for (IServicePlaylistMaintenanceCallback callback : playlistMaintenanceCallbacks) {
                            callback.onCreateFailed(getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                    tokenMap.get("name")));
                        }
                    }
                } else if ("rename".equals(tokens.get(1))) {
                    HashMap<String, String> tokenMap = parseTokens(tokens);
                    if (tokenMap.get("dry_run") != null) {
                        if (tokenMap.get("overwritten_playlist_id") != null) {
                            for (IServicePlaylistMaintenanceCallback callback : playlistMaintenanceCallbacks) {
                                callback.onRenameFailed(getString(R.string.PLAYLIST_EXISTS_MESSAGE,
                                                tokenMap.get("newname")));
                            }
                        } else {
                            cli.sendCommandImmediately(
                                    "playlists rename playlist_id:" + tokenMap.get("playlist_id")
                                            + " newname:" + Util.encode(tokenMap.get("newname")));
                        }
                    }
                } else if ("tracks".equals(tokens.get(1))) {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists tracks"), tokens);
                } else {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists"), tokens);
                }
            }
        });
        handlers.put("login", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Authenticated: " + tokens);
                onAuthenticated();
            }
        });
        handlers.put("pref", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Preference received: " + tokens);
                if ("httpport".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState.setHttpPort(Integer.parseInt(tokens.get(2)));
                }
                if ("jivealbumsort".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState.setPreferedAlbumSort(tokens.get(2));
                }
                if ("mediadirs".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState.setMediaDirs(Util.decode(tokens.get(2)));
                }
            }
        });
        handlers.put("can", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Capability received: " + tokens);
                if ("favorites".equals(tokens.get(1)) && tokens.size() >= 4) {
                    connectionState.setCanFavorites(Util.parseDecimalIntOrZero(tokens.get(3)) == 1);
                }
                if ("musicfolder".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState
                            .setCanMusicfolder(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }
                if ("myapps".equals(tokens.get(1)) && tokens.size() >= 4) {
                    connectionState.setCanMyApps(Util.parseDecimalIntOrZero(tokens.get(3)) == 1);
                }
                if ("randomplay".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState
                            .setCanRandomplay(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }
            }
        });
        handlers.put("getstring", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                int maxOrdinal = 0;
                Map<String, String> tokenMap = parseTokens(tokens);
                for (Entry<String, String> entry : tokenMap.entrySet()) {
                    if (entry.getValue() != null) {
                        ServerString serverString = ServerString.valueOf(entry.getKey());
                        serverString.setLocalizedString(entry.getValue());
                        if (serverString.ordinal() > maxOrdinal) {
                            maxOrdinal = serverString.ordinal();
                        }
                    }
                }

                // Fetch the next strings until the list is completely translated
                if (maxOrdinal < ServerString.values().length - 1) {
                    cli.sendCommandImmediately(
                            "getstring " + ServerString.values()[maxOrdinal + 1].name());
                }
            }
        });
        handlers.put("version", new CmdHandler() {
            /**
             * Seeing the <code>version</code> result indicates that the
             * handshake has completed (see
             * {@link SqueezeService#onCliPortConnectionEstablished(String, String)}), call any handshake
             * callbacks that have been registered.
             */
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "Version received: " + tokens);
                mHandshakeComplete = true;
                strings();

                for (IServiceHandshakeCallback callback : mHandshakeCallbacks) {
                    callback.onHandshakeCompleted();
                }
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HandlerList.PREFIXED) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    private Map<String, CmdHandler> initializePlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HandlerList.PLAYER_SPECIFIC) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }
        handlers.put("play", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "play registered");
                updatePlayStatus(PlayerState.PlayStatus.play);
            }
        });
        handlers.put("stop", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "stop registered");
                updatePlayStatus(PlayerState.PlayStatus.stop);
            }
        });
        handlers.put("pause", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "pause registered: " + tokens);
                parsePause(tokens.size() >= 3 ? tokens.get(2) : null);
            }
        });
        handlers.put("playlist", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                parsePlaylistNotification(tokens);
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializeGlobalPlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        handlers.put("client", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.i(TAG, "client received: " + tokens);
                // Something has happened to the player list, we just fetch the full list again
                // This is simpler and handles any missed client events
                fetchPlayers();
            }
        });
        handlers.put("status", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                if (tokens.size() >= 3 && "-".equals(tokens.get(2))) {
                    PlayerState playerState = parseStatusLine(tokens);
                    for (IServicePlayerStateCallback callback : mPlayerStateCallbacks) {
                        callback.onPlayerStateReceived(playerState);
                    }
                    if (playerState.getPlayerId().equals(getActivePlayerId())) {
                        updateStatus(playerState);
                    }
                } else {
                    cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("status"), tokens);
                }
            }
        });
        handlers.put("prefset", new CmdHandler() {
            @Override
            public void handle(List<String> tokens) {
                Log.v(TAG, "Prefset received: " + tokens);
                if (tokens.size() > 4 && tokens.get(2).equals("server") && tokens.get(3)
                        .equals("volume")) {
                    String playerId = Util.decode(tokens.get(0));
                    int newVolume = Util.parseDecimalIntOrZero(tokens.get(4));
                    updatePlayerVolume(playerId, newVolume);
                }
            }
        });

        return handlers;
    }

    private Map<String, CmdHandler> initializePrefixedPlayerSpecificHandlers() {
        Map<String, CmdHandler> handlers = new HashMap<String, CmdHandler>();

        for (final CliClient.ExtendedQueryFormatCmd cmd : cli.extQueryFormatCmds) {
            if (cmd.handlerList == CliClient.HandlerList.PREFIXED_PLAYER_SPECIFIC) {
                handlers.put(cmd.cmd, new CmdHandler() {
                    @Override
                    public void handle(List<String> tokens) {
                        cli.parseSqueezerList(cmd, tokens);
                    }
                });
            }
        }

        return handlers;
    }

    private final Map<String, CmdHandler> globalHandlers = initializeGlobalHandlers();

    private final Map<String, CmdHandler> prefixedHandlers = initializePrefixedHandlers();

    private final Map<String, CmdHandler> playerSpecificHandlers
            = initializePlayerSpecificHandlers();

    private final Map<String, CmdHandler> globalPlayerSpecificHandlers
            = initializeGlobalPlayerSpecificHandlers();

    private final Map<String, CmdHandler> prefixedPlayerSpecificHandlers
            = initializePrefixedPlayerSpecificHandlers();

    void onLineReceived(String serverLine) {
        Log.v(TAG, "LINE: " + serverLine);
        ACRA.getErrorReporter().putCustomData("lastReceivedLine", serverLine);

        List<String> tokens = Arrays.asList(serverLine.split(" "));
        if (tokens.size() < 2) {
            return;
        }

        CmdHandler handler;
        if ((handler = globalHandlers.get(tokens.get(0))) != null) {
            handler.handle(tokens);
            return;
        }
        if ((handler = prefixedHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }
        if ((handler = globalPlayerSpecificHandlers.get(tokens.get(1))) != null) {
            handler.handle(tokens);
            return;
        }

        // Player-specific commands for our active player.
        if (Util.decode(tokens.get(0)).equals(getActivePlayerId())) {
            if ((handler = playerSpecificHandlers.get(tokens.get(1))) != null) {
                handler.handle(tokens);
                return;
            }
            if (tokens.size() > 2
                    && (handler = prefixedPlayerSpecificHandlers.get(tokens.get(2))) != null) {
                handler.handle(tokens);
            }
        }
    }

    private String getActivePlayerId() {
        return (connectionState.getActivePlayer() != null ? connectionState
                .getActivePlayer().getId() : null);
    }

    private void updatePlayerVolume(String playerId, int newVolume) {
        Player player = connectionState.getPlayer(playerId);
        if (playerId.equals(getActivePlayerId())) {
            playerState.setCurrentVolume(newVolume);
        }
        for (IServiceVolumeCallback callback : mVolumeCallbacks) {
            if (callback.wantAllPlayers() || playerId.equals(getActivePlayerId()))
            callback.onVolumeChanged(newVolume, player);
        }
    }


    private void parsePlaylistNotification(List<String> tokens) {
        Log.v(TAG, "Playlist notification received: " + tokens);
        String notification = tokens.get(2);
        if ("newsong".equals(notification)) {
            // When we don't subscribe to the current players status, we rely
            // on playlist notifications and order song details here.
            // TODO keep track of subscribe status
            cli.sendPlayerCommand("status - 1 tags:" + SONGTAGS);
        } else if ("play".equals(notification)) {
            updatePlayStatus(PlayerState.PlayStatus.play);
        } else if ("stop".equals(notification)) {
            updatePlayStatus(PlayerState.PlayStatus.stop);
        } else if ("pause".equals(notification)) {
            parsePause(tokens.size() >= 4 ? tokens.get(3) : null);
        } else if ("addtracks".equals(notification)) {
            for (IServiceCurrentPlaylistCallback callback : mCurrentPlaylistCallbacks) {
                callback.onAddTracks(playerState);
            }
        } else if ("delete".equals(notification)) {
            for (IServiceCurrentPlaylistCallback callback : mCurrentPlaylistCallbacks) {
                callback.onDelete(playerState, Integer.parseInt(tokens.get(3)));
            }
        }
    }

    private void parsePause(String explicitPause) {
        if ("0".equals(explicitPause)) {
            updatePlayStatus(PlayerState.PlayStatus.play);
        } else if ("1".equals(explicitPause)) {
            updatePlayStatus(PlayerState.PlayStatus.pause);
        }
    }

    private HashMap<String, String> parseTokens(List<String> tokens) {
        HashMap<String, String> tokenMap = new HashMap<String, String>();
        String key, value;
        for (String token : tokens) {
            if (token == null || token.length() == 0) {
                continue;
            }
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                key = Util.decode(token);
                value = null;
            } else {
                key = Util.decode(token.substring(0, colonPos));
                value = Util.decode(token.substring(colonPos + 3));
            }
            tokenMap.put(key, value);
        }
        return tokenMap;
    }

    /**
     *
     * TODO: This allocates a new PlayerState every time a status line is read (approx once
     * per second). Could fix by keeping a single spare PlayerState object around and reading
     * in to that.
     */
    private PlayerState parseStatusLine(List<String> tokens) {
        PlayerState result = new PlayerState();
        HashMap<String, String> tokenMap = parseTokens(tokens);

        result.setPlayerId(Util.decode(tokens.get(0)));
        result.setPoweredOn(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);
        result.setPlayStatus(tokenMap.get("mode"));
        result.setShuffleStatus(tokenMap.get("playlist shuffle"));
        result.setRepeatStatus(tokenMap.get("playlist repeat"));
        result.setCurrentPlaylist(tokenMap.get("playlist_name"));
        result.setCurrentPlaylistIndex(Util.parseDecimalIntOrZero(tokenMap.get("playlist_cur_index")));
        result.setSleepDuration(Util.parseDecimalIntOrZero(tokenMap.get("sleep")));
        result.setSleep(Util.parseDecimalIntOrZero(tokenMap.get("will_sleep_in")));
        result.setCurrentSong(new Song(tokenMap));
        result.setCurrentSongDuration(Util.parseDecimalIntOrZero(tokenMap.get("duration")));
        result.setCurrentTimeSecond(Util.parseDecimalIntOrZero(tokenMap.get("time")));
        result.setCurrentVolume(Util.parseDecimalIntOrZero(tokenMap.get("mixer volume")));

        return result;
    }

    /**
     * Updates various pieces of book-keeping information when the player state changes, and
     * ensures that relevant callbacks are called.
     *
     * @param newPlayerState The new playerState.
     */
    private void updateStatus(PlayerState newPlayerState) {
        updatePowerStatus(newPlayerState.isPoweredOn());
        updatePlayStatus(newPlayerState.getPlayStatus());
        updateShuffleStatus(newPlayerState.getShuffleStatus());
        updateRepeatStatus(newPlayerState.getRepeatStatus());
        updateCurrentSong(newPlayerState.getCurrentSong());
        updateTimes(newPlayerState.getCurrentTimeSecond(), newPlayerState.getCurrentSongDuration());

        // Ensure that all other player state is saved as well.
        playerState = newPlayerState;
    }

    /**
     * Updates the power status of the current player.
     * <p/>
     * If the power status has changed then calls the
     * {@link IServiceCallback#onPowerStatusChanged(boolean, boolean)} method of any callbacks
     * registered using {@link SqueezeServiceBinder#registerCallback(IServiceCallback)}.
     *
     * @param powerStatus The new power status.
     */
    private void updatePowerStatus(boolean powerStatus) {
        Boolean currentPowerStatus = playerState.getPoweredOn();
        if (currentPowerStatus  == null || powerStatus != currentPowerStatus) {
            playerState.setPoweredOn(powerStatus);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onPowerStatusChanged(squeezeService.canPowerOn(),
                        squeezeService.canPowerOff());
            }
        }
    }

    /**
     * Updates the playing status of the current player.
     * <p/>
     * Updates the Wi-Fi lock and ongoing status notification as necessary.
     * <p/>
     * Calls the {@link IServiceCallback#onPlayStatusChanged(String)} method of any callbacks
     * registered using {@link SqueezeServiceBinder#registerCallback(IServiceCallback)}.
     *
     * @param playStatus The new playing status.
     */
    private void updatePlayStatus(PlayStatus playStatus) {
        if (playStatus != null && playStatus != playerState.getPlayStatus()) {
            playerState.setPlayStatus(playStatus);
            //TODO when do we want to keep the wiFi lock ?
            connectionState.updateWifiLock(playerState.isPlaying());
            updateOngoingNotification();
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onPlayStatusChanged(playStatus.name());
            }
        }
    }

    /**
     * Updates the shuffle status of the current player.
     * <p/>
     * If the shuffle status has changed then calls the
     * {@link IServiceCallback#onShuffleStatusChanged(boolean, int)}  method of any
     * callbacks registered using {@link SqueezeServiceBinder#registerCallback(IServiceCallback)}.
     *
     * @param shuffleStatus The new shuffle status.
     */
    private void updateShuffleStatus(ShuffleStatus shuffleStatus) {
        if (shuffleStatus != null && shuffleStatus != playerState.getShuffleStatus()) {
            boolean wasUnknown = playerState.getShuffleStatus() == null;
            playerState.setShuffleStatus(shuffleStatus);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onShuffleStatusChanged(wasUnknown, shuffleStatus.getId());
            }
        }
    }

    /**
     * Updates the repeat status of the current player.
     * <p/>
     * If the repeat status has changed then Calls the
     * {@link IServiceCallback#onRepeatStatusChanged(boolean, int)} method of any callbacks
     * registered using {@link SqueezeServiceBinder#registerCallback(IServiceCallback)}.
     *
     * @param repeatStatus The new repeat status.
     */
    private void updateRepeatStatus(RepeatStatus repeatStatus) {
        if (repeatStatus != null && repeatStatus != playerState.getRepeatStatus()) {
            boolean wasUnknown = playerState.getRepeatStatus() == null;
            playerState.setRepeatStatus(repeatStatus);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onRepeatStatusChanged(wasUnknown, repeatStatus.getId());
            }
        }
    }

    /**
     * Updates the current song in the player state, and ongoing notification.
     * <p/>
     * If the song has changed then calls the
     * {@link IServiceMusicChangedCallback#onMusicChanged(uk.org.ngo.squeezer.model.PlayerState)}
     * method of any callbacks registered using
     * {@link SqueezeServiceBinder#registerMusicChangedCallback(IServiceMusicChangedCallback)}.
     *
     * @param song The new song.
     */
    private void updateCurrentSong(Song song) {
        Song currentSong = playerState.getCurrentSong();
        if ((song == null ? (currentSong != null) : !song.equals(currentSong))) {
            Log.d(TAG, "updateCurrentSong: " + song);
            playerState.setCurrentSong(song);
            updateOngoingNotification();
            for (IServiceMusicChangedCallback callback : mMusicChangedCallbacks) {
                callback.onMusicChanged(playerState);
            }
        }
    }

    /**
     * Updates the current position and duration for the current song.
     * <p/>
     * If the current position has changed then calls the
     * {@link IServiceCallback#onTimeInSongChange(int, int)} method of any callbacks registered
     * using {@link SqueezeServiceBinder#registerCallback(IServiceCallback)}.
     *
     * @param secondsIn The new position in the song.
     * @param secondsTotal The song's duration.
     */
    private void updateTimes(int secondsIn, int secondsTotal) {
        playerState.setCurrentSongDuration(secondsTotal);
        if (playerState.getCurrentTimeSecond() != secondsIn) {
            playerState.setCurrentTimeSecond(secondsIn);
            for (IServiceCallback callback : mServiceCallbacks) {
                callback.onTimeInSongChange(secondsIn, secondsTotal);
            }
        }
    }

    void changeActivePlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        Log.i(TAG, "Active player now: " + newPlayer);
        final String playerId = newPlayer.getId();
        String oldPlayerId = getActivePlayerId();
        boolean changed = false;
        if (oldPlayerId == null || !oldPlayerId.equals(playerId)) {
            connectionState.setActivePlayer(newPlayer);
            playerState = new PlayerState();
            changed = true;
        }

        // Start an async fetch of its status.
        cli.sendPlayerCommand("status - 1 tags:" + SONGTAGS);

        if (changed) {
            updatePlayerSubscriptionState();

            // NOTE: this involves a write and can block (sqlite lookup via binder call), so
            // should be done off-thread, so we can process service requests & send our callback
            // as quickly as possible.
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Saving " + Preferences.KEY_LASTPLAYER + "=" + playerId);
                    final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                            MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(Preferences.KEY_LASTPLAYER, playerId);
                    editor.commit();
                }
            });
        }

        for (IServicePlayersCallback callback : mPlayersCallbacks) {
            callback.onPlayersChanged(connectionState.getPlayers(), newPlayer);
        }
    }

    private void updatePlayerSubscriptionState() {
        // Subscribe or unsubscribe to the players realtime status updates depending on whether we
        // have a client that cares about second-to-second updates for any player or the active
        // player
        Player activePlayer = connectionState.getActivePlayer();
        for (Player player : connectionState.getPlayers()) {
            if (mPlayerStateCallbacks.count() > 0 ||
                    (mServiceCallbacks.count() > 0 && player.equals(activePlayer))) {
                cli.sendPlayerCommand(player, "status - 1 subscribe:1 tags:" + SONGTAGS);
            } else {
                cli.sendPlayerCommand(player, "status - 1 subscribe:- tags:" + SONGTAGS);
            }
        }
    }

    /**
     * Manages the state of any ongoing notification based on the player and connection state.
     */
    private void updateOngoingNotification() {
        boolean playing = playerState.isPlaying();
        String songName = playerState.getCurrentSongName();
        String playerName = connectionState.getActivePlayer() != null ? connectionState
                .getActivePlayer().getName() : "squeezer";

        // Update scrobble state, if either we're currently scrobbling, or we
        // were (to catch the case where we started scrobbling a song, and the
        // user went in to settings to disable scrobbling).
        if (scrobblingEnabled || scrobblingPreviouslyEnabled) {
            scrobblingPreviouslyEnabled = scrobblingEnabled;
            Song s = playerState.getCurrentSong();

            if (s != null) {
                Log.v(TAG, "Scrobbling, playing is: " + playing);
                Intent i = new Intent();

                if (Scrobble.haveScrobbleDroid()) {
                    // http://code.google.com/p/scrobbledroid/wiki/DeveloperAPI
                    i.setAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
                    i.putExtra("playing", playing);
                    i.putExtra("track", songName);
                    i.putExtra("album", s.getAlbum());
                    i.putExtra("artist", s.getArtist());
                    i.putExtra("secs", playerState.getCurrentSongDuration());
                    i.putExtra("source", "P");
                } else if (Scrobble.haveSls()) {
                    // http://code.google.com/p/a-simple-lastfm-scrobbler/wiki/Developers
                    i.setAction("com.adam.aslfms.notify.playstatechanged");
                    i.putExtra("state", playing ? 0 : 2);
                    i.putExtra("app-name", getText(R.string.app_name));
                    i.putExtra("app-package", "uk.org.ngo.squeezer");
                    i.putExtra("track", songName);
                    i.putExtra("album", s.getAlbum());
                    i.putExtra("artist", s.getArtist());
                    i.putExtra("duration", playerState.getCurrentSongDuration());
                    i.putExtra("source", "P");
                }
                sendBroadcast(i);
            }
        }

        if (!playing) {
            if (!mUpdateOngoingNotification) {
                clearOngoingNotification();
                return;
            }
        }

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification status = new Notification();
        //status.contentView = views;
        Intent showNowPlaying = new Intent(this, NowPlayingActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, showNowPlaying, 0);
        if (playing) {
            status.setLatestEventInfo(this,
                    getString(R.string.notification_playing_text, playerName), songName, pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
        } else {
            status.setLatestEventInfo(this,
                    getString(R.string.notification_connected_text, playerName), "-", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.ic_launcher;
        }
        nm.notify(PLAYBACKSERVICE_STATUS, status);
    }

    private void clearOngoingNotification() {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }

    /**
     * Authenticate on the SqueezeServer.
     * <p/>
     * The server does
     * <pre>
     * login user wrongpassword
     * login user ******
     * (Connection terminted)
     * </pre>
     * instead of as documented
     * <pre>
     * login user wrongpassword
     * (Connection terminted)
     * </pre>
     * therefore a disconnect when handshake (the next step after authentication) is not completed,
     * is considered an authentication failure.
     */
    void onCliPortConnectionEstablished(final String userName, final String password) {
        cli.sendCommandImmediately("login " + Util.encode(userName) + " " + Util.encode(password));
    }

    /**
     * Handshake with the SqueezeServer, learn some of its supported features, and start listening
     * for asynchronous updates of server state.
     */
    private void onAuthenticated() {
        fetchPlayers();
        cli.sendCommandImmediately(
                "listen 1", // subscribe to all server notifications
                "can musicfolder ?", // learn music folder browsing support
                "can randomplay ?", // learn random play function functionality
                "can favorites items ?", // learn support for "Favorites" plugin
                "can myapps items ?", // lean support for "MyApps" plugin
                "pref httpport ?", // learn the HTTP port (needed for images)
                "pref jivealbumsort ?", // learn the preferred album sort order
                "pref mediadirs ?", // learn the base path(s) of the server music library

                // Fetch the version number. This must be the last thing
                // fetched, as seeing the result triggers the
                // "handshake is complete" logic elsewhere.
                "version ?"
        );
    }

    private void fetchPlayers() {
        // initiate an async player fetch
        connectionState.clearPlayers();
        cli.requestItems("players", -1, new IServiceItemListCallback<Player>() {
            @Override
            public void onItemsReceived(int count, int start, Map<String, String> parameters, List<Player> items, Class<Player> dataType) {
                connectionState.addPlayers(items);
                if (start + items.size() >= count) {
                    Player initialPlayer = getInitialPlayer();
                    if (initialPlayer != null) {
                        changeActivePlayer(initialPlayer);
                    }
                }
            }

            private Player getInitialPlayer() {
                final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
                final String lastConnectedPlayer = preferences.getString(Preferences.KEY_LASTPLAYER,
                        null);
                Log.i(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);

                List<Player> players = connectionState.getPlayers();
                for (Player player : players) {
                    if (player.getId().equals(lastConnectedPlayer)) {
                        return player;
                    }
                }
                return players.size() > 0 ? players.get(0) : null;
            }

            @Override
            public Object getClient() {
                return SqueezeService.this;
            }
        });
    }

    /* Start an asynchronous fetch of the squeezeservers localized strings */
    private void strings() {
        cli.sendCommandImmediately("getstring " + ServerString.values()[0].name());
    }

    /** A download request will be passed to the download manager for each song called back to this */
    private final IServiceItemListCallback<Song> songDownloadCallback = new IServiceItemListCallback<Song>() {
        @Override
        public void onItemsReceived(int count, int start, Map<String, String> parameters, List<Song> items, Class<Song> dataType) {
            for (Song item : items) {
                downloadSong(item.getId(), item.getName(), item.getUrl());
            }
        }

        @Override
        public Object getClient() {
            return this;
        }
    };

    /**
     * For each item called to this:
     * If it is a folder: recursive lookup items in the folder
     * If is is a track: Enqueue a download request to the download manager
     */
    private final IServiceItemListCallback<MusicFolderItem> musicFolderDownloadCallback = new IServiceItemListCallback<MusicFolderItem>() {
        @Override
        public void onItemsReceived(int count, int start, Map<String, String> parameters, List<MusicFolderItem> items, Class<MusicFolderItem> dataType) {
            for (MusicFolderItem item : items) {
                squeezeService.downloadItem(item);
            }
        }

        @Override
        public Object getClient() {
            return this;
        }
    };

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void downloadSong(String songId, String title, String serverUrl) {
        if (songId == null) {
            return;
        }

        // If running on Gingerbread or greater use the Download Manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(squeezeService.getSongDownloadUrl(songId));
            DownloadDatabase downloadDatabase = new DownloadDatabase(this);
            String localPath = getLocalFile(serverUrl);
            String tempFile = UUID.randomUUID().toString();
            String credentials = connectionState.getUserName() + ":" + connectionState.getPassword();
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(title)
                    .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MUSIC, tempFile)
                    .setVisibleInDownloadsUi(false)
                    .addRequestHeader("Authorization", "Basic " + base64EncodedCredentials);
            long downloadId = downloadManager.enqueue(request);
            if (!downloadDatabase.registerDownload(downloadId, tempFile, localPath)) {
                Log.w(TAG, "Could not register download entry, download cancelled");
                downloadManager.remove(downloadId);
            }
        }
    }

    /**
     * Tries to get the relative path to the server music library.
     * <p/>
     * If this is not possible resort to the last path segment of the server path
     */
    private String getLocalFile(String serverUrl) {
        Uri serverUri = Uri.parse(serverUrl);
        String serverPath = serverUri.getPath();
        String mediaDir = null;
        for (String dir : connectionState.getMediaDirs()) {
            if (serverPath.startsWith(dir)) {
                mediaDir = dir;
                break;
            }
        }
        if (mediaDir != null)
            return serverPath.substring(mediaDir.length(), serverPath.length());
        else
            return serverUri.getLastPathSegment();
    }

    private final ISqueezeService squeezeService = new SqueezeServiceBinder();
    private class SqueezeServiceBinder extends Binder implements ISqueezeService {

        @Override
        public void registerCallback(IServiceCallback callback) {
            mServiceCallbacks.register(callback);
            updatePlayerSubscriptionState();
        }

        @Override
        public void registerConnectionCallback(IServiceConnectionCallback callback) {
            mConnectionCallbacks.register(callback);
        }

        @Override
        public void registerPlayersCallback(IServicePlayersCallback callback) {
            mPlayersCallbacks.register(callback);

            // Call back immediately if we have players
            List<Player> players = connectionState.getPlayers();
            if (players.size() > 0) {
                callback.onPlayersChanged(players, connectionState.getActivePlayer());
            }
        }

        @Override
        public void registerVolumeCallback(IServiceVolumeCallback callback) {
            mVolumeCallbacks.register(callback);
        }

        @Override
        public void registerCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback) {
            mCurrentPlaylistCallbacks.register(callback);
        }

        @Override
        public void registerMusicChangedCallback(IServiceMusicChangedCallback callback) {
            mMusicChangedCallbacks.register(callback);
        }

        @Override
        public void registerHandshakeCallback(IServiceHandshakeCallback callback) {
            mHandshakeCallbacks.register(callback);

            // Call onHandshakeCompleted() immediately if handshaking is done.
            if (mHandshakeComplete) {
                callback.onHandshakeCompleted();
            }
        }

        @Override
        public void registerPlayerStateCallback(IServicePlayerStateCallback callback) {
            mPlayerStateCallbacks.register(callback);
            updatePlayerSubscriptionState();
        }


        @Override
        public void adjustVolumeTo(Player player, int newVolume) {
            cli.sendPlayerCommand(player, "mixer volume " + Math.min(100, Math.max(0, newVolume)));
        }

        @Override
        public void adjustVolumeTo(int newVolume) {
            cli.sendPlayerCommand("mixer volume " + Math.min(100, Math.max(0, newVolume)));
        }

        @Override
        public void adjustVolumeBy(int delta) {
            if (delta > 0) {
                cli.sendPlayerCommand("mixer volume %2B" + delta);
            } else if (delta < 0) {
                cli.sendPlayerCommand("mixer volume " + delta);
            }
        }

        @Override
        public boolean isConnected() {
            return connectionState.isConnected();
        }

        @Override
        public boolean isConnectInProgress() {
            return connectionState.isConnectInProgress();
        }

        @Override
        public void startConnect(String hostPort, String userName, String password) {
            connectionState.startConnect(SqueezeService.this, hostPort, userName, password);
        }

        @Override
        public void disconnect() {
            if (!isConnected()) {
                return;
            }
            SqueezeService.this.disconnect();
        }

        @Override
        public void powerOn() {
            cli.sendPlayerCommand("power 1");
        }

        @Override
        public void powerOff() {
            cli.sendPlayerCommand("power 0");
        }

        @Override
        public void togglePower(Player player) {
            cli.sendPlayerCommand(player, "power");
        }

        @Override
        public void playerRename(Player player, String newName) {
            cli.sendPlayerCommand(player, "name " + Util.encode(newName));
        }

        @Override
        public void sleep(Player player, int duration) {
            cli.sendPlayerCommand(player, "sleep " + duration);
        }

        @Override
        public boolean canPowerOn() {
            return canPower() && !playerState.isPoweredOn();
        }

        @Override
        public boolean canPowerOff() {
            return canPower() && playerState.isPoweredOn();
        }

        private boolean canPower() {
            Player player = connectionState.getActivePlayer();
            return connectionState.isConnected() && player != null && player.isCanpoweroff();
        }

        /**
         * Does the server support the "<code>favorites items</code>" command?
         *
         * @return True if it does, false otherwise.
         */
        @Override
        public boolean canFavorites() {
            return connectionState.canFavorites();
        }

        /**
         * Does the server support the "<code>musicfolders</code>" command?
         *
         * @return True if it does, false otherwise.
         */
        @Override
        public boolean canMusicfolder() {
            return connectionState.canMusicfolder();
        }

        /**
         * Does the server support the "<code>myapps items</code>" command?
         *
         * @return True if it does, false otherwise.
         */
        @Override
        public boolean canMyApps() {
            return connectionState.canMyApps();
        }

        /**
         * Does the server support the "<code>randomplay</code>" command?
         *
         * @return True if it does, false otherwise.
         */
        @Override
        public boolean canRandomplay() {
            return connectionState.canRandomplay();
        }

        @Override
        public String preferredAlbumSort() {
            return connectionState.getPreferredAlbumSort();
        }

        private String fadeInSecs() {
            return mFadeInSecs > 0 ? " " + mFadeInSecs : "";
        }

        @Override
        public boolean togglePausePlay() {
            if (!isConnected()) {
                return false;
            }

            PlayerState.PlayStatus playStatus = playerState.getPlayStatus();

            // May be null (e.g., connected to a server with no connected
            // players. TODO: Handle this better, since it's not obvious in the
            // UI.
            if (playStatus == null) {
                return false;
            }

            switch (playStatus) {
                case play:
                    // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
                    // because then we'd get confused when they came back in to us, not being
                    // able to differentiate ours coming back on the listen channel vs. those
                    // of those idiots at the dinner party messing around.
                    cli.sendPlayerCommand("pause 1");
                    break;
                case stop:
                    cli.sendPlayerCommand("play" + fadeInSecs());
                    break;
                case pause:
                    cli.sendPlayerCommand("pause 0" + fadeInSecs());
                    break;
            }
            return true;
        }

        @Override
        public boolean play() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("play" + fadeInSecs());
            return true;
        }

        @Override
        public boolean stop() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("stop");
            return true;
        }

        @Override
        public boolean nextTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            cli.sendPlayerCommand("button jump_fwd");
            return true;
        }

        @Override
        public boolean previousTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            cli.sendPlayerCommand("button jump_rew");
            return true;
        }

        @Override
        public boolean toggleShuffle() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist shuffle");
            return true;
        }

        @Override
        public boolean toggleRepeat() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist repeat");
            return true;
        }

        @Override
        public boolean playlistControl(String cmd, PlaylistItem playlistItem) {
            if (!isConnected()) {
                return false;
            }

            cli.sendPlayerCommand("playlistcontrol cmd:" + cmd + " " + playlistItem.getPlaylistParameter());
            return true;
        }

        @Override
        public boolean randomPlay(String type) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("randomplay " + type);
            return true;
        }

        /**
         * Start playing the song in the current playlist at the given index.
         *
         * @param index the index to jump to
         */
        @Override
        public boolean playlistIndex(int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist index " + index + fadeInSecs());
            return true;
        }

        @Override
        public boolean playlistRemove(int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist delete " + index);
            return true;
        }

        @Override
        public boolean playlistMove(int fromIndex, int toIndex) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist move " + fromIndex + " " + toIndex);
            return true;
        }

        @Override
        public boolean playlistClear() {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist clear");
            return true;
        }

        @Override
        public boolean playlistSave(String name) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand("playlist save " + Util.encode(name));
            return true;
        }

        @Override
        public boolean pluginPlaylistControl(Plugin plugin, String cmd, String itemId) {
            if (!isConnected()) {
                return false;
            }
            cli.sendPlayerCommand(plugin.getId() + " playlist " + cmd + " item_id:" + itemId);
            return true;

        }

        private boolean isPlaying() {
            return playerState.isPlaying();
        }

        @Override
        public void setActivePlayer(final Player player) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    changeActivePlayer(player);
                }
            });
        }

        @Override
        public Player getActivePlayer() {
            return connectionState.getActivePlayer();
        }

        @Override
        public PlayerState getPlayerState() {
            return playerState;
        }

        @Override
        public String getCurrentPlaylist() {
            return playerState.getCurrentPlaylist();
        }

        @Override
        public String getAlbumArtUrl(String artworkTrackId) {
            return getAbsoluteUrl(artworkTrackIdUrl(artworkTrackId));
        }

        private String artworkTrackIdUrl(String artworkTrackId) {
            return "/music/" + artworkTrackId + "/cover.jpg";
        }

        /**
         * Returns a URL to download a song.
         *
         * @param songId the song ID
         * @return The URL (as a string)
         */
        @Override
        public String getSongDownloadUrl(String songId) {
            return getAbsoluteUrl(songDownloadUrl(songId));
        }

        private String songDownloadUrl(String songId) {
            return "/music/" + songId + "/download";
        }

        @Override
        public String getIconUrl(String icon) {
            return getAbsoluteUrl('/' + icon);
        }

        private String getAbsoluteUrl(String relativeUrl) {
            Integer port = connectionState.getHttpPort();
            if (port == null || port == 0) {
                return "";
            }
            return "http://" + connectionState.getCurrentHost() + ":" + port + relativeUrl;
        }

        @Override
        public boolean setSecondsElapsed(int seconds) {
            if (!isConnected()) {
                return false;
            }
            if (seconds < 0) {
                return false;
            }

            cli.sendPlayerCommand("time " + seconds);

            return true;
        }

        @Override
        public void preferenceChanged(String key) {
            Log.i(TAG, "Preference changed: " + key);
            if (Preferences.KEY_NOTIFY_OF_CONNECTION.equals(key)) {
                updateOngoingNotification();
                return;
            }

            // If the server address changed then disconnect.
            if (Preferences.KEY_SERVERADDR.equals(key)) {
                disconnect();
                return;
            }

            getPreferences();
        }


        @Override
        public void cancelItemListRequests(Object client) {
            cli.cancelClientRequests(client);
        }

        @Override
        public void cancelSubscriptions(Object client) {
            for (Entry<ServiceCallback, ServiceCallbackList> entry : callbacks.entrySet()) {
                if (entry.getKey().getClient() == client) {
                    entry.getValue().unregister(entry.getKey());
                }
            }
            updatePlayerSubscriptionState();
        }

        @Override
        public void players(int start, IServiceItemListCallback<Player> callback) {
            if (!isConnected()) {
                return;
            }

            // Call back immediately if we have players
            List<Player> players = connectionState.getPlayers();
            if (players != null)
                callback.onItemsReceived(players.size(), 0, null, players, Player.class);
            else
                cli.requestItems("players", start, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's albums, which are matching the given parameters */
        @Override
        public void albums(IServiceItemListCallback<Album> callback, int start, String sortOrder, String searchString, FilterItem... filters) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + ALBUMTAGS);
            parameters.add("sort:" + sortOrder);
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            for (FilterItem filter : filters)
                if (filter != null)
                    parameters.add(filter.getFilterParameter());
            cli.requestItems("albums", start, parameters, callback);
        }


        /* Start an async fetch of the SqueezeboxServer's artists */
        @Override
        public void artists(IServiceItemListCallback<Artist> callback, int start, String searchString, FilterItem... filters) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            for (FilterItem filter : filters)
                if (filter != null)
                    parameters.add(filter.getFilterParameter());
            cli.requestItems("artists", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's years */
        @Override
        public void years(int start, IServiceItemListCallback<Year> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("years", start, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's genres */
        @Override
        public void genres(int start, String searchString, IServiceItemListCallback<Genre> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            cli.requestItems("genres", start, parameters, callback);
        }

        /**
         * Starts an async fetch of the contents of a SqueezerboxServer's music
         * folders in the given folderId.
         * <p>
         * folderId may be null, in which case the contents of the root music
         * folder are returned.
         * <p>
         * Results are returned through the given callback.
         *
         * @param start Where in the list of folders to start.
         * @param musicFolderItem The folder to view.
         * @param callback Results will be returned through this
         */
        @Override
        public void musicFolders(int start, MusicFolderItem musicFolderItem, IServiceItemListCallback<MusicFolderItem> callback) {
            if (!isConnected()) {
                return;
            }

            List<String> parameters = new ArrayList<String>();

            parameters.add("tags:u");//TODO only available from version 7.6 so instead keep track of path
            if (musicFolderItem != null) {
                parameters.add(musicFolderItem.getFilterParameter());
            }

            cli.requestItems("musicfolder", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's songs */
        @Override
        public void songs(IServiceItemListCallback<Song> callback, int start, String sortOrder, String searchString, FilterItem... filters) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + SONGTAGS);
            parameters.add("sort:" + sortOrder);
            if (searchString != null && searchString.length() > 0) {
                parameters.add("search:" + searchString);
            }
            for (FilterItem filter : filters)
                if (filter != null)
                    parameters.add(filter.getFilterParameter());
            cli.requestItems("songs", start, parameters, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's current playlist */
        @Override
        public void currentPlaylist(int start, IServiceItemListCallback<Song> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestPlayerItems("status", start, Arrays.asList("tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the songs of the supplied playlist */
        @Override
        public void playlistSongs(int start, Playlist playlist, IServiceItemListCallback<Song> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("playlists tracks", start,
                    Arrays.asList(playlist.getFilterParameter(), "tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the SqueezeboxServer's playlists */
        @Override
        public void playlists(int start, IServiceItemListCallback<Playlist> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("playlists", start, callback);
        }

        @Override
        public void registerPlaylistMaintenanceCallback(
                IServicePlaylistMaintenanceCallback callback) {
            playlistMaintenanceCallbacks.register(callback);
        }

        @Override
        public boolean playlistsDelete(Playlist playlist) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists delete " + playlist.getFilterParameter());
            return true;
        }

        @Override
        public boolean playlistsMove(Playlist playlist, int index, int toindex) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists edit cmd:move " + playlist.getFilterParameter()
                    + " index:" + index + " toindex:" + toindex);
            return true;
        }

        @Override
        public boolean playlistsNew(String name) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists new name:" + Util.encode(name));
            return true;
        }

        @Override
        public boolean playlistsRemove(Playlist playlist, int index) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand("playlists edit cmd:delete " + playlist.getFilterParameter() + " index:"
                    + index);
            return true;
        }

        @Override
        public boolean playlistsRename(Playlist playlist, String newname) {
            if (!isConnected()) {
                return false;
            }
            cli.sendCommand(
                    "playlists rename " + playlist.getFilterParameter() + " dry_run:1 newname:"
                            + Util.encode(newname));
            return true;
        }

        /* Start an asynchronous search of the SqueezeboxServer's library */
        @Override
        public void search(int start, String searchString, IServiceItemListCallback itemListCallback) {
            if (!isConnected()) {
                return;
            }

            AlbumViewDialog.AlbumsSortOrder albumSortOrder = AlbumViewDialog.AlbumsSortOrder
                    .valueOf(
                            preferredAlbumSort());

            artists(itemListCallback, start, searchString);
            albums(itemListCallback, start, albumSortOrder.name().replace("__", ""), searchString);
            genres(start, searchString, itemListCallback);
            songs(itemListCallback, start, SongViewDialog.SongsSortOrder.title.name(), searchString);
        }

        /* Start an asynchronous fetch of the squeezeservers radio type plugins */
        @Override
        public void radios(int start, IServiceItemListCallback<Plugin> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("radios", start, callback);
        }

        /* Start an asynchronous fetch of the squeezeservers radio application plugins */
        @Override
        public void apps(int start, IServiceItemListCallback<Plugin> callback) {
            if (!isConnected()) {
                return;
            }
            cli.requestItems("apps", start, callback);
        }


        /* Start an asynchronous fetch of the squeezeservers items of the given type */
        @Override
        public void pluginItems(int start, Plugin plugin, PluginItem parent, String search, IServiceItemListCallback<PluginItem> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            if (parent != null) {
                parameters.add("item_id:" + parent.getId());
            }
            if (search != null && search.length() > 0) {
                parameters.add("search:" + search);
            }
            cli.requestPlayerItems(plugin.getId() + " items", start, parameters, callback);
        }

        @Override
        public void downloadItem(FilterItem item) {
            if (item instanceof Song) {
                Song song = (Song) item;
                if (!song.isRemote()) {
                    downloadSong(song.getId(), song.getName(), song.getUrl());
                }
            } else if (item instanceof Playlist) {
                playlistSongs(-1, (Playlist) item, songDownloadCallback);
            } else if (item instanceof MusicFolderItem) {
                MusicFolderItem musicFolderItem = (MusicFolderItem) item;
                if (musicFolderItem.getType().equals("track")) {
                    downloadSong(item.getId(), musicFolderItem.getName(), musicFolderItem.getUrl());
                } else if (musicFolderItem.getType().equals("folder")) {
                    musicFolders(-1, musicFolderItem, musicFolderDownloadCallback);
                }
            } else if (item != null) {
                songs(songDownloadCallback, -1, SongViewDialog.SongsSortOrder.title.name(), null, item);
            }
        }
    }

}
