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

import android.os.Build;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class Player extends Item implements Comparable {

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
    public int compareTo(Object otherPlayer) {
        return this.mName.compareToIgnoreCase(((Player)otherPlayer).mName);
    }

    public static class Pref {
        /** The types of player preferences. */
        @StringDef({ALARM_DEFAULT_VOLUME, ALARM_FADE_SECONDS, ALARM_SNOOZE_SECONDS, ALARM_TIMEOUT_SECONDS,
                Pref.ALARMS_ENABLED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Name {}
        public static final String ALARM_DEFAULT_VOLUME = "alarmDefaultVolume";
        public static final String ALARM_FADE_SECONDS = "alarmfadeseconds";
        public static final String ALARM_SNOOZE_SECONDS = "alarmSnoozeSeconds";
        public static final String ALARM_TIMEOUT_SECONDS = "alarmTimeoutSeconds";
        public static final String ALARMS_ENABLED = "alarmsEnabled";
        public static final Set<String> VALID_PLAYER_PREFS = Sets.newHashSet(
                ALARM_DEFAULT_VOLUME, ALARM_FADE_SECONDS, ALARM_SNOOZE_SECONDS, ALARM_TIMEOUT_SECONDS,
                ALARMS_ENABLED);
    }

    public Player(Map<String, String> record) {
        setId(record.get("playerid"));
        mIp = record.get("ip");
        mName = record.get("name");
        mModel = record.get("model");
        mCanPowerOff = Util.parseDecimalIntOrZero(record.get("canpoweroff")) == 1;
        mConnected = Util.parseDecimalIntOrZero(record.get("connected")) == 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mHashCode = mHashFunction.hashString(getId(), Charsets.UTF_8);
        } else {
            // API versions < GINGERBREAD do not have String.getBytes(Charset charset),
            // which hashString() ends up calling. This will trigger an exception.
            byte[] bytes;
            try {
                bytes = getId().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Can't happen, Android's native charset is UTF-8. But just in case
                // we're running on something wacky, fallback to the un-parsed bytes.
                bytes = getId().getBytes();
            }
            mHashCode = mHashFunction.hashBytes(bytes);
        }
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

    @Override
    public String getName() {
        return mName;
    }

    public Player setName(String name) {
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
    public String toStringOpen() {
        return super.toStringOpen() + ", model: " + mModel + ", canpoweroff: " + mCanPowerOff
                + ", ip: " + mIp + ", connected: " + mConnected;
    }
}
