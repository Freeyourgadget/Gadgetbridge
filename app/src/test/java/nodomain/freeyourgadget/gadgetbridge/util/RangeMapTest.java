package nodomain.freeyourgadget.gadgetbridge.util;

import static org.junit.Assert.*;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class RangeMapTest extends TestBase {
    @Test
    public void testLowerBound() {
        final RangeMap<Integer, Integer> map = new RangeMap<>();
        assertEquals(0, map.size());
        assertNull(map.get(0));

        map.put(10, 20);
        assertNull(map.get(0));
        assertEquals(20, map.get(10).intValue());
        assertEquals(20, map.get(20).intValue());

        map.put(20, 30);
        map.put(30, 40);
        assertNull(map.get(0));
        assertEquals(20, map.get(10).intValue());
        assertEquals(20, map.get(15).intValue());
        assertEquals(30, map.get(20).intValue());
        assertEquals(30, map.get(25).intValue());
        assertEquals(40, map.get(30).intValue());
    }

    @Test
    public void testUpperBound() {
        final RangeMap<Integer, Integer> map = new RangeMap<>(RangeMap.Mode.UPPER_BOUND);
        assertEquals(0, map.size());
        assertNull(map.get(0));

        map.put(10, 20);
        assertNull(map.get(20));
        assertEquals(20, map.get(10).intValue());
        assertEquals(20, map.get(0).intValue());

        map.put(20, 30);
        map.put(30, 40);
        assertNull(map.get(50));
        assertEquals(40, map.get(30).intValue());
        assertEquals(40, map.get(25).intValue());
        assertEquals(30, map.get(20).intValue());
        assertEquals(30, map.get(15).intValue());
        assertEquals(20, map.get(10).intValue());
    }
}
