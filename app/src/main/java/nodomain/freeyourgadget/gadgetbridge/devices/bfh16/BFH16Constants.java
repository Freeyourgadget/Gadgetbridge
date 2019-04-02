package nodomain.freeyourgadget.gadgetbridge.devices.bfh16;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public final class BFH16Constants {

    //public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString(String.format(BASE_UUID, "FEF5") );
    public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
    //public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString(String.format(BASE_UUID, "FEE7") );
    public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");

    public static final UUID BFH16_MAIN_SERVICE = UUID.fromString(String.format(BASE_UUID, "33F4") );


    //Known Services
    public static final UUID BFH16_SERVICE_1 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE_2 = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"); //Attribute
    public static final UUID BFH16_SERVICE_3 = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb"); //Service
    public static final UUID BFH16_SERVICE_4 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb"); //Service



    //I Think i discovered that this device (BFH16) uses communication similar to JYOU
    //Therefore i copied the following vars:
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
    public static final byte RECEIVE_HEARTRATE = (byte)0xE8;

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
