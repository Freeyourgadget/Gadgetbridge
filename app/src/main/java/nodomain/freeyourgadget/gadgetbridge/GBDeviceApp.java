package nodomain.freeyourgadget.gadgetbridge;

public class GBDeviceApp {
    private final String name;
    private final String creator;
    private final String version;
    private final int id;
    private final int index;
    private final Type type;

    public GBDeviceApp(int id, int index, String name, String creator, String version, Type type) {
        this.id = id;
        this.index = index;
        this.name = name;
        this.creator = creator;
        this.version = version;
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public enum Type {
        UNKNOWN,
        WATCHFACE,
        APP_GENERIC,
        APP_ACTIVITYTRACKER,
    }
}
