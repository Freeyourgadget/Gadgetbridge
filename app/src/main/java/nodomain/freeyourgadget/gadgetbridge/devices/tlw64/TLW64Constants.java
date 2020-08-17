/*  Copyright (C) 2020 Erik Bloß

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

package nodomain.freeyourgadget.gadgetbridge.devices.tlw64;

import java.util.UUID;

public final class TLW64Constants {

    public static final UUID UUID_SERVICE_NO1            = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_NOTIFY  = UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb");

    // Command bytes
    public static final byte CMD_DISPLAY_SETTINGS = (byte) 0xa0;
    public static final byte CMD_FIRMWARE_VERSION = (byte) 0xa1;
    public static final byte CMD_BATTERY          = (byte) 0xa2;
    public static final byte CMD_DATETIME         = (byte) 0xa3;
    public static final byte CMD_USER_DATA        = (byte) 0xa9;
    public static final byte CMD_ALARM            = (byte) 0xab;
    public static final byte CMD_FACTORY_RESET    = (byte) 0xad;
    public static final byte CMD_FETCH_STEPS      = (byte) 0xb2;
    public static final byte CMD_FETCH_SLEEP      = (byte) 0xb3;
    public static final byte CMD_NOTIFICATION     = (byte) 0xc1;
    public static final byte CMD_ICON             = (byte) 0xc3;
    public static final byte CMD_DEVICE_SETTINGS  = (byte) 0xd3;

    // Notifications
    public static final byte NOTIFICATION_HEADER = (byte) 0x01;
    public static final byte NOTIFICATION_CALL   = (byte) 0x02;    // displays "call" on screen
    public static final byte NOTIFICATION_SMS    = (byte) 0x03;    // displays "mms" on screen
    public static final byte NOTIFICATION_STOP   = (byte) 0x04;    // to stop showing incoming call

    // Icons
    public static final byte ICON_QQ     = (byte) 0x01;
    public static final byte ICON_WECHAT = (byte) 0x02;
    public static final byte ICON_MAIL   = (byte) 0x04;

    // Alarm arguments
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_SUNDAY    =  (byte) 0x01;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_MONDAY    =  (byte) 0x02;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_TUESDAY   =  (byte) 0x04;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_WEDNESDAY =  (byte) 0x08;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_THURSDAY  =  (byte) 0x10;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_FRIDAY    =  (byte) 0x20;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_SATURDAY  =  (byte) 0x40;

}
