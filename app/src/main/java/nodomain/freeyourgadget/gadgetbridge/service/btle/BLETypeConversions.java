/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Johannes Krude, José Rebelo, Lukas Veneziano, Maxim Baz, Petr
    Kadlec, Robbert Gurdeep Singh, uli

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;

/**
 * Provides methods to convert standard BLE units to byte sequences and vice versa.
 */
public class BLETypeConversions {
    public static final int TZ_FLAG_NONE = 0;
    public static final int TZ_FLAG_INCLUDE_DST_IN_TZ = 1;

    /**
     * Converts a timestamp to the byte sequence to be sent to the current time characteristic
     *
     * @param timestamp
     * @param reason
     * @return
     * @see GattCharacteristic#UUID_CHARACTERISTIC_CURRENT_TIME
     */
    public static byte[] calendarToCurrentTime(Calendar timestamp, int reason) {
        // year,year,month,dayofmonth,hour,minute,second,dayofweek,fractions256,reason

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
                fromUint8((int) (timestamp.get(Calendar.MILLISECOND) / 1000. * 256)),
                (byte) reason, // use 0 if unknown reason
        };
    }

    /**
     * Converts a timestamp to the byte sequence to be sent to the current time characteristic
     *
     * @param timestamp
     * @param reason
     * @return
     * @see GattCharacteristic#UUID_CHARACTERISTIC_CURRENT_TIME
     */
    public static byte[] toCurrentTime(ZonedDateTime timestamp, int reason) {
        // year,year,month,dayofmonth,hour,minute,second,dayofweek,fractions256,reason

        byte[] year = fromUint16(timestamp.getYear());
        return new byte[] {
                year[0],
                year[1],
                fromUint8(timestamp.getMonthValue()),
                fromUint8(timestamp.getDayOfMonth()),
                fromUint8(timestamp.getHour()),
                fromUint8(timestamp.getMinute()),
                fromUint8(timestamp.getSecond()),
                fromUint8(timestamp.getDayOfWeek().getValue()),
                fromUint8((int) (timestamp.getNano() / 1000000000. * 256)),
                (byte) reason, // use 0 if unknown reason
        };
    }

    /**
     * Converts a timestamp to the byte sequence to be sent to the local time characteristic
     *
     * Values are expressed in quarters of hours
     * Following the BLE specification, timezone is constant over dst changes
     *
     * @param timestamp
     * @return
     * @see GattCharacteristic#UUID_CHARACTERISTIC_LOCAL_TIME
     */
    public static byte[] calendarToLocalTime(Calendar timestamp) {
        TimeZone timeZone = timestamp.getTimeZone();
        int offsetMillisTimezone = timeZone.getRawOffset();
        int utcOffsetInQuarterHours = (offsetMillisTimezone / (1000 * 60 * 15));

        int offsetMillisIncludingDST = timestamp.getTimeZone().getOffset(timestamp.getTimeInMillis());
        int dstOffsetMillis = offsetMillisIncludingDST - offsetMillisTimezone;
        int dstOffsetInQuarterHours = (dstOffsetMillis / (1000 * 60 * 15));

        return new byte[] {
                (byte) utcOffsetInQuarterHours,
                (byte) dstOffsetInQuarterHours
        };
    }

    /**
     * Similar to calendarToRawBytes, but only up to (and including) the MINUTES.
     * @param timestamp
     * @return byte array of 6 bytes
     */
    public static byte[] shortCalendarToRawBytes(Calendar timestamp) {
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

    public static byte dayOfWeekToRawBytes(Calendar cal) {
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
    public static GregorianCalendar rawBytesToCalendar(byte[] value) {
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
                /* when we get a timezone offset via BLE, we cannot know which timeszone this is (only its offset), so
                   set to UTC which does not use DST to prevent bugs when setting the raw offset below */
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                timeZone.setRawOffset(value[7] * 15 * 60 * 1000);
                timestamp.setTimeZone(timeZone);
            }
            return timestamp;
        }

        return createCalendar();
    }

    public static long toUnsigned(int unsignedInt) {
        return ((long) unsignedInt) & 0xffffffffL;
    }
    public static int toUnsigned(short value) {
        return value & 0xffff;
    }

    public static int toUnsigned(byte value) {
        return value & 0xff;
    }

    public static int toUnsigned(byte[] bytes, int offset) {
        return bytes[offset + 0] & 0xff;
    }

    public static int toUint16(byte value) {
        return toUnsigned(value);
    }

    public static int toUint16(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);
    }

    public static int toUint16(byte[] bytes, int offset) {
        return (bytes[offset + 0] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    public static int toInt16(byte... bytes) {
        return (short) (bytes[0] & 0xff | ((bytes[1] & 0xff) << 8));
    }

    public static int toUint24(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8) | ((bytes[2] & 0xff) << 16);
    }

    public static int toUint24(byte[] bytes, int offset) {
        return (bytes[offset + 0] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16);
    }

    public static int toUint32(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8) | ((bytes[2] & 0xff) << 16) | ((bytes[3] & 0xff) << 24);
    }

    public static int toUint32(byte[] bytes, int offset) {
        return (bytes[offset + 0] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16) | ((bytes[offset + 3] & 0xff) << 24);
    }

    public static long toUint64(byte... bytes) {
        return (bytes[0] & 0xFFL) | ((bytes[1] & 0xFFL) << 8) | ((bytes[2] & 0xFFL) << 16) | ((bytes[3] & 0xFFL) << 24) |
                ((bytes[4] & 0xFFL) << 32) | ((bytes[5] & 0xFFL) << 40) | ((bytes[6] & 0xFFL) << 48) | ((bytes[7] & 0xFFL) << 56);
    }

    public static long toUint64(byte[] bytes, int offset) {
        return (bytes[offset + 0] & 0xFFL) | ((bytes[offset + 1] & 0xFFL) << 8) | ((bytes[offset + 2] & 0xFFL) << 16) | ((bytes[offset + 3] & 0xFFL) << 24) |
                ((bytes[offset + 4] & 0xFFL) << 32) | ((bytes[offset + 5] & 0xFFL) << 40) | ((bytes[offset + 6] & 0xFFL) << 48) | ((bytes[offset + 7] & 0xFFL) << 56);
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

    public static byte[] fromUint64(long value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
                (byte) ((value >> 32) & 0xff),
                (byte) ((value >> 40) & 0xff),
                (byte) ((value >> 48) & 0xff),
                (byte) ((value >> 56) & 0xff),
        };
    }

    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }

    public static void writeUint8(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
    }

    public static void writeUint16(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
    }

    public static void writeUint32(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
    }

    public static void writeUint64(byte[] array, int offset, long value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
        array[offset + 4] = (byte) (value >> 32);
        array[offset + 5] = (byte) (value >> 40);
        array[offset + 6] = (byte) (value >> 48);
        array[offset + 7] = (byte) (value >> 56);
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
        int offsetMillis = timeZone.getRawOffset();
        int utcOffsetInQuarterHours = (offsetMillis / (1000 * 60 * 15));
        return (byte) utcOffsetInQuarterHours;
    }

    /**
     * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.time_zone.xml
     *
     * @param calendar
     * @return sint8 value from -48..+56
     */
    public static byte mapTimeZone(Calendar calendar, int timezoneFlags) {
        int offsetMillis = calendar.getTimeZone().getRawOffset();
        if (timezoneFlags == TZ_FLAG_INCLUDE_DST_IN_TZ) {
            offsetMillis = calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
        }
        int utcOffsetInQuarterHours = (offsetMillis / (1000 * 60 * 15));
        return (byte) utcOffsetInQuarterHours;
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
            case WIRE:
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
