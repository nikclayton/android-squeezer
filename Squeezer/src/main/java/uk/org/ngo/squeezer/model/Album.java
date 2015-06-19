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

import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.google.common.base.Strings;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ArtworkItem;

@JsonObject
public class Album extends ArtworkItem {

    @Override
    public String getPlaylistTag() {
        return "album_id";
    }

    @Override
    public String getFilterTag() {
        return "album_id";
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @NonNull
    private String mArtworkUrl = "";

    @NonNull
    public String getArtworkUrl() {
        return mArtworkUrl;
    }

    public void setArtworkUrl(@NonNull String artworkUrl) {
        mArtworkUrl = artworkUrl;
    }

    public Album setName(String name) {
        this.name = name;
        return this;
    }

    private String artist;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String model) {
        this.artist = model;
    }

    private int year;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Album(String albumId, String album) {
        setId(albumId);
        setName(album);
    }

    public Album(Map<String, String> record) {
        setId(record.containsKey("album_id") ? record.get("album_id") : record.get("id"));
        setName(record.get("album"));
        setArtist(record.get("artist"));
        setYear(Util.parseDecimalIntOrZero(record.get("year")));
        setArtwork_track_id(record.get("artwork_track_id"));
        mArtworkUrl = Strings.nullToEmpty(record.get("artwork_url"));
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }

        @Override
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }
    };

    public Album() {

    }

    private Album(Parcel source) {
        setId(source.readString());
        name = source.readString();
        artist = source.readString();
        year = source.readInt();
        setArtwork_track_id(source.readString());
        mArtworkUrl = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(artist);
        dest.writeInt(year);
        dest.writeString(getArtwork_track_id());
        dest.writeString(mArtworkUrl);
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", artist: " + artist + ", year: " + year;
    }

}
