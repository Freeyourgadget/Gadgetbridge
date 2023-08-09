/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

public class Huami2021Service {
    /**
     * Endpoints for 2021 chunked protocol
     */
    public static final short CHUNKED2021_ENDPOINT_WEATHER = 0x000e;
    public static final short CHUNKED2021_ENDPOINT_CONNECTION = 0x0015;
    public static final short CHUNKED2021_ENDPOINT_USER_INFO = 0x0017;
    public static final short CHUNKED2021_ENDPOINT_STEPS = 0x0016;
    public static final short CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS = 0x0018;
    public static final short CHUNKED2021_ENDPOINT_WORKOUT = 0x0019;
    public static final short CHUNKED2021_ENDPOINT_FIND_DEVICE = 0x001a;
    public static final short CHUNKED2021_ENDPOINT_HEARTRATE = 0x001d;
    public static final short CHUNKED2021_ENDPOINT_BATTERY = 0x0029;
    public static final short CHUNKED2021_ENDPOINT_SILENT_MODE = 0x003b;
    public static final short CHUNKED2021_ENDPOINT_AUTH = 0x0082;
    public static final short CHUNKED2021_ENDPOINT_COMPAT = 0x0090;

    /**
     * Find Device, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_FIND_DEVICE}.
     */
    public static final byte FIND_BAND_START = 0x03;
    public static final byte FIND_BAND_ACK = 0x04;
    public static final byte FIND_BAND_STOP_FROM_PHONE = 0x06;
    public static final byte FIND_BAND_STOP_FROM_BAND = 0x07;
    public static final byte FIND_PHONE_START = 0x11;
    public static final byte FIND_PHONE_ACK = 0x12;
    public static final byte FIND_PHONE_STOP_FROM_BAND = 0x13;
    public static final byte FIND_PHONE_STOP_FROM_PHONE = 0x14;
    public static final byte FIND_PHONE_MODE = 0x15;

    /**
     * Steps, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_STEPS}.
     */
    public static final byte STEPS_CMD_GET = 0x03;
    public static final byte STEPS_CMD_REPLY = 0x04;
    public static final byte STEPS_CMD_ENABLE_REALTIME = 0x05;
    public static final byte STEPS_CMD_ENABLE_REALTIME_ACK = 0x06;
    public static final byte STEPS_CMD_REALTIME_NOTIFICATION = 0x07;

    /**
     * Vibration Patterns, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS}.
     */
    public static final byte VIBRATION_PATTERN_SET = 0x03;
    public static final byte VIBRATION_PATTERN_ACK = 0x04;

    /**
     * Battery, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_BATTERY}.
     */
    public static final byte BATTERY_REQUEST = 0x03;
    public static final byte BATTERY_REPLY = 0x04;

    /**
     * Silent Mode, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_SILENT_MODE}.
     */
    public static final byte SILENT_MODE_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte SILENT_MODE_CMD_CAPABILITIES_RESPONSE = 0x02;
    // Notify silent mode, from phone
    public static final byte SILENT_MODE_CMD_NOTIFY_BAND = 0x03;
    public static final byte SILENT_MODE_CMD_NOTIFY_BAND_ACK = 0x04;
    // Query silent mode on phone, from band
    public static final byte SILENT_MODE_CMD_QUERY = 0x05;
    public static final byte SILENT_MODE_CMD_REPLY = 0x06;
    // Set silent mode on phone, from band
    // After this, phone sends ACK + NOTIFY
    public static final byte SILENT_MODE_CMD_SET = 0x07;
    public static final byte SILENT_MODE_CMD_ACK = 0x08;

    /**
     * Connection, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_CONNECTION}.
     */
    public static final byte CONNECTION_CMD_MTU_REQUEST = 0x01;
    public static final byte CONNECTION_CMD_MTU_RESPONSE = 0x02;
    public static final byte CONNECTION_CMD_UNKNOWN_3 = 0x03;
    public static final byte CONNECTION_CMD_UNKNOWN_4 = 0x04;

    /**
     * Notifications, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_HEARTRATE}.
     */
    public static final byte HEART_RATE_CMD_REALTIME_SET = 0x04;
    public static final byte HEART_RATE_CMD_REALTIME_ACK = 0x05;
    public static final byte HEART_RATE_CMD_SLEEP = 0x06;
    public static final byte HEART_RATE_FALL_ASLEEP = 0x01;
    public static final byte HEART_RATE_WAKE_UP = 0x00;
    public static final byte HEART_RATE_REALTIME_MODE_STOP = 0x00;
    public static final byte HEART_RATE_REALTIME_MODE_START = 0x01;
    public static final byte HEART_RATE_REALTIME_MODE_CONTINUE = 0x02;

    /**
     * Workout, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_WORKOUT}.
     */
    public static final byte WORKOUT_CMD_GPS_LOCATION = 0x04;
    public static final byte WORKOUT_CMD_APP_OPEN = 0x20;
    public static final byte WORKOUT_CMD_STATUS = 0x11;
    public static final int WORKOUT_GPS_FLAG_STATUS = 0x1;
    public static final int WORKOUT_GPS_FLAG_POSITION = 0x40000;
    public static final byte WORKOUT_STATUS_START = 0x01;
    public static final byte WORKOUT_STATUS_END = 0x04;

    /**
     * Weather, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_WEATHER}.
     */
    public static final byte WEATHER_CMD_SET_DEFAULT_LOCATION = 0x09;
    public static final byte WEATHER_CMD_DEFAULT_LOCATION_ACK = 0x0a;

    /**
     * User Info, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_USER_INFO}.
     */
    public static final byte USER_INFO_CMD_SET = 0x01;
    public static final byte USER_INFO_CMD_SET_ACK = 0x02;

    /**
     * Raw sensor control.
     */
    public static final byte[] CMD_RAW_SENSOR_START_1 = new byte[]{0x01, 0x03, 0x19}; // band replies 10:01:03:05
    public static final byte[] CMD_RAW_SENSOR_START_2 = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x19}; // band replies 10:01:01:05
    public static final byte[] CMD_RAW_SENSOR_START_3 = new byte[]{0x02}; // band replies 10:02:01
    public static final byte[] CMD_RAW_SENSOR_STOP = new byte[]{0x03}; // band replies 10:03:01
}
