package nodomain.freeyourgadget.gadgetbridge;

public class GBDeviceApp {
    private final String name;
    private final String creator;
    private final String version;
    private final int id;
    private final int index;

    public GBDeviceApp(int id, int index, String name, String creator, String version) {
        this.id = id;
        this.index = index;
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

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }
}
