package nodomain.freeyourgadget.gadgetbridge;

public class GBDeviceApp {
    private final String name;
    private final String creator;
    private final String version;

    public GBDeviceApp(String name, String creator, String version) {
        this.name = name;
        this.creator = creator;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public String getVersion() {
        return version;
    }
}
