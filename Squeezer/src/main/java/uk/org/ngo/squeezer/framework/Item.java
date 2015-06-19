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

package uk.org.ngo.squeezer.framework;

import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Base class for SqueezeServer data. Specializations must implement all the necessary boilerplate
 * code. This is okay for now, because we only have few data types.
 *
 * @author Kurt Aaholst
 */
@JsonObject
public abstract class Item implements Parcelable {

    @JsonField
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    abstract public String getName();

    public Item() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return (getId() != null ? getId().hashCode() : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o.getClass() != getClass()) {
            // There is no guarantee that SqueezeServer items have globally unique IDs.
            return false;
        }

        // Both might be empty items. For example a Song initialised
        // with an empty token map, because no song is currently playing.
        if (getId() == null && ((Item) o).getId() == null) {
            return true;
        }

        return getId() != null && getId().equals(((Item) o).getId());
    }

    protected String toStringOpen() {
        return getClass().getSimpleName() + " { id: " + getId() + ", name: " + getName();
    }

    @Override
    public String toString() {
        return toStringOpen() + " }";
    }
}
