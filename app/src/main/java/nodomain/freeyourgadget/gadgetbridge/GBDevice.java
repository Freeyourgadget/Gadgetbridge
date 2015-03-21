package nodomain.freeyourgadget.gadgetbridge;

public class GBDevice {
    private final String name;
    private final String address;

    public GBDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return "";
    }
}
