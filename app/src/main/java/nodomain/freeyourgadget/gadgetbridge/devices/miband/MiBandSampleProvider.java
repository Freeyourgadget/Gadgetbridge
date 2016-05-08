package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import de.greenrobot.dao.AbstractDao;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class MiBandSampleProvider extends AbstractSampleProvider<MiBandActivitySample> {
    public static final int TYPE_DEEP_SLEEP = 5;
    public static final int TYPE_LIGHT_SLEEP = 4;
    public static final int TYPE_ACTIVITY = -1;
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_NONWEAR = 3;
    public static final int TYPE_CHARGING = 6;

//    public static final byte TYPE_NREM = 5; // DEEP SLEEP
//    public static final byte TYPE_ONBED = 7;
//    public static final byte TYPE_REM = 4; // LIGHT SLEEP
//    public static final byte TYPE_RUNNING = 2;
//    public static final byte TYPE_SLIENT = 0;
//    public static final byte TYPE_USER = 100;
//    public static final byte TYPE_WALKING = 1;

    // maybe this should be configurable 256 seems way off, though.
    private final float movementDivisor = 180.0f; //256.0f;

    public MiBandSampleProvider(DaoSession session) {
        super(session);
    }

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
            case TYPE_DEEP_SLEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case TYPE_LIGHT_SLEEP:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            case TYPE_ACTIVITY:
                return ActivityKind.TYPE_ACTIVITY;
            case TYPE_NONWEAR:
                return ActivityKind.TYPE_NOT_WORN;
            case TYPE_CHARGING:
                return ActivityKind.TYPE_NOT_WORN; //I believe it's a safe assumption
            default:
//            case TYPE_UNKNOWN: // fall through
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_ACTIVITY:
                return TYPE_ACTIVITY;
            case ActivityKind.TYPE_DEEP_SLEEP:
                return TYPE_DEEP_SLEEP;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return TYPE_LIGHT_SLEEP;
            case ActivityKind.TYPE_NOT_WORN:
                return TYPE_NONWEAR;
            case ActivityKind.TYPE_UNKNOWN: // fall through
            default:
                return TYPE_UNKNOWN;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_MIBAND;
    }

    @Override
    protected AbstractDao<MiBandActivitySample, ?> getSampleDao() {
        return getSession().getMiBandActivitySampleDao();
    }
}
