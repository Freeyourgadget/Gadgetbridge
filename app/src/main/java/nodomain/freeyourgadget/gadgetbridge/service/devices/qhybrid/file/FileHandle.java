package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file;

public enum FileHandle {
    OTA_FILE(0x00, 0x00),
    FONT_FILE(0x03, 0x00),
    MUSIC_INFO(0x04, 0x00),
    UI_CONTROL(0x05, 0x00),
    HAND_ACTIONS(0x06, 0x00),
    SETTINGS_BUTTONS(0x06, 0x00),
    ASSET_BACKGROUND_IMAGES(0x07, 0x00),
    ASSET_NOTIFICATION_IMAGES(0x07, 0x01),
    ASSET_TRANSLATIONS(0x07, 0x02),
    ASSET_REPLY_IMAGES(0x07, 0x03),
    CONFIGURATION(0x08, 0x00),
    NOTIFICATION_PLAY(0x09, 0x00),
    ALARMS(0x0A, 0x00),
    DEVICE_INFO(0x0b, 0x00),
    NOTIFICATION_FILTER(0x0C, 0x00),
    WATCH_PARAMETERS(0x0E, 0x00),
    LOOK_UP_TABLE(0x0f, 0x00),
    RATE(0x10, 0x00),
    REPLY_MESSAGES(0x13, 0x00),
    ;

    private byte handle, subHandle;

    FileHandle(int handle, int subHandle) {
        this.handle = (byte) handle;
        this.subHandle = (byte) subHandle;
    }

    public short getHandle(){
        return (short)((handle << 8) | (subHandle));
    }
}
