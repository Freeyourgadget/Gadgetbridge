package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class MiBandDateConverter {
    /**
     * uses the standard algorithm to convert bytes received from the MiBand to a Calendar object
     * @param value
     * @return
     */
    public static GregorianCalendar rawBytesToCalendar(byte[] value) {
        GregorianCalendar timestamp = new GregorianCalendar();

        if (value.length == 6) {
            timestamp.set(Calendar.YEAR, (2000 + value[0]));
            timestamp.set(Calendar.MONTH, value[1]);
            timestamp.set(Calendar.DATE, value[2]);
            timestamp.set(Calendar.HOUR_OF_DAY, value[3]);
            timestamp.set(Calendar.MINUTE, value[4]);
            timestamp.set(Calendar.SECOND, value[5]);
        }

        return timestamp;
    }

    /**
     * uses the standard algorithm to convert a Calendar object to a byte array to send to MiBand
     * @param timestamp
     * @return
     */
    public static byte[] calendarToRawBytes(Calendar timestamp) {
        return new byte[]{
                (byte) (timestamp.get(Calendar.YEAR) - 2000),
                (byte) timestamp.get(Calendar.MONTH),
                (byte) timestamp.get(Calendar.DATE),
                (byte) timestamp.get(Calendar.HOUR_OF_DAY),
                (byte) timestamp.get(Calendar.MINUTE),
                (byte) timestamp.get(Calendar.SECOND)
        };
    }
}
