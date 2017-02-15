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

package uk.org.ngo.squeezer.test.util;

import junit.framework.TestCase;

import uk.org.ngo.squeezer.util.ImageCache;

public class ImageCacheTest extends TestCase {
    /** Verify that hashKeyForDisk returns correct MD5 checksums. */
    public void testHashKeyForDisk() {
        assertEquals("acbd18db4cc2f85cedef654fccc4a4d8", ImageCache.hashKeyForDisk("foo"));
        assertEquals("37b51d194a7513e45b56f6524f2d51f2", ImageCache.hashKeyForDisk("bar"));
        assertEquals("73feffa4b7f6bb68e44cf984c85f6e88", ImageCache.hashKeyForDisk("baz"));
    }
}
