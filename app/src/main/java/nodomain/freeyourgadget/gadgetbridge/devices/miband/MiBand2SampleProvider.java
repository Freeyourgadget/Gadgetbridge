package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class MiBand2SampleProvider extends AbstractMiBandSampleProvider {
    // these are all bogus atm (come from Mi1)
    public static final int TYPE_DEEP_SLEEP = 11;
    public static final int TYPE_LIGHT_SLEEP = 5;
    public static final int TYPE_ACTIVITY = -1;
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_NONWEAR = 3;
    public static final int TYPE_CHARGING = 6;
    // appears to be a measurement problem resulting in type = 10 and intensity = 20, at least
    // with fw 1.0.0.39
    public static final int TYPE_IGNORE = 10;

    // observed the following values so far:
    // 00 01 02 09 0a 0b 0c 10 11

    public MiBand2SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_MIBAND2;
    }

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
//            case TYPE_DEEP_SLEEP:
//                return ActivityKind.TYPE_DEEP_SLEEP;
//            case TYPE_LIGHT_SLEEP:
//                return ActivityKind.TYPE_LIGHT_SLEEP;
//            case TYPE_ACTIVITY:
//                return ActivityKind.TYPE_ACTIVITY;
//            case TYPE_NONWEAR:
//                return ActivityKind.TYPE_NOT_WORN;
//            case TYPE_CHARGING:
//                return ActivityKind.TYPE_NOT_WORN; //I believe it's a safe assumption
//            case TYPE_IGNORE:
            default:
//            case TYPE_UNKNOWN: // fall through
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(int activityKind) {
//        switch (activityKind) {
//            case ActivityKind.TYPE_ACTIVITY:
//                return TYPE_ACTIVITY;
//            case ActivityKind.TYPE_DEEP_SLEEP:
//                return TYPE_DEEP_SLEEP;
//            case ActivityKind.TYPE_LIGHT_SLEEP:
//                return TYPE_LIGHT_SLEEP;
//            case ActivityKind.TYPE_NOT_WORN:
//                return TYPE_NONWEAR;
//            case ActivityKind.TYPE_UNKNOWN: // fall through
//            default:
                return TYPE_UNKNOWN;
//        }
    }
}
