package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class PebbleMorpheuzSampleProvider extends AbstractSampleProvider<PebbleMorpheuzSample> {
    // raw types
    public static final int TYPE_DEEP_SLEEP = 5;
    public static final int TYPE_LIGHT_SLEEP = 4;
    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_UNKNOWN = 0;

    protected float movementDivisor = 5000f;

    public PebbleMorpheuzSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<PebbleMorpheuzSample, ?> getSampleDao() {
        return getSession().getPebbleMorpheuzSampleDao();
    }

    @Override
    protected Property getTimestampSampleProperty() {
        return PebbleMorpheuzSampleDao.Properties.Timestamp;
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return null; // not supported
    }

    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleMorpheuzSampleDao.Properties.DeviceId;
    }

    @Override
    public PebbleMorpheuzSample createActivitySample() {
        return new PebbleMorpheuzSample();
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
            default:
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
            default:
                return TYPE_UNKNOWN;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public List<PebbleMorpheuzSample> getActivitySamples(int timestamp_from, int timestamp_to) {
        List<PebbleMorpheuzSample> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<PebbleMorpheuzSample> filteredSamples = new ArrayList<>();
        for (PebbleMorpheuzSample sample : samples) {
            if (sample.getRawIntensity() > 1000) {
                sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
                filteredSamples.add(sample);
            }
        }

        return filteredSamples;
    }

    @Override
    public List<PebbleMorpheuzSample> getSleepSamples(int timestamp_from, int timestamp_to) {
        List<PebbleMorpheuzSample> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<PebbleMorpheuzSample> filteredSamples = new ArrayList<>();
        for (PebbleMorpheuzSample sample : samples) {
            if (sample.getRawIntensity() < 1000) {
                if (sample.getRawIntensity() <= 120) {
                    sample.setRawKind(ActivityKind.TYPE_DEEP_SLEEP);
                } else {
                    sample.setRawKind(ActivityKind.TYPE_LIGHT_SLEEP);
                }
                filteredSamples.add(sample);
            }
        }

        return filteredSamples;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MORPHEUZ;
    }
}
