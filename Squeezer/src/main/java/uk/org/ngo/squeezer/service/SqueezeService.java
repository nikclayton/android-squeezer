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

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.MediaScannerConnection;
import android.media.session.MediaSession;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.RandomplayActivity;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.download.DownloadDatabase;
import uk.org.ngo.squeezer.download.DownloadStorage;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.FilterItem;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.PluginItemListActivity;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SongViewDialog;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlayStatusChanged;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.service.event.SongTimeChanged;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.ImageWorker;
import uk.org.ngo.squeezer.util.Scrobble;


public class SqueezeService extends Service implements ServiceCallbackList.ServicePublisher {

    private static final String TAG = "SqueezeService";

    private static final int PLAYBACKSERVICE_STATUS = 1;
    private static final int DOWNLOAD_ERROR = 2;

    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    private static final String ALBUMTAGS = "alyj";

    /**
     * Information that will be requested about songs.
     * <p>
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
    public static final String SONGTAGS = "aCdejJKlstxyu";

    /** Service-specific eventbus. All events generated by the service will be sent here. */
    private final EventBus mEventBus = new EventBus();

    /** Executor for off-main-thread work. */
    @NonNull
    private final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(1);

    /** Handler for main-thread work. */
    @NonNull
    private final Handler mMainThreadHandler = new Handler();

    /** True if the handshake with the server has completed, otherwise false. */
    private volatile boolean mHandshakeComplete = false;

    /** Media session to associate with ongoing notifications. */
    private MediaSession mMediaSession;

    /** The player state that the most recent notifcation was for. */
    private PlayerState mNotifiedPlayerState;

    /**
     * Keeps track of all subscriptions, so we can cancel all subscriptions for a client at once
     */
    final Map<ServiceCallback, ServiceCallbackList> callbacks = new ConcurrentHashMap<ServiceCallback, ServiceCallbackList>();

    @Override
    public void addClient(ServiceCallbackList callbackList, ServiceCallback item) {
        callbacks.put(item, callbackList);
    }

    @Override
    public void removeClient(ServiceCallback item) {
        callbacks.remove(item);
    }

    final CliClient cli = new CliClient(mEventBus);

    /**
     * Is scrobbling enabled?
     */
    private boolean scrobblingEnabled;

    /**
     * Was scrobbling enabled?
     */
    private boolean scrobblingPreviouslyEnabled;

    /** User's preferred notification type. */
    @Preferences.NotificationType
    private String mNotificationType = Preferences.NOTIFICATION_TYPE_NONE;

    int mFadeInSecs;

    @Nullable String mUsername;

    @Nullable String mPassword;

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new ConcurrentHashMap<>();

    /** The active player (the player to which commands are sent by default). */
    private final AtomicReference<Player> mActivePlayer = new AtomicReference<Player>();

    private static final String ACTION_NEXT_TRACK = "uk.org.ngo.squeezer.service.ACTION_NEXT_TRACK";
    private static final String ACTION_PREV_TRACK = "uk.org.ngo.squeezer.service.ACTION_PREV_TRACK";
    private static final String ACTION_PLAY = "uk.org.ngo.squeezer.service.ACTION_PLAY";
    private static final String ACTION_PAUSE = "uk.org.ngo.squeezer.service.ACTION_PAUSE";
    private static final String ACTION_CLOSE = "uk.org.ngo.squeezer.service.ACTION_CLOSE";

    private static final String ACTION_DOWNLOAD_COMPLETE = "uk.org.ngo.squeezer.service.ACTION_DOWNLOAD_COMPLETE";
    private static final String EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID";

