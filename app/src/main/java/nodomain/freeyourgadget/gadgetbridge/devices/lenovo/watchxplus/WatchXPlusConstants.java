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
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_2 = UUID.fromString("0000a802-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_3 = UUID.fromString("0000a803-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_4 = UUID.fromString("0000a804-0000-1000-8000-00805f9b34fb");

    public static final int NOTIFICATION_CHANNEL_DEFAULT = 0;
    public static final int NOTIFICATION_CHANNEL_PHONE_CALL = 10;

    public static final byte[] CMD_WEATHER_SET                  = new byte[]{0x01, 0x10};
    public static final byte[] CMD_RETRIEVE_DATA                = new byte[]{(byte)0xF0, 0x10};
    public static final byte[] CMD_RETRIEVE_DATA_DETAILS        = new byte[]{(byte)0xF0, 0x11};
    public static final byte[] HEART_RATE_DATA_TYPE             = new byte[]{0x00, 0x02};

    public static final byte[] CMD_NOTIFICATION_TEXT_TASK       = new byte[]{0x03, 0x06};
    public static final byte[] CMD_NOTIFICATION_SETTINGS        = new byte[]{0x03, 0x02};
    public static final byte[] CMD_DO_NOT_DISTURB_SETTINGS      = new byte[]{0x03, 0x61};

    public static final byte[] CMD_FITNESS_GOAL_SETTINGS        = new byte[]{0x10, 0x02};
    public static final byte[] CMD_DAY_STEPS_INFO               = new byte[]{0x10, 0x03};

    public static final byte[] RESP_AUTHORIZATION_TASK          = new byte[]{0x01, 0x01, 0x05};
    public static final byte[] RESP_DAY_STEPS_INDICATOR         = new byte[]{0x08, 0x10, 0x03};
    public static final byte[] RESP_HEARTRATE                   = new byte[]{-0x80, 0x15, 0x03};

    public static final byte[] RESP_HEART_RATE_DATA             = new byte[]{0x08, (byte)0xF0, 0x10};
    public static final byte[] RESP_HEART_RATE_DATA_DETAILS     = new byte[]{0x08, (byte)0xF0, 0x11};

}
