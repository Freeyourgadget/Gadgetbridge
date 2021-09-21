/*  Copyright (C) 2016-2020 Petr Vaněk

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitpro;

import java.util.UUID;

public class FitProConstants {

    // cd 00 2a 05 01 0c 00 0c 0000012d000000bb000018
    // |  |  |  |  |  |  |  |  |-- payload ->
    // |  |  |  |  |  |  |  |----- payload length low
    // |  |  |  |  |  |  |-------- payload length high
    // |  |  |  |  |  |----------- command
    // |  |  |  |  |-------------- delimiter/version
    // |  |  |  |----------------- command group
    // |  |  |-------------------- full length low
    // |  |----------------------- full length high
    // |-------------------------- header

    public static final byte DATA_HEADER = (byte) 0xCD;
    public static final byte DATA_HEADER_ACK = (byte) 0xDC;

    public static final byte[] DATA_TEMPLATE = {
            (byte) DATA_HEADER, // header
            (byte) 0x00, // delimiter or first byte of argument count?
            (byte) 0,    // argument count, calculated
            (byte) 0,    // command 1
            (byte) 0x1,  // delimiter?
            (byte) 0,    // command 2
            (byte) 0x0,  // delimiter or first byte of data length?
            (byte) 0,    // data length calculated
            // data payload
    };

    public static final UUID UUID_CHARACTERISTIC_UART = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9d");
    public static final UUID UUID_CHARACTERISTIC_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9d");
    public static final UUID UUID_CHARACTERISTIC_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9d");
    //public static final UUID LT716_OTA = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1912"); //when in OTA mode

    public static final byte CMD_GROUP_GENERAL = (byte) 0x12;
    public static final byte CMD_GROUP_BAND_INFO = (byte) 0x20;
    public static final byte CMD_GROUP_RECEIVE_BUTTON_DATA = 0x1c;
    public static final byte CMD_GROUP_RECEIVE_SPORTS_DATA = 0x15;
    public static final byte CMD_GROUP_HEARTRATE_SETTINGS = 0x16;
    public static final byte CMD_GROUP_REQUEST_DATA = 0x1a;
    public static final byte CMD_GROUP_BIND = 0x14;
    public static final byte CMD_GROUP_RESET = 0x1d;

    //group general 0x12
    public static final byte CMD_FIND_BAND = (byte) 0x0b;
    public static final byte CMD_SET_DATE_TIME = (byte) 0x1;
    public static final byte CMD_SET_LANGUAGE = (byte) 0x15;
    public static final byte CMD_NOTIFICATION_MESSAGE = (byte) 0x12;
    public static final byte CMD_NOTIFICATION_CALL = (byte) 0x11;
    public static final byte CMD_WEATHER = (byte) 0x20;
    public static final byte CMD_CAMERA = (byte) 0xc;
    public static final byte CMD_HEART_RATE_MEASUREMENT = 0x18; //on/off
    public static final byte CMD_DND = (byte) 0x14;
    public static final byte CMD_INIT1 = 0xa;
    public static final byte CMD_INIT2 = 0xc;
    public static final byte CMD_INIT3 = (byte) 0xff;

    public static final byte CMD_SET_SLEEP_TIMES = (byte) 0xF;
    public static final byte CMD_ALARM = (byte) 0x2;
    public static final byte CMD_SET_ARM = (byte) 0x6;
    public static final byte CMD_GET_HR = (byte) 0xd; //0/1
    public static final byte CMD_GET_PRESS = (byte) 0xe; //0/1

    public static final byte CMD_NOTIFICATIONS_ENABLE = 0x7;
    public static final byte[] VALUE_SET_NOTIFICATIONS_ENABLE_ON = new byte[]{0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1};
    public static final byte[] VALUE_SET_NOTIFICATIONS_ENABLE_OFF = new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};

    public static final byte CMD_SET_LONG_SIT_REMINDER = (byte) 0x5;
    public static final byte[] VALUE_SET_LONG_SIT_REMINDER_ON = new byte[]{0x0, 0x1, 0x0, (byte) 0x96};
    public static final byte[] VALUE_SET_LONG_SIT_REMINDER_OFF = new byte[]{0x0, 0x0, 0x0, (byte) 0x96, 0x4, 0x8, 0x16, 0x7f};

    public static final byte CMD_SET_DISPLAY_ON_LIFT = (byte) 0x9;
    public static final byte CMD_SET_STEP_GOAL = (byte) 0x3;
    public static final byte CMD_SET_USER_DATA = (byte) 0x4;
    public static final byte CMD_SET_DEVICE_VIBRATIONS = (byte) 0x8;

    //group receive sports data 0x15
    public static final byte CMD_REQUEST_DAY_STEPS_SUMMARY = 0x6;
    //request steps/hr, as per
    // https://github.com/vanous/MFitX/blob/AntiGoogle/app/src/main/java/anonymouls/dev/mgcex/app/backend/LM517CommandInterpreter.kt#L242
    public static final byte CMD_REQUEST_FETCH_DAY_STEPS_DATA = 0xd;
    public static final byte CMD_REQUEST_STEPS_DATA1 = 0x6;
    public static final byte CMD_REQUEST_STEPS_DATA0x7 = 0x7;
    public static final byte CMD_REQUEST_STEPS_DATA0x8 = 0x8;
    public static final byte CMD_REQUEST_STEPS_DATA0x10 = 0x10;

    //also sleep data as per mfitx
    //https://github.com/vanous/MFitX/blob/AntiGoogle/app/src/main/java/anonymouls/dev/mgcex/app/backend/LM517CommandInterpreter.kt#L235

    public static final byte RX_HEART_RATE_DATA = 0x0e;
    public static final byte RX_SPORTS_MEASUREMENT = 0x18; //0/1
    public static final byte SPORTS_RECEIVE_KEY = 0x1; //0/1

    public static final byte RX_SLEEP_DATA = 0x3;
    public static final byte RX_STEP_DATA = 0x2;
    // https://github.com/vanous/MFitX/blob/AntiGoogle/app/src/main/java/anonymouls/dev/mgcex/app/backend/LM517CommandInterpreter.kt#L162

    public static final byte RX_SPORTS_DAY_DATA = 0xc; //0/1

    //group get band info 0x20
    public static final byte CMD_RX_BAND_INFO = (byte) 0x2;

    //group request data 0x1a
    public static final byte CMD_GET_STEPS_TARGET = 0x2;
    public static final byte CMD_GET_HW_INFO = 0x10;
    public static final byte CMD_GET_AUTO_HR = 0x8;
    public static final byte CMD_GET_CONTACTS = 0xd;

    //group 0x14
    public static final byte CMD_UNBIND = (byte) 0x0;

    //group 0x1d
    public static final byte CMD_RESET = (byte) 0x1;

    // group receive data
    public static final byte RX_FIND_PHONE = (byte) 0x01;
    public static final byte RX_CAMERA1 = (byte) 0x02;
    public static final byte RX_CAMERA2 = (byte) 0x03;
    public static final byte RX_CAMERA3 = (byte) 0x04;
    public static final byte RX_MEDIA_PLAY_PAUSE = (byte) 0x0b;
    public static final byte RX_MEDIA_FORW = (byte) 0x0c;
    public static final byte RX_MEDIA_BACK = (byte) 0x0a;


    //values
    public static final byte VALUE_ON = (byte) 0x1;
    public static final byte VALUE_OFF = (byte) 0x0;

    public static final byte UNIT_METRIC = (byte) 0x1;
    public static final byte UNIT_IMPERIAL = (byte) 0x2;

    public static final byte GENDER_MALE = (byte) 0x1;
    public static final byte GENDER_FEMALE = (byte) 0x0;

    public static final byte VALUE_SET_ARM_LEFT = (byte) 0x0; //guessing
    public static final byte VALUE_SET_ARM_RIGHT = (byte) 0x1;


    public static final byte[] VALUE_SET_DEVICE_VIBRATIONS_ENABLE = new byte[]{0x1, 0x1, 0x1, 0x1};
    public static final byte[] VALUE_SET_DEVICE_VIBRATIONS_DISABLE = new byte[]{0, 0, 0, 0};

    public static final byte NOTIFICATION_ICON_FACEBOOK = (byte) 0x4;
    public static final byte NOTIFICATION_ICON_TWITTER = (byte) 0x5;
    public static final byte NOTIFICATION_ICON_WHATSAPP = (byte) 0x8;
    public static final byte NOTIFICATION_ICON_LINE = (byte) 0x7;
    public static final byte NOTIFICATION_ICON_SMS = (byte) 0x1;
    public static final byte NOTIFICATION_ICON_WECHAT = (byte) 0x3;
    public static final byte NOTIFICATION_ICON_QQ = (byte) 0x2;
    public static final byte NOTIFICATION_ICON_INSTAGRAM = (byte) 0x10;

    public static final int LANG_CHINESE = 0x0;
    public static final int LANG_PORTUGUESE = 0xc;
    public static final int LANG_GERMAN = 0x5;
    public static final int LANG_SPANISH = 0x6;
    public static final int LANG_FRENCH = 0x7;
    public static final int LANG_NETHERLANDS = 0xa;
    public static final int LANG_POLISH = 0xb;
    public static final int LANG_RUSSIAN = 0xd;
    public static final int LANG_TURKISH = 0x10;
    public static final int LANG_ENGLISH = 0x1;
    public static final int LANG_CZECH = 0x4;
    public static final int LANG_ITALIAN = 0x12;

    //00       01         00      96            03            08      16    7f
    //^^ zeros ^^ on/off  ^^ sep  ^^unknown     ^^minutes     ^^from  ^^ to ^^ unknown
    //minutes are array of minutes by 15, in 45,60, 75...→ 3,4,5...
    //the byte 4 → 96 could be experimented with to set different values..., byte 3 is probably Hi, byte 4 Low
    //public static final byte[] CMD_SET_LONG_SIT_REMINDER = new byte[]{(byte) 0x12, (byte) 0x5};
    //maybe could be usign the ON/OFF?
    //public static final byte[] VALUE_SET_LONG_SIT_REMINDER_ON = new byte[]{0x0, 0x1, 0x0, (byte) 0x96};
    //public static final byte[] VALUE_SET_LONG_SIT_REMINDER_OFF = new byte[]{0x0, 0x0, 0x0, (byte) 0x96, 0x4, 0x8, 0x16, 0x7f};

    //Value: cd 00 0a 12 01 09 00 05 01 01 e0 05 28 ON
    //Value: cd 00 0a 12 01 09 00 05 00 01 e0 05 28 OFF
    //       00 1  2  3  4  5  6  7  8  9  10 11 12
    //byte 8 on/off
    // 9,10,11,12 → time from/to
    //public static final byte[] CMD_SET_DISPLAY_ON_LIFT = new byte[]{(byte) 0x12, (byte) 0x9};
    //maybe could be usign the ON/OFF?
    //public static final byte VALUE_SET_DISPLAY_ON_LIFT_ON = 0x1;
    //public static final byte[] VALUE_SET_DISPLAY_ON_LIFT_OFF = new byte[]{0x0, 0x0, 0x0, 0x0, 0x0};

    //time
    //0xCD 0x00 0x09 0x12 0x01 0x01 0x00 0x04 0xA5 0x83 0x73 0xDB
    //find watch
    //new byte[]{(byte) 0xcd, (byte) 0x00, (byte) 0x06,
    // (byte) 0x12, (byte) 0x01, (byte) 0x0b, (byte) 0x00, (byte) 0x01, (byte) 0x01};

    /*init procedure:
    get pair cd 00 06 12 01 0a 00 01 02 → 18, 10
    cd 00 09 12 01 01 00 04 55 f8 36 90
    cd 00 05 1a 01 0a 00 00
    cd 00 05 1a 01 0c 00 00
    cd 00 06 12 01 15 00 01 01
    cd 00 06 12 01 ff 00 01 01
    cd 00 05 1a 01 01 00 00
    dc 00 05 1a 01 00 0c 01
    cd 00 05 1a 01 0f 00 00
    cd 00 05 1a 01 10 00 00
    dc 00 05 1a 01 00 1c 01
    something cd 00 05 20 01 02 00 00 → 32, 2
    real time step cd 00 06 15 01 06 00 01 01 → 21, 6
     */

    //received heartrate
    //0xcd 0x00 0x11 0x15 0x01 0x0e 0x00 0x0c 0x2a 0xf8 0x00 0x01 0x00 0x00 0x31 0x2c 0x62 0x75 0x54 0x47
    //0xcd 0x00 0x11 0x15 0x01 0x0e 0x00 0x0c 0x2b 0x04 0x00 0x01 0x00 0x01 0x43 0x94 0x62 0x5c 0x48 0x53
    //cd001115010e000c2b04000100014394625c4853

    //find band
    // cd 00 06 12 01 0b 00 01 01

    //set celsius/fahrenheit?
    //cd0006120121000100
    //cd0006120121000101

    //find phone
    //0xcd 0x00 0x05 0x1c 0x01 0x01 0x00 0x00
    //camera:
    //0xcd 0x00 0x05 0x1c 0x01 0x03 0x00 0x00
    //0xcd 0x00 0x05 0x1c 0x01 0x02 0x00 0x00
    //0xcd 0x00 0x05 0x1c 0x01 0x04 0x00 0x00

    // <<   0xCD 0x00 0x05 0x1C 0x01 0x0A 0x00 0x00
    //pause 0xCD 0x00 0x05 0x1C 0x01 0x0B 0x00 0x00
    // >>   0xCD 0x00 0x05 0x1C 0x01 0x0C 0x00 0x00

    //0xCD 0x00 0x05 0x1C 0x01 0x03 0x00 0x00

}