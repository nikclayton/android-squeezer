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
package uk.org.ngo.squeezer.test.model;

import android.os.Parcel;
import android.test.AndroidTestCase;

import uk.org.ngo.squeezer.model.ContributorRole;
import uk.org.ngo.squeezer.model.ContributorRoles;

public class ContributorRolesTest extends AndroidTestCase {

    public void testWritingAndReadingFromAParcel() {
        final ContributorRoles original = new ContributorRoles(ContributorRole.ARTIST,
                ContributorRole.ALBUMARTIST,
                ContributorRole.TRACKARTIST);

        final Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        final ContributorRoles copy = ContributorRoles.CREATOR.createFromParcel(parcel);

        assertEquals(original.getId(), copy.getId());
    }

    public void testGetId() {
        final ContributorRoles original = new ContributorRoles(
                ContributorRole.BAND,
                ContributorRole.COMPOSER,
                ContributorRole.CONDUCTOR);

        assertEquals(original.getId(), "BAND,COMPOSER,CONDUCTOR");
    }

    public void testGetFilterTag() {
        final ContributorRoles original = new ContributorRoles(
                ContributorRole.ALBUMARTIST);

        assertEquals(original.getFilterTag(), "role_id");
    }

    public void testGetFilterParameter() {
        final ContributorRoles original = new ContributorRoles(
                ContributorRole.ALBUMARTIST,
                ContributorRole.CONDUCTOR);

        assertEquals(original.getFilterParameter(), "role_id:ALBUMARTIST,CONDUCTOR");
    }
}