/*  Copyright (C) 2017-2019 Sami Alaoui

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

package nodomain.freeyourgadget.gadgetbridge.devices.bfh16;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public final class BFH16Constants {

    //Known Services
    public static final UUID BFH16_GENERIC_ACCESS_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_GENERIC_ARTTRIBUTE_SERVICE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");

    public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");


    public static final UUID BFH16_SERVICE1         = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE1_WRITE   = UUID.fromString("000033f3-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE1_NOTIFY  = UUID.fromString("000033f4-0000-1000-8000-00805f9b34fb");

    public static final UUID BFH16_SERVICE2         = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE2_WRITE   = UUID.fromString("0000fec7-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE2_INDICATE= UUID.fromString("0000fec8-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE2_READ    = UUID.fromString("0000fec9-0000-1000-8000-00805f9b34fb");


    //Verified command bytes
    public static final byte CMD_SET_ALARM_1 = (byte)0x09;
    public static final byte CMD_SET_ALARM_2 = (byte)0x22;
    public static final byte CMD_SET_ALARM_3 = (byte)0x23;

    public static final byte CMD_SET_DATE_AND_TIME = 0x08;


    public static final byte CMD_MEASURE_HEART = (byte)0x0D;    //param1: 0, param2: 0 -> STOP | 1 -> START
    public static final byte CMD_VIBRATE = (byte)0x07;  //param1: 0, param2: 1


    public static final byte CMD_SWITCH_PHOTO_MODE = (byte)0x25;        //param1: 0, param2: 0 -> OFF | 1 -> ON
    public static final byte CMD_SWITCH_12HOUR_MODE = (byte)0x3E;       //byte1: 1 -> 12HourMode | 0 -> 24HourMode
    public static final byte CMD_SWITCH_METRIC_IMPERIAL = (byte)0x3A;   //param1: 0, param2: 0 -> METRIC | 1 -> IMPERIAL //Also requests walked steps


    //Verified receive bytes
    public static final byte RECEIVE_BATTERY_LEVEL = (byte)0xF7;
    public static final byte RECEIVE_STEPS_DATA = (byte)0xF9;
    public static final byte RECEIVE_HEART_DATA = (byte)0xE8;
    public static final byte RECEIVE_PHOTO_TRIGGER = (byte)0xF3;



    //Most probably correct command bytes
    public static final byte CMD_SET_STEPLENGTH = (byte)0x3F;  //param1: 0, param2: STEPLENGTH


    //Probably correct command bytes
    public static final byte CMD_SET_INACTIVITY_WARNING_TIME = (byte)0x24;   //param1: 0, param2: time

    public static final byte CMD_SET_HEART_TARGET = (byte)0x01;  //param1: 0, param2: HEART TARGET
    public static final byte CMD_SET_STEP_TARGET = (byte)0x03;  //param1: 0, param2: STEP TARGET

    public static final byte CMD_FIND_DEVICE = (byte)0x36;          //param1: 0, param2: 1
    public static final byte CMD_SET_DISCONNECT_REMIND = (byte)0x37;    //param1: 0, param2: 0 -> ??? | 1 -> ???
    public static final byte CMD_SET_AUTODETECT_HEART = (byte)0x38;     //param1: 0, param2: 0 -> ??? | 1 -> ???

    public static final byte CMD_READ_HISTORY_SLEEP_COUNT = (byte)0x32; //param1: 0, param2: 0

    public static final byte CMD_SET_NOON_TIME = (byte)0x26;    //param1: start time, param2: end time
    public static final byte CMD_SET_SLEEP_TIME = (byte)0x27;   //param1: start time, param2: end time


    //Could be correct command bytes
        //Send PhoneName 0x17 and 0x18
        //Send PhoneNumber 0x19 and 0x20
        //Weather 0x3B
        //Power Management 0x39
        //User Id 0x35
        //



    //______________________________________________________________________________________________
    //It may be that BFH16 uses the same communication protocol as JYOU
    //copied the following JYOU vars:

    public static final byte CMD_SET_HEARTRATE_AUTO = 0x38;
    public static final byte CMD_SET_HEARTRATE_WARNING_VALUE = 0x01;
    public static final byte CMD_SET_TARGET_STEPS = 0x03;
    //public static final byte CMD_GET_STEP_COUNT = 0x1D;
    public static final byte CMD_GET_SLEEP_TIME = 0x32;
    public static final byte CMD_SET_DND_SETTINGS = 0x39;

    public static final byte CMD_ACTION_HEARTRATE_SWITCH = 0x0D;
    public static final byte CMD_ACTION_SHOW_NOTIFICATION = 0x2C;
    public static final byte CMD_ACTION_REBOOT_DEVICE = 0x0E;


    public static final byte RECEIVE_DEVICE_INFO = (byte)0xF6;


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
