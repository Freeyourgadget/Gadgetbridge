package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;

/**
 * Provides methods to convert standard BLE units to byte sequences and vice versa.
 */
public class BLETypeConversions {
    /**
     * Converts a timestamp to the byte sequence to be sent to the current time characteristic
     *
     * @param timestamp
     * @return
     * @see GattCharacteristic#UUID_CHARACTERISTIC_CURRENT_TIME
     */
    public static byte[] calendarToRawBytes(Calendar timestamp, boolean honorDeviceTimeOffset) {

        // The mi-band device currently records sleep
        // only if it happens after 10pm and before 7am.
        // The offset is used to trick the device to record sleep
        // in non-standard hours.
        // If you usually sleep, say, from 6am to 2pm, set the
        // shift to -8, so at 6am the device thinks it's still 10pm
        // of the day before.
        if (honorDeviceTimeOffset) {
            int offsetInHours = MiBandCoordinator.getDeviceTimeOffsetHours();
            if (offsetInHours != 0) {
                timestamp.add(Calendar.HOUR_OF_DAY, offsetInHours);
            }
        }

        byte[] year = fromUint16(timestamp.get(Calendar.YEAR));
        return new byte[] {
                year[0],
                year[1],
                fromUint8(timestamp.get(Calendar.MONTH) + 1),
                fromUint8(timestamp.get(Calendar.DATE)),
                fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
                fromUint8(timestamp.get(Calendar.MINUTE)),
                fromUint8(timestamp.get(Calendar.SECOND))
        };
    }

    /**
     * uses the standard algorithm to convert bytes received from the MiBand to a Calendar object
     *
     * @param value
     * @return
     */
    public static GregorianCalendar rawBytesToCalendar(byte[] value, boolean honorDeviceTimeOffset) {
        if (value.length >= 7) {
            int year = toUint16(value[0], value[1]);
            GregorianCalendar timestamp = new GregorianCalendar(
                    year,
                    value[2],
                    value[3],
                    value[4],
                    value[5],
                    value[6]
            );

            if (honorDeviceTimeOffset) {
                int offsetInHours = MiBandCoordinator.getDeviceTimeOffsetHours();
                if (offsetInHours != 0) {
                    timestamp.add(Calendar.HOUR_OF_DAY,-offsetInHours);
                }
            }

            return timestamp;
        }

        return createCalendar();
    }

    public static int toUint16(byte... bytes) {
        return bytes[0] | (bytes[1] << 8);
    }

    public static byte[] fromUint16(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }
    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }

    /**
     * Creates a calendar object representing the current date and time.
     */
    public static GregorianCalendar createCalendar() {
        return new GregorianCalendar();
    }

    public static byte[] join(byte[] start, byte[] end) {
        if (start == null || start.length == 0) {
            return end;
        }
        if (end == null || end.length == 0) {
            return start;
        }

        byte[] result = new byte[start.length + end.length];
        System.arraycopy(start, 0, result, 0, start.length);
        System.arraycopy(end, 0, result, start.length, end.length);
        return result;
    }
}
