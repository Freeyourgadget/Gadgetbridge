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
package nodomain.freeyourgadget.gadgetbridge.devices.lenovo;

public class LenovoWatchConstants {

    public static final byte RESPONSE = 0x13;
    public static final byte REQUEST = 0x31;

    public static final byte WRITE_VALUE = 0x01;
    public static final byte READ_VALUE = 0x02;
    public static final byte TASK = 0x04;
    public static final byte KEEP_ALIVE = -0x80;

    public static final byte[] CMD_HEADER = new byte[]{0x23, 0x01, 0x00, 0x00, 0x00};

    // byte[] COMMAND = new byte[]{0x23, 0x01, 0x00, 0x31, 0x00, ... , 0x00}
    //                              |     |     |     |     |     |     └ Checksum
    //                              |     |     |     |     |     └ Command + value
    //                              |     |     |     |     └ Sequence number
    //                              |     |     |     └ Response/Request indicator
    //                              |     |     └ Value length
    //                              |     |
    //                              └-----└ Header

    public static final byte[] CMD_FIRMWARE_INFO                = new byte[]{0x01, 0x02};
    public static final byte[] CMD_AUTHORIZATION_TASK           = new byte[]{0x01, 0x05};
    public static final byte[] CMD_TIME_SETTINGS                = new byte[]{0x01, 0x08};
    public static final byte[] CMD_ALARM_SETTINGS               = new byte[]{0x01, 0x0A};
    public static final byte[] CMD_BATTERY_INFO                 = new byte[]{0x01, 0x14};

    public static final byte[] CMD_NOTIFICATION_TASK            = new byte[]{0x03, 0x01};
    public static final byte[] CMD_NOTIFICATION_SETTINGS        = new byte[]{0x03, 0x02};
    public static final byte[] CMD_CALIBRATION_INIT_TASK        = new byte[]{0x03, 0x31};
    public static final byte[] CMD_CALIBRATION_TASK             = new byte[]{0x03, 0x33, 0x01};
    public static final byte[] CMD_CALIBRATION_KEEP_ALIVE       = new byte[]{0x03, 0x34};
    public static final byte[] CMD_DO_NOT_DISTURB_SETTINGS      = new byte[]{0x03, 0x61};

    public static final byte[] CMD_FITNESS_GOAL_SETTINGS        = new byte[]{0x10, 0x02};

    public static final byte[] RESP_AUTHORIZATION_TASK          = new byte[]{0x01, 0x01, 0x05};
    public static final byte[] RESP_BUTTON_INDICATOR            = new byte[]{0x04, 0x03, 0x11};
    public static final byte[] RESP_ALARM_INDICATOR             = new byte[]{-0x80, 0x01, 0x0A};

    public static final byte[] RESP_FIRMWARE_INFO               = new byte[]{0x08, 0x01, 0x02};
    public static final byte[] RESP_TIME_SETTINGS               = new byte[]{0x08, 0x01, 0x08};
    public static final byte[] RESP_BATTERY_INFO                = new byte[]{0x08, 0x01, 0x14};
    public static final byte[] RESP_NOTIFICATION_SETTINGS       = new byte[]{0x01, 0x03, 0x02};

    public static final String ACTION_ENABLE = "action.watch9.enable";

    public static final String ACTION_CALIBRATION
            = "nodomain.freeyourgadget.gadgetbridge.devices.action.lenovowatch.start_calibration";
    public static final String ACTION_CALIBRATION_SEND
            = "nodomain.freeyourgadget.gadgetbridge.devices.action.lenovowatch.send_calibration";
    public static final String ACTION_CALIBRATION_HOLD
            = "nodomain.freeyourgadget.gadgetbridge.devices.action.lenovowatch.keep_calibrating";
    public static final String VALUE_CALIBRATION_HOUR
            = "value.lenovowatch.calibration_hour";
    public static final String VALUE_CALIBRATION_MINUTE
            = "value.lenovowatch.calibration_minute";
    public static final String VALUE_CALIBRATION_SECOND
            = "value.lenovowatch.calibration_second";

}
