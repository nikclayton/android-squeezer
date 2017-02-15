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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class Plugin extends Item {

    public static final Plugin FAVORITE = new Plugin("favorites", R.drawable.ic_favorites);
    public static final Plugin MY_APPS = new Plugin("myapps", R.drawable.ic_my_apps);

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Plugin setName(String name) {
        this.name = name;
        return this;
    }

    private String icon;

    /**
     * @return Relative URL path to an icon for this radio or music service, for example
     * "plugins/Picks/html/images/icon.png"
     */
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    private int iconResource;

    /**
     * @return Icon resource for this plugin if it is embedded in the Squeezer app, or null.
     */
    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    private int weight;

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSearchable() {
        return "xmlbrowser_search".equals(type);
    }

    private Plugin(String cmd, int iconResource) {
        setId(cmd);
        setIconResource(iconResource);
    }

    public Plugin(Map<String, String> record) {
        setId(record.get("cmd"));
        name = record.get("name");
        type = record.get("type");
        icon = record.get("icon");
        weight = Util.parseDecimalIntOrZero(record.get("weight"));
    }

    public static final Creator<Plugin> CREATOR = new Creator<Plugin>() {
        public Plugin[] newArray(int size) {
            return new Plugin[size];
        }

        public Plugin createFromParcel(Parcel source) {
            return new Plugin(source);
        }
    };

    private Plugin(Parcel source) {
        setId(source.readString());
        name = source.readString();
        type = source.readString();
        icon = source.readString();
        iconResource = source.readInt();
        weight = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(icon);
        dest.writeInt(iconResource);
        dest.writeInt(weight);
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", name=" + name;
    }

}
