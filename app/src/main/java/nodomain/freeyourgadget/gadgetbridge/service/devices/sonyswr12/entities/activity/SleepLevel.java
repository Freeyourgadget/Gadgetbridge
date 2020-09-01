package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public enum SleepLevel {
    AWAKE(0),
    LIGHT(1),
    DEEP(2);

    final int value;

    SleepLevel(int value){
        this.value = value;
    }

    public static SleepLevel fromInt(int i) {
        for (SleepLevel level : values()){
            if (level.value == i)
                return level;
        }
        throw new RuntimeException("wrong sleep level: " + i);
    }
}
