package uk.org.ngo.squeezer.test.util;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Song;

public class UtilTest extends TestCase {

    public void testAtomicReferenceUpdated() {
        AtomicReference<String> atomicString = new AtomicReference<String>();
        assertFalse(Util.atomicReferenceUpdated(atomicString, null));
        assertEquals(null, atomicString.get());
        assertTrue(Util.atomicReferenceUpdated(atomicString, "test"));
        assertEquals("test", atomicString.get());
        assertFalse(Util.atomicReferenceUpdated(atomicString, "test"));
        assertEquals("test", atomicString.get());
        assertTrue(Util.atomicReferenceUpdated(atomicString, "change"));
        assertEquals("change", atomicString.get());
        assertTrue(Util.atomicReferenceUpdated(atomicString, null));
        assertEquals(null, atomicString.get());
        assertTrue(Util.atomicReferenceUpdated(atomicString, "change"));
        assertEquals("change", atomicString.get());
        assertTrue(Util.atomicReferenceUpdated(atomicString, null));
        assertEquals(null, atomicString.get());
        assertFalse(Util.atomicReferenceUpdated(atomicString, null));
        assertEquals(null, atomicString.get());

        AtomicReference<Item> atomicItem = new AtomicReference<Item>();
        Album album = new Album("1", "album");
        Song song = new Song(new HashMap<String, String>());
        song.setId("1");

        assertFalse(Util.atomicReferenceUpdated(atomicItem, null));
        assertEquals(null, atomicItem.get());
        assertTrue(Util.atomicReferenceUpdated(atomicItem, album));
        assertEquals(album, atomicItem.get());

        album.setName("newname");
        assertFalse(Util.atomicReferenceUpdated(atomicItem, album));
        assertEquals(album, atomicItem.get());

        assertTrue(Util.atomicReferenceUpdated(atomicItem, song));
        assertEquals(song, atomicItem.get());

        album.setId("2");
        assertTrue(Util.atomicReferenceUpdated(atomicItem, album));
        assertEquals(album, atomicItem.get());

        assertTrue(Util.atomicReferenceUpdated(atomicItem, null));
        assertEquals(null, atomicItem.get());
    }

    public void testParseInt() {
        assertEquals(2, Util.parseDecimalIntOrZero("2"));
        assertEquals(0, Util.parseDecimalIntOrZero("2x"));
        assertEquals(2, Util.parseDecimalIntOrZero("2.0"));
        assertEquals(2, Util.parseDecimalIntOrZero("2.9"));
        assertEquals(0, Util.parseDecimalIntOrZero(null));
        assertEquals(-2, Util.parseDecimalIntOrZero("-2"));
        assertEquals(0, Util.parseDecimalIntOrZero("2,0"));

        assertEquals(123456789, Util.parseDecimalInt("123456789", -1));
        assertEquals(-1, Util.parseDecimalInt("0x8", -1));
    }

    public void testTimeString() {
        assertEquals("0:00", Util.formatElapsedTime(0));
        assertEquals("0:01", Util.formatElapsedTime(1));
        assertEquals("0:10", Util.formatElapsedTime(10));
        assertEquals("0:59", Util.formatElapsedTime(59));
        assertEquals("1:00", Util.formatElapsedTime(60));
        assertEquals("1:01", Util.formatElapsedTime(61));
        assertEquals("1:59", Util.formatElapsedTime(119));
        assertEquals("2:00", Util.formatElapsedTime(120));
        assertEquals("2:01", Util.formatElapsedTime(121));
        assertEquals("18:39", Util.formatElapsedTime(1119));
        assertEquals("19:59", Util.formatElapsedTime(1199));
        assertEquals("20:00", Util.formatElapsedTime(1200));
        assertEquals("20:01", Util.formatElapsedTime(1201));
        assertEquals("20:11", Util.formatElapsedTime(1211));
    }

    public void testEncoding() {
        assertEquals("test", Util.decode("test"));
        assertEquals("test", Util.encode("test"));
        assertEquals("test", Util.decode(Util.encode("test")));

        assertEquals("test:test", Util.decode("test%3Atest"));
        assertEquals("test%3Atest", Util.encode("test:test"));
        assertEquals("test:test", Util.decode(Util.encode("test:test")));

        assertEquals("test test", Util.decode("test%20test"));
        assertEquals("test%20test", Util.encode("test test"));
        assertEquals("test test", Util.decode(Util.encode("test test")));

        assertEquals("test:æøåÆØÅ'éüõÛ-_/ ;.test", Util.decode(
                "test%3A%C3%A6%C3%B8%C3%A5%C3%86%C3%98%C3%85%27%C3%A9%C3%BC%C3%B5%C3%9B-_%2F%20%3B.test"));
        assertEquals(
                "test%3A%C3%A6%C3%B8%C3%A5%C3%86%C3%98%C3%85%27%C3%A9%C3%BC%C3%B5%C3%9B-_%2F%20%3B.test",
                Util.encode("test:æøåÆØÅ'éüõÛ-_/ ;.test"));
        assertEquals("test:æøåÆØÅ'éüũÛ-_/ ;.test",
                Util.decode(Util.encode("test:æøåÆØÅ'éüũÛ-_/ ;.test")));

        // Apparently LMS doesn't encode all the characters our version does, but luckily we still decode correctly
        assertEquals("album:#1's", Util.decode("album%3A%231's"));
        assertEquals("album:100 80'er hits (disc 1)",
                Util.decode("album%3A100%2080'er%20hits%20(disc%201)"));
    }

}
