/*  Copyright (C) 2019-2020 Andreas Shimokawa, Cre3per

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

    // TODO: This doesn't belong here, but I don't want to touch other files to avoid
    // TODO: breaking someone else's device support.
    public static final String PREF_HEADS_UP_SCREEN = "activate_display_on_lift_wrist";
    public static final String PREF_LOST_REMINDER = "disconnect_notification";
    public static final String PREF_DO_NOT_DISTURB = "do_not_disturb_no_auto";
    public static final String PREF_DO_NOT_DISTURB_START = "do_not_disturb_no_auto_start";
    public static final String PREF_DO_NOT_DISTURB_END = "do_not_disturb_no_auto_end";
    public static final String PREF_FIND_PHONE = "prefs_find_phone";
    public static final String PREF_FIND_PHONE_DURATION = "prefs_find_phone_duration";

    public static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_REPORT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

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
    //  6e400002-b5a3-f393-e0a9-e50e24dcca9e // control (RX)
    //  6e400003-b5a3-f393-e0a9-e50e24dcca9e // report
    // 0000fee7-0000-1000-8000-00805f9b34fb
    //  0000fec9-0000-1000-8000-00805f9b34fb
    //  0000fea1-0000-1000-8000-00805f9b34fb
    //  0000fea2-0000-1000-8000-00805f9b34fb

    // Command structure
    // ab 00 [argument_count] ff [command] 80 [arguments]
    // where [argument_count] is [arguments].length + 3
    // 80 might by different.


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

    // blood oxygen percentage
    public static final byte[] RPRT_BLOOD_OXYGEN = new byte[]{ (byte) 0x31, (byte) 0x12 };


    // blood oxygen percentage
    // blood oxygen percentage
    public static final byte[] RPRT_SINGLE_BLOOD_OXYGEN = new byte[]{ (byte) 0x31, (byte) 0x11 };


    // steps might take up more bytes. I don't know which ones and I won't walk that much.
    // Only sent after we send CMD_51
    // 00 (maybe also used for steps)
    // [steps hi]
    // [steps lo]
    // 00
    // 00
    // 01 (also was 0b. Maybe minutes of activity.)
    // 00
    // 00
    // 00
    // 00
    // 00
    public static final byte[] RPRT_FITNESS = new byte[]{ (byte) 0x51, 0x08 };


    // year (+2000)
    // month
    // day
    // hour
    // minute
    // heart rate
    // heart rate
    public static final byte[] RPRT_HEART_RATE_SAMPLE = new byte[]{ (byte) 0x51, (byte) 0x11 };


    // WearFit says "walking" in the step details. This is probably also in here, but
    // I don't run :O
    // year (+2000)
    // month
    // day
    // hour (start of measurement. interval is 1h. Might be longer when running.)
    // 00 (either used for steps or minute)
    // accumulated steps (hi)
    // accumulated steps (lo)
    // 00
    // 00
    // ?? (changes whenever steps change. Ranges from 00 to 16.)
    // 00
    // 00
    // 00
    // 00
    public static final byte[] RPRT_STEPS_SAMPLE = new byte[]{ (byte) 0x51, (byte) 0x20 };


    // enable (00/01)
    public static final byte RPRT_REVERSE_FIND_DEVICE = (byte) 0x7d;


    // The proximity sensor sees air..
    public static final byte ARG_HEARTRATE_NO_TARGET = (byte) 0xff;
    // The hr sensor didn't find the heart rate yet.
    public static final byte ARG_HEARTRATE_NO_READING = (byte) 0x00;

    // heart rate
    public static final byte RPRT_HEARTRATE = (byte) 0x84;


    // charging (00/01)
    // battery percentage (step size is 20).
    public static final byte RPRT_BATTERY = (byte) 0x91;

    // firmware_major
    // firmware_minor
    // 37
    // 00
    // 00
    // 00
    // 00
    // 00
    // 00
    // 20
    // 0e
    public static final byte RPRT_SOFTWARE = (byte) 0x92;

    // 00
    public static final byte CMD_FACTORY_RESET = (byte) 0x23;


    // enable (00/01)
    public static final byte[] CMD_SET_REAL_TIME_BLOOD_OXYGEN = new byte[]{ (byte) 0x31, (byte) 0x12 };


    // After disabling, the watch replies with RPRT_SINGLE_BLOOD_OXYGEN
    // enable (00/01)
    public static final byte[] CMD_SET_SINGLE_BLOOD_OXYGEN = new byte[]{ (byte) 0x31, (byte) 0x11 };

    // device replies with
    // {@link MakibesHR3Constants#RPRT_HEART_RATE_SAMPLE}
    // {@link MakibesHR3Constants#RPRT_STEPS_SAMPLE} (Only if steps are non-zero)
    // {@link MakibesHR3Constants#RPRT_FITNESS}
    // there are also multiple 6 * 00 reports
    // 00
    // year (+2000) steps after
    // month steps after
    // day steps after
    // hour steps after
    // minute steps after
    // year (+2000) heart rate after
    // month heart rate after
    // day heart rate after
    // hour heart rate after
    // minute heart rate after
    public static final byte CMD_REQUEST_FITNESS = (byte) 0x51;


    // Manually sending this doesn't yield a reply. The heart rate history is sent in response to
    // CMD_CMD_REQUEST_FITNESS.
    // 00
    // year (+2000) (probably not current)
    // month (not current!)
    // day (not current!)
    // hour (current)
    // minute (current)
    public static final byte CMD_52 = (byte) 0x52;


    // vibrates 6 times
    public static final byte CMD_FIND_DEVICE = (byte) 0x71;


    // WearFit writes uses other sources as well. They don't do anything though.
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
    // 02 (This is 00 and 01 during connection. Doesn't seem to do anything.)
    // ASCII
    public static final byte CMD_SEND_NOTIFICATION = (byte) 0x72;


    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_WEEKDAY = (byte) 0x1F;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_EVERY_DAY = (byte) 0x7F;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_ONE_TIME = (byte) 0x80;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_MONDAY = (byte) 0x01;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_TUESDAY = (byte) 0x02;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_WEDNESDAY = (byte) 0x04;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_THURSDAY = (byte) 0x08;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_FRIDAY = (byte) 0x10;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_SATURDAY = (byte) 0x20;
    public static final byte ARG_SET_ALARM_REMINDER_REPEAT_SUNDAY = (byte) 0x40;

    // reminder id starting at 0
    // enable (00/01)
    // hour
    // minute
    // bit field of ARG_SET_ALARM_REMINDER_REPEAT_*
    public static final byte CMD_SET_ALARM_REMINDER = (byte) 0x73;


    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_MILES = (byte) 0x00;
    public static final byte ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_KILOMETERS = (byte) 0x01;
    // step length (in/cm)
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


    // Looks like enable/disable.
    public static final byte CMD_78 = (byte) 0x78;


    // The watch enters photograph mode, but doesn't appear to send a trigger signal.
    // enable (00/01)
    public static final byte CMD_SET_PHOTOGRAPH_MODE = (byte) 0x79;


    // enable (00/01)
    public static final byte CMD_SET_LOST_REMINDER = (byte) 0x7a;


    // 7b has 1 argument. Looks like enable/disable.

    public static final byte ARG_SET_TIMEMODE_24H = 0x00;
    public static final byte ARG_SET_TIMEMODE_12H = 0x01;
    // ARG_SET_TIMEMODE_*
    public static final byte CMD_SET_TIMEMODE = (byte) 0x7c;


    // 14 arguments. Watch might reply with RPRT_BATTERY.
    public static final byte CMD_7e = (byte) 0x7e;


    // 01
    // fall hour
    // fall minute
    // awake hour
    // awake minute
    public static final byte CMD_SET_SLEEP_TIME = (byte) 0x7f;


    // enable (00/01)
    public static final byte CMD_SET_REAL_TIME_HEART_RATE = (byte) 0x84;


    // looks like enable/disable.
    public static final byte CMD_85 = (byte) 0x85;


    // 00
    // year hi
    // year lo
    // month
    // day
    // hour
    // minute
    // second
    public static final byte CMD_SET_DATE_TIME = (byte) 0x93;

    // 3 arguments. Sent when saving personal information.
    public static final byte CMD_95 = (byte) 0x95;

    // looks like enable/disable.
    public static final byte CMD_96 = (byte) 0x96;


    // looks like enable/disable.
    public static final byte CMD_e5 = (byte) 0xe5;


    // If this is sent after {@link CMD_FACTORY_RESET}, it's a shutdown, not a reboot.
    // Rebooting resets the watch face and wallpaper.
    public static final byte CMD_REBOOT = (byte) 0xff;
}
