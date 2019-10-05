/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, João
    Paulo Barraca

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
package nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3;

import java.util.UUID;

public final class MakibesHR3Constants {


    public static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    // time
    // mode ab:00:04:ff:7c:80:** (00: 24h, 01: 12h)

    // confirm write?
    // ab:00:09:ff:52:80:00:13:06:09:0f:0b

    // disconnect?
    // ab:00:03:ff:ff:80

    // Services and Characteristics
    // 00001801-0000-1000-8000-00805f9b34fb
    //  00002a05-0000-1000-8000-00805f9b34fb
    // 00001800-0000-1000-8000-00805f9b34fb
    //  00002a00-0000-1000-8000-00805f9b34fb
    //  00002a01-0000-1000-8000-00805f9b34fb
    //  00002a02-0000-1000-8000-00805f9b34fb
    //  00002a04-0000-1000-8000-00805f9b34fb
    //  00002aa6-0000-1000-8000-00805f9b34fb
    // 6e400001-b5a3-f393-e0a9-e50e24dcca9e // Nordic UART Service
    //  6e400002-b5a3-f393-e0a9-e50e24dcca9e //  control
    //  6e400003-b5a3-f393-e0a9-e50e24dcca9e
    // 0000fee7-0000-1000-8000-00805f9b34fb
    //  0000fec9-0000-1000-8000-00805f9b34fb
    //  0000fea1-0000-1000-8000-00805f9b34fb
    //  0000fea2-0000-1000-8000-00805f9b34fb

    // Command structure
    // ab 00 [argument_count] ff [command] 80 [arguments]
    // where [argument_count] is [arguments].length + 3

    // refresh sends
    // 51
    // 52
    // 93 (CMD_SET_DATE_TIME)

    public static final byte[] DATA_TEMPLATE = {
            (byte) 0xab,
            (byte) 0x00,
            (byte) 0, // argument_count
            (byte) 0xff,
            (byte) 0, // command
            (byte) 0x80
//           ,arguments
    };

    public static final int DATA_ARGUMENT_COUNT_INDEX = 2;
    public static final int DATA_COMMAND_INDEX = 4;
    public static final int DATA_ARGUMENTS_INDEX = 6;


    // 00
    public static final byte CMD_FACTORY_RESET = (byte) 0x23;


    // 00
    // year (+2000)
    // month
    // day
    // 0b
    // 00
    // year (+2000)
    // month
    // day
    // 0b
    // 19
    public static final byte CMD_UNKNOWN_51 = (byte) 0x51;

    // this is the last command sent on sync
    // 00
    // year (+2000)
    // month
    // 14 this isn't the current day
    // hour (current)
    // minute (current)
    public static final byte CMD_UNKNOWN_52 = (byte) 0x52;


    public static final byte CMD_FIND_DEVICE = (byte) 0x71;


    public static final byte ARG_SEND_NOTIFICATION_SOURCE_CALL = (byte) 0x01;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_STOP_CALL = (byte) 0x02;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_MESSAGE = (byte) 0x03;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_QQ = (byte) 0x07;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_WECHAT = (byte) 0x09;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_WHATSAPP = (byte) 0x0a;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_LINE = (byte) 0x0e;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_TWITTER = (byte) 0x0f;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_FACEBOOK = (byte) 0x10;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_FACEBOOK2 = (byte) 0x11;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_WEIBO = (byte) 0x13;
    public static final byte ARG_SEND_NOTIFICATION_SOURCE_KAKOTALK = (byte) 0x14;
    // ARG_SET_NOTIFICATION_SOURCE_*
    // 02
    // ASCII
    public static final byte CMD_SEND_NOTIFICATION = (byte) 0x72;


    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_WEEKDAY = (byte) 0x1F;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_CUSTOM = (byte) 0x40;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_EVERY_DAY = (byte) 0x7F;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_ONE_TIME = (byte) 0x80;
    // reminder id starting at 0
    // enable (00/01)
    // hour
    // minute
    // ARG_SET_ALARM_REMINDER_REPEAT_*
    public static final byte CMD_SET_ALARM_REMINDER = (byte) 0x73;


    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_MILES = (byte) 0x00;
    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_KILOMETERS = (byte) 0x01;
    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_LENGTH_INCHES = (byte) 0x00;
    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_LENGTH_CENTIMETERS = (byte) 0x01;
    // step length (in/cm)
    // age (years)
    // height (in/cm)
    // weight (lb/kg)
    // ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_*
    // target step count (kilo)
    // 5a
    // 82
    // 3c
    // 5a
    // 28
    // b4
    // 5d
    // 64
    public static final byte CMD_SET_PERSONAL_INFORMATION = (byte) 0x74;


    // enable (00/01)
    // start hour
    // start minute
    // end hour
    // end minute
    // 2d
    public static final byte CMD_SET_SEDENTARY_REMINDER = (byte) 0x75;


    // enable (00/01)
    // start hour
    // start minute
    // end hour
    // end minute
    public static final byte CMD_SET_QUITE_HOURS = (byte) 0x76;


    // enable (00/01)
    public static final byte CMD_SET_HEADS_UP_SCREEN = (byte) 0x77;


    // The watch enters photograph mode, but doesn't appear to send a trigger signal.
    // enable (00/01)
    public static final byte CMD_SET_PHOTOGRAPH_MODE = (byte) 0x79;


    // enable (00/01)
    public static final byte CMD_SET_LOST_REMINDER = (byte) 0x7a;


    // 7b has 1 argument. Looks like enable/disable.

    // 7e has 14 arguments.

    public static final byte ARG_SET_TIMEMODE_24H = 0x00;
    public static final byte ARG_SET_TIMEMODE_12H = 0x01;
    // ARG_SET_TIMEMODE_*
    public static final byte CMD_SET_TIMEMODE = (byte) 0x7c;


    // 00
    // year hi
    // year lo
    // month
    // day
    // hour
    // minute
    // second
    public static final byte CMD_SET_DATE_TIME = (byte) 0x93;


    // If this is sent after {@link CMD_FACTORY_RESET}, it's a shutdown, not a reboot.
    // Rebooting resets the watch face and wallpaper.
    public static final byte CMD_REBOOT = (byte) 0xff;
}
