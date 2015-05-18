package nodomain.freeyourgadget.gadgetbridge;

import java.util.UUID;

public class GBDeviceApp {
    private final String name;
    private final String creator;
    private final String version;
    private final UUID uuid;
    private final Type type;

    public GBDeviceApp(UUID uuid, String name, String creator, String version, Type type) {
        this.uuid = uuid;
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

    public UUID getUUID() {
        return uuid;
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
