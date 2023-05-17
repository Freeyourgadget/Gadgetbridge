package nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSensorActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSensorActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CyclingSensorActivitySampleProvider extends AbstractSampleProvider<CyclingSensorActivitySample> {
    public CyclingSensorActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<CyclingSensorActivitySample, ?> getSampleDao() {
        return getSession().getCyclingSensorActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return CyclingSensorActivitySampleDao.Properties.RevolutionCount;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CyclingSensorActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CyclingSensorActivitySampleDao.Properties.DeviceId;
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
        return 0;
    }

    @Override
    public CyclingSensorActivitySample createActivitySample() {
        return new CyclingSensorActivitySample();
    }
}
