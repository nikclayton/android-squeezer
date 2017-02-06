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


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.EnumIdLookup;
import uk.org.ngo.squeezer.framework.EnumWithId;
import uk.org.ngo.squeezer.service.ServerString;


public class PlayerState implements Parcelable {

    @StringDef({NOTIFY_NONE, NOTIFY_ON_CHANGE, NOTIFY_REAL_TIME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSubscriptionType {}
    public static final String NOTIFY_NONE = "-";
    public static final String NOTIFY_ON_CHANGE = "0";
    public static final String NOTIFY_REAL_TIME = "1";

    public PlayerState() {
    }

    public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
        @Override
        public PlayerState[] newArray(int size) {
            return new PlayerState[size];
        }

        @Override
        public PlayerState createFromParcel(Parcel source) {
            return new PlayerState(source);
        }
    };

    private PlayerState(Parcel source) {
        playerId = source.readString();
        playStatus = source.readString();
        poweredOn = (source.readByte() == 1);
        shuffleStatus = ShuffleStatus.valueOf(source.readInt());
        repeatStatus = RepeatStatus.valueOf(source.readInt());
        currentSong = source.readParcelable(null);
        currentPlaylist = source.readString();
        currentPlaylistIndex = source.readInt();
        currentTimeSecond = source.readInt();
        currentSongDuration = source.readInt();
        currentVolume = source.readInt();
        sleepDuration = source.readInt();
        sleep = source.readInt();
        mSyncMaster = source.readString();
        source.readStringList(mSyncSlaves);
        mPlayerSubscriptionType = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playerId);
        dest.writeString(playStatus);
        dest.writeByte(poweredOn ? (byte) 1 : (byte) 0);
        dest.writeInt(shuffleStatus.getId());
        dest.writeInt(repeatStatus.getId());
        dest.writeParcelable(currentSong, 0);
        dest.writeString(currentPlaylist);
        dest.writeInt(currentPlaylistIndex);
        dest.writeInt(currentTimeSecond);
        dest.writeInt(currentSongDuration);
        dest.writeInt(currentVolume);
        dest.writeInt(sleepDuration);
        dest.writeInt(sleep);
        dest.writeString(mSyncMaster);
        dest.writeStringList(mSyncSlaves);
        dest.writeString(mPlayerSubscriptionType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private String playerId;

    private boolean poweredOn;

    private @PlayState String playStatus;

    private ShuffleStatus shuffleStatus;

    private RepeatStatus repeatStatus;

    private Song currentSong;

    /** The name of the current playlist, which may be the empty string. */
    @NonNull
    private String currentPlaylist;

    private int currentPlaylistTracksNum;

    private int currentPlaylistIndex;

    private int currentTimeSecond;

    private int currentSongDuration;

    private int currentVolume;

    private int sleepDuration;

    private int sleep;

    /** The player this player is synced to (null if none). */
    @Nullable
    private String mSyncMaster;

    /** The players synced to this player. */
    private ImmutableList<String> mSyncSlaves = new ImmutableList.Builder<String>().build();

    /** How the server is subscribed to the player's status changes. */
    @NonNull
    @PlayerSubscriptionType private String mPlayerSubscriptionType = NOTIFY_NONE;

    public boolean isPlaying() {
        return PLAY_STATE_PLAY.equals(playStatus);
    }

    /**
     * @return the player's state. May be null, which indicates that Squeezer has received
     *     a "players" response for this player, but has not yet received a status message
     *     for it.
     */
    @Nullable
    @PlayState
    public String getPlayStatus() {
        return playStatus;
    }

    public boolean setPlayStatus(@NonNull @PlayState String s) {
        if (s.equals(playStatus)) {
            return false;
        }

        playStatus = s;

        return true;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public boolean getPoweredOn() {
        return poweredOn;
    }

    public boolean isPoweredOn() {
        return poweredOn;
    }

    public boolean setPoweredOn(boolean state) {
        if (state == poweredOn)
            return false;

        poweredOn = state;
        return true;
    }

    public ShuffleStatus getShuffleStatus() {
        return shuffleStatus;
    }

    public boolean setShuffleStatus(ShuffleStatus status) {
        if (status == shuffleStatus)
            return false;

        shuffleStatus = status;
        return true;
    }

    public boolean setShuffleStatus(String s) {
        return setShuffleStatus(s != null ? ShuffleStatus.valueOf(Util.parseDecimalIntOrZero(s)) : null);
    }

    public RepeatStatus getRepeatStatus() {
        return repeatStatus;
    }

    public boolean setRepeatStatus(RepeatStatus status) {
        if (status == repeatStatus)
            return false;

        repeatStatus = status;
        return true;
    }

    public boolean setRepeatStatus(String s) {
        return setRepeatStatus(s != null ? RepeatStatus.valueOf(Util.parseDecimalIntOrZero(s)) : null);
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    @NonNull
    public String getCurrentSongName() {
        return (currentSong != null) ? currentSong.getName() : "";
    }

    public boolean setCurrentSong(Song song) {
        if (song.equals(currentSong))
            return false;

        currentSong = song;
        return true;
    }

    /** @return the name of the current playlist, may be the empty string. */
    @NonNull
    public String getCurrentPlaylist() {
        return currentPlaylist;
    }

    /** @return the number of tracks in the current playlist */
    public int getCurrentPlaylistTracksNum() {
        return currentPlaylistTracksNum;
    }

    public int getCurrentPlaylistIndex() {
        return currentPlaylistIndex;
    }

    public boolean setCurrentPlaylist(@Nullable String playlist) {
        if (playlist == null)
            playlist = "";

        if (playlist.equals(currentPlaylist))
            return false;

        currentPlaylist = playlist;
        return true;
    }

    // set the number of tracks in the current playlist
    public boolean setCurrentPlaylistTracksNum(int value) {
        if (value == currentPlaylistTracksNum)
            return false;

        currentPlaylistTracksNum = value;
        return true;
    }

    public boolean setCurrentPlaylistIndex(int value) {
        if (value == currentPlaylistIndex)
            return false;

        currentPlaylistIndex = value;
        return true;
    }

    public int getCurrentTimeSecond() {
        return currentTimeSecond;
    }

    public boolean setCurrentTimeSecond(int value) {
        if (value == currentTimeSecond)
            return false;

        currentTimeSecond = value;
        return true;
    }

    public int getCurrentSongDuration() {
        return currentSongDuration;
    }

    public boolean setCurrentSongDuration(int value) {
        if (value == currentSongDuration)
            return false;

        currentSongDuration = value;
        return true;
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public boolean setCurrentVolume(int value) {
        if (value == currentVolume)
            return false;

        currentVolume = value;
        return true;
    }

    public int getSleepDuration() {
        return sleepDuration;
    }

    public boolean setSleepDuration(int sleepDuration) {
        if (sleepDuration == this.sleepDuration)
            return false;

        this.sleepDuration = sleepDuration;
        return true;
    }

    /** @return seconds left until the player sleeps. */
    public int getSleep() {
        return sleep;
    }

    /**
     *
     * @param sleep seconds left until the player sleeps.
     * @return True if the sleep value was changed, false otherwise.
     */
    public boolean setSleep(int sleep) {
        if (sleep == this.sleep)
            return false;

        this.sleep = sleep;
        return true;
    }

    public boolean setSyncMaster(@Nullable String syncMaster) {
        if (syncMaster == null && mSyncMaster == null)
            return false;

        if (syncMaster != null) {
            if (syncMaster.equals(mSyncMaster))
                return false;
        }

        mSyncMaster = syncMaster;
        return true;
    }

    @Nullable
    public String getSyncMaster() {
        return mSyncMaster;
    }

    public boolean setSyncSlaves(@NonNull List<String> syncSlaves) {
        if (syncSlaves.equals(mSyncSlaves))
            return false;

        mSyncSlaves = ImmutableList.copyOf(syncSlaves);
        return true;
    }

    public ImmutableList<String> getSyncSlaves() {
        return mSyncSlaves;
    }

    @PlayerSubscriptionType public String getSubscriptionType() {
        return mPlayerSubscriptionType;
    }

    public boolean setSubscriptionType(@Nullable @PlayerSubscriptionType String type) {
        if (Strings.isNullOrEmpty(type))
            return setSubscriptionType(NOTIFY_NONE);

        mPlayerSubscriptionType = type;
        return true;
    }

    @StringDef({PLAY_STATE_PLAY, PLAY_STATE_PAUSE, PLAY_STATE_STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayState {}
    public static final String PLAY_STATE_PLAY = "play";
    public static final String PLAY_STATE_PAUSE = "pause";
    public static final String PLAY_STATE_STOP = "stop";

    public enum ShuffleStatus implements EnumWithId {
        SHUFFLE_OFF(0, R.attr.ic_action_av_shuffle_off, ServerString.SHUFFLE_OFF),
        SHUFFLE_SONG(1, R.attr.ic_action_av_shuffle_song, ServerString.SHUFFLE_ON_SONGS),
        SHUFFLE_ALBUM(2, R.attr.ic_action_av_shuffle_album, ServerString.SHUFFLE_ON_ALBUMS);

        private final int id;

        private final int icon;

        private final ServerString text;

        private static final EnumIdLookup<ShuffleStatus> lookup = new EnumIdLookup<ShuffleStatus>(
                ShuffleStatus.class);

        ShuffleStatus(int id, int icon, ServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public ServerString getText() {
            return text;
        }

        public static ShuffleStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public enum RepeatStatus implements EnumWithId {
        REPEAT_OFF(0, R.attr.ic_action_av_repeat_off, ServerString.REPEAT_OFF),
        REPEAT_ONE(1, R.attr.ic_action_av_repeat_one, ServerString.REPEAT_ONE),
        REPEAT_ALL(2, R.attr.ic_action_av_repeat_all, ServerString.REPEAT_ALL);

        private final int id;

        private final int icon;

        private final ServerString text;

        private static final EnumIdLookup<RepeatStatus> lookup = new EnumIdLookup<RepeatStatus>(
                RepeatStatus.class);

        RepeatStatus(int id, int icon, ServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public ServerString getText() {
            return text;
        }

        public static RepeatStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

}
