package nodomain.freeyourgadget.gadgetbridge.devices.id115;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class ID115Constants {
    public static final UUID UUID_SERVICE_ID115 = UUID.fromString(String.format(BASE_UUID, "0AF0"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF6"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF7"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF1"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF2"));

    public static final byte CMD_ID_WARE_UPDATE = 0x01;
    public static final byte CMD_ID_GET_INFO = 0x02;
    public static final byte CMD_ID_SETTINGS = 0x03;
    public static final byte CMD_ID_BIND_UNBIND = 0x04;
    public static final byte CMD_ID_NOTIFY = 0x05;
    public static final byte CMD_ID_APP_CONTROL = 0x06;
    public static final byte CMD_ID_BLE_CONTROL = 0x07;
    public static final byte CMD_ID_HEALTH_DATA = 0x08;
    public static final byte CMD_ID_DUMP_STACK = 0x20;
    public static final byte CMD_ID_LOG = 0x21;
    public static final byte CMD_ID_FACTORY = (byte)0xaa;
    public static final byte CMD_ID_DEVICE_RESTART = (byte)0xf0;

    // CMD_ID_SETTINGS
    public static final byte CMD_KEY_SET_TIME = 0x01;
    public static final byte CMD_KEY_SET_DISPLAY_MODE = 0x2B;

    // CMD_ID_NOTIFY
    public static final byte CMD_KEY_NOTIFY_CALL = 0x01;
    public static final byte CMD_KEY_NOTIFY_STOP = 0x02;
    public static final byte CMD_KEY_NOTIFY_MSG = 0x03;

    // CMD_ID_DEVICE_RESTART
    public static final byte CMD_KEY_REBOOT = 0x01;

    public static byte getNotificationType(NotificationType type) {
        switch (type) {
//            case GENERIC_EMAIL:
//                return 2; // Icon is not supported
            case WECHAT:
                return 3;
//            case QQ:
//                return 4;
            case FACEBOOK:
                return 6;
            case TWITTER:
                return 7;
            case WHATSAPP:
                return 8;
            case FACEBOOK_MESSENGER:
                return 9;
            case INSTAGRAM:
                return 10;
            case LINKEDIN:
                return 11;
//            case GENERIC_CALENDAR:
//                return 12; // Icon is not supported
//            case SKYPE:
//                return 13; // Icon is not supported
//            case LINE:
//                return 17; // Icon is not supported
//            case VIBER:
//                return 18; // Icon is not supported
//            case KAKAO_TALK:
//                return 19; // Icon is not supported
//            case VK:
//                return 16; // Icon is not supported
//            case GMAIL:
//                return 20; // Icon is not supported
//            case OUTLOOK:
//                return 21; // Icon is not supported
//            case SNAPCHAT:
//                return 22; // Icon is not supported
        }
        return 1;
    }
}
