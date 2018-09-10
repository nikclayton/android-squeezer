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
package uk.org.ngo.squeezer.service;

import android.support.annotation.NonNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class ServerVersion implements Comparable<ServerVersion> {
    private static final String VERSION_PART_SEPARATOR = "\\.";

    private List<Integer> parts = new ArrayList<>();

    public ServerVersion(@NonNull String version) {
        Preconditions.checkNotNull(version);
        for (String part : version.split(VERSION_PART_SEPARATOR)) {
            parts.add(Integer.parseInt(part));
        }
    }

    @Override
    public int compareTo(@NonNull ServerVersion o) {
        int index = 0;
        while (index < Math.max(parts.size(), o.parts.size())) {
            final int difference = getOrDefault(parts, index, 0) - getOrDefault(o.parts, index, 0);
            if (difference != 0) {
                return difference;
            }
            index++;
        }
        return 0;
    }

    private <T> T getOrDefault(List<T> list, int index, T defaultValue) {
        if (index < list.size()) {
            return list.get(index);
        } else {
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return Joiner.on(".").join(parts);
    }
}
