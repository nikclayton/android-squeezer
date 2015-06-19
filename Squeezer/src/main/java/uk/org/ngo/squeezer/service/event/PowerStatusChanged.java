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

import android.support.annotation.NonNull;

import uk.org.ngo.squeezer.model.Player;

/** Event sent when the power status of the player has changed. */
public class PowerStatusChanged {
    /** The player with changed state. */
    @NonNull public final Player player;

    /** Whether the active player supports being powered on by the server. */
    public final boolean canPowerOn;

    /** Whether the active player supports being turned off by the server. */
    public final boolean canPowerOff;

    public PowerStatusChanged(@NonNull Player player, boolean canPowerOn, boolean canPowerOff) {
        this.player = player;
        this.canPowerOn = canPowerOn;
        this.canPowerOff = canPowerOff;
    }
}
