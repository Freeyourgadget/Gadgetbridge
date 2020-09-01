package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public enum ActivityType {
    WALK(1),
    RUN(2),
    SLEEP(3),
    HEART_RATE(10),
    END(14);

    final int value;

    ActivityType(int value) {
        this.value = value;
    }

    public static ActivityType fromInt(int i) {
        for (ActivityType type : values()){
            if (type.value == i)
                return type;
        }
        throw new IllegalArgumentException("wrong activity type: " + i);
    }
}
