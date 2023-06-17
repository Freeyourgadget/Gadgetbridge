package nodomain.freeyourgadget.gadgetbridge.test;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import org.junit.Test;

import java.util.Arrays;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BLETypeConversionsTest extends TestBase {
    @Test
    public void testTimeParsing1() {
        byte[] requested = new byte[]{
                (byte) 0xe1, 0x07, 0x0a, 0x1d, 0x00, 0x1c, 0x00, 0x08
        };
        byte[] received = new byte[]{
                (byte) 0xe1, 0x07, 0x0a, 0x1c, 0x17, 0x1c, 0x00, 0x04
        };
        GregorianCalendar calRequested = BLETypeConversions.rawBytesToCalendar(requested);
        GregorianCalendar calReceived = BLETypeConversions.rawBytesToCalendar(received);

        assertEquals(calRequested.getTime(), calReceived.getTime());
    }

    @Test
    public void testTimeParsingWithDST() {
        byte[] requested = new byte[]{
                (byte) 0xe1, 0x07, 0x0a, 0x09, 0x11, 0x23, 0x00, 0x08
        };
        byte[] received = new byte[]{
                (byte) 0xe1, 0x07, 0x0a, 0x09, 0x10, 0x23, 0x00, 0x04
        };
        GregorianCalendar calRequested = BLETypeConversions.rawBytesToCalendar(requested);
        GregorianCalendar calReceived = BLETypeConversions.rawBytesToCalendar(received);

        assertEquals(calRequested.getTime(), calReceived.getTime());
    }

    @Test
    public void testToUnsignedInt() {
        assertEquals(0L, BLETypeConversions.toUnsigned(0));
        assertEquals(12345L, BLETypeConversions.toUnsigned(12345));
        assertEquals(0x7FFFFFFFL, BLETypeConversions.toUnsigned(0x7FFFFFFF));
        assertEquals(0x80000000L, BLETypeConversions.toUnsigned(0x80000000));
        assertEquals(0xFFFFFFFFL, BLETypeConversions.toUnsigned(0xFFFFFFFF));
    }

    @Test
    public void testToUnsignedShort() {
        assertEquals(0, BLETypeConversions.toUnsigned((short) 0));
        assertEquals(12345, BLETypeConversions.toUnsigned((short) 12345));
        assertEquals(0x7FFF, BLETypeConversions.toUnsigned((short) 0x7FFF));
        assertEquals(0x8000, BLETypeConversions.toUnsigned((short) 0x8000));
        assertEquals(0xFFFF, BLETypeConversions.toUnsigned((short) 0xFFFF));
    }

    @Test
    public void testToUnsignedByte() {
        assertEquals(0, BLETypeConversions.toUnsigned((byte) 0));
        assertEquals(123, BLETypeConversions.toUnsigned((byte) 123));
        assertEquals(0x7F, BLETypeConversions.toUnsigned((byte) 0x7F));
        assertEquals(0x80, BLETypeConversions.toUnsigned((byte) 0x80));
        assertEquals(0xFF, BLETypeConversions.toUnsigned((byte) 0xFF));
    }

    @Test
    public void testToUnsignedByteArray() {
        final byte[] array = new byte[]{(byte) 0x12, (byte) 0x00, (byte) 123, (byte) 0x7F, (byte) 0x80, (byte) 0xFF};
        assertEquals(0, BLETypeConversions.toUnsigned(array, 1));
        assertEquals(123, BLETypeConversions.toUnsigned(array, 2));
        assertEquals(0x7F, BLETypeConversions.toUnsigned(array, 3));
        assertEquals(0x80, BLETypeConversions.toUnsigned(array, 4));
        assertEquals(0xFF, BLETypeConversions.toUnsigned(array, 5));
    }

    @Test
    public void testToUint16Single() {
        assertEquals(0, BLETypeConversions.toUint16((byte) 0));
        assertEquals(123, BLETypeConversions.toUint16((byte) 123));
        assertEquals(0x7F, BLETypeConversions.toUint16((byte) 0x7F));
        assertEquals(0x80, BLETypeConversions.toUint16((byte) 0x80));
        assertEquals(0xFF, BLETypeConversions.toUint16((byte) 0xFF));
    }

    @Test
    public void testToUint16Bytes() {
        assertEquals(0, BLETypeConversions.toUint16((byte) 0, (byte) 0));
        assertEquals(0x1234, BLETypeConversions.toUint16((byte) 0x34, (byte) 0x12));
        assertEquals(0x369C, BLETypeConversions.toUint16((byte) 0x9C, (byte) 0x36));
        assertEquals(0x7FFF, BLETypeConversions.toUint16((byte) 0xFF, (byte) 0x7F));
        assertEquals(0x8000, BLETypeConversions.toUint16((byte) 0x00, (byte) 0x80));
        assertEquals(0xFFFF, BLETypeConversions.toUint16((byte) 0xFF, (byte) 0xFF));
    }

    @Test
    public void testToUint16Array() {
        final byte[] array = new byte[]{
                (byte) 0x12,
                (byte) 0x00, (byte) 0x00,
                (byte) 0x34, (byte) 0x12,
                (byte) 0x9C, (byte) 0x36,
                (byte) 0xFF, (byte) 0x7F,
                (byte) 0x00, (byte) 0x80,
                (byte) 0xFF, (byte) 0xFF,
        };
        assertEquals(0, BLETypeConversions.toUint16(array, 1));
        assertEquals(0x1234, BLETypeConversions.toUint16(array, 3));
        assertEquals(0x369C, BLETypeConversions.toUint16(array, 5));
        assertEquals(0x7FFF, BLETypeConversions.toUint16(array, 7));
        assertEquals(0x8000, BLETypeConversions.toUint16(array, 9));
        assertEquals(0xFFFF, BLETypeConversions.toUint16(array, 11));
    }

    @Test
    public void testToUint32Bytes() {
        assertEquals(0, BLETypeConversions.toUint32((byte) 0, (byte) 0, (byte) 0, (byte) 0));
        assertEquals(0x12345678, BLETypeConversions.toUint32((byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12));
        assertEquals(0x2468ACE0, BLETypeConversions.toUint32((byte) 0xE0, (byte) 0xAC, (byte) 0x68, (byte) 0x24));
        assertEquals(0x7FFFFFFF, BLETypeConversions.toUint32((byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F));
        assertEquals(0x80000000, BLETypeConversions.toUint32((byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80));
        assertEquals(0xFFFFFFFF, BLETypeConversions.toUint32((byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF));
    }

    @Test
    public void testToUint32Array() {
        final byte[] array = new byte[]{
                (byte) 0x12,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12,
                (byte) 0xE0, (byte) 0xAC, (byte) 0x68, (byte) 0x24,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        };
        assertEquals(0, BLETypeConversions.toUint32(array, 1));
        assertEquals(0x12345678, BLETypeConversions.toUint32(array, 5));
        assertEquals(0x2468ACE0, BLETypeConversions.toUint32(array, 9));
        assertEquals(0x7FFFFFFF, BLETypeConversions.toUint32(array, 13));
        assertEquals(0x80000000, BLETypeConversions.toUint32(array, 17));
        assertEquals(0xFFFFFFFF, BLETypeConversions.toUint32(array, 21));
    }

    @Test
    public void testToUint64Bytes() {
        assertEquals(0, BLETypeConversions.toUint64((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0));
        assertEquals(0x123456789ABCDEF0L, BLETypeConversions.toUint64((byte) 0xF0, (byte) 0xDE, (byte) 0xBC, (byte) 0x9A, (byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12));
        assertEquals(0x7FFFFFFFFFFFFFFFL, BLETypeConversions.toUint64((byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F));
        assertEquals(0x8000000000000000L, BLETypeConversions.toUint64((byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80));
        assertEquals(0xFFFFFFFFFFFFFFFFL, BLETypeConversions.toUint64((byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF));
    }

    @Test
    public void testToUint64Array() {
        final byte[] array = new byte[]{
                (byte) 0x12,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xF0, (byte) 0xDE, (byte) 0xBC, (byte) 0x9A, (byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        };
        assertEquals(0, BLETypeConversions.toUint64(array, 1));
        assertEquals(0x123456789ABCDEF0L, BLETypeConversions.toUint64(array, 9));
        assertEquals(0x7FFFFFFFFFFFFFFFL, BLETypeConversions.toUint64(array, 17));
        assertEquals(0x8000000000000000L, BLETypeConversions.toUint64(array, 25));
        assertEquals(0xFFFFFFFFFFFFFFFFL, BLETypeConversions.toUint64(array, 33));
    }

    @Test
    public void testWriteUint8() {
        for (final int testedValue : new int[]{
                0x00, 123, 0x7F, 0x80, 0xFF
        }) {
            final byte[] buffer = new byte[2];
            Arrays.fill(buffer, (byte) 0xFF);
            BLETypeConversions.writeUint8(buffer, 1, testedValue);
            assertEquals(testedValue, BLETypeConversions.toUnsigned(buffer, 1));
        }
    }

    @Test
    public void testWriteUint16() {
        for (final int testedValue : new int[]{
                0x0000, 12345, 0x7FFF, 0x8000, 0xFFFF
        }) {
            final byte[] buffer = new byte[3];
            Arrays.fill(buffer, (byte) 0xFF);
            BLETypeConversions.writeUint16(buffer, 1, testedValue);
            assertEquals(testedValue, BLETypeConversions.toUint16(buffer, 1));
        }
    }

    @Test
    public void testWriteUint32() {
        for (final int testedValue : new int[]{
                0, 0x12345678, 0x7FFFFFFF, 0x80000000, 0xFFFFFFFF
        }) {
            final byte[] buffer = new byte[5];
            Arrays.fill(buffer, (byte) 0xFF);
            BLETypeConversions.writeUint32(buffer, 1, testedValue);
            assertEquals(testedValue, BLETypeConversions.toUint32(buffer, 1));
        }
    }

    @Test
    public void testWriteUint64() {
        for (final long testedValue : new long[]{
                0L, 0x123456789ABCDEF0L, 0x7FFFFFFFFFFFFFFFL, 0x8000000000000000L, 0xFFFFFFFFFFFFFFFFL
        }) {
            final byte[] buffer = new byte[9];
            Arrays.fill(buffer, (byte) 0xFF);
            BLETypeConversions.writeUint64(buffer, 1, testedValue);
            assertEquals(testedValue, BLETypeConversions.toUint64(buffer, 1));
        }
    }
}
