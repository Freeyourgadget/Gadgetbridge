package nodomain.freeyourgadget.gadgetbridge.protocol;

public class GBDeviceCommandSendBytes extends GBDeviceCommand {
    public byte[] encodedBytes;

    public GBDeviceCommandSendBytes() {
        commandClass = CommandClass.SEND_BYTES;
    }
}
