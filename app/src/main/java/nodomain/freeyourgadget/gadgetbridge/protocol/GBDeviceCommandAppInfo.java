package nodomain.freeyourgadget.gadgetbridge.protocol;

import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;

public class GBDeviceCommandAppInfo extends GBDeviceCommand {
    public GBDeviceApp apps[];
    public byte freeSlot = -1;

    public GBDeviceCommandAppInfo() {
        commandClass = CommandClass.APP_INFO;
    }
}
