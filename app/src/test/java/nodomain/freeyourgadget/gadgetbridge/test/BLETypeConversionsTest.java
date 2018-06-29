package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

import static org.junit.Assert.assertTrue;

public class BLETypeConversionsTest extends TestBase {
    @Test
    public void testTimeParsing1() {
        byte[] requested = new byte[] {
                (byte) 0xe1, 0x07, 0x0a, 0x1d, 0x00, 0x1c, 0x00,0x08
        };
        byte[] received = new byte[] {
                (byte) 0xe1, 0x07, 0x0a, 0x1c, 0x17, 0x1c, 0x00, 0x04
        };
        GregorianCalendar calRequested = BLETypeConversions.rawBytesToCalendar(requested, false);
        GregorianCalendar calReceived = BLETypeConversions.rawBytesToCalendar(received, false);

        assertTrue(calRequested.getTime().equals(calReceived.getTime()));
    }

    @Test
    public void testTimeParsingWithDST() {
        byte[] requested = new byte[] {
                (byte) 0xe1,0x07,0x0a,0x09,0x11,0x23,0x00,0x08
        };
        byte[] received = new byte[] {
                (byte) 0xe1,0x07,0x0a,0x09,0x10,0x23,0x00,0x04
        };
        GregorianCalendar calRequested = BLETypeConversions.rawBytesToCalendar(requested, false);
        GregorianCalendar calReceived = BLETypeConversions.rawBytesToCalendar(received, false);

        assertTrue(calRequested.getTime().equals(calReceived.getTime()));
    }
}
