/*  Copyright (C) 2018-2019 maxirnilian

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
package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.LenovoWatchConstants;

public final class WatchXPlusConstants extends LenovoWatchConstants {
    public static final UUID UUID_SERVICE_WATCHXPLUS = UUID.fromString("0000a800-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_UNKNOWN_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_CHARACTERISTIC_WRITE = UUID.fromString("0000a801-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_DATABASE_READ = UUID.fromString("0000a802-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_3 = UUID.fromString("0000a803-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_4 = UUID.fromString("0000a804-0000-1000-8000-00805f9b34fb");



    public static final String PREF_FIND_PHONE = "prefs_find_phone";
    public static final String PREF_FIND_PHONE_DURATION = "prefs_find_phone_duration";

    // new
    public static final String PREF_CONTINIOUS_RING = "notification_enable_continious_ring";
    public static final String PREF_REPEAT_RING = "notification_repeat_ring";
    public static final String PREF_MISSED_CALL_ENABLE = "notification_enable_missed_call";
    public static final String PREF_MISSED_CALL_REPEAT = "notification_repeat_missed_call";
    public static final String PREF_BUTTON_REJECT = "notification_button_reject";
    public static final String PREF_SHAKE_REJECT = "notification_shake_reject";
    public static final String PREF_FORCE_TIME = "pref_device_spec_settings_force_time";
    public static final String PREF_BP_CAL_LOW = "pref_sensors_bp_calibration_low";
    public static final String PREF_BP_CAL_HIGH = "pref_sensors_bp_calibration_high";

    public static final String PREF_DO_NOT_DISTURB = "do_not_disturb_no_auto";
    public static final String PREF_DO_NOT_DISTURB_START = "do_not_disturb_no_auto_start";
    public static final String PREF_DO_NOT_DISTURB_END = "do_not_disturb_no_auto_end";

    public static final String PREF_LONGSIT_START = "pref_longsit_start";
    public static final String PREF_LONGSIT_END = "pref_longsit_end";
    public static final String PREF_SHOW_RAW_GRAPH = "show_raw_graph";
    // moved to gear icon (per device settings)
    public static final String PREF_LANGUAGE = "language";

    // time format constants
    public static final byte ARG_SET_TIMEMODE_24H = 0x00;
    public static final byte ARG_SET_TIMEMODE_12H = 0x01;

    public static final int NOTIFICATION_CHANNEL_DEFAULT = 0;
    public static final int NOTIFICATION_CHANNEL_PHONE_CALL = 10;

    public static final byte[] CMD_WEATHER_SET                  = new byte[]{0x01, 0x10};
    public static final byte[] CMD_RETRIEVE_DATA_COUNT          = new byte[]{(byte)0xF0, 0x10};
    public static final byte[] CMD_RETRIEVE_DATA_DETAILS        = new byte[]{(byte)0xF0, 0x11};
    public static final byte[] CMD_RETRIEVE_DATA_CONTENT        = new byte[]{(byte)0xF0, 0x12};
    public static final byte[] CMD_REMOVE_DATA_CONTENT          = new byte[]{(byte)0xF0, 0x32};
    public static final byte[] CMD_BLOOD_PRESSURE_MEASURE       = new byte[]{0x05, 0x0D};
    public static final byte[] CMD_HEART_RATE_MEASURE           = new byte[]{0x03, 0x23};
    public static final byte[] CMD_IS_BP_CALIBRATED             = new byte[]{0x05, 0x0B};
    public static final byte[] CMD_BP_CALIBRATION               = new byte[]{0x05, 0x0C};

    public static final byte[] CMD_NOTIFICATION_TEXT_TASK       = new byte[]{0x03, 0x06};
    public static final byte[] CMD_NOTIFICATION_CANCEL          = new byte[]{0x03, 0x04};
    public static final byte[] CMD_NOTIFICATION_SETTINGS        = new byte[]{0x03, 0x02};
    public static final byte[] CMD_POWER_MODE                   = new byte[]{0x03, -0x7F};
    public static final byte[] CMD_SET_DND_HOURS_TIME         = new byte[]{0x03, 0x62};
    public static final byte[] CMD_SET_DND_HOURS_SWITCH       = new byte[]{0x03, 0x61};
    public static final byte[] CMD_SET_PERSONAL_INFO            = new byte[]{0x01, 0x0E};
    public static final byte[] CMD_INACTIVITY_REMINDER_SWITCH   = new byte[]{0x03, 0x51};
    public static final byte[] CMD_INACTIVITY_REMINDER_SET      = new byte[]{0x03, 0x52};
    public static final byte[] CMD_SET_UNITS                    = new byte[]{0x03, -0x6D};

    public static final byte[] CMD_FITNESS_GOAL_SETTINGS        = new byte[]{0x10, 0x02};
    public static final byte[] CMD_DAY_STEPS_INFO               = new byte[]{0x10, 0x03};

    public static final byte[] CMD_SHAKE_SWITCH                 = new byte[]{0x03, -0x6E};
    public static final byte[] CMD_DISCONNECT_REMIND            = new byte[]{0x00, 0x11};
    public static final byte[] CMD_TIME_LANGUAGE                = new byte[]{0x03, -0x6F};
    public static final byte[] CMD_ALTITUDE                     = new byte[]{0x05, 0x0A};

    public static final byte[] RESP_SHAKE_SWITCH                = new byte[]{0x08, 0x03, -0x6E};
    public static final byte[] RESP_DISCONNECT_REMIND           = new byte[]{0x08, 0x00, 0x11};
    public static final byte[] RESP_IS_BP_CALIBRATED            = new byte[]{0x08, 0x05, 0x0B};
    public static final byte[] RESP_BUTTON_WHILE_RING           = new byte[]{0x04, 0x03, 0x03};
    public static final byte[] RESP_BP_CALIBRATION              = new byte[]{0x08, 0x05, 0x0C};
    public static final byte[] RESP_SET_PERSONAL_INFO           = new byte[]{0x08, 0x01, 0x0E};
    public static final byte[] RESP_GOAL_AIM_STATUS             = new byte[]{0x08, 0x10, 0x02};
    public static final byte[] RESP_INACTIVITY_REMINDER_SWITCH  = new byte[]{0x08, 0x03, 0x51};
    public static final byte[] RESP_INACTIVITY_REMINDER_SET     = new byte[]{0x08, 0x03, 0x52};

    public static final byte[] RESP_AUTHORIZATION_TASK          = new byte[]{0x01, 0x01, 0x05};
    public static final byte[] RESP_DAY_STEPS_INDICATOR         = new byte[]{0x08, 0x10, 0x03};
    public static final byte[] RESP_HEARTRATE                   = new byte[]{(byte) 0x80, 0x15, 0x03};

    public static final byte[] RESP_DATA_COUNT                  = new byte[]{0x08, (byte)0xF0, 0x10};
    public static final byte[] RESP_DATA_DETAILS                = new byte[]{0x08, (byte)0xF0, 0x11};
    public static final byte[] RESP_DATA_CONTENT                = new byte[]{0x08, (byte)0xF0, 0x12};
    public static final byte[] RESP_DATA_CONTENT_REMOVE         = new byte[]{-0x80, (byte)0xF0, 0x32};
    public static final byte[] RESP_BP_MEASURE_STARTED          = new byte[]{0x08, 0x05, 0x0D};

}