    private final BroadcastReceiver deviceIdleModeReceiver = new BroadcastReceiver() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onReceive(Context context, Intent intent) {
            // On M and above going in to Doze mode suspends the network but does not shut down
            // existing network connections or cause them to generate exceptions. Explicitly
            // disconnect here, so that resuming from Doze mode forces a reconnect. See
            // https://github.com/nikclayton/android-squeezer/issues/177.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                if (pm.isDeviceIdleMode()) {
                    Log.d(TAG, "Entering doze mode, disconnecting");
                    disconnect();
                }
            }
        }
    };


    /**
     * Thrown when the service is asked to send a command to the server before the server
     * handshake completes.
     */
    public static class HandshakeNotCompleteException extends IllegalStateException {
        public HandshakeNotCompleteException() { super(); }
        public HandshakeNotCompleteException(String message) { super(message); }
        public HandshakeNotCompleteException(String message, Throwable cause) { super(message, cause); }
        public HandshakeNotCompleteException(Throwable cause) { super(cause); }
    }

    public static void onDownloadComplete(Context context, long downloadId) {
        context.startService(new Intent(context, SqueezeService.class)
                .setAction(ACTION_DOWNLOAD_COMPLETE)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Clear leftover notification in case this service previously got killed while playing
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);

        cachePreferences();

        setWifiLock(((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(
                WifiManager.WIFI_MODE_FULL, "Squeezer_WifiLock"));

        mEventBus.register(this, 1);  // Get events before other subscribers
        cli.initialize();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(deviceIdleModeReceiver, new IntentFilter(
                    PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
            if(intent != null && intent.getAction()!= null ) {
                if (intent.getAction().equals(ACTION_NEXT_TRACK)) {
                    squeezeService.nextTrack();
                } else if (intent.getAction().equals(ACTION_PREV_TRACK)) {
                    squeezeService.previousTrack();
                } else if (intent.getAction().equals(ACTION_PLAY)) {
                    squeezeService.play();
                } else if (intent.getAction().equals(ACTION_PAUSE)) {
                    squeezeService.pause();
                } else if (intent.getAction().equals(ACTION_CLOSE)) {
                    squeezeService.disconnect();
                } else if (intent.getAction().equals(ACTION_DOWNLOAD_COMPLETE)) {
                    handleDownloadComplete(intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1));
                }
            }
        } catch(Exception e) {

        }
        return START_STICKY;
    }

    /**
     * Cache the value of various preferences.
     */
    private void cachePreferences() {
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        scrobblingEnabled = preferences.getBoolean(Preferences.KEY_SCROBBLE_ENABLED, false);
        mFadeInSecs = preferences.getInt(Preferences.KEY_FADE_IN_SECS, 0);
        //noinspection ResourceType
        mNotificationType = preferences.getString(Preferences.KEY_NOTIFICATION_TYPE,
                Preferences.NOTIFICATION_TYPE_PLAYING);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSession = new MediaSession(getApplicationContext(), "squeezer");
        }
        return (IBinder) squeezeService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mMediaSession != null) {
                mMediaSession.release();
            }
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
        mEventBus.unregister(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                unregisterReceiver(deviceIdleModeReceiver);
            } catch (IllegalArgumentException e) {
                // Do nothing. This can occur in testing when we destroy the service before the
                // receiver is registered.
            }
        }
    }

    void disconnect() {
        disconnect(false);
    }

    void disconnect(boolean isServerDisconnect) {
        cli.disconnect(isServerDisconnect && !mHandshakeComplete);
    }

    private String getActivePlayerId() {
        return (mActivePlayer.get() != null ? mActivePlayer.get().getId() : null);
    }

    @Nullable
    public PlayerState getPlayerState(String playerId) {
        Player player = mPlayers.get(playerId);

        if (player == null)
            return null;

        return player.getPlayerState();
    }

    /**
     * Send the specified command for the active player to the SqueezeboxServer
     *
     * @param command The command to send
     */
    public void sendActivePlayerCommand(final String command) {
        Player player = mActivePlayer.get();
        if (player == null) {
            return;
        }
        cli.sendPlayerCommand(player, command);
    }

    @Nullable public PlayerState getActivePlayerState() {
        if (mActivePlayer.get() == null)
            return null;

        return mActivePlayer.get().getPlayerState();
    }

    /**
     * The player state change might warrant a new subscription type (e.g., if the
     * player didn't have a sleep duration set, and now does).
     * @param event
     */
    public void onEvent(PlayerStateChanged event) {
        updatePlayerSubscription(event.player, calculateSubscriptionTypeFor(event.player));
    }

    /**
     * Updates the playing status of the current player.
     * <p>
     * Updates the Wi-Fi lock and ongoing status notification as necessary.
     */
    public void onEvent(PlayStatusChanged event) {
        if (event.player.equals(mActivePlayer.get())) {
            updateWifiLock(event.player.getPlayerState().isPlaying());
            updateOngoingNotification();
        }

        updatePlayerSubscription(event.player, calculateSubscriptionTypeFor(event.player));
    }

    /**
     * Change the player that is controlled by Squeezer (the "active" player).
     *
     * @param newActivePlayer The new active player. May be null, in which case no players
     *     are controlled.
     */
    void changeActivePlayer(@Nullable final Player newActivePlayer) {
        Player prevActivePlayer = mActivePlayer.get();

        // Do nothing if they player hasn't actually changed.
        if (prevActivePlayer == newActivePlayer) {
            return;
        }

        mActivePlayer.set(newActivePlayer);
        updateAllPlayerSubscriptionStates();

        Log.i(TAG, "Active player now: " + newActivePlayer);

        // If this is a new player then start an async fetch of its status.
        if (newActivePlayer != null) {
            cli.sendPlayerCommand(newActivePlayer, "status - 1 tags:" + SqueezeService.SONGTAGS);
        }

        // NOTE: this involves a write and can block (sqlite lookup via binder call), so
        // should be done off-thread, so we can process service requests & send our callback
        // as quickly as possible.
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences preferences = Squeezer.getContext().getSharedPreferences(Preferences.NAME,
                        Squeezer.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                if (newActivePlayer == null) {
                    Log.v(TAG, "Clearing " + Preferences.KEY_LAST_PLAYER);
                    editor.remove(Preferences.KEY_LAST_PLAYER);
                } else {
                    Log.v(TAG, "Saving " + Preferences.KEY_LAST_PLAYER + "=" + newActivePlayer.getId());
                    editor.putString(Preferences.KEY_LAST_PLAYER, newActivePlayer.getId());
                }

                editor.commit();
            }
        });
    }

    /**
     * Adjusts the subscription to players' status updates.
     */
    private void updateAllPlayerSubscriptionStates() {
        for (Player player : mPlayers.values()) {
            updatePlayerSubscription(player, calculateSubscriptionTypeFor(player));
        }
    }

    /**
     * Determine the correct status subscription type for the given player, based on
     * how frequently we need to know its status.
     */
    private @PlayerState.PlayerSubscriptionType String calculateSubscriptionTypeFor(Player player) {
        Player activePlayer = this.mActivePlayer.get();

        if (mEventBus.hasSubscriberForEvent(PlayerStateChanged.class) ||
                (mEventBus.hasSubscriberForEvent(SongTimeChanged.class) && player.equals(activePlayer))) {
            if (player.equals(activePlayer)) {
                // If it's the active player then get second-to-second updates.
                return PlayerState.NOTIFY_REAL_TIME;
            } else {
                // For other players get updates only when the player status changes...
                // ... unless the player has a sleep duration set. In that case we need
                // real_time updates, as on_change events are not fired as the will_sleep_in
                // timer counts down.
                if (player.getPlayerState().getSleep() > 0) {
                    return PlayerState.NOTIFY_REAL_TIME;
                } else {
                    return PlayerState.NOTIFY_ON_CHANGE;
                }
            }
        } else {
            // Disable subscription for this player's status updates.
            return PlayerState.NOTIFY_NONE;
        }
    }

    /**
     * Manage subscription to a player's status updates.
     *
     * @param player player to manage.
     * @param playerSubscriptionType the new subscription type
     */
    private void updatePlayerSubscription(
            Player player,
            @NonNull @PlayerState.PlayerSubscriptionType String playerSubscriptionType) {
        PlayerState playerState = player.getPlayerState();

        // Do nothing if the player subscription type hasn't changed. This prevents sending a
        // subscription update "status" message which will be echoed back by the server and
        // trigger processing of the status message by the service.
        if (playerState != null) {
            if (playerState.getSubscriptionType().equals(playerSubscriptionType)) {
                return;
            }
        }

        cli.sendPlayerCommand(player, "status - 1 subscribe:" + playerSubscriptionType + " tags:" + SONGTAGS);
    }

    /**
     * Manages the state of any ongoing notification based on the player and connection state.
     */
    @TargetApi(21)
    private void updateOngoingNotification() {
        Player activePlayer = this.mActivePlayer.get();
        PlayerState activePlayerState = getActivePlayerState();

        // Update scrobble state, if either we're currently scrobbling, or we
        // were (to catch the case where we started scrobbling a song, and the
        // user went in to settings to disable scrobbling).
        if (scrobblingEnabled || scrobblingPreviouslyEnabled) {
            scrobblingPreviouslyEnabled = scrobblingEnabled;
            Scrobble.scrobbleFromPlayerState(this, activePlayerState);
        }

        // If there's no active player then kill the notification and get out.
        // TODO: Have a "There are no connected players" notification text.
        if (activePlayer == null || activePlayerState == null) {
            clearOngoingNotification();
            return;
        }

        // If the user doesn't want notifications then kill it and get out.
        if (Preferences.NOTIFICATION_TYPE_NONE.equals(mNotificationType)) {
            clearOngoingNotification();
            return;
        }

        boolean playing = activePlayerState.isPlaying();

        // If the song is not playing and the user wants notifications only when playing then
        // kill the notification and get out.
        if (!playing && Preferences.NOTIFICATION_TYPE_PLAYING.equals(mNotificationType)) {
            clearOngoingNotification();
            return;
        }

        // If there's no current song then kill the notification and get out.
        // TODO: Have a "There's nothing playing" notification text.
        final Song currentSong = activePlayerState.getCurrentSong();
        if (currentSong == null) {
            clearOngoingNotification();
            return;
        }

        // Compare the current state with the state when the notification was last updated.
        // If there are no changes (same song, same playing state) then there's nothing to do.
        String songName = currentSong.getName();
        String albumName = currentSong.getAlbumName();
        String artistName = currentSong.getArtist();
        Uri url = currentSong.getArtworkUrl();
        String playerName = activePlayer.getName();

        if (mNotifiedPlayerState == null) {
            mNotifiedPlayerState = new PlayerState();
        } else {
            boolean lastPlaying = mNotifiedPlayerState.isPlaying();
            Song lastNotifiedSong = mNotifiedPlayerState.getCurrentSong();

            // No change in state
            if (playing == lastPlaying && currentSong.equals(lastNotifiedSong)) {
                return;
            }
        }

        mNotifiedPlayerState.setCurrentSong(currentSong);
        mNotifiedPlayerState.setPlayStatus(activePlayerState.getPlayStatus());
        final NotificationManagerCompat nm = NotificationManagerCompat.from(this);

        PendingIntent nextPendingIntent = getPendingIntent(ACTION_NEXT_TRACK);
        PendingIntent prevPendingIntent = getPendingIntent(ACTION_PREV_TRACK);
        PendingIntent playPendingIntent = getPendingIntent(ACTION_PLAY);
        PendingIntent pausePendingIntent = getPendingIntent(ACTION_PAUSE);
        PendingIntent closePendingIntent = getPendingIntent(ACTION_CLOSE);

        Intent showNowPlaying = new Intent(this, NowPlayingActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, showNowPlaying, 0);
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Notification.Builder builder = new Notification.Builder(this);
            builder.setContentIntent(pIntent);
            builder.setSmallIcon(R.drawable.squeezer_notification);
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            builder.setShowWhen(false);
            builder.setContentTitle(songName);
            builder.setContentText(albumName);
            builder.setSubText(playerName);
            builder.setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(1, 2)
                    .setMediaSession(mMediaSession.getSessionToken()));

            final MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder();
            metaBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artistName);
            metaBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, albumName);
            metaBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, songName);
            mMediaSession.setMetadata(metaBuilder.build());

            // Don't set an ongoing notification, otherwise wearable's won't show it.
            builder.setOngoing(false);

            builder.setDeleteIntent(closePendingIntent);
            if (playing) {
                builder.addAction(new Notification.Action(R.drawable.ic_action_previous, "Previous", prevPendingIntent))
                        .addAction(new Notification.Action(R.drawable.ic_action_pause, "Pause", pausePendingIntent))
                        .addAction(new Notification.Action(R.drawable.ic_action_next, "Next", nextPendingIntent));
            } else {
                builder.addAction(new Notification.Action(R.drawable.ic_action_previous, "Previous", prevPendingIntent))
                        .addAction(new Notification.Action(R.drawable.ic_action_play, "Play", playPendingIntent))
                        .addAction(new Notification.Action(R.drawable.ic_action_next, "Next", nextPendingIntent));
            }

            ImageFetcher.getInstance(this).loadImage(url,
                    getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                    new ImageWorker.ImageWorkerCallback() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        public void process(Object data, @Nullable Bitmap bitmap) {
                            if (bitmap == null) {
                                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_album_noart);
                            }

                            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
                            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                            mMediaSession.setMetadata(metaBuilder.build());
                            builder.setLargeIcon(bitmap);
                            nm.notify(PLAYBACKSERVICE_STATUS, builder.build());
                        }
                    });
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            builder.setOngoing(true);
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            builder.setSmallIcon(R.drawable.squeezer_notification);

            RemoteViews normalView = new RemoteViews(this.getPackageName(), R.layout.notification_player_normal);
            RemoteViews expandedView = new RemoteViews(this.getPackageName(), R.layout.notification_player_expanded);

            normalView.setOnClickPendingIntent(R.id.next, nextPendingIntent);

            expandedView.setOnClickPendingIntent(R.id.previous, prevPendingIntent);
            expandedView.setOnClickPendingIntent(R.id.next, nextPendingIntent);

            builder.setContent(normalView);

            normalView.setTextViewText(R.id.trackname, songName);
            normalView.setTextViewText(R.id.albumname, albumName);

            expandedView.setTextViewText(R.id.trackname, songName);
            expandedView.setTextViewText(R.id.albumname, albumName);
            expandedView.setTextViewText(R.id.player_name, playerName);

            if (playing) {
                normalView.setImageViewResource(R.id.pause, R.drawable.ic_action_pause);
                normalView.setOnClickPendingIntent(R.id.pause, pausePendingIntent);

                expandedView.setImageViewResource(R.id.pause, R.drawable.ic_action_pause);
                expandedView.setOnClickPendingIntent(R.id.pause, pausePendingIntent);
            } else {
                normalView.setImageViewResource(R.id.pause, R.drawable.ic_action_play);
                normalView.setOnClickPendingIntent(R.id.pause, playPendingIntent);

                expandedView.setImageViewResource(R.id.pause, R.drawable.ic_action_play);
                expandedView.setOnClickPendingIntent(R.id.pause, playPendingIntent);
            }

            builder.setContentTitle(songName);
            builder.setContentText(getString(R.string.notification_playing_text, playerName));
            builder.setContentIntent(pIntent);

            notification = builder.build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.bigContentView = expandedView;
            }

            nm.notify(PLAYBACKSERVICE_STATUS, notification);

            ImageFetcher.getInstance(this).loadImage(this, url, normalView, R.id.album,
                    getResources().getDimensionPixelSize(R.dimen.album_art_icon_normal_notification_width),
                    getResources().getDimensionPixelSize(R.dimen.album_art_icon_normal_notification_height),
                    nm, PLAYBACKSERVICE_STATUS, notification);
            ImageFetcher.getInstance(this).loadImage(this, url, expandedView, R.id.album,
                    getResources().getDimensionPixelSize(R.dimen.album_art_icon_expanded_notification_width),
                    getResources().getDimensionPixelSize(R.dimen.album_art_icon_expanded_notification_height),
                    nm, PLAYBACKSERVICE_STATUS, notification);
        }
    }

    /**
     * @param action The action to be performed.
     * @return A new {@link PendingIntent} for {@literal action} that will update any existing
     *     intents that use the same action.
     */
    @NonNull
    private PendingIntent getPendingIntent(@NonNull String action){
        Intent intent = new Intent(this, SqueezeService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void clearOngoingNotification() {
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        nm.cancel(PLAYBACKSERVICE_STATUS);
        mNotifiedPlayerState = null;
    }

    public void onEvent(ConnectionChanged event) {
        if (event.connectionState == ConnectionState.DISCONNECTED) {
            mPlayers.clear();
            mEventBus.removeAllStickyEvents();
            mActivePlayer.set(null);
            mHandshakeComplete = false;
            clearOngoingNotification();
        }
    }

    public void onEvent(HandshakeComplete event) {
        mHandshakeComplete = true;
        strings();
    }

    public void onEvent(MusicChanged event) {
        if (event.player.equals(mActivePlayer.get())) {
            updateOngoingNotification();
        }
    }

    public void onEvent(PlayersChanged event) {
        mPlayers.clear();
        mPlayers.putAll(event.players);

        // Figure out the new active player, let everyone know.
        changeActivePlayer(getPreferredPlayer());
    }

    /**
     * @return The player that should be chosen as the (new) active player. This is either the
     *     last active player (if known), the first player the server knows about if there are
     *     connected players, or null if there are no connected players.
     */
    private @Nullable Player getPreferredPlayer() {
        final SharedPreferences preferences = Squeezer.getContext().getSharedPreferences(Preferences.NAME,
                Context.MODE_PRIVATE);
        final String lastConnectedPlayer = preferences.getString(Preferences.KEY_LAST_PLAYER,
                null);
        Log.i(TAG, "lastConnectedPlayer was: " + lastConnectedPlayer);

        Collection<Player> players = mPlayers.values();
        Log.i(TAG, "mPlayers empty?: " + mPlayers.isEmpty());
        for (Player player : players) {
            if (player.getId().equals(lastConnectedPlayer)) {
                return player;
            }
        }
        return !players.isEmpty() ? players.iterator().next() : null;
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
                downloadSong(item);
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

    private void downloadSong(Song song) {
        final Preferences preferences = new Preferences(this);
        if (preferences.isDownloadUseServerPath()) {
            downloadSong(song.getDownloadUrl(), song.getName(), song.getUrl(), song.getArtworkUrl());
        } else {
            final String lastPathSegment = song.getUrl().getLastPathSegment();
            final String fileExtension = Files.getFileExtension(lastPathSegment);
            final String localPath = song.getLocalPath(preferences.getDownloadPathStructure(), preferences.getDownloadFilenameStructure());
            downloadSong(song.getDownloadUrl(), song.getName(), localPath+"."+fileExtension, song.getArtworkUrl());
        }
    }

    private void downloadSong(@NonNull Uri url, String title, @NonNull Uri serverUrl, @NonNull Uri albumArtUrl) {
        downloadSong(url, title, getLocalFile(serverUrl), albumArtUrl);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void downloadSong(@NonNull Uri url, String title, String localPath, @NonNull Uri albumArtUrl) {
        if (url.equals(Uri.EMPTY)) {
            return;
        }

        if (localPath == null) {
            return;
        }

        // Convert VFAT-unfriendly characters to "_".
        localPath =  localPath.replaceAll("[?<>\\\\:*|\"]", "_");

        // If running on Gingerbread or greater use the Download Manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadDatabase downloadDatabase = new DownloadDatabase(this);
            String tempFile = UUID.randomUUID().toString();
            String credentials = mUsername + ":" + mPassword;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            DownloadManager.Request request = new DownloadManager.Request(url)
                    .setTitle(title)
                    .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MUSIC, tempFile)
                    .setVisibleInDownloadsUi(false)
                    .addRequestHeader("Authorization", "Basic " + base64EncodedCredentials);
            long downloadId = downloadManager.enqueue(request);

            if (!downloadDatabase.registerDownload(downloadId, tempFile, localPath, albumArtUrl)) {
                Util.crashlyticsLog(Log.WARN, TAG, "Could not register download entry for: " + downloadId);
                downloadManager.remove(downloadId);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void handleDownloadComplete(long id) {
        final DownloadStorage downloadStorage = new DownloadStorage(this);
        final DownloadDatabase downloadDatabase = new DownloadDatabase(this);
        final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);

        final Cursor cursor = downloadManager.query(query);
        try {
            if (!cursor.moveToNext()) {
                // Download complete events may still come in, even after DownloadManager.remove is
                // called, so don't log this
                //Logger.logError(TAG, "Download manager does not have an entry for " + id);
                return;
            }

            int downloadId = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
            String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
            String url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
            String local_url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

            final DownloadDatabase.DownloadEntry downloadEntry = downloadDatabase.popDownloadEntry(downloadId);
            if (downloadEntry == null) {
                Util.crashlyticsLog(Log.ERROR, TAG, "Download database does not have an entry for " + format(status, reason, title, url, local_url));
                return;
            }
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                Util.crashlyticsLog(Log.ERROR, TAG, "Unsuccessful download " + format(status, reason, title, url, local_url));
                return;
            }

            File tempFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), downloadEntry.tempName);
            try {
                File localFile = new File(downloadStorage.getDownloadDir(), downloadEntry.fileName);
                Util.moveFile(tempFile, localFile);
                MediaScannerConnection.scanFile(
                        getApplicationContext(),
                        new String[]{localFile.getAbsolutePath()},
                        null,
                        new DownloadOnScanCompletedListener(downloadEntry)
                );
            } catch (IOException e) {
                Util.crashlyticsLogException(e);
            }
        } finally {
            cursor.close();
        }
    }

    private String format(int status, int reason, String title, String url, String local_url) {
        return "{status:" + status + ", reason:" + reason + ", title:'" + title + "', url:'" + url + "', local url:'" + local_url + "'}";
    }

    /**
     * Tries to get the path relative to the server music library.
     * <p>
     * If this is not possible resort to the last path segment of the server path.
     */
    @Nullable
    private String getLocalFile(@NonNull Uri serverUrl) {
        String serverPath = serverUrl.getPath();
        String mediaDir = null;
        String path;
        for (String dir : cli.getMediaDirs()) {
            if (serverPath.startsWith(dir)) {
                mediaDir = dir;
                break;
            }
        }
        if (mediaDir != null) {
            path = serverPath.substring(mediaDir.length(), serverPath.length());
        } else {
            // Note: if serverUrl is the empty string this can return null.
            path = serverUrl.getLastPathSegment();
        }

        return path;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class DownloadOnScanCompletedListener implements MediaScannerConnection.OnScanCompletedListener {
        private final DownloadDatabase.DownloadEntry downloadEntry;

        public DownloadOnScanCompletedListener(DownloadDatabase.DownloadEntry downloadEntry) {
            this.downloadEntry = downloadEntry;
        }

        @Override
        public void onScanCompleted(String path, final Uri uri) {
            if (uri == null) {
                // Scanning failed, probably the file format is not supported.
                Log.i(TAG, "'" + path + "' could not be added to the media database");
                if (!new File(path).delete()) {
                    Util.crashlyticsLog(Log.ERROR, TAG, "Could not delete '" + path + "', which could not be added to the media database");
                }
                notifyFailedMediaScan(downloadEntry.fileName);
            }
        }

        private void notifyFailedMediaScan(String fileName) {
            String name = Util.getBaseName(fileName);

            // Content intent is required on some API levels even if
            // https://developer.android.com/guide/topics/ui/notifiers/notifications.html
            // says it's optional
            PendingIntent emptyPendingIntent = PendingIntent.getService(
                    SqueezeService.this,
                    0,
                    new Intent(),  //Dummy Intent do nothing
                    0);

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(SqueezeService.this);
            builder.setContentIntent(emptyPendingIntent);
            builder.setOngoing(false);
            builder.setOnlyAlertOnce(true);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.squeezer_notification);
            builder.setTicker(name + " " + getString(R.string.NOTIFICATION_DOWNLOAD_MEDIA_SCANNER_ERROR));
            builder.setContentTitle(name);
            builder.setContentText(getString(R.string.NOTIFICATION_DOWNLOAD_MEDIA_SCANNER_ERROR));

            final NotificationManagerCompat nm = NotificationManagerCompat.from(SqueezeService.this);
            nm.notify(DOWNLOAD_ERROR, builder.build());
        }
    }


    private WifiManager.WifiLock wifiLock;

    void setWifiLock(WifiManager.WifiLock wifiLock) {
        this.wifiLock = wifiLock;
    }

    void updateWifiLock(boolean state) {
        // TODO: this might be running in the wrong thread.  Is wifiLock thread-safe?
        if (state && !wifiLock.isHeld()) {
            Log.v(TAG, "Locking wifi while playing.");
            wifiLock.acquire();
        }
        if (!state && wifiLock.isHeld()) {
            Log.v(TAG, "Unlocking wifi.");
            try {
                wifiLock.release();
                // Seen a crash here with:
                //
                // Permission Denial: broadcastIntent() requesting a sticky
                // broadcast
                // from pid=29506, uid=10061 requires
                // android.permission.BROADCAST_STICKY
                //
                // Catching the exception (which seems harmless) seems better
                // than requesting an additional permission.

                // Seen a crash here with
                //
                // java.lang.RuntimeException: WifiLock under-locked
                // Squeezer_WifiLock
                //
                // Both crashes occurred when the wifi was disabled, on HTC Hero
                // devices running 2.1-update1.
            } catch (SecurityException e) {
                Log.v(TAG, "Caught odd SecurityException releasing wifilock");
            }
        }
    }

    private final ISqueezeService squeezeService = new SqueezeServiceBinder();
    private class SqueezeServiceBinder extends Binder implements ISqueezeService {

        @Override
        @NonNull
        public EventBus getEventBus() {
            return mEventBus;
        }

        @Override
        public void adjustVolumeTo(Player player, int newVolume) {
            cli.sendPlayerCommand(player, "mixer volume " + Math.min(100, Math.max(0, newVolume)));
        }

        @Override
        public void adjustVolumeTo(int newVolume) {
            sendActivePlayerCommand("mixer volume " + Math.min(100, Math.max(0, newVolume)));
        }

        @Override
        public void adjustVolumeBy(int delta) {
            if (delta > 0) {
                sendActivePlayerCommand("mixer volume %2B" + delta);
            } else if (delta < 0) {
                sendActivePlayerCommand("mixer volume " + delta);
            }
        }

        @Override
        public boolean isConnected() {
            return cli.isConnected();
        }

        @Override
        public boolean isConnectInProgress() {
            return cli.isConnectInProgress();
        }

        @Override
        public void startConnect(String hostPort, String userName, String password) {
            mUsername = userName;
            mPassword = password;
            cli.startConnect(SqueezeService.this, hostPort, userName, password);
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
            sendActivePlayerCommand("power 1");
        }

        @Override
        public void powerOff() {
            sendActivePlayerCommand("power 0");
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
        public void syncPlayerToPlayer(@NonNull Player slave, @NonNull String masterId) {
            Player master = mPlayers.get(masterId);
            cli.sendPlayerCommand(master, "sync " + Util.encode(slave.getId()));
        }

        @Override
        public void unsyncPlayer(@NonNull Player player) {
            cli.sendPlayerCommand(player, "sync -");
        }


        @Override
        @Nullable
        public PlayerState getActivePlayerState() {
            if (mActivePlayer == null) {
                return null;
            }
            Player activePlayer = mActivePlayer.get();
            if (activePlayer == null) {
                return null;
            }

            return activePlayer.getPlayerState();
        }

        @Override
        @Nullable
        public PlayerState getPlayerState(String playerId) {
            Player player = mPlayers.get(playerId);
            if (player == null) {
                return null;
            }

            return player.getPlayerState();
        }

        /**
         * Issues a query for given player preference.
         *
         * @param playerPref
         */
        @Override
        public void playerPref(@Player.Pref.Name String playerPref) {
            playerPref(playerPref, "?");
        }

        @Override
        public void playerPref(@Player.Pref.Name String playerPref, String value) {
            sendActivePlayerCommand("playerpref " + playerPref + " " + value);
        }

        @Override
        public boolean canPowerOn() {
            Player activePlayer = getActivePlayer();

            if (activePlayer == null) {
                return false;
            } else {
                PlayerState playerState = activePlayer.getPlayerState();
                return canPower() && activePlayer.getConnected() && playerState != null
                        && !playerState.isPoweredOn();
            }
        }

        @Override
        public boolean canPowerOff() {
            Player activePlayer = getActivePlayer();

            if (activePlayer == null) {
                return false;
            } else {
                PlayerState playerState = activePlayer.getPlayerState();
                return canPower() && activePlayer.getConnected() && playerState != null
                        && playerState.isPoweredOn();
            }
        }

        private boolean canPower() {
            Player player = mActivePlayer.get();
            return cli.isConnected() && player != null && player.isCanpoweroff();
        }

        @Override
        public ServerVersion getServerVersion() throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            return cli.getServerVersion();
        }

        @Override
        public String preferredAlbumSort() throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            return cli.getPreferredAlbumSort();
        }

        @Override
        public void setPreferredAlbumSort(String preferredAlbumSort) {
            if (isConnected()) {
                cli.sendCommand("pref jivealbumsort " + Util.encode(preferredAlbumSort));
            }
        }

        private String fadeInSecs() {
            return mFadeInSecs > 0 ? " " + mFadeInSecs : "";
        }

        @Override
        public boolean togglePausePlay() {
            if (!isConnected()) {
                return false;
            }

            PlayerState activePlayerState = getActivePlayerState();

            // May be null (e.g., connected to a server with no connected
            // players. TODO: Handle this better, since it's not obvious in the
            // UI.
            if (activePlayerState == null)
                return false;

            @PlayerState.PlayState String playStatus = activePlayerState.getPlayStatus();

            // May be null -- race condition when connecting to a server that
            // has a player. Squeezer knows the player exists, but has not yet
            // determined its state.
            if (playStatus == null)
                return false;

            if (playStatus.equals(PlayerState.PLAY_STATE_PLAY)) {
                // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
                // because then we'd get confused when they came back in to us, not being
                // able to differentiate ours coming back on the listen channel vs. those
                // of those idiots at the dinner party messing around.
                sendActivePlayerCommand("pause 1");
                return true;
            }

            if (playStatus.equals(PlayerState.PLAY_STATE_STOP)) {
                sendActivePlayerCommand("play" + fadeInSecs());
                return true;
            }

            if (playStatus.equals(PlayerState.PLAY_STATE_PAUSE)) {
                sendActivePlayerCommand("pause 0" + fadeInSecs());
                return true;
            }

            return true;
        }

        @Override
        public boolean play() {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("play" + fadeInSecs());
            return true;
        }

        @Override
        public boolean pause() {
            if(!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("pause 1" + fadeInSecs());
            return true;
        }

        @Override
        public boolean stop() {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("stop");
            return true;
        }

        @Override
        public boolean nextTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            sendActivePlayerCommand("button jump_fwd");
            return true;
        }

        @Override
        public boolean previousTrack() {
            if (!isConnected() || !isPlaying()) {
                return false;
            }
            sendActivePlayerCommand("button jump_rew");
            return true;
        }

        @Override
        public boolean toggleShuffle() {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist shuffle");
            return true;
        }

        @Override
        public boolean toggleRepeat() {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist repeat");
            return true;
        }

        @Override
        public boolean playlistControl(@BaseActivity.PlaylistControlCmd String cmd, PlaylistItem playlistItem, int index) {
            if (!isConnected()) {
                return false;
            }

            sendActivePlayerCommand(
                    "playlistcontrol cmd:" + cmd + " " + playlistItem.getPlaylistParameter() + " play_index:" + index);
            return true;
        }

        @Override
        public boolean randomPlay(@RandomplayActivity.RandomplayType String type) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            sendActivePlayerCommand("randomplay " + type);
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
            sendActivePlayerCommand("playlist index " + index + fadeInSecs());
            return true;
        }

        @Override
        public boolean playlistRemove(int index) {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist delete " + index);
            return true;
        }

        @Override
        public boolean playlistMove(int fromIndex, int toIndex) {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist move " + fromIndex + " " + toIndex);
            return true;
        }

        @Override
        public boolean playlistClear() {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist clear");
            return true;
        }

        @Override
        public boolean playlistSave(String name) {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand("playlist save " + Util.encode(name));
            return true;
        }

        @Override
        public boolean pluginPlaylistControl(
                Plugin plugin, @PluginItemListActivity.PluginPlaylistControlCmd String cmd,
                String itemId) {
            if (!isConnected()) {
                return false;
            }
            sendActivePlayerCommand(plugin.getId() + " playlist " + cmd + " item_id:" + itemId);
            return true;

        }

        private boolean isPlaying() {
            PlayerState playerState = getActivePlayerState();
            return playerState != null && playerState.isPlaying();
        }

        /**
         * Change the player that is controlled by Squeezer (the "active" player).
         *
         * @param newActivePlayer May be null, in which case no players are controlled.
         */
        @Override
        public void setActivePlayer(@Nullable final Player newActivePlayer) {
            changeActivePlayer(newActivePlayer);
        }

        @Override
        @Nullable
        public Player getActivePlayer() {
            return mActivePlayer.get();
        }

        @Override
        public List<Player> getPlayers() {
            // TODO: Return a Collection, instead of casting? Or return an ImmutableList?
            return (List<Player>) new ArrayList<Player>(mPlayers.values());
        }

        @Override
        public java.util.Collection<Player> getConnectedPlayers() {
            return mPlayers.values();
        }

        @Override
        public PlayerState getPlayerState() {
            return getActivePlayerState();
        }

        /**
         * @return null if there is no active player, otherwise the name of the current playlist,
         *     which may be the empty string.
         */
        @Override
        @Nullable
        public String getCurrentPlaylist() {
            PlayerState playerState = getActivePlayerState();

            if (playerState == null)
                return null;

            return playerState.getCurrentPlaylist();
        }

        @Override
        public boolean setSecondsElapsed(int seconds) {
            if (!isConnected()) {
                return false;
            }
            if (seconds < 0) {
                return false;
            }

            sendActivePlayerCommand("time " + seconds);

            return true;
        }

        @Override
        public void preferenceChanged(String key) {
            Log.i(TAG, "Preference changed: " + key);
            cachePreferences();

            if (Preferences.KEY_NOTIFICATION_TYPE.equals(key)) {
                updateOngoingNotification();
                return;
            }

            // If the server address changed then disconnect.
            if (key.startsWith(Preferences.KEY_SERVER_ADDRESS)) {
                disconnect();
                return;
            }
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
            updateAllPlayerSubscriptionStates();
        }

        // XXX: Is this method needed? What calls it?
        @Override
        public void players() throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            //fetchPlayers();
        }

        @Override
        public void alarms(int start, IServiceItemListCallback<Alarm> callback) {
            if (!isConnected()) {
                return;
            }
            List<String> parameters = new ArrayList<String>();
            parameters.add("filter:all");
            cli.requestPlayerItems(mActivePlayer.get(), "alarms", start, parameters, callback);
        }

        @Override
        public void alarmPlaylists(IServiceItemListCallback<AlarmPlaylist> callback) {
            if (!isConnected()) {
                return;
            }
            // The LMS documentation states that
            // The "alarm playlists" returns all the playlists, sounds, favorites etc. available to alarms.
            // This will however return only one playlist: the current playlist.
            // Inspection of the LMS code reveals that the "alarm playlists" command takes the
            // customary <start> and <itemsPerResponse> parameters, but these are interpreted as
            // categories (eg. Favorites, Natural Sounds etc.), but the returned list is flattened,
            // i.e. contains all items of the requested categories.
            // So we order all playlists like below, hoping there are no more than 99 categories.
            cli.requestItems("alarm playlists", 0, 99, callback);
        }

        @Override
        public void alarmAdd(int time) {
            if (!isConnected()) {
                return;
            }
            sendActivePlayerCommand("alarm add time:" + time);
        }

        @Override
        public void alarmDelete(String id) {
            if (!isConnected()) {
                return;
            }
            sendActivePlayerCommand("alarm delete id:" + Util.encode(id));
        }

        @Override
        public void alarmSetTime(String id, int time) {
            if (!isConnected()) {
                return;
            }
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " time:" + time);
        }

        @Override
        public void alarmAddDay(String id, int day) {
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " dowAdd:" + day);
        }

        @Override
        public void alarmRemoveDay(String id, int day) {
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " dowDel:" + day);
        }

        @Override
        public void alarmEnable(String id, boolean enabled) {
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " enabled:" + (enabled ? "1" : "0"));
        }

        @Override
        public void alarmRepeat(String id, boolean repeat) {
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " repeat:" + (repeat ? "1" : "0"));
        }

        @Override
        public void alarmSetPlaylist(String id, AlarmPlaylist playlist) {
            String url = "".equals(playlist.getId()) ? "0" : playlist.getId();
            sendActivePlayerCommand("alarm update id:" + Util.encode(id) + " url:" + Util.encode(url));
        }

        /* Start an async fetch of the SqueezeboxServer's albums, which are matching the given parameters */
        @Override
        public void albums(IServiceItemListCallback<Album> callback, int start, String sortOrder, String searchString, FilterItem... filters) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void artists(IServiceItemListCallback<Artist> callback, int start, String searchString, FilterItem... filters) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void years(int start, IServiceItemListCallback<Year> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestItems("years", start, callback);
        }

        /* Start an async fetch of the SqueezeboxServer's genres */
        @Override
        public void genres(int start, String searchString, IServiceItemListCallback<Genre> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void musicFolders(int start, MusicFolderItem musicFolderItem, IServiceItemListCallback<MusicFolderItem> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void songs(IServiceItemListCallback<Song> callback, int start, String sortOrder, String searchString, FilterItem... filters) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void currentPlaylist(int start, IServiceItemListCallback<Song> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestPlayerItems(mActivePlayer.get(), "status", start, Arrays.asList("tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the songs of the supplied playlist */
        @Override
        public void playlistSongs(int start, Playlist playlist, IServiceItemListCallback<Song> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestItems("playlists tracks", start,
                    Arrays.asList(playlist.getFilterParameter(), "tags:" + SONGTAGS), callback);
        }

        /* Start an async fetch of the SqueezeboxServer's playlists */
        @Override
        public void playlists(int start, IServiceItemListCallback<Playlist> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestItems("playlists", start, callback);
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
        public void search(int start, String searchString, IServiceItemListCallback itemListCallback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
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
        public void radios(int start, IServiceItemListCallback<Plugin> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestItems("radios", start, callback);
        }

        /* Start an asynchronous fetch of the squeezeservers radio application plugins */
        @Override
        public void apps(int start, IServiceItemListCallback<Plugin> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            cli.requestItems("apps", start, callback);
        }


        /* Start an asynchronous fetch of the squeezeservers items of the given type */
        @Override
        public void pluginItems(int start, Plugin plugin, PluginItem parent, String search, IServiceItemListCallback<PluginItem> callback) throws HandshakeNotCompleteException {
            if (!mHandshakeComplete) {
                throw new HandshakeNotCompleteException("Handshake with server has not completed.");
            }
            List<String> parameters = new ArrayList<String>();
            if (parent != null) {
                parameters.add("item_id:" + parent.getId());
            }
            if (search != null && search.length() > 0) {
                parameters.add("search:" + search);
            }
            cli.requestPlayerItems(mActivePlayer.get(), plugin.getId() + " items", start, parameters, callback);
        }

        @Override
        public void downloadItem(FilterItem item) throws HandshakeNotCompleteException {
            if (item instanceof Song) {
                Song song = (Song) item;
                if (!song.isRemote()) {
                    downloadSong(song);
                }
            } else if (item instanceof Playlist) {
                playlistSongs(-1, (Playlist) item, songDownloadCallback);
            } else if (item instanceof MusicFolderItem) {
                MusicFolderItem musicFolderItem = (MusicFolderItem) item;
                if ("track".equals(musicFolderItem.getType())) {
                    Uri url = musicFolderItem.getUrl();
                    if (! url.equals(Uri.EMPTY)) {
                        downloadSong(musicFolderItem.getDownloadUrl(), musicFolderItem.getName(), url, Uri.EMPTY);
                    }
                } else if ("folder".equals(musicFolderItem.getType())) {
                    musicFolders(-1, musicFolderItem, musicFolderDownloadCallback);
                }
            } else if (item != null) {
                songs(songDownloadCallback, -1, SongViewDialog.SongsSortOrder.title.name(), null, item);
            }
        }
    }

    /**
     * Calculate and set player subscription states every time a client of the bus
     * un/registers.
     * <p>
     * For example, this ensures that if a new client subscribes and needs real
     * time updates, the player subscription states will be updated accordingly.
     */
    class EventBus extends de.greenrobot.event.EventBus {

        @Override
        public void register(Object subscriber) {
            super.register(subscriber);
            updateAllPlayerSubscriptionStates();
        }

        @Override
        public void register(Object subscriber, int priority) {
            super.register(subscriber, priority);
            updateAllPlayerSubscriptionStates();
        }

        @Override
        public void post(Object event) {
            Log.v("EventBus", "post() " + event.getClass().getSimpleName() + ": " + event);
            super.post(event);
        }

        @Override
        public void postSticky(Object event) {
            Log.v("EventBus", "postSticky() " + event.getClass().getSimpleName() + ": " + event);
            super.postSticky(event);
        }

        @Override
        public void registerSticky(Object subscriber) {
            super.registerSticky(subscriber);
            updateAllPlayerSubscriptionStates();
        }

        @Override
        public void registerSticky(Object subscriber, int priority) {
            super.registerSticky(subscriber, priority);
            updateAllPlayerSubscriptionStates();
        }

        @Override
        public synchronized void unregister(Object subscriber) {
            super.unregister(subscriber);
            updateAllPlayerSubscriptionStates();
        }
    }
}
