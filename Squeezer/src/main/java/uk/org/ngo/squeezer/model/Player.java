/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.service.event.SongTimeChanged;


public class Player extends Item implements Comparable<Player> {

    private String mName;

    private final String mIp;

    private final String mModel;

    private final boolean mCanPowerOff;

    /** Hash function to generate at least 64 bits of hashcode from a player's ID. */
    private static final HashFunction mHashFunction = Hashing.goodFastHash(64);

    /**  A hash of the player's ID. */
    private final HashCode mHashCode;

    private PlayerState mPlayerState = new PlayerState();

    /** Is the player connected? */
    private boolean mConnected;

    @Override
    public int compareTo(@NonNull Player otherPlayer) {
        return this.mName.compareToIgnoreCase((otherPlayer).mName);
    }

    public static class Pref {
        /** The types of player preferences. */
        @StringDef({ALARM_DEFAULT_VOLUME, ALARM_FADE_SECONDS, ALARM_SNOOZE_SECONDS, ALARM_TIMEOUT_SECONDS,
                ALARMS_ENABLED, PLAY_TRACK_ALBUM, DEFEAT_DESTRUCTIVE_TTP})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Name {}
        public static final String ALARM_DEFAULT_VOLUME = "alarmDefaultVolume";
        public static final String ALARM_FADE_SECONDS = "alarmfadeseconds";
        public static final String ALARM_SNOOZE_SECONDS = "alarmSnoozeSeconds";
        public static final String ALARM_TIMEOUT_SECONDS = "alarmTimeoutSeconds";
        public static final String ALARMS_ENABLED = "alarmsEnabled";
        public static final String PLAY_TRACK_ALBUM = "playtrackalbum";
        public static final String DEFEAT_DESTRUCTIVE_TTP = "defeatDestructiveTouchToPlay";
        public static final String MEDIA_DIRS = "mediadirs";
    }

    public Player(Map<String, Object> record) {
        setId(getString(record, "playerid"));
        mIp = getString(record, "ip");
        mName = getString(record, "name");
        mModel = getString(record, "model");
        mCanPowerOff = getInt(record, "canpoweroff") == 1;
        mConnected = getInt(record, "connected") == 1;
        mHashCode = calcHashCode();

        if (record.containsKey(Pref.PLAY_TRACK_ALBUM)) {
            mPlayerState.prefs.put(Pref.PLAY_TRACK_ALBUM, Util.getString(record, Pref.PLAY_TRACK_ALBUM));
        }
        if (record.containsKey(Pref.DEFEAT_DESTRUCTIVE_TTP)) {
            mPlayerState.prefs.put(Pref.DEFEAT_DESTRUCTIVE_TTP, Util.getString(record, Pref.DEFEAT_DESTRUCTIVE_TTP));
        }
    }

    private HashCode calcHashCode() {
        return mHashFunction.hashString(getId(), Charsets.UTF_8);
    }

    private Player(Parcel source) {
        setId(source.readString());
        mIp = source.readString();
        mName = source.readString();
        mModel = source.readString();
        mCanPowerOff = (source.readByte() == 1);
        mConnected = (source.readByte() == 1);
        mHashCode = HashCode.fromString(source.readString());
    }

    @NonNull
    @Override
    public String getName() {
        return mName;
    }

    public Player setName(@NonNull String name) {
        this.mName = name;
        return this;
    }

    public String getIp() {
        return mIp;
    }

    public String getModel() {
        return mModel;
    }

    public boolean isCanpoweroff() {
        return mCanPowerOff;
    }

    public void setConnected(boolean connected) {
        mConnected = connected;
    }

    public boolean getConnected() {
        return mConnected;
    }

    @NonNull
    public PlayerState getPlayerState() {
        return mPlayerState;
    }

    public void setPlayerState(@NonNull PlayerState playerState) {
        mPlayerState = playerState;
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        @Override
        public Player[] newArray(int size) {
            return new Player[size];
        }

        @Override
        public Player createFromParcel(Parcel source) {
            return new Player(source);
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mIp);
        dest.writeString(mName);
        dest.writeString(mModel);
        dest.writeByte(mCanPowerOff ? (byte) 1 : (byte) 0);
        dest.writeByte(mConnected ? (byte) 1 : (byte) 0);
        dest.writeString(mHashCode.toString());
    }

    /**
     * Returns a 64 bit identifier for the player.  The ID tracked by the server is a unique
     * string that identifies the player.  It may be -- but is not required to be -- the
     * player's MAC address.  Rather than assume it is the MAC address, calculate a 64 bit
     * hash of the ID and use that.
     *
     * @return The hash of the player's ID.
     */
    public long getIdAsLong() {
        return mHashCode.asLong();
    }

    /**
     * Comparator to compare two players by ID.
     */
    public static final Comparator<Player> compareById = new Comparator<Player>() {
        @Override
        public int compare(Player lhs, Player rhs) {
            return lhs.getId().compareTo(rhs.getId());
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        // super.equals() has already checked that o is not null and is of the same class.
        Player p = (Player) o;

        return getName().equals(p.getName());
    }

    @NonNull
    @Override
    public String toString() {
        return "Player{" +
                "mName='" + mName + '\'' +
                ", mIp='" + mIp + '\'' +
                ", mModel='" + mModel + '\'' +
                ", mCanPowerOff=" + mCanPowerOff +
                ", mHashCode=" + mHashCode +
                ", mPlayerState=" + mPlayerState +
                ", mConnected=" + mConnected +
                '}';
    }

    public SongTimeChanged getTrackElapsed() {
        double now = SystemClock.elapsedRealtime() / 1000.0;
        double trackCorrection = mPlayerState.rate * (now - mPlayerState.statusSeen);
        int trackElapsed = (int) (trackCorrection <= 0 ? mPlayerState.getCurrentTimeSecond() : mPlayerState.getCurrentTimeSecond() + trackCorrection);
        if (trackElapsed > mPlayerState.getCurrentSongDuration()) {
            trackElapsed = mPlayerState.getCurrentSongDuration();
        }

        return new SongTimeChanged(this, trackElapsed, mPlayerState.getCurrentSongDuration());
    }

    public int getSleepingIn() {
        double now = SystemClock.elapsedRealtime() / 1000.0;
        double correction = now - mPlayerState.statusSeen;
        double remaining = (correction <= 0 ? mPlayerState.getSleep() : mPlayerState.getSleep() - correction);

        return (int) remaining;
    }
}
