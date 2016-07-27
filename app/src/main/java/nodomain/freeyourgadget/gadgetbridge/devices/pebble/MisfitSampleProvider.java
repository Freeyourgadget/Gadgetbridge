package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMisfitSample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMisfitSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class MisfitSampleProvider implements SampleProvider<PebbleMisfitSample> {
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected final float movementDivisor = 300f;

    public MisfitSampleProvider(GBDevice device, DaoSession session) {
        mSession = session;
        mDevice = device;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public PebbleMisfitSample createActivitySample() {
        return null;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }

    @Override
    public List<PebbleMisfitSample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    @Override
    public List<PebbleMisfitSample> getActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
    }

    @Override
    public List<PebbleMisfitSample> getSleepSamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP);
    }

    @Override
    public int fetchLatestTimestamp() {
        QueryBuilder<PebbleMisfitSample> qb = getSampleDao().queryBuilder();
        qb.orderDesc(getTimestampSampleProperty());
        qb.limit(1);
        List<PebbleMisfitSample> list = qb.build().list();
        if (list.size() >= 1) {
            return list.get(0).getTimestamp();
        }
        return -1;
    }

    @Override
    public void addGBActivitySample(PebbleMisfitSample activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addGBActivitySamples(PebbleMisfitSample[] activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind) {
    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind) {
    }

    protected List<PebbleMisfitSample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        QueryBuilder<PebbleMisfitSample> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(mDevice, mSession);
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestamp_from))
                .where(timestampProperty.le(timestamp_to));
        List<PebbleMisfitSample> samples = qb.build().list();
        List<PebbleMisfitSample> filteredSamples = new ArrayList<>();
        for (PebbleMisfitSample sample : samples) {
            if ((sample.getRawKind() & activityType) != 0) {
                sample.setProvider(this);
                filteredSamples.add(sample);
            }
        }

        return filteredSamples;
    }

    public AbstractDao<PebbleMisfitSample, ?> getSampleDao() {
        return mSession.getPebbleMisfitSampleDao();
    }

    protected Property getTimestampSampleProperty() {
        return PebbleMisfitSampleDao.Properties.Timestamp;
    }

    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleMisfitSampleDao.Properties.DeviceId;
    }

}
