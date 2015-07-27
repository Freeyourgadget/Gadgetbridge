package nodomain.freeyourgadget.gadgetbridge.miband;

import nodomain.freeyourgadget.gadgetbridge.charts.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

public class MiBandSampleProvider implements SampleProvider {
    public static final byte TYPE_DEEP_SLEEP = 5;
    public static final byte TYPE_LIGHT_SLEEP = 4;
    public static final byte TYPE_ACTIVITY = -1;
    public static final byte TYPE_UNKNOWN = -1;

//    public static final byte TYPE_CHARGING = 6;
//    public static final byte TYPE_NONWEAR = 3;
//    public static final byte TYPE_NREM = 5; // DEEP SLEEP
//    public static final byte TYPE_ONBED = 7;
//    public static final byte TYPE_REM = 4; // LIGHT SLEEP
//    public static final byte TYPE_RUNNING = 2;
//    public static final byte TYPE_SLIENT = 0;
//    public static final byte TYPE_USER = 100;
//    public static final byte TYPE_WALKING = 1;

    // maybe this should be configurable 256 seems way off, though.
    private float movementDivisor = 180.0f; //256.0f;

    @Override
    public int normalizeType(byte rawType) {
        switch (rawType) {
            case TYPE_DEEP_SLEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case TYPE_LIGHT_SLEEP:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            case TYPE_ACTIVITY:
                return ActivityKind.TYPE_ACTIVITY;
            default:
//            case TYPE_UNKNOWN: // fall through
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
                return TYPE_UNKNOWN;
        }
    }

    @Override
    public float normalizeIntensity(short rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public byte getID() {
        return SampleProvider.PROVIDER_MIBAND;
    }
}
