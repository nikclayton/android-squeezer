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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ServerVersionCompareToTest {

    private enum ExpectedResult {
        GREATER, EQUAL, LESS
    }

    @Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"4", "10", ExpectedResult.LESS},
                {"4.1", "10.2", ExpectedResult.LESS},
                {"4", "10.1", ExpectedResult.LESS},
                {"1.0.0", "1.0.0.1", ExpectedResult.LESS},
                {"1", "1", ExpectedResult.EQUAL},
                {"4.0", "4.0.0.0", ExpectedResult.EQUAL},
                {"10", "4", ExpectedResult.GREATER},
                {"10.2", "4.1", ExpectedResult.GREATER},
                {"10.1", "4", ExpectedResult.GREATER},
                {"1.0.0.1", "1.0.0", ExpectedResult.GREATER}
        });
    }

    private final String versionString1;

    private final String versionString2;

    private final ExpectedResult expectedResult;

    public ServerVersionCompareToTest(final String versionString1,
            final String versionString2,
            final ExpectedResult expectedResult) {
        this.versionString1 = versionString1;
        this.versionString2 = versionString2;
        this.expectedResult = expectedResult;
    }

    @Test
    public void compareTo() throws Exception {
        ServerVersion version1 = new ServerVersion(versionString1);
        ServerVersion version2 = new ServerVersion(versionString2);
        switch (expectedResult) {
            case EQUAL:
                assertEquals("Expected " + version1 + " to be equal to " + version2,
                        version1.compareTo(version2), 0);
                break;
            case GREATER:
                assertTrue("Expected " + version1 + " to be greater than " + version2,
                        version1.compareTo(version2) > 0);
                break;
            case LESS:
                assertTrue("Expected " + version1 + " to be less than " + version2,
                        version1.compareTo(version2) < 0);
                break;
            default:
                fail("Unexpected enum value");
        }
    }

}