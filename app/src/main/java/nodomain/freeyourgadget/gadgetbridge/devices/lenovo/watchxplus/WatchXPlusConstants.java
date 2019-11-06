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

    public static final String PREF_ACTIVATE_DISPLAY = "activate_display_on_lift_wrist";
    public static final String PREF_DISCONNECT_REMIND = "disconnect_notification";
    public static final String PREF_FIND_PHONE = "prefs_find_phone";
    public static final String PREF_FIND_PHONE_DURATION = "prefs_find_phone_duration";
    public static final String PREF_ALTITUDE = "watchxplus_altitude";
    public static final String PREF_REPEAT = "watchxplus_repeat";
    public static final String PREF_CONTINIOUS = "watchxplus_continious";
    public static final String PREF_MISSED_CALL = "watchxplus_missed";
    public static final String PREF_IS_BP_CALIBRATED = "watchxplus_is_bp_calibrated";
    public static final String PREF_BUTTON_REJECT = "watchxplus_button_reject";
    public static final String PREF_SHAKE_REJECT = "watchxplus_shake_reject";


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
    public static final byte[] CMD_DO_NOT_DISTURB_SETTINGS      = new byte[]{0x03, 0x61};

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

    public static final byte[] RESP_AUTHORIZATION_TASK          = new byte[]{0x01, 0x01, 0x05};
    public static final byte[] RESP_DAY_STEPS_INDICATOR         = new byte[]{0x08, 0x10, 0x03};
    public static final byte[] RESP_HEARTRATE                   = new byte[]{-0x80, 0x15, 0x03};

    public static final byte[] RESP_DATA_COUNT                  = new byte[]{0x08, (byte)0xF0, 0x10};
    public static final byte[] RESP_DATA_DETAILS                = new byte[]{0x08, (byte)0xF0, 0x11};
    public static final byte[] RESP_DATA_CONTENT                = new byte[]{0x08, (byte)0xF0, 0x12};
    public static final byte[] RESP_DATA_CONTENT_REMOVE         = new byte[]{-0x80, (byte)0xF0, 0x32};
    public static final byte[] RESP_BP_MEASURE_STARTED          = new byte[]{0x08, 0x05, 0x0D};

}
