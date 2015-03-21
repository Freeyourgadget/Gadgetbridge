package nodomain.freeyourgadget.gadgetbridge;

public class GBDevice {
    private boolean isConnected = false;
    private final String name;
    private final String address;
    private String firmwareVersion;

    public GBDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        if (firmwareVersion != null) {
            return "Firmware Version: " + firmwareVersion;
        } else {
            return null;
        }
    }
}
