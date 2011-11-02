/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package com.danga.squeezer;

oneway interface IServiceCallback {
  void onMusicChanged();

  // Empty strings to denote no default player.
  void onPlayerChanged(in String playerId, in String playerName);

  // postConnect is only true for the very first callback after a new initial connect.
  void onConnectionChanged(boolean isConnected, boolean postConnect);
  
  void onPlayStatusChanged(boolean isPlaying);
  void onTimeInSongChange(int secondsIn, int secondsTotal);
}

