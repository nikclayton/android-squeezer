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

import uk.org.ngo.squeezer.framework.FilterItem;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.itemlist.IServiceCurrentPlaylistCallback;
import uk.org.ngo.squeezer.itemlist.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Year;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;

public interface ISqueezeService {
    // For the activity to get callbacks on interesting events
    void registerCallback(IServiceCallback callback);

    // For the activity to get callback when the connection changes.
    void registerConnectionCallback(IServiceConnectionCallback callback);

    // For the activity to get callback when the active or connected players changes.
    void registerPlayersCallback(IServicePlayersCallback callback);

    // For the activity to get callback when the volume changes.
    void registerVolumeCallback(IServiceVolumeCallback callback);

    // For the activity to get callback when the current playlist is modified
    void registerCurrentPlaylistCallback(IServiceCurrentPlaylistCallback callback);

    // For the activity to get callback when music changes
    void registerMusicChangedCallback(IServiceMusicChangedCallback callback);

    // For the activity to get callback when handshake completes
    void registerHandshakeCallback(IServiceHandshakeCallback callback);

    // For the activity to get callback when status for a player is received.
    void registerPlayerStateCallback(IServicePlayerStateCallback callback);

    // Instructing the service to connect to the SqueezeCenter server:
    // hostPort is the port of the CLI interface.
    void startConnect(String hostPort, String userName, String password);
    void disconnect();
    boolean isConnected();
    boolean isConnectInProgress();

    // For the SettingsActivity to notify the Service that a setting changed.
    void preferenceChanged(String key);

    // Call this to change the player we are controlling
    void setActivePlayer(Player player);

    // Returns the player we are currently controlling
    Player getActivePlayer();

    // Player control
    void togglePower(Player player);
    void playerRename(Player player, String newName);
    void sleep(Player player, int duration);

    ////////////////////
    // Depends on active player:

    boolean canPowerOn();
    boolean canPowerOff();
    void powerOn();
    void powerOff();
    boolean canFavorites();
    boolean canMusicfolder();
    boolean canMyApps();
    boolean canRandomplay();
    String preferredAlbumSort();
    boolean togglePausePlay();
    boolean play();
    boolean stop();
    boolean nextTrack();
    boolean previousTrack();
    boolean toggleShuffle();
    boolean toggleRepeat();
    boolean playlistControl(String cmd, PlaylistItem playlistItem);
    boolean randomPlay(String type);
    boolean playlistIndex(int index);
    boolean playlistRemove(int index);
    boolean playlistMove(int fromIndex, int toIndex);
    boolean playlistClear();
    boolean playlistSave(String name);
    boolean pluginPlaylistControl(Plugin plugin, String cmd, String id);

    boolean setSecondsElapsed(int seconds);

    PlayerState getPlayerState();
    String getCurrentPlaylist();
    String getAlbumArtUrl(String artworkTrackId);
    String getIconUrl(String icon);

    String getSongDownloadUrl(String songTrackId);

    /**
     * Sets the volume to the absolute volume in newVolume, which will be clamped to the
     * interval [0, 100].
     *
     * @param newVolume
     */
    void adjustVolumeTo(Player player, int newVolume);
    void adjustVolumeTo(int newVolume);
    void adjustVolumeBy(int delta);

    /** Cancel any pending callbacks for client */
    void cancelItemListRequests(Object client);

    /** Cancel any subscriptions for client */
    void cancelSubscriptions(Object client);

    /** Start an async fetch of the SqueezeboxServer's players */
    void players(int start, IServiceItemListCallback<Player> callback);

    // Album list
    /**
     * Starts an asynchronous fetch of album data from the server. The supplied
     * will be called when the data is fetched.
     */
    void albums(IServiceItemListCallback<Album> callback, int start, String sortOrder, String searchString, FilterItem... filters);

    // Artist list
    void artists(IServiceItemListCallback<Artist> callback, int start, String searchString, FilterItem... filters);

    // Year list
    void years(int start, IServiceItemListCallback<Year> callback);

    // Genre list
    void genres(int start, String searchString, IServiceItemListCallback<Genre> callback);

    // MusicFolder list
    void musicFolders(int start, MusicFolderItem musicFolderItem, IServiceItemListCallback<MusicFolderItem> callback);

    // Song list
    void songs(IServiceItemListCallback<Song> callback, int start, String sortOrder, String searchString, FilterItem... filters);
    void currentPlaylist(int start, IServiceItemListCallback<Song> callback);
    void playlistSongs(int start, Playlist playlist, IServiceItemListCallback<Song> callback);

    // Playlists
    void playlists(int start, IServiceItemListCallback<Playlist> callback);

    // Named playlist maintenance
    void registerPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback);
    boolean playlistsNew(String name);
    boolean playlistsRename(Playlist playlist, String newname);
    boolean playlistsDelete(Playlist playlist);
    boolean playlistsMove(Playlist playlist, int index, int toindex);
    boolean playlistsRemove(Playlist playlist, int index);

    // Search
    void search(int start, String searchString, IServiceItemListCallback itemListCallback);

    // Radios/plugins
    void radios(int start, IServiceItemListCallback<Plugin> callback);
    void apps(int start, IServiceItemListCallback<Plugin> callback);

    void pluginItems(int start, Plugin plugin, PluginItem parent, String search, IServiceItemListCallback<PluginItem> callback);

    /**
     * Initiate download of songs for the supplied item.
     *
     * @param item Song or item with songs to download
     */
    void downloadItem(FilterItem item);


}
