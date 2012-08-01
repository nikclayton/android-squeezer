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

package uk.org.ngo.squeezer.model;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import android.os.Parcel;
import android.os.Parcelable;


public class SqueezerPlayerState implements Parcelable {

    public SqueezerPlayerState() {
    }

    public static final Creator<SqueezerPlayerState> CREATOR = new Creator<SqueezerPlayerState>() {
        public SqueezerPlayerState[] newArray(int size) {
            return new SqueezerPlayerState[size];
        }

        public SqueezerPlayerState createFromParcel(Parcel source) {
            return new SqueezerPlayerState(source);
        }
    };
    private SqueezerPlayerState(Parcel source) {
        playing = (source.readByte() == 1);
        poweredOn = (source.readByte() == 1);
        currentSong = source.readParcelable(null);
        currentPlaylistIndex = source.readInt();
        currentTimeSecond = source.readInt();
        currentSongDuration = source.readInt();
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(playing ? (byte)1 : (byte)0);
        dest.writeByte(poweredOn ? (byte)1 : (byte)0);
        dest.writeParcelable(currentSong, 0);
        dest.writeInt(currentPlaylistIndex);
        dest.writeInt(currentTimeSecond);
        dest.writeInt(currentSongDuration);
    }

    public int describeContents() {
        return 0;
    }

    
    private boolean playing;
    private boolean poweredOn;

    private SqueezerSong currentSong;
    private String currentPlaylist;
    private int currentPlaylistIndex;
    private int currentTimeSecond;
    private int currentSongDuration;


	public boolean isPlaying() {
		return playing;
	}
    public SqueezerPlayerState setPlaying(boolean state) {
    	playing = state;
    	return this;
    }

    public boolean isPoweredOn() {
		return poweredOn;
	}
    public SqueezerPlayerState setPoweredOn(boolean state) {
    	poweredOn = state;
    	return this;
    }

    public SqueezerSong getCurrentSong() {
    	return currentSong;
    }
    public String getCurrentSongName() {
    	return (currentSong != null) ? currentSong.getName() :  "";
    }
    public SqueezerPlayerState setCurrentSong(SqueezerSong song) {
    	currentSong = song;
    	return this;
    }

    public String getCurrentPlaylist() {
        return currentPlaylist;
    }


    public int getCurrentPlaylistIndex() {
        return currentPlaylistIndex;
    }
    public SqueezerPlayerState setCurrentPlaylistIndex(int value) {
        currentPlaylistIndex = value;
        return this;
    }

    public int getCurrentTimeSecond() {
        return currentTimeSecond;
    }
    public SqueezerPlayerState setCurrentTimeSecond(int value) {
        currentTimeSecond = value;
        return this;
    }

	public int getCurrentSongDuration() {
		return currentSongDuration;
	}
    public SqueezerPlayerState setCurrentSongDuration(int value) {
    	currentSongDuration = value;
    	return this;
    }

    public PlayerStateChanged update(Map<String, String> tokenMap) {
        PlayerStateChanged playerStateChanged = new PlayerStateChanged();

        SqueezerSong newSong = new SqueezerSong(tokenMap);
        playerStateChanged.musicHasChanged = !newSong.equals(currentSong);
        if (playerStateChanged.musicHasChanged) currentSong = newSong;

        setPoweredOn(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);
        currentPlaylist = tokenMap.get("playlist_name");

        int lastTime = getCurrentTimeSecond();
        currentTimeSecond = Util.parseDecimalIntOrZero(tokenMap.get("time"));
        currentSongDuration = Util.parseDecimalIntOrZero(tokenMap.get("duration"));
        currentPlaylistIndex = Util.parseDecimalIntOrZero(tokenMap.get("playlist_cur_index"));
        playerStateChanged.timeHasChanged = (currentTimeSecond != lastTime);
        
        return playerStateChanged;
    }

    public class PlayerStateChanged {
        public boolean musicHasChanged;
        public boolean timeHasChanged;
    }

}
