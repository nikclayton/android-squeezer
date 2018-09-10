/*
 * Copyright (c) 2017 Michał Szkutnik
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

import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.List;

import uk.org.ngo.squeezer.framework.FilterItem;

public class ContributorRoles implements FilterItem, Parcelable {

    protected ContributorRoles(Parcel in) {
        roles = in.readArrayList(ContributorRoles.class.getClassLoader());
    }

    public static final Creator<ContributorRoles> CREATOR = new Creator<ContributorRoles>() {
        @Override
        public ContributorRoles createFromParcel(Parcel in) {
            return new ContributorRoles(in);
        }

        @Override
        public ContributorRoles[] newArray(int size) {
            return new ContributorRoles[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(roles);
    }

    private final List<ContributorRole> roles;

    public ContributorRoles(ContributorRole... roles) {
        this.roles = Arrays.asList(roles);
    }

    @Override
    public String getId() {
        return Joiner.on(",").join(roles);
    }

    @Override
    public String getFilterTag() {
        return "role_id";
    }

    @Override
    public String getFilterParameter() {
        return getFilterTag() + ":" + getId();
    }
}
