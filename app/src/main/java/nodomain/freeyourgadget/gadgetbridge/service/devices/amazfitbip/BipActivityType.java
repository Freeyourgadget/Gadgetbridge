package nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public enum BipActivityType {
    Outdoor(1),
    Treadmill(2),
    Walking(3),
    Cycling(4);

    private final int code;

    BipActivityType(final int code) {
        this.code = code;
    }

    public int toActivityKind() {
        switch (this) {
            case Outdoor:
                return ActivityKind.TYPE_RUNNING;
            case Treadmill:
                return ActivityKind.TYPE_TREADMILL;
            case Cycling:
                return ActivityKind.TYPE_CYCLING;
            case Walking:
                return ActivityKind.TYPE_WALKING;
        }
        throw new RuntimeException("Not mapped activity kind for: " + this);
    }

    public static BipActivityType fromCode(int bipCode) {
        for (BipActivityType type : values()) {
            if (type.code == bipCode) {
                return type;
            }
        }
        throw new RuntimeException("No matching BipActivityType for code: " + bipCode);
    }

    public static BipActivityType fromActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_RUNNING:
                return Outdoor;
            case ActivityKind.TYPE_TREADMILL:
                return Treadmill;
            case ActivityKind.TYPE_CYCLING:
                return Cycling;
            case ActivityKind.TYPE_WALKING:
                return Walking;
        }
        throw new RuntimeException("No matching activity activityKind: " + activityKind);
    }
}
