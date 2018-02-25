/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;

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

        // MiBand2:
        // year,year,month,dayofmonth,hour,minute,second,dayofweek,0,0,tz

        byte[] year = fromUint16(timestamp.get(Calendar.YEAR));
        return new byte[] {
                year[0],
                year[1],
                fromUint8(timestamp.get(Calendar.MONTH) + 1),
                fromUint8(timestamp.get(Calendar.DATE)),
                fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
                fromUint8(timestamp.get(Calendar.MINUTE)),
                fromUint8(timestamp.get(Calendar.SECOND)),
                dayOfWeekToRawBytes(timestamp),
                0, // fractions256 (not set)
                // 0 (DST offset?) Mi2
                // k (tz) Mi2
        };
    }

    /**
     * Similar to calendarToRawBytes, but only up to (and including) the MINUTES.
     * @param timestamp
     * @param honorDeviceTimeOffset
     * @return
     */
    public static byte[] shortCalendarToRawBytes(Calendar timestamp, boolean honorDeviceTimeOffset) {

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

        // MiBand2:
        // year,year,month,dayofmonth,hour,minute

        byte[] year = fromUint16(timestamp.get(Calendar.YEAR));
        return new byte[] {
                year[0],
                year[1],
                fromUint8(timestamp.get(Calendar.MONTH) + 1),
                fromUint8(timestamp.get(Calendar.DATE)),
                fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
                fromUint8(timestamp.get(Calendar.MINUTE))
        };
    }

    private static int getMiBand2TimeZone(int rawOffset) {
        int offsetMinutes = rawOffset / 1000 / 60;
        rawOffset = offsetMinutes < 0 ? -1 : 1;
        offsetMinutes = Math.abs(offsetMinutes);
        int offsetHours = offsetMinutes / 60;
        rawOffset *= offsetMinutes % 60 / 15 + offsetHours * 4;
        return rawOffset;
    }

    private static byte dayOfWeekToRawBytes(Calendar cal) {
        int calValue = cal.get(Calendar.DAY_OF_WEEK);
        switch (calValue) {
            case Calendar.SUNDAY:
                return 7;
            default:
                return (byte) (calValue - 1);
        }
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
                    (value[2] & 0xff) - 1,
                    value[3] & 0xff,
                    value[4] & 0xff,
                    value[5] & 0xff,
                    value[6] & 0xff
            );

            if (value.length > 7) {
                TimeZone timeZone = TimeZone.getDefault();
                timeZone.setRawOffset(value[7] * 15 * 60 * 1000);
                timestamp.setTimeZone(timeZone);
            }

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
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);
    }

    public static byte[] fromUint16(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }

    public static byte[] fromUint24(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
        };
    }

    public static byte[] fromUint32(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
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

    public static byte[] calendarToLocalTimeBytes(GregorianCalendar now) {
        byte[] result = new byte[2];
        result[0] = mapTimeZone(now.getTimeZone());
        result[1] = mapDstOffset(now);
        return result;
    }

    /**
     * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_zone.xml
     * @param timeZone
     * @return sint8 value from -48..+56
     */
    public static byte mapTimeZone(TimeZone timeZone) {
        int utcOffsetInHours =  (timeZone.getRawOffset() / (1000 * 60 * 60));
        return (byte) (utcOffsetInHours * 4);
    }

    /**
     * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.dst_offset.xml
     * @param now
     * @return the DST offset for the given time; 0 if none; 255 if unknown
     */
    public static byte mapDstOffset(Calendar now) {
        TimeZone timeZone = now.getTimeZone();
        int dstSavings = timeZone.getDSTSavings();
        if (dstSavings == 0) {
            return 0;
        }
        if (timeZone.inDaylightTime(now.getTime())) {
            int dstInMinutes = dstSavings / (1000 * 60);
            switch (dstInMinutes) {
                case 30:
                    return 2;
                case 60:
                    return 4;
                case 120:
                    return 8;
            }
            return fromUint8(255); // unknown
        }
        return 0;
    }

    public static byte[] toUtf8s(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    public static AlertCategory toAlertCategory(NotificationType type) {
        switch (type) {
            case GENERIC_ALARM_CLOCK:
                return AlertCategory.HighPriorityAlert;
            case GENERIC_SMS:
                return AlertCategory.SMS;
            case GENERIC_EMAIL:
            case GMAIL:
            case OUTLOOK:
            case YAHOO_MAIL:
                return AlertCategory.Email;
            case GENERIC_NAVIGATION:
                return AlertCategory.Simple;
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case GOOGLE_MESSENGER:
            case GOOGLE_HANGOUTS:
            case HIPCHAT:
            case KAKAO_TALK:
            case LINE:
            case RIOT:
            case SIGNAL:
            case SKYPE:
            case SNAPCHAT:
            case TELEGRAM:
            case THREEMA:
            case KONTALK:
            case ANTOX:
            case TWITTER:
            case WHATSAPP:
            case VIBER:
            case WECHAT:
                return AlertCategory.InstantMessage;
        }
        return AlertCategory.Simple;
    }
}
