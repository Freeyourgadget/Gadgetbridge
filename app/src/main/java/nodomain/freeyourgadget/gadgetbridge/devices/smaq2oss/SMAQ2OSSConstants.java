package nodomain.freeyourgadget.gadgetbridge.devices.smaq2oss;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;


public class SMAQ2OSSConstants {

    // Nordic UART Service UUID
//    public static final UUID UUID_SERVICE_SMAQ2OSS = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
//    public static final UUID UUID_CHARACTERISTIC_WRITE_NORMAL = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
//    public static final UUID UUID_CHARACTERISTIC_NOTIFY_NORMAL = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // SMA-Q2-OSS watch service UUID 51be0000-c182-4f3a-9359-21337bce51f6
    public static final UUID UUID_SERVICE_SMAQ2OSS = UUID.fromString("51be0001-c182-4f3a-9359-21337bce51f6");
    public static final UUID UUID_CHARACTERISTIC_WRITE_NORMAL = UUID.fromString("51be0002-c182-4f3a-9359-21337bce51f6");
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_NORMAL = UUID.fromString("51be0003-c182-4f3a-9359-21337bce51f6");

    public static final byte MSG_SET_TIME = 0x01;
    public static final byte MSG_BATTERY_STATE = 0x02;
    public static final byte MSG_MUSIC_EVENT = 0x03;
    public static final byte MSG_SET_WEATHER = 0x04;
    public static final byte MSG_SET_MUSIC_INFO = 0x05;
    public static final byte MSG_CALL_NOTIFICATION = 0x06;
    public static final byte MSG_CALL_COMMAND = 0x07;
    public static final byte MSG_NOTIFICATION = 0x08;

    public static final byte EVT_PLAY_PAUSE = 0x00;
    public static final byte EVT_FWD = 0x01;
    public static final byte EVT_REV = 0x02;
    public static final byte EVT_VOL_UP = 0x03;
    public static final byte EVT_VOL_DOWN = 0x04;

    public static final int MUSIC_ARTIST_MAX_LEN = 32;
    public static final int MUSIC_ALBUM_MAX_LEN = 32;
    public static final int MUSIC_TRACK_MAX_LEN = 64;

    public static final int CALL_NAME_MAX_LEN = 32;
    public static final int CALL_NUMBER_MAX_LEN = 16;

    public static final int NOTIFICATION_SENDER_MAX_LEN = 32;
    public static final int NOTIFICATION_SUBJECT_MAX_LEN = 32;
    public static final int NOTIFICATION_BODY_MAX_LEN = 200;
}