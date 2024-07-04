package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class TimeTest {

    @Test
    public void testGetRawDataWithValues() throws Exception {
        // arrange
        Time time = new Time();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'Z");
        Date date = sdf.parse("2000-04-01 12:00:00 GMT+0200");
        time.setNow(Instant.ofEpochMilli(date.getTime()));
        time.setTimeOffsetInSeconds(7200);
        date = sdf.parse("2000-10-26 02:00:00 GMT+0200");
        time.setNextDaylightSavingTransition(Instant.ofEpochMilli(date.getTime()));
        time.setNextDaylightSavingTransitionOffsetInSeconds(2400);

        // act
        byte[] rawData = time.getRawData();

        // assert
        assertEquals("0501001038e5c8a000001c2039f7740000000960", StringUtils.bytesToHex(rawData).toLowerCase());
    }
}