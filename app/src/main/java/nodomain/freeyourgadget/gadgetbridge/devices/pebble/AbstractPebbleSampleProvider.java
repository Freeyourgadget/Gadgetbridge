package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySampleDao;

public abstract class AbstractPebbleSampleProvider extends AbstractSampleProvider<PebbleActivitySample> {
    protected AbstractPebbleSampleProvider(DaoSession session) {
        super(session);
    }

    @Override
    public AbstractDao<PebbleActivitySample, ?> getSampleDao() {
        return getSession().getPebbleActivitySampleDao();
    }

    @Override
    protected Property getTimestampSampleProperty() {
        return PebbleActivitySampleDao.Properties.Timestamp;
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return PebbleActivitySampleDao.Properties.RawKind;
    }

    @Override
    public PebbleActivitySample createActivitySample() {
        return new PebbleActivitySample();
    }
}
