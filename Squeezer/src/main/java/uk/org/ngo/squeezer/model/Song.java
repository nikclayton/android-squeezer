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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ArtworkItem;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class Song extends ArtworkItem {
    private static final String TAG = "Song";

    @Override
    public String getPlaylistTag() {
        return "track_id";
    }

    @Override
    public String getFilterTag() {
        return "track_id";
    }

    /** The "track" or "title" value from the server. */
    @NonNull private final String mName;

    @Override
    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull private final String mArtist;

    @NonNull
    public String getArtist() {
        return mArtist;
    }

    @NonNull private Album mAlbum;

    @NonNull
    public Album getAlbum() {
        return mAlbum;
    }

    @NonNull private final String mAlbumName;

    @NonNull
    public String getAlbumName() {
        return mAlbumName;
    }

    private final boolean mCompilation;

    public boolean getCompilation() {
        return mCompilation;
    }

    private final int mDuration;

    public int getDuration() {
        return mDuration;
    }

    private final int mYear;

    public int getYear() {
        return mYear;
    }

    @NonNull private final String mArtistId;

    @NonNull
    public String getArtistId() {
        return mArtistId;
    }

    @NonNull private final String mAlbumId;

    @NonNull
    public String getAlbumId() {
        return mAlbumId;
    }

    private boolean mRemote;

    public boolean isRemote() {
        return mRemote;
    }

    private final int mTrackNum;

    public int getTrackNum() {
        return mTrackNum;
    }

    /** The URL of the track on the server. This is the file:/// URL, not the URL to download it. */
    @NonNull private final String mUrl;

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    /** The URL to use to download the song. */
    @NonNull private String mDownloadUrl = "";

    @NonNull
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    @NonNull private final String mButtons;

    @NonNull
    public String getButtons() {
        return mButtons;
    }

    @NonNull private String mArtworkUrl;

    /**
     * @return Whether the song has artwork associated with it.
     */
    public boolean hasArtwork() {
        return ! "".equals(mArtworkUrl);
    }

    @NonNull
    public String getArtworkUrl() {
        return mArtworkUrl;
    }

    public Song(Map<String, String> record) {
        if (getId() == null) {
            setId(record.get("track_id"));
        }
        if (getId() == null) {
            setId(record.get("id"));
        }

        mName = record.containsKey("track") ? Strings.nullToEmpty(record.get("track"))
                : Strings.nullToEmpty(record.get("title"));

        mArtist = Strings.nullToEmpty(record.get("artist"));
        mAlbumName = Strings.nullToEmpty(record.get("album"));
        mCompilation = Util.parseDecimalIntOrZero(record.get("compilation")) == 1;
        mDuration = Util.parseDecimalIntOrZero(record.get("duration"));
        mYear = Util.parseDecimalIntOrZero(record.get("year"));
        mArtistId = Strings.nullToEmpty(record.get("artist_id"));
        mAlbumId = Strings.nullToEmpty(record.get("album_id"));
        mRemote = Util.parseDecimalIntOrZero(record.get("remote")) != 0;
        mTrackNum = Util.parseDecimalInt(record.get("tracknum"), 1);
        mArtworkUrl = Strings.nullToEmpty(record.get("artwork_url"));
        mUrl = Strings.nullToEmpty(record.get("url"));
        mDownloadUrl = Strings.nullToEmpty(record.get("download_url"));
        mButtons = Strings.nullToEmpty(record.get("buttons"));

        String artworkTrackId = record.get("artwork_track_id");

        Album album = new Album(mAlbumId, mAlbumName);
        album.setArtist(mCompilation ? "Various" : mArtist);
        album.setArtwork_track_id(artworkTrackId);
        album.setArtworkUrl(mArtworkUrl);
        album.setYear(mYear);
        mAlbum = album;
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }

        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }
    };

    private Song(Parcel source) {
        setId(source.readString());
        mName = source.readString();
        mArtist = source.readString();
        mAlbumName = source.readString();
        mCompilation = source.readInt() == 1;
        mDuration = source.readInt();
        mYear = source.readInt();
        mArtistId = source.readString();
        mAlbumId = source.readString();
        setArtwork_track_id(source.readString());
        mTrackNum = source.readInt();
        mUrl = source.readString();
        mButtons = source.readString();
        mDownloadUrl = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
        dest.writeString(mArtist);
        dest.writeString(mAlbumName);
        dest.writeInt(mCompilation ? 1 : 0);
        dest.writeInt(mDuration);
        dest.writeInt(mYear);
        dest.writeString(mArtistId);
        dest.writeString(mAlbumId);
        dest.writeString(getArtwork_track_id());
        dest.writeInt(mTrackNum);
        dest.writeString(mUrl);
        dest.writeString(mButtons);
        dest.writeString(mDownloadUrl);
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", mArtist: " + mArtist + ", year: " + mYear;
    }

    /**
     * Extend the equality test by looking at additional track information.
     * <p/>
     * This is to deal with songs from remote streams where the stream might provide a single
     * song ID for multiple consecutive songs in the stream.
     *
     * @param o The object to test.
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        // super.equals() has already checked that o is not null and is of the same class.
        Song s = (Song)o;

        if (! s.getName().equals(mName)) {
            return false;
        }

        if (! s.getAlbumName().equals(mAlbumName)) {
            return false;
        }

        if (! s.getArtist().equals(mArtist)) {
            return false;
        }

        if (! s.getArtworkUrl().equals(mArtworkUrl)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), mName, mAlbumName, mArtist, mArtworkUrl);
    }
}
