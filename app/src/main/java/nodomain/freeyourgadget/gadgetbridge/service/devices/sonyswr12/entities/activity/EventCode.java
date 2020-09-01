package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public enum EventCode {
    STEPS(3),
    ACTIVITY_DATA(5),
    HEART_RATE(9);

    final int value;

    EventCode(int value) {
        this.value = value;
    }

    static EventCode fromInt(int i) {
        for (EventCode code : values()){
            if (code.value == i)
                return code;
        }
        throw new RuntimeException("wrong event code: " + i);
    }
}
