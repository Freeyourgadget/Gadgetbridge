package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HealthSampleProvider implements SampleProvider {
    public static final byte TYPE_DEEP_SLEEP = 5;
    public static final byte TYPE_LIGHT_SLEEP = 4;
    public static final byte TYPE_ACTIVITY = -1;

    protected final float movementDivisor = 8000f;

    @Override
    public int normalizeType(byte rawType) {
        switch (rawType) {
        case TYPE_DEEP_SLEEP:
            return ActivityKind.TYPE_DEEP_SLEEP;
        case TYPE_LIGHT_SLEEP:
            return ActivityKind.TYPE_LIGHT_SLEEP;
        case TYPE_ACTIVITY:
        default:
            return ActivityKind.TYPE_UNKNOWN;
    }
    }

    @Override
    public byte toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_ACTIVITY:
                return TYPE_ACTIVITY;
            case ActivityKind.TYPE_DEEP_SLEEP:
                return TYPE_DEEP_SLEEP;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return TYPE_LIGHT_SLEEP;
            case ActivityKind.TYPE_UNKNOWN: // fall through
            default:
                return TYPE_ACTIVITY;
        }
    }


    @Override
    public float normalizeIntensity(short rawIntensity) {
        return rawIntensity / movementDivisor;
    }


    @Override
    public byte getID() {
        return SampleProvider.PROVIDER_PEBBLE_HEALTH;
    }
}
