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

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;


public class Year extends PlaylistItem {

    @Override
    public String getPlaylistTag() {
        return "year_id";
    }

    @Override
    public String getFilterTag() {
        return "year";
    }

    public Year(Map<String, String> record) {
        setId(record.get("year"));
    }

    public static final Creator<Year> CREATOR = new Creator<Year>() {
        public Year[] newArray(int size) {
            return new Year[size];
        }

        public Year createFromParcel(Parcel source) {
            return new Year(source);
        }
    };

    private Year(Parcel source) {
        setId(source.readString());
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public String toString() {
        return "year=" + getId();
    }

}
