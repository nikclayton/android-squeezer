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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.IServiceCallback;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SqueezerActivity;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.VolumePanel;
import uk.org.ngo.squeezer.itemlists.IServiceAlbumListCallback;
import uk.org.ngo.squeezer.itemlists.IServiceArtistListCallback;
import uk.org.ngo.squeezer.itemlists.IServiceGenreListCallback;
import uk.org.ngo.squeezer.itemlists.IServiceMusicFolderListCallback;
import uk.org.ngo.squeezer.itemlists.IServicePlayerListCallback;
import uk.org.ngo.squeezer.itemlists.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.itemlists.IServicePlaylistsCallback;
import uk.org.ngo.squeezer.itemlists.IServicePluginItemListCallback;
import uk.org.ngo.squeezer.itemlists.IServicePluginListCallback;
import uk.org.ngo.squeezer.itemlists.IServiceSongListCallback;
import uk.org.ngo.squeezer.itemlists.IServiceYearListCallback;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerMusicFolderItem;
import uk.org.ngo.squeezer.model.SqueezerPlayer;
import uk.org.ngo.squeezer.model.SqueezerPlayerState;
import uk.org.ngo.squeezer.model.SqueezerPlayerState.PlayerStateChanged;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import uk.org.ngo.squeezer.model.SqueezerPlugin;
import uk.org.ngo.squeezer.model.SqueezerPluginItem;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class SqueezeService extends Service {
	private static final String TAG = "SqueezeService";
    private static final int PLAYBACKSERVICE_STATUS = 1;

	private static final String ALBUMTAGS = "alyj";
    private static final String SONGTAGS = "asleyjJxK";

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    final AtomicReference<IServicePlayerListCallback> playerListCallback = new AtomicReference<IServicePlayerListCallback>();
	final AtomicReference<IServiceAlbumListCallback> albumListCallback = new AtomicReference<IServiceAlbumListCallback>();
	final AtomicReference<IServiceArtistListCallback> artistListCallback = new AtomicReference<IServiceArtistListCallback>();
	final AtomicReference<IServiceYearListCallback> yearListCallback = new AtomicReference<IServiceYearListCallback>();
	final AtomicReference<IServiceGenreListCallback> genreListCallback = new AtomicReference<IServiceGenreListCallback>();
	final AtomicReference<IServiceSongListCallback> songListCallback = new AtomicReference<IServiceSongListCallback>();
	final AtomicReference<IServicePlaylistsCallback> playlistsCallback = new AtomicReference<IServicePlaylistsCallback>();
	final AtomicReference<IServicePlaylistMaintenanceCallback> playlistMaintenanceCallback = new AtomicReference<IServicePlaylistMaintenanceCallback>();
	final AtomicReference<IServicePluginListCallback> pluginListCallback = new AtomicReference<IServicePluginListCallback>();
	final AtomicReference<IServicePluginItemListCallback> pluginItemListCallback = new AtomicReference<IServicePluginItemListCallback>();
    final AtomicReference<IServiceMusicFolderListCallback> musicFolderListCallback = new AtomicReference<IServiceMusicFolderListCallback>();

    SqueezerConnectionState connectionState = new SqueezerConnectionState();
    SqueezerPlayerState playerState = new SqueezerPlayerState();
    SqueezerCLIImpl cli = new SqueezerCLIImpl(this);

    private VolumePanel mVolumePanel;

    private static final int SCROBBLE_NONE = 0;
    private static final int SCROBBLE_SCROBBLEDROID = 1;
    private static final int SCROBBLE_SLS = 2;

    int scrobbleType;
    boolean debugLogging = false;

    SharedPreferences preferences;

    @Override
    public void onCreate() {
    	super.onCreate();

    	// Create the volume panel
    	mVolumePanel = new VolumePanel(this.getApplicationContext());

        // Clear leftover notification in case this service previously got killed while playing
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
        connectionState.setWifiLock(((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(
                WifiManager.WIFI_MODE_FULL, "Squeezer_WifiLock"));

        preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        getPreferences();

        cli.initialize();
    }

	private void getPreferences() {
		scrobbleType = Integer.parseInt(preferences.getString(Preferences.KEY_SCROBBLE, "0"));
		debugLogging = preferences.getBoolean(Preferences.KEY_DEBUG_LOGGING, false);
	}

	@Override
	public IBinder onBind(Intent intent) {
        return squeezeService;
    }

    @Override
	public void onDestroy() {
        super.onDestroy();
        disconnect();
        connectionState.setCallback(null);
    }

    void disconnect() {
    	connectionState.disconnect();
        clearOngoingNotification();
        playerState = new SqueezerPlayerState();
    }


    private interface SqueezerCmdHandler {
    	public void handle(List<String> tokens);
    }

	private Map<String, SqueezerCmdHandler> initializeGlobalHandlers() {
		Map<String, SqueezerCmdHandler> handlers = new HashMap<String, SqueezerCmdHandler>();

		for (final SqueezerCLIImpl.ExtendedQueryFormatCmd cmd: cli.extQueryFormatCmds) {
			if (!(cmd.playerSpecific || cmd.prefixed)) handlers.put(cmd.cmd, new SqueezerCmdHandler() {
				public void handle(List<String> tokens) {
					cli.parseSqueezerList(cmd, tokens);
				}
			});
		}
		handlers.put("playlists", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
				if ("delete".equals(tokens.get(1)))
					;
				else if ("edit".equals(tokens.get(1)))
					;
				else if ("new".equals(tokens.get(1))) {
					HashMap<String, String> tokenMap = parseTokens(tokens);
					if (tokenMap.get("overwritten_playlist_id") != null) {
						if (playlistMaintenanceCallback.get() != null) {
							try {
								playlistMaintenanceCallback.get().onCreateFailed(getString(R.string.PLAYLIST_EXISTS_MESSAGE, tokenMap.get("name")));
							} catch (RemoteException e) {
								Log.e(TAG, getString(R.string.PLAYLIST_EXISTS_MESSAGE, tokenMap.get("name")));
							}
						}
					}
				} else if ("rename".equals(tokens.get(1))) {
					HashMap<String, String> tokenMap = parseTokens(tokens);
					if (tokenMap.get("dry_run") != null) {
						if (tokenMap.get("overwritten_playlist_id") != null) {
							if (playlistMaintenanceCallback.get() != null) {
								try {
									playlistMaintenanceCallback.get().onRenameFailed(getString(R.string.PLAYLIST_EXISTS_MESSAGE, tokenMap.get("newname")));
								} catch (RemoteException e) {
									Log.e(TAG, getString(R.string.PLAYLIST_EXISTS_MESSAGE, tokenMap.get("newname")));
								}
							}
						} else
							cli.sendCommand("playlists rename playlist_id:" + tokenMap.get("playlist_id") + " newname:" + Util.encode(tokenMap.get("newname")));
					}
				} else if ("tracks".equals(tokens.get(1)))
					cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists tracks"), tokens);
				else
					cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("playlists"), tokens);
			}
		});
		handlers.put("pref", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
				if ("httpport".equals(tokens.get(1)) && tokens.size() >= 3) {
					connectionState.setHttpPort(Integer.parseInt(tokens.get(2)));
				}
			}
		});
		handlers.put("can", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
                if ("musicfolder".equals(tokens.get(1)) && tokens.size() >= 3) {
                    connectionState.setCanMusicfolder(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
                }

				if ("randomplay".equals(tokens.get(1)) && tokens.size() >= 3) {
					connectionState.setCanRandomplay(Util.parseDecimalIntOrZero(tokens.get(2)) == 1);
				}
			}
		});

		return handlers;
	}

	private Map<String, SqueezerCmdHandler> initializePrefixedHandlers() {
		Map<String, SqueezerCmdHandler> handlers = new HashMap<String, SqueezerCmdHandler>();

		for (final SqueezerCLIImpl.ExtendedQueryFormatCmd cmd: cli.extQueryFormatCmds) {
			if (cmd.prefixed && !cmd.playerSpecific) handlers.put(cmd.cmd, new SqueezerCmdHandler() {
				public void handle(List<String> tokens) {
					cli.parseSqueezerList(cmd, tokens);
				}
			});
		}

		return handlers;
	}

	private Map<String, SqueezerCmdHandler> initializePlayerSpecificHandlers() {
		Map<String, SqueezerCmdHandler> handlers = new HashMap<String, SqueezerCmdHandler>();

		for (final SqueezerCLIImpl.ExtendedQueryFormatCmd cmd: cli.extQueryFormatCmds) {
			if (cmd.playerSpecific && !cmd.prefixed) handlers.put(cmd.cmd, new SqueezerCmdHandler() {
				public void handle(List<String> tokens) {
					cli.parseSqueezerList(cmd, tokens);
				}
			});
		}
		handlers.put("prefset", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
				if (tokens.size() > 4 && tokens.get(2).equals("server") && tokens.get(3).equals("volume")) {
		            String newVolume = tokens.get(4);
		            Log.v(TAG, "New volume is: " + newVolume);
		            sendNewVolumeCallback(Util.parseDecimalIntOrZero(newVolume));
				}
			}
		});
		handlers.put("play", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
	            setPlayingState(true);
			}
		});
		handlers.put("stop", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
	            setPlayingState(false);
			}
		});
		handlers.put("pause", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
	        	parsePause(tokens.size() >= 3 ? tokens.get(2) : null);
			}
		});
		handlers.put("status", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
	        	if (tokens.size() >= 3 && "-".equals(tokens.get(2)))
	        		parseStatusLine(tokens);
	        	else {
	            	cli.parseSqueezerList(cli.extQueryFormatCmdMap.get("status"), tokens);
	        	}
			}
		});
		handlers.put("playlist", new SqueezerCmdHandler() {
			public void handle(List<String> tokens) {
	            parsePlaylistNotification(tokens);
			}
		});

		return handlers;
	}

	private Map<String, SqueezerCmdHandler> initializePrefixedPlayerSpecificHandlers() {
		Map<String, SqueezerCmdHandler> handlers = new HashMap<String, SqueezerCmdHandler>();

		for (final SqueezerCLIImpl.ExtendedQueryFormatCmd cmd: cli.extQueryFormatCmds) {
			if (cmd.playerSpecific && cmd.prefixed) handlers.put(cmd.cmd, new SqueezerCmdHandler() {
				public void handle(List<String> tokens) {
					cli.parseSqueezerList(cmd, tokens);
				}
			});
		}

		return handlers;
	}

    private final Map<String,SqueezerCmdHandler> globalHandlers = initializeGlobalHandlers();
    private final Map<String,SqueezerCmdHandler> prefixedHandlers = initializePrefixedHandlers();
    private final Map<String,SqueezerCmdHandler> playerSpecificHandlers = initializePlayerSpecificHandlers();
    private final Map<String,SqueezerCmdHandler> prefixedPlayerSpecificHandlers = initializePrefixedPlayerSpecificHandlers();

    void onLineReceived(String serverLine) {
        if (debugLogging) Log.v(TAG, "LINE: " + serverLine);
        List<String> tokens = Arrays.asList(serverLine.split(" "));
        if (tokens.size() < 2) return;

        SqueezerCmdHandler handler;
        if ((handler = globalHandlers.get(tokens.get(0))) != null) {
        	handler.handle(tokens);
        	return;
        }
        if ((handler = prefixedHandlers.get(tokens.get(1))) != null) {
        	handler.handle(tokens);
        	return;
        }

        // Player-specific commands follow.  But we ignore all that aren't for our
        // active player.
        String activePlayerId = (connectionState.getActivePlayer() != null ? connectionState.getActivePlayer().getId() : null);
        if (activePlayerId == null || activePlayerId.length() == 0 ||
            !Util.decode(tokens.get(0)).equals(activePlayerId)) {
            // Different player that we're not interested in.
            // (yet? maybe later.)
            return;
        }
        if ((handler = playerSpecificHandlers.get(tokens.get(1))) != null) {
        	handler.handle(tokens);
        	return;
        }
        if (tokens.size() > 2 && (handler = prefixedPlayerSpecificHandlers.get(tokens.get(2))) != null) {
        	handler.handle(tokens);
        	return;
        }
    }

	private void sendNewVolumeCallback(int newVolume) {
    	SqueezerPlayer player = connectionState.getActivePlayer();
		mVolumePanel.postVolumeChanged(newVolume, player == null ? "" : player.getName());
    }

    private void sendNewTimeCallback(int secondsIn, int secondsTotal) {
        if (connectionState.getCallback() == null) return;
        try {
        	connectionState.getCallback().onTimeInSongChange(secondsIn, secondsTotal);
        } catch (RemoteException e) {
        }
    }

	private void parsePlaylistNotification(List<String> tokens) {
		String notification = tokens.get(2);
		if ("newsong".equals(notification)) {
			// Also ask for the rest of the status.
			// TODO: Why? This information isn't then used
			cli.sendPlayerCommand("status - 1 tags:" + SONGTAGS);
		} else if ("stop".equals(notification)) {
//			setPlayingState(false);
		} else if ("pause".equals(notification)) {
			parsePause(tokens.size() >= 4 ? tokens.get(3) : null);
		}
	}

	private void parsePause(String explicitPause) {
//		boolean playing = playerState.isPlaying();
//		if ("0".equals(explicitPause)) {
//			if (playing) setPlayingState(false);
//		} else if ("1".equals(explicitPause)) {
//			if (!playing) setPlayingState(true);
//		}
	}

	private void parseMode(String newMode) {
		boolean playing = playerState.isPlaying();
		if ("pause".equals(newMode)) {
			if (playing) setPlayingState(false);
		} else if ("stop".equals(newMode)) {
			if (playing) setPlayingState(false);
		} else if ("play".equals(newMode)) {
			if (!playing) setPlayingState(true);
		}
	}

    private HashMap<String, String> parseTokens(List<String> tokens) {
		HashMap<String, String> tokenMap = new HashMap<String, String>();
        String key, value;
		for (String token : tokens) {
            if (token == null || token.length() == 0) continue;
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

    private void parseStatusLine(List<String> tokens) {
		HashMap<String, String> tokenMap = parseTokens(tokens);
        PlayerStateChanged stateChanged = playerState.update(tokenMap);

		parseMode(tokenMap.get("mode"));

        if (stateChanged.musicHasChanged) {
            updateOngoingNotification();
            sendMusicChangedCallback();
        }

        if (stateChanged.timeHasChanged) {
            sendNewTimeCallback(playerState.getCurrentTimeSecond(), playerState.getCurrentSongDuration());
        }
    }

    void changeActivePlayer(SqueezerPlayer newPlayer) {
		if (newPlayer == null) {
            return;
        }

        Log.v(TAG, "Active player now: " + newPlayer);
        final String playerId = newPlayer.getId();
        String oldPlayerId =  (connectionState.getActivePlayer() != null ? connectionState.getActivePlayer().getId() : null);
        boolean changed = false;
        if (oldPlayerId == null || !oldPlayerId.equals(playerId)) {
        	if (oldPlayerId != null) {
	            // Unsubscribe from the old player's status.  (despite what
	            // the docs say, multiple subscribes can be active and flood us.)
	            cli.sendCommand(Util.encode(oldPlayerId) + " status - 1 subscribe:-");
        	}

            connectionState.setActivePlayer(newPlayer);
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
                public void run() {
                    Log.v(TAG, "Saving " + Preferences.KEY_LASTPLAYER + "=" + playerId);
                    final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(Preferences.KEY_LASTPLAYER, playerId);
                    editor.commit();
                }
            });
        }

        if (connectionState.getCallback() != null) {
            try {
            	connectionState.getCallback().onPlayerChanged(playerId, newPlayer.getName());
            } catch (RemoteException e) {}
        }
    }


    private void updatePlayerSubscriptionState() {
        // Subscribe or unsubscribe to the player's realtime status updates
        // depending on whether we have an Activity or some sort of client
        // that cares about second-to-second updates.
    	//
    	// Note: If scrobbling is turned on then that counts as caring
    	// about second-to-second updates -- otherwise we miss events from
    	// buttons on the player, the web interface, and so on
        if (connectionState.getCallback() != null || (scrobbleType != SCROBBLE_NONE)) {
            cli.sendPlayerCommand("status - 1 subscribe:1 tags:" + SONGTAGS);
        } else {
            cli.sendPlayerCommand("status - 1 subscribe:-");
        }
    }

    void onCliPortConnectionEstablished() {
        cli.sendCommand("listen 1",
                "players 0 1",   // initiate an async player fetch
                "can musicfolder ?", // learn music folder browsing support
                "can randomplay ?",   // learn random play function functionality
                "pref httpport ?"  // learn the HTTP port (needed for images)
        );
    }

    private void setPlayingState(boolean state) {
    	connectionState.updateWifiLock(state);

        playerState.setPlaying(state);
        updateOngoingNotification();

        if (connectionState.getCallback() == null) {
            return;
        }
        try {
        	connectionState.getCallback().onPlayStatusChanged(state);
        } catch (RemoteException e) {
        }

    }

    private void updateOngoingNotification() {
        boolean playing = playerState.isPlaying();
        if (!playing) {
            if (!preferences.getBoolean(Preferences.KEY_NOTIFY_OF_CONNECTION, false)) {
                clearOngoingNotification();
                return;
            }
        }
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification status = new Notification();
        //status.contentView = views;
        Intent showNowPlaying = new Intent(this, SqueezerActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, showNowPlaying, 0);
        String songName = playerState.getCurrentSongName();
        String playerName = connectionState.getActivePlayer() != null ?  connectionState.getActivePlayer().getName() : "squeezer";
        if (playing) {
            status.setLatestEventInfo(this, getString(R.string.notification_playing_text, playerName), songName, pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
        } else {
            status.setLatestEventInfo(this, getString(R.string.notification_connected_text, playerName), "-", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.logo;
        }
        nm.notify(PLAYBACKSERVICE_STATUS, status);

        if (scrobbleType != SCROBBLE_NONE) {
        	SqueezerSong s = playerState.getCurrentSong();
        	if (s != null) {
            	Intent i = new Intent();

            	switch (scrobbleType) {
    	        case SCROBBLE_SCROBBLEDROID:
    	        	// http://code.google.com/p/scrobbledroid/wiki/DeveloperAPI
    	        	i.setAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
    	        	i.putExtra("playing", playing);
    	        	i.putExtra("track", songName);
    	        	i.putExtra("album", s.getAlbum());
    	        	i.putExtra("artist", s.getArtist());
    	        	i.putExtra("secs", playerState.getCurrentSongDuration());
    	        	i.putExtra("source", "P");
    	        	break;
    	        case SCROBBLE_SLS:
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
    	        	break;
            	}
            	sendBroadcast(i);
        	}
        }
    }

    private void sendMusicChangedCallback() {
        if (connectionState.getCallback() == null) {
            return;
        }
        try {
        	connectionState.getCallback().onMusicChanged();
        } catch (RemoteException e) {
        }
    }

    private void clearOngoingNotification() {
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }

    private final ISqueezeService.Stub squeezeService = new ISqueezeService.Stub() {

        public void registerCallback(IServiceCallback callback) throws RemoteException {
            Log.v(TAG, "Callback attached.");
            connectionState.setCallback(callback);
	    	updatePlayerSubscriptionState();
	    }

	    public void unregisterCallback(IServiceCallback callback) throws RemoteException {
            Log.v(TAG, "Callback detached.");
	    	connectionState.callbackCompareAndSet(callback, null);
            updatePlayerSubscriptionState();
	    }

	    public int adjustVolumeBy(int delta) throws RemoteException {
            if (delta > 0) {
                cli.sendPlayerCommand("mixer volume %2B" + delta);
            } else if (delta < 0) {
                cli.sendPlayerCommand("mixer volume " + delta);
            }
            return 50 + delta;  // TODO: return non-blocking dead-reckoning value
        }

        public boolean isConnected() throws RemoteException {
        	return connectionState.isConnected();
        }

        public void startConnect(String hostPort) throws RemoteException {
        	connectionState.startConnect(SqueezeService.this, executor, hostPort);
        }

        public void disconnect() throws RemoteException {
            if (!isConnected()) return;
            SqueezeService.this.disconnect();
        }

        public boolean powerOn() throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("power 1");
            return true;
        }

        public boolean powerOff() throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("power 0");
            return true;
        }

        public boolean canPowerOn() {
        	return canPower() && !playerState.isPoweredOn();
        }

        public boolean canPowerOff() {
        	return canPower() && playerState.isPoweredOn();
        }

        private boolean canPower() {
            SqueezerPlayer player = connectionState.getActivePlayer();
        	return connectionState.isConnected() && player != null && player.isCanpoweroff();
        }

        /**
         * Determines whether the Squeezeserver supports the
         * <code>musicfolders</code> command.
         *
         * @return <code>true</code> if it does, <code>false</code> otherwise.
         */
        public boolean canMusicfolder() {
            return connectionState.canMusicfolder();
        }

        public boolean canRandomplay() {
        	return connectionState.canRandomplay();
        }

        public boolean togglePausePlay() throws RemoteException {
            if (!isConnected()) return false;
            Log.v(TAG, "pause...");
            if (playerState.isPlaying()) {
                // NOTE: we never send ambiguous "pause" toggle commands (without the '1')
                // because then we'd get confused when they came back in to us, not being
                // able to differentiate ours coming back on the listen channel vs. those
                // of those idiots at the dinner party messing around.
                cli.sendPlayerCommand("pause 1");
            } else {
                // TODO: use 'pause 0 <fade_in_secs>' to fade-in if we knew it was
                // actually paused (as opposed to not playing at all)
                cli.sendPlayerCommand("play");
            }
            Log.v(TAG, "paused.");
            return true;
        }

        public boolean play() throws RemoteException {
            if (!isConnected()) return false;
            Log.v(TAG, "play..");
            playerState.setPlaying(true);
            cli.sendPlayerCommand("play");
            Log.v(TAG, "played.");
            return true;
        }

        public boolean stop() throws RemoteException {
            if (!isConnected()) return false;
            playerState.setPlaying(false);
            cli.sendPlayerCommand("stop");
            return true;
        }

        public boolean nextTrack() throws RemoteException {
            if (!isConnected() || !isPlaying()) return false;
            cli.sendPlayerCommand("button jump_fwd");
            return true;
        }

        public boolean previousTrack() throws RemoteException {
            if (!isConnected() || !isPlaying()) return false;
            cli.sendPlayerCommand("button jump_rew");
            return true;
        }

        public boolean playlistControl(String cmd, String tag, String itemId)
                throws RemoteException {
            if (!isConnected()) return false;

            cli.sendPlayerCommand("playlistcontrol cmd:" + cmd + " " + tag + ":" + itemId);
            return true;
        }

        public boolean randomPlay(String type) throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("randomplay " + type);
            return true;
        }

        /**
         * Start playing the song in the current playlist at the given index.
         *
         * @param index the index to jump to
         */
        public boolean playlistIndex(int index) throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("playlist index " + index);
            return true;
        }

        public boolean playlistRemove(int index) throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("playlist delete " + index);
            return true;
        }

        public boolean playlistMove(int fromIndex, int toIndex) throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("playlist move " + fromIndex + " " + toIndex);
            return true;
        }

        public boolean playlistClear() throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand("playlist clear");
            return true;
        }

        public boolean playlistSave(String name) throws RemoteException {
        	if (!isConnected()) return false;
        	cli.sendPlayerCommand("playlist save " + Util.encode(name));
        	return true;
        }

        public boolean pluginPlaylistControl(SqueezerPlugin plugin, String cmd, String itemId) throws RemoteException {
            if (!isConnected()) return false;
            cli.sendPlayerCommand(plugin.getId() + " playlist " + cmd + " item_id:" + itemId);
            return true;

        }

        public boolean isPlaying() throws RemoteException {
            return playerState.isPlaying();
        }

        public void setActivePlayer(SqueezerPlayer player) throws RemoteException {
            changeActivePlayer(player);
        }

        public SqueezerPlayer getActivePlayer() throws RemoteException {
            return connectionState.getActivePlayer();
        }

        public String getActivePlayerName() throws RemoteException {
            SqueezerPlayer player = connectionState.getActivePlayer();
            return (player != null ? player.getName() : null);
        }

        public SqueezerPlayerState getPlayerState() throws RemoteException {
            return playerState;
        }

        public SqueezerSong currentSong() throws RemoteException {
            return playerState.getCurrentSong();
        }

        public String getCurrentPlaylist() {
            return playerState.getCurrentPlaylist();
        }


        public String getAlbumArtUrl(String artworkTrackId) throws RemoteException {
            Integer port = connectionState.getHttpPort();
            if (port == null || port == 0) return "";
			return "http://" + connectionState.getCurrentHost() + ":" + port + artworkTrackIdUrl(artworkTrackId);
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
        public String getSongDownloadUrl(String songId) throws RemoteException {
            Integer port = connectionState.getHttpPort();
            if (port == null || port == 0)
                return "";
            return "http://" + connectionState.getCurrentHost() + ":" + port
                    + songDownloadUrl(songId);
        }

        private String songDownloadUrl(String songId) {
            return "/music/" + songId + "/download";
        }

        public String getIconUrl(String icon) throws RemoteException {
            Integer port = connectionState.getHttpPort();
            if (port == null || port == 0) return "";
			return "http://" + connectionState.getCurrentHost() + ":" + port + '/' + icon;
        }

        public int getSecondsElapsed() throws RemoteException {
        	return playerState.getCurrentTimeSecond();
        }

        public boolean setSecondsElapsed(int seconds) throws RemoteException {
        	if (!isConnected()) return false;
        	if (seconds < 0) return false;

        	cli.sendPlayerCommand("time " + seconds);

        	return true;
        }

        public int getSecondsTotal() throws RemoteException {
            return playerState.getCurrentSongDuration();
        }

        public void preferenceChanged(String key) throws RemoteException {
            Log.v(TAG, "Preference changed: " + key);
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

        /* Start an async fetch of the SqueezeboxServer's players */
        public boolean players(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("players", start);
            return true;
        }

		public void registerPlayerListCallback(IServicePlayerListCallback callback)
				throws RemoteException {
            Log.v(TAG, "PlayerListCallback attached.");
	    	SqueezeService.this.playerListCallback.set(callback);
		}

		public void unregisterPlayerListCallback(IServicePlayerListCallback callback) throws RemoteException {
            Log.v(TAG, "PlayerListCallback detached.");
	    	SqueezeService.this.playerListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerPlayer.class);
		}

        /* Start an async fetch of the SqueezeboxServer's albums, which are matching the given parameters */
        public boolean albums(int start, String sortOrder, String searchString,
                SqueezerArtist artist, SqueezerYear year, SqueezerGenre genre, SqueezerSong song)
                throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
            parameters.add("tags:" + ALBUMTAGS);
            parameters.add("sort:" + sortOrder);
            if (searchString != null && searchString.length() > 0)
                parameters.add("search:" + searchString);
            if (artist != null)
                parameters.add("artist_id:" + artist.getId());
            if (year != null)
                parameters.add("year:" + year.getId());
            if (genre != null)
                parameters.add("genre_id:" + genre.getId());
            if (song != null)
                parameters.add("track_id:" + song.getId());
            cli.requestItems("albums", start, parameters);
            return true;
        }

		public void registerAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback attached.");
	    	SqueezeService.this.albumListCallback.set(callback);
		}

		public void unregisterAlbumListCallback(IServiceAlbumListCallback callback) throws RemoteException {
            Log.v(TAG, "AlbumListCallback detached.");
	    	SqueezeService.this.albumListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerAlbum.class);
		}


        /* Start an async fetch of the SqueezeboxServer's artists */
		public boolean artists(int start, String searchString, SqueezerAlbum album,
				SqueezerGenre genre) throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
			if (searchString != null && searchString.length() > 0)
				parameters.add("search:" + searchString);
			if (album != null)
				parameters.add("album_id:" + album.getId());
			if (genre != null)
				parameters.add("genre_id:" + genre.getId());
			cli.requestItems("artists", start, parameters);
            return true;
        }

		public void registerArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback attached.");
	    	SqueezeService.this.artistListCallback.set(callback);
		}

		public void unregisterArtistListCallback(IServiceArtistListCallback callback) throws RemoteException {
            Log.v(TAG, "ArtistListCallback detached.");
	    	SqueezeService.this.artistListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerArtist.class);
		}


        /* Start an async fetch of the SqueezeboxServer's years */
        public boolean years(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("years", start);
            return true;
        }

		public void registerYearListCallback(IServiceYearListCallback callback) throws RemoteException {
            Log.v(TAG, "YearListCallback attached.");
	    	SqueezeService.this.yearListCallback.set(callback);
		}

		public void unregisterYearListCallback(IServiceYearListCallback callback) throws RemoteException {
            Log.v(TAG, "YearListCallback detached.");
	    	SqueezeService.this.yearListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerYear.class);
		}


        /* Start an async fetch of the SqueezeboxServer's genres */
        public boolean genres(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("genres", start);
            return true;
        }

		public void registerGenreListCallback(IServiceGenreListCallback callback) throws RemoteException {
            Log.v(TAG, "GenreListCallback attached.");
	    	SqueezeService.this.genreListCallback.set(callback);
		}

		public void unregisterGenreListCallback(IServiceGenreListCallback callback) throws RemoteException {
            Log.v(TAG, "GenreListCallback detached.");
	    	SqueezeService.this.genreListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerGenre.class);
		}

        /**
         * Starts an async fetch of the contents of a SqueezerboxServer's music
         * folders in the given folderId.
         * <p>
         * folderId may be null, in which case the contents of the root music
         * folder are returned.
         * <p>
         * Results are returned through the callback registered with
         * {@link registerMusicFolderListCallback}.
         *
         * @param start Where in the list of folders to start.
         * @param folderId The folder to view.
         * @return <code>true</code> if the request was sent, <code>false</code>
         *         if the service is not connected.
         */
        public boolean musicFolders(int start, String folderId) throws RemoteException {
            if (!isConnected()) {
                return false;
            }

            List<String> parameters = new ArrayList<String>();

            if (folderId != null) {
                parameters.add("folder_id:" + folderId);
            }

            cli.requestItems("musicfolder", start, parameters);
            return true;
        }

        public void registerMusicFolderListCallback(IServiceMusicFolderListCallback callback)
                throws RemoteException {
            Log.v(TAG, "MusicFolderListCallback attached.");
            SqueezeService.this.musicFolderListCallback.set(callback);
        }

        public void unregisterMusicFolderListCallback(IServiceMusicFolderListCallback callback)
                throws RemoteException {
            Log.v(TAG, "MusicFolderListCallback detached.");
            SqueezeService.this.musicFolderListCallback.compareAndSet(callback, null);
            cli.cancelRequests(SqueezerMusicFolderItem.class);
        }

		/* Start an async fetch of the SqueezeboxServer's songs */
		public boolean songs(int start, String sortOrder, String searchString, SqueezerAlbum album,
				SqueezerArtist artist, SqueezerYear year, SqueezerGenre genre) throws RemoteException {
			if (!isConnected())
				return false;
			List<String> parameters = new ArrayList<String>();
			parameters.add("tags:" + SONGTAGS);
			parameters.add("sort:" + sortOrder);
			if (searchString != null && searchString.length() > 0)
				parameters.add("search:" + searchString);
			if (album != null)
				parameters.add("album_id:" + album.getId());
			if (artist != null)
				parameters.add("artist_id:" + artist.getId());
			if (year != null)
				parameters.add("year:" + year.getId());
			if (genre != null)
				parameters.add("genre_id:" + genre.getId());
			cli.requestItems("songs", start, parameters);
			return true;
		}

        /* Start an async fetch of the SqueezeboxServer's current playlist */
        public boolean currentPlaylist(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestPlayerItems("status", start, Arrays.asList("tags:" + SONGTAGS));
            return true;
        }

        /* Start an async fetch of the songs of the supplied playlist */
        public boolean playlistSongs(int start, SqueezerPlaylist playlist) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("playlists tracks", start, Arrays.asList("playlist_id:" + playlist.getId(), "tags:" + SONGTAGS));
            return true;
        }

		public void registerSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback attached.");
	    	SqueezeService.this.songListCallback.set(callback);
		}

		public void unregisterSongListCallback(IServiceSongListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback detached.");
	    	SqueezeService.this.songListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerSong.class);
		}


        /* Start an async fetch of the SqueezeboxServer's playlists */
        public boolean playlists(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("playlists", start);
            return true;
        }

		public void registerPlaylistsCallback(IServicePlaylistsCallback callback) throws RemoteException {
            Log.v(TAG, "PlaylistsCallback attached.");
	    	SqueezeService.this.playlistsCallback.set(callback);
		}

		public void unregisterPlaylistsCallback(IServicePlaylistsCallback callback) throws RemoteException {
            Log.v(TAG, "PlaylistsCallback detached.");
	    	SqueezeService.this.playlistsCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerPlaylist.class);
		}


		public void registerPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback) throws RemoteException {
            Log.v(TAG, "PlaylistMaintenanceCallback attached.");
	    	playlistMaintenanceCallback.set(callback);
		}

		public void unregisterPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback) throws RemoteException {
            Log.v(TAG, "PlaylistMaintenanceCallback detached.");
            playlistMaintenanceCallback.compareAndSet(callback, null);
		}

		public boolean playlistsDelete(SqueezerPlaylist playlist) throws RemoteException {
			if (!isConnected()) return false;
			cli.sendCommand("playlists delete playlist_id:" + playlist.getId());
			return true;
		}

		public boolean playlistsMove(SqueezerPlaylist playlist, int index, int toindex) throws RemoteException {
			if (!isConnected()) return false;
			cli.sendCommand("playlists edit cmd:move playlist_id:" + playlist.getId()
					+ " index:" + index + " toindex:" + toindex);
			return true;
		}

		public boolean playlistsNew(String name) throws RemoteException {
			if (!isConnected()) return false;
			cli.sendCommand("playlists new name:" + Util.encode(name));
			return true;
		}

		public boolean playlistsRemove(SqueezerPlaylist playlist, int index) throws RemoteException {
			if (!isConnected()) return false;
			cli.sendCommand("playlists edit cmd:delete playlist_id:" + playlist.getId() + " index:" + index);
			return true;
		}

		public boolean playlistsRename(SqueezerPlaylist playlist, String newname) throws RemoteException {
			if (!isConnected()) return false;
			cli.sendCommand("playlists rename playlist_id:" + playlist.getId() + " dry_run:1 newname:" + Util.encode(newname));
			return true;
		}


        /* Start an asynchronous search of the SqueezeboxServer's library */
   		public boolean search(int start, String searchString) throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
			if (searchString != null && searchString.length() > 0)
				parameters.add("term:" + searchString);
			cli.requestItems("search", start, parameters);
            return true;
        }

        /* Start an asynchronous fetch of the squeezeservers radio type plugins */
   		public boolean radios(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("radios", start);
            return true;
   		}

        /* Start an asynchronous fetch of the squeezeservers radio application plugins */
   		public boolean apps(int start) throws RemoteException {
            if (!isConnected()) return false;
            cli.requestItems("apps", start);
            return true;
   		}

		public void registerPluginListCallback(IServicePluginListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback attached.");
	    	SqueezeService.this.pluginListCallback.set(callback);
		}

		public void unregisterPluginListCallback(IServicePluginListCallback callback) throws RemoteException {
            Log.v(TAG, "PluginListCallback detached.");
	    	SqueezeService.this.pluginListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerPlugin.class);
		}


        /* Start an asynchronous fetch of the squeezeservers items of the given type */
   		public boolean pluginItems(int start, SqueezerPlugin plugin, SqueezerPluginItem parent, String search) throws RemoteException {
            if (!isConnected()) return false;
            List<String> parameters = new ArrayList<String>();
			if (parent != null)
				parameters.add("item_id:" + parent.getId());
			if (search != null && search.length() > 0)
				parameters.add("search:" + search);
            cli.requestPlayerItems(plugin.getId() + " items", start, parameters);
            return true;
   		}

		public void registerPluginItemListCallback(IServicePluginItemListCallback callback) throws RemoteException {
            Log.v(TAG, "SongListCallback attached.");
	    	SqueezeService.this.pluginItemListCallback.set(callback);
		}

		public void unregisterPluginItemListCallback(IServicePluginItemListCallback callback) throws RemoteException {
            Log.v(TAG, "PluginItemListCallback detached.");
	    	SqueezeService.this.pluginItemListCallback.compareAndSet(callback, null);
			cli.cancelRequests(SqueezerPluginItem.class);
		}

	};
}
