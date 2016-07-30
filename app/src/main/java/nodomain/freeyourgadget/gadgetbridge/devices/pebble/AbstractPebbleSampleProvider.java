package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class AbstractPebbleSampleProvider extends AbstractSampleProvider<PebbleHealthActivitySample> {
    protected AbstractPebbleSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
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
        return PebbleHealthActivitySampleDao.Properties.RawKind;
    }

    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleHealthActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public PebbleHealthActivitySample createActivitySample() {
        return new PebbleHealthActivitySample();
    }
}
