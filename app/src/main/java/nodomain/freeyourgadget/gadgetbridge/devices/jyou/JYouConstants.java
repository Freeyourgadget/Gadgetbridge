/*  Copyright (C) 2017-2018 Sami Alaoui

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
package nodomain.freeyourgadget.gadgetbridge.devices.jyou;

import java.util.UUID;

public final class JYouConstants {
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f3-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("000033f4-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_JYOU = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb");

    public static final byte CMD_SET_DATE_AND_TIME = 0x08;
    public static final byte CMD_SET_HEARTRATE_AUTO = 0x38;
    public static final byte CMD_SET_HEARTRATE_WARNING_VALUE = 0x01;
    public static final byte CMD_SET_TARGET_STEPS = 0x03;
    public static final byte CMD_SET_ALARM_1 = 0x09;
    public static final byte CMD_SET_ALARM_2 = 0x22;
    public static final byte CMD_SET_ALARM_3 = 0x23;
    public static final byte CMD_GET_STEP_COUNT = 0x1D;
    public static final byte CMD_GET_SLEEP_TIME = 0x32;
    public static final byte CMD_SET_NOON_TIME = 0x26;
    public static final byte CMD_SET_SLEEP_TIME = 0x27;
    public static final byte CMD_SET_DND_SETTINGS = 0x39;
    public static final byte CMD_SET_INACTIVITY_WARNING_TIME = 0x24;
    public static final byte CMD_ACTION_HEARTRATE_SWITCH = 0x0D;
    public static final byte CMD_ACTION_SHOW_NOTIFICATION = 0x2C;
    public static final byte CMD_ACTION_REBOOT_DEVICE = 0x0E;

    public static final byte RECEIVE_BATTERY_LEVEL = (byte)0xF7;
    public static final byte RECEIVE_DEVICE_INFO = (byte)0xF6;
    public static final byte RECEIVE_STEPS_DATA = (byte)0xF9;
    public static final byte RECEIVE_HEARTRATE = (byte)0xFC;

    public static final byte ICON_CALL = 0;
    public static final byte ICON_SMS = 1;
    public static final byte ICON_WECHAT = 2;
    public static final byte ICON_QQ = 3;
    public static final byte ICON_FACEBOOK = 4;
    public static final byte ICON_SKYPE = 5;
    public static final byte ICON_TWITTER = 6;
    public static final byte ICON_WHATSAPP = 7;
    public static final byte ICON_LINE = 8;
}
