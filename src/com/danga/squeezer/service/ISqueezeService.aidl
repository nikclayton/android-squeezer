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

package com.danga.squeezer.service;

import com.danga.squeezer.IServiceCallback;
import com.danga.squeezer.itemlists.IServicePlayerListCallback;
import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.itemlists.IServiceYearListCallback;
import com.danga.squeezer.itemlists.IServiceGenreListCallback;
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.itemlists.IServicePlaylistsCallback;
import com.danga.squeezer.itemlists.IServicePlaylistMaintenanceCallback;
import com.danga.squeezer.itemlists.IServicePluginListCallback;
import com.danga.squeezer.itemlists.IServicePluginItemListCallback;
import com.danga.squeezer.model.SqueezerPlayer;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerYear;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerPlaylist;
import com.danga.squeezer.model.SqueezerPlugin;
import com.danga.squeezer.model.SqueezerPluginItem;

interface ISqueezeService {
	    // For the activity to get callbacks on interesting events:
	    void registerCallback(IServiceCallback callback);
        void unregisterCallback(IServiceCallback callback);

	    // Instructing the service to connect to the SqueezeCenter server:
	    // hostPort is the port of the CLI interface.
		void startConnect(String hostPort);
		void disconnect();
        boolean isConnected();
        
        // For the SettingsActivity to notify the Service that a setting changed.
        void preferenceChanged(String key);

		// Call this to change the player we are controlling
	    void setActivePlayer(in SqueezerPlayer player);

		// Returns the empty string (not null) if no player is set. 
        String getActivePlayerName();

	    ////////////////////
  	    // Depends on active player:
  	    
  	    boolean canPowerOn();
  	    boolean canPowerOff();
        boolean powerOn();
        boolean powerOff();
        boolean canRandomplay();
        boolean isPlaying();
        boolean togglePausePlay();
        boolean play();
        boolean stop();
        boolean nextTrack();
        boolean previousTrack();
        boolean playlistControl(String cmd, String className, String id);
        boolean randomPlay(String type);
        boolean playlistIndex(int index);
        boolean playlistRemove(int index);
        boolean playlistMove(int fromIndex, int toIndex);
        boolean playlistClear();
        boolean playlistSave(String name);
        boolean pluginPlaylistControl(in SqueezerPlugin plugin, String cmd, String id);
        
        // Return 0 if unknown:
        int getSecondsTotal();
        int getSecondsElapsed();
        boolean setSecondsElapsed(int seconds);
        
        SqueezerSong currentSong();
        String getAlbumArtUrl(String artworkTrackId);
        String getIconUrl(String icon);

        // Returns new (predicted) volume.  Typical deltas are +10 or -10.
        // Note the volume changed callback will also still be run with
        // the correct value as returned by the server later.
        int adjustVolumeBy(int delta);
        
        // Player list
        boolean players(int start);
	    void registerPlayerListCallback(IServicePlayerListCallback callback);
        void unregisterPlayerListCallback(IServicePlayerListCallback callback);
        
        // Album list
       	/**
		 * Starts an asynchronous fetch of album data from the server.  Any
		 * callback registered with {@link registerAlbumListCallBack} will
		 * be called when the data is fetched.
		 * 
		 * @param start
		 * @param sortOrder
		 * @param searchString
		 * @param artist
		 * @param year
		 * @param genre
		 */
        boolean albums(int start, String sortOrder, String searchString, in SqueezerArtist artist, in SqueezerYear year, in SqueezerGenre genre, in SqueezerSong song);
	    void registerAlbumListCallback(IServiceAlbumListCallback callback);
        void unregisterAlbumListCallback(IServiceAlbumListCallback callback);
        
        // Artist list
        boolean artists(int start, String searchString, in SqueezerAlbum album, in SqueezerGenre genre);
	    void registerArtistListCallback(IServiceArtistListCallback callback);
        void unregisterArtistListCallback(IServiceArtistListCallback callback);
        
        // Year list
        boolean years(int start);
	    void registerYearListCallback(IServiceYearListCallback callback);
        void unregisterYearListCallback(IServiceYearListCallback callback);
        
        // Genre list
        boolean genres(int start);
	    void registerGenreListCallback(IServiceGenreListCallback callback);
        void unregisterGenreListCallback(IServiceGenreListCallback callback);
        
        // Song list
        boolean songs(int start, String sortOrder, String searchString, in SqueezerAlbum album, in SqueezerArtist artist, in SqueezerYear year, in SqueezerGenre genre);
        boolean currentPlaylist(int start);
        boolean playlistSongs(int start, in SqueezerPlaylist playlist);
	    void registerSongListCallback(IServiceSongListCallback callback);
        void unregisterSongListCallback(IServiceSongListCallback callback);
        
        // Playlists
        boolean playlists(int start);
	    void registerPlaylistsCallback(IServicePlaylistsCallback callback);
        void unregisterPlaylistsCallback(IServicePlaylistsCallback callback);
        
        // Named playlist maintenance
	    void registerPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback);
        void unregisterPlaylistMaintenanceCallback(IServicePlaylistMaintenanceCallback callback);
        boolean playlistsNew(String name);
        boolean playlistsRename(in SqueezerPlaylist playlist, String newname);
        boolean playlistsDelete(in SqueezerPlaylist playlist);
        boolean playlistsMove(in SqueezerPlaylist playlist, int index, int toindex);
        boolean playlistsRemove(in SqueezerPlaylist playlist, int index);
        
        
        // Search
        boolean search(int start, String searchString);

        
        // Radios/plugins
        boolean radios(int start);
        boolean apps(int start);
	    void registerPluginListCallback(IServicePluginListCallback callback);
	    void unregisterPluginListCallback(IServicePluginListCallback callback);

        boolean pluginItems(int start, in SqueezerPlugin plugin, in SqueezerPluginItem parent, String search);
	    void registerPluginItemListCallback(IServicePluginItemListCallback callback);
	    void unregisterPluginItemListCallback(IServicePluginItemListCallback callback);

}
