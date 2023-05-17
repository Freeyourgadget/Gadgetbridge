package nodomain.freeyourgadget.gadgetbridge.devices.bicycle_sensor.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BicycleSensorActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BicycleSensorActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CyclingSensorActivitySampleProvider extends AbstractSampleProvider<BicycleSensorActivitySample> {
    public CyclingSensorActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<BicycleSensorActivitySample, ?> getSampleDao() {
        return getSession().getBicycleSensorActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return BicycleSensorActivitySampleDao.Properties.RevolutionCount;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return BicycleSensorActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return BicycleSensorActivitySampleDao.Properties.DeviceId;
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
    public BicycleSensorActivitySample createActivitySample() {
        return new BicycleSensorActivitySample();
    }
}
