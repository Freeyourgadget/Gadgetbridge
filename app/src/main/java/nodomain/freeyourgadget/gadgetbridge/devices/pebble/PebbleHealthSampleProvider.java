package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class PebbleHealthSampleProvider extends AbstractSampleProvider<PebbleHealthActivitySample> {
    public static final int TYPE_LIGHT_SLEEP = 1;
    public static final int TYPE_DEEP_SLEEP = 2;
    public static final int TYPE_LIGHT_NAP = 3; //probably
    public static final int TYPE_DEEP_NAP = 4; //probably
    public static final int TYPE_WALK = 5; //probably
    public static final int TYPE_ACTIVITY = -1;

    protected final float movementDivisor = 8000f;

    public PebbleHealthSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public List<PebbleHealthActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        List<PebbleHealthActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);

        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }

        QueryBuilder<PebbleHealthActivityOverlay> qb = getSession().getPebbleHealthActivityOverlayDao().queryBuilder();

        // I assume it returns the records by id ascending ... (last overlay is dominant)
        qb.where(PebbleHealthActivityOverlayDao.Properties.DeviceId.eq(dbDevice.getId()), PebbleHealthActivityOverlayDao.Properties.TimestampTo.ge(timestamp_from))
                .where(PebbleHealthActivityOverlayDao.Properties.TimestampFrom.le(timestamp_to));
        List<PebbleHealthActivityOverlay> overlayRecords = qb.build().list();

        for (PebbleHealthActivityOverlay overlay : overlayRecords) {
            for (PebbleHealthActivitySample sample : samples) {
                if (overlay.getTimestampFrom() <= sample.getTimestamp() && sample.getTimestamp() < overlay.getTimestampTo()) {
                    // patch in the raw kind
                    sample.setRawKind(overlay.getRawKind());
                }
            }
        }
        detachFromSession();
        return samples;
    }

    @Override
    public AbstractDao<PebbleHealthActivitySample, ?> getSampleDao() {
        return getSession().getPebbleHealthActivitySampleDao();
    }

    @Override
    protected Property getTimestampSampleProperty() {
        return PebbleHealthActivitySampleDao.Properties.Timestamp;
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return null;
        // it is still in the database just hide it for now. remove these two commented lines later
        //return PebbleHealthActivitySampleDao.Properties.RawKind;
    }

    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleHealthActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public PebbleHealthActivitySample createActivitySample() {
        return new PebbleHealthActivitySample();
    }

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
            case TYPE_DEEP_NAP:
            case TYPE_DEEP_SLEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case TYPE_LIGHT_NAP:
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
                return TYPE_ACTIVITY;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_HEALTH;
    }
}
