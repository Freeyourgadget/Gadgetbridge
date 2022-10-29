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
    public static final short CHUNKED2021_ENDPOINT_HTTP = 0x0001;
    public static final short CHUNKED2021_ENDPOINT_CALENDAR = 0x0007;
    public static final short CHUNKED2021_ENDPOINT_WEATHER = 0x000e;
    public static final short CHUNKED2021_ENDPOINT_ALARMS = 0x000f;
    public static final short CHUNKED2021_ENDPOINT_CANNED_MESSAGES = 0x0013;
    public static final short CHUNKED2021_ENDPOINT_CONNECTION = 0x0015;
    public static final short CHUNKED2021_ENDPOINT_USER_INFO = 0x0017;
    public static final short CHUNKED2021_ENDPOINT_STEPS = 0x0016;
    public static final short CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS = 0x0018;
    public static final short CHUNKED2021_ENDPOINT_WORKOUT = 0x0019;
    public static final short CHUNKED2021_ENDPOINT_FIND_DEVICE = 0x001a;
    public static final short CHUNKED2021_ENDPOINT_MUSIC = 0x001b;
    public static final short CHUNKED2021_ENDPOINT_HEARTRATE = 0x001d;
    public static final short CHUNKED2021_ENDPOINT_NOTIFICATIONS = 0x001e;
    public static final short CHUNKED2021_ENDPOINT_DISPLAY_ITEMS = 0x0026;
    public static final short CHUNKED2021_ENDPOINT_BATTERY = 0x0029;
    public static final short CHUNKED2021_ENDPOINT_REMINDERS = 0x0038;
    public static final short CHUNKED2021_ENDPOINT_SILENT_MODE = 0x003b;
    public static final short CHUNKED2021_ENDPOINT_AUTH = 0x0082;
    public static final short CHUNKED2021_ENDPOINT_COMPAT = 0x0090;

    /**
     * HTTP, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_HTTP}.
     */
    public static final byte HTTP_CMD_REQUEST = 0x01;
    public static final byte HTTP_CMD_RESPONSE = 0x02;
    public static final byte HTTP_RESPONSE_SUCCESS = 0x01;
    public static final byte HTTP_RESPONSE_NO_INTERNET = 0x02;

    /**
     * Alarms, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_ALARMS}.
     */
    public static final byte ALARMS_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte ALARMS_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte ALARMS_CMD_CREATE = 0x03;
    public static final byte ALARMS_CMD_CREATE_ACK = 0x04;
    public static final byte ALARMS_CMD_DELETE = 0x05;
    public static final byte ALARMS_CMD_DELETE_ACK = 0x06;
    public static final byte ALARMS_CMD_UPDATE = 0x07;
    public static final byte ALARMS_CMD_UPDATE_ACK = 0x08;
    public static final byte ALARMS_CMD_REQUEST = 0x09;
    public static final byte ALARMS_CMD_RESPONSE = 0x0a;
    public static final byte ALARMS_CMD_NOTIFY_CHANGE = 0x0f;
    public static final int ALARM_IDX_FLAGS = 0;
    public static final int ALARM_IDX_POSITION = 1;
    public static final int ALARM_IDX_HOUR = 2;
    public static final int ALARM_IDX_MINUTE = 3;
    public static final int ALARM_IDX_REPETITION = 4;
    public static final int ALARM_FLAG_SMART = 0x01;
    public static final int ALARM_FLAG_UNKNOWN_2 = 0x02;
    public static final int ALARM_FLAG_ENABLED = 0x04;

    /**
     * Display Items, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_DISPLAY_ITEMS}.
     */
    public static final byte DISPLAY_ITEMS_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte DISPLAY_ITEMS_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte DISPLAY_ITEMS_CMD_REQUEST = 0x03;
    public static final byte DISPLAY_ITEMS_CMD_RESPONSE = 0x04;
    public static final byte DISPLAY_ITEMS_CMD_CREATE = 0x05;
    public static final byte DISPLAY_ITEMS_CMD_CREATE_ACK = 0x06;
    public static final byte DISPLAY_ITEMS_MENU = 0x01;
    public static final byte DISPLAY_ITEMS_SHORTCUTS = 0x02;
    public static final byte DISPLAY_ITEMS_CONTROL_CENTER = 0x03;
    public static final byte DISPLAY_ITEMS_SECTION_MAIN = 0x01;
    public static final byte DISPLAY_ITEMS_SECTION_MORE = 0x02;
    public static final byte DISPLAY_ITEMS_SECTION_DISABLED = 0x03;

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
     * Canned Messages, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_CANNED_MESSAGES}.
     */
    public static final byte CANNED_MESSAGES_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CANNED_MESSAGES_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CANNED_MESSAGES_CMD_REQUEST = 0x03;
    public static final byte CANNED_MESSAGES_CMD_RESPONSE = 0x04;
    public static final byte CANNED_MESSAGES_CMD_SET = 0x05;
    public static final byte CANNED_MESSAGES_CMD_SET_ACK = 0x06;
    public static final byte CANNED_MESSAGES_CMD_DELETE = 0x07;
    public static final byte CANNED_MESSAGES_CMD_DELETE_ACK = 0x08;
    public static final byte CANNED_MESSAGES_CMD_REPLY_SMS = 0x0b;
    public static final byte CANNED_MESSAGES_CMD_REPLY_SMS_ACK = 0x0c;
    public static final byte CANNED_MESSAGES_CMD_REPLY_SMS_CHECK = 0x0d;
    public static final byte CANNED_MESSAGES_CMD_REPLY_SMS_ALLOW = 0x0e;

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
     * Notifications, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_NOTIFICATIONS}.
     */
    public static final byte NOTIFICATION_CMD_SEND = 0x03;
    public static final byte NOTIFICATION_CMD_REPLY = 0x04;
    public static final byte NOTIFICATION_CMD_DISMISS = 0x05;
    public static final byte NOTIFICATION_CMD_REPLY_ACK = 0x06;
    public static final byte NOTIFICATION_CMD_ICON_REQUEST = 0x10;
    public static final byte NOTIFICATION_CMD_ICON_REQUEST_ACK = 0x11;
    public static final byte NOTIFICATION_TYPE_NORMAL = (byte) 0xfa;
    public static final byte NOTIFICATION_TYPE_CALL = 0x03;
    public static final byte NOTIFICATION_TYPE_SMS = (byte) 0x05;
    public static final byte NOTIFICATION_SUBCMD_SHOW = 0x00;
    public static final byte NOTIFICATION_SUBCMD_DISMISS_FROM_PHONE = 0x02;
    public static final byte NOTIFICATION_DISMISS_NOTIFICATION = 0x03;
    public static final byte NOTIFICATION_DISMISS_MUTE_CALL = 0x02;
    public static final byte NOTIFICATION_DISMISS_REJECT_CALL = 0x01;
    public static final byte NOTIFICATION_CALL_STATE_START = 0x00;
    public static final byte NOTIFICATION_CALL_STATE_END = 0x02;

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
     * Music, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_MUSIC}.
     */
    public static final byte MUSIC_CMD_MEDIA_INFO = 0x03;
    public static final byte MUSIC_CMD_APP_STATE = 0x04;
    public static final byte MUSIC_CMD_BUTTON_PRESS = 0x05;
    public static final byte MUSIC_APP_OPEN = 0x01;
    public static final byte MUSIC_APP_CLOSE = 0x02;
    public static final byte MUSIC_BUTTON_PLAY = 0x00;
    public static final byte MUSIC_BUTTON_PAUSE = 0x01;
    public static final byte MUSIC_BUTTON_NEXT = 0x03;
    public static final byte MUSIC_BUTTON_PREVIOUS = 0x04;
    public static final byte MUSIC_BUTTON_VOLUME_UP = 0x05;
    public static final byte MUSIC_BUTTON_VOLUME_DOWN = 0x06;

    /**
     * Reminders, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_REMINDERS}.
     */
    public static final byte REMINDERS_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte REMINDERS_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte REMINDERS_CMD_REQUEST = 0x03;
    public static final byte REMINDERS_CMD_RESPONSE = 0x04;
    public static final byte REMINDERS_CMD_CREATE = 0x05;
    public static final byte REMINDERS_CMD_CREATE_ACK = 0x06;
    public static final byte REMINDERS_CMD_UPDATE = 0x07;
    public static final byte REMINDERS_CMD_UPDATE_ACK = 0x08;
    public static final byte REMINDERS_CMD_DELETE = 0x09;
    public static final byte REMINDERS_CMD_DELETE_ACK = 0x0a;
    public static final int REMINDER_FLAG_ENABLED = 0x0001;
    public static final int REMINDER_FLAG_TEXT = 0x0008;
    public static final int REMINDER_FLAG_REPEAT_MONTH = 0x1000;
    public static final int REMINDER_FLAG_REPEAT_YEAR = 0x2000;
    public static final String REMINDERS_PREF_CAPABILITY = "huami_2021_capability_reminders";

    /**
     * Calendar, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_CALENDAR}.
     */
    public static final byte CALENDAR_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CALENDAR_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CALENDAR_CMD_EVENTS_REQUEST = 0x05;
    public static final byte CALENDAR_CMD_EVENTS_RESPONSE = 0x06;
    public static final byte CALENDAR_CMD_CREATE_EVENT = 0x07;
    public static final byte CALENDAR_CMD_CREATE_EVENT_ACK = 0x08;
    public static final byte CALENDAR_CMD_DELETE_EVENT = 0x09;
    public static final byte CALENDAR_CMD_DELETE_EVENT_ACK = 0x0a;

    /**
     * Weather, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_WEATHER}.
     */
    public static final byte WEATHER_CMD_SET_DEFAULT_LOCATION = 0x09;
    public static final byte WEATHER_CMD_DEFAULT_LOCATION_ACK = 0x0a;
}
