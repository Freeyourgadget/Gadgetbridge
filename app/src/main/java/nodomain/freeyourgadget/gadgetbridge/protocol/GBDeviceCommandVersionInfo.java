package nodomain.freeyourgadget.gadgetbridge.protocol;

public class GBDeviceCommandVersionInfo extends GBDeviceCommand {
    public String fwVersion = "N/A";
    public String hwVersion = "N/A";

    public GBDeviceCommandVersionInfo() {
        commandClass = CommandClass.VERSION_INFO;
    }
}
