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

import org.junit.Test;

public class ServerVersionTest {

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowForNullString() {
        new ServerVersion(null);
    }

    @Test(expected = NumberFormatException.class)
    public void constructorShouldThrowForNonIntegerPart() {
        new ServerVersion("foo.bar");
    }

    @Test(expected = NumberFormatException.class)
    public void constructorShouldThrowForSpuriousSpaces() {
        new ServerVersion("1. 2");
    }

    @Test(expected = NumberFormatException.class)
    public void constructorShouldThrowForEmptyString() {
        new ServerVersion("");
    }

    @Test(expected = NumberFormatException.class)
    public void constructorShouldThrowForWhitespaceOnlyString() {
        new ServerVersion("    ");
    }
}