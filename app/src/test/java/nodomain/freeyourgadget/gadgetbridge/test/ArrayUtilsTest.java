package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests ArrayUtils
 */
public class ArrayUtilsTest extends TestBase {

    private static final byte[] EMPTY = new byte[0];
    private static final byte[] DATA_5 = new byte[] { 1, 2, 3, 4, 5};

    public ArrayUtilsTest() throws Exception {
    }

    @Test
    public void testEqualsException1() throws Exception {
        try {
            ArrayUtils.equals(null, EMPTY, 0);
            fail("equals should throw on bad argument");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testEqualsException2() throws Exception {
        try {
            ArrayUtils.equals(EMPTY, null, 0);
            fail("equals should throw on bad argument");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testEqualsException5() throws Exception {
        try {
            ArrayUtils.equals(EMPTY, EMPTY, -1);
            fail("equals should throw on bad argument");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testEqualsEmptyData() throws Exception {
        assertFalse(ArrayUtils.equals(EMPTY, DATA_5, 0));
    }

    @Test
    public void testEqualsEmptyTest() throws Exception {
        // testing for 0 equal bytes may be senseless, but always true
        assertTrue(ArrayUtils.equals(EMPTY, EMPTY, 0));
        assertTrue(ArrayUtils.equals(DATA_5, EMPTY, 0));
    }

    @Test
    public void testEquals1() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, b(1), 0));
    }

    @Test
    public void testEquals2() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, b(2), 1));
    }

    @Test
    public void testEquals5() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, b(5), 4));
    }

    @Test
    public void testEqualsOutOfRange() throws Exception {
        assertFalse(ArrayUtils.equals(DATA_5, b(5), 5));
    }

    @Test
    public void testEquals123() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, new byte[] {1, 2, 3}, 0));
    }

    @Test
    public void testEquals234() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, new byte[] {2, 3, 4}, 1));
    }

    @Test
    public void testEquals345() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, new byte[] {3, 4, 5}, 2));
    }

    @Test
    public void testEquals12345() throws Exception {
        assertTrue(ArrayUtils.equals(DATA_5, DATA_5, 0));
    }

    @Test
    public void testEqualsWrongStart() throws Exception {
        assertFalse(ArrayUtils.equals(DATA_5, new byte[] {0, 2, 3}, 0));
    }

    @Test
    public void testEqualsWrongEnd() throws Exception {
        assertFalse(ArrayUtils.equals(DATA_5, new byte[] {3, 4, 6}, 2));
    }

    private byte[] b(int b) {
        return new byte[] {(byte) b};
    }
}
