/*
 * Copyright (c) 2015 Kurt Aaholst <kaaholst@gmail.com>
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
 *
 */

package uk.org.ngo.squeezer.framework;

import uk.org.ngo.squeezer.service.ServerVersion;

/**
 * Helper interface suitable for testing whether the current server version supports a specific
 * enum value
 */
public interface VersionedEnumWithText extends EnumWithText {

    boolean can(ServerVersion serverVersion);
}
