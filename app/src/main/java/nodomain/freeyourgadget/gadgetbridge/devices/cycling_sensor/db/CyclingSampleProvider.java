package nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CyclingSampleProvider extends AbstractTimeSampleProvider<CyclingSample> {
    public CyclingSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<CyclingSample, ?> getSampleDao() {
        return getSession().getCyclingSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CyclingSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CyclingSampleDao.Properties.DeviceId;
    }

    @Override
    public CyclingSample createSample() {
        return new CyclingSample();
    }
}
