/*
 * Copyright (c) 2012 Google Inc.
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

import com.google.common.base.Strings;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;

/**
 * Encapsulate a music folder item on the Squeezeserver.
 * <p/>
 * An item has a name and a type. The name is free text, the type may be one of "track", "folder",
 * "playlist", or "unknown".
 *
 * @author nik
 */
public class MusicFolderItem extends PlaylistItem {

    @Override
    public String getPlaylistTag() {
        if ("track".equals(type)) {
            return "track_id";
        }

        if ("playlist".equals(type)) {
            return "playlist_id";
        }

        if ("folder".equals(type)) {
            return "folder_id";
        }

        return "Unknown_type_in_getTag()";
    }

    @Override
    public String getFilterTag() {
        return "folder_id";
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public MusicFolderItem setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * The folder item's type, "track", "folder", "playlist", "unknown".
     */
    // XXX: Should be an enum.
    private String type;

    public String getType() {
        return type;
    }

    public MusicFolderItem setType(String type) {
        this.type = type;
        return this;
    }

    @Nullable
    private String url;

    @Nullable
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    /** The URL to use to download the song. */
    @NonNull
    private String mDownloadUrl = "";

    @NonNull
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public MusicFolderItem(Map<String, String> record) {
        setId(record.get("id"));
        name = record.get("filename");
        type = record.get("type");
        url = record.get("url");
        mDownloadUrl = Strings.nullToEmpty(record.get("download_url"));
    }

    public static final Creator<MusicFolderItem> CREATOR = new Creator<MusicFolderItem>() {
        @Override
        public MusicFolderItem[] newArray(int size) {
            return new MusicFolderItem[size];
        }

        @Override
        public MusicFolderItem createFromParcel(Parcel source) {
            return new MusicFolderItem(source);
        }
    };

    private MusicFolderItem(Parcel source) {
        setId(source.readString());
        name = source.readString();
        type = source.readString();
        url = source.readString();
        mDownloadUrl = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(url);
        dest.writeString(mDownloadUrl);
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", type: " + type;
    }
}
