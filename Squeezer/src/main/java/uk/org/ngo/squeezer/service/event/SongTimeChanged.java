/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service.event;

/** Event sent when the duration or current play position of the current song has changed. */
public class SongTimeChanged {
    /** The current position of the player in the song, measured in seconds. */
    public final int currentPosition;

    /** The song's duration, measured in seconds. */
    public final int duration;

    public SongTimeChanged(int currentPosition, int duration) {
        this.currentPosition = currentPosition;
        this.duration = duration;
    }
}
