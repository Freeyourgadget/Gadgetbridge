package nodomain.freeyourgadget.gadgetbridge.devices.bicycle_sensor.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class BicycleSensorActivitySampleProvider extends AbstractSampleProvider<BicycleSensorActivitySample> {
    protected BicycleSensorActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<BicycleSensorActivitySample, ?> getSampleDao() {
        return getSession().getBicycleSensorSampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return BicycleSensorActivitySample.Properties.RevolutionCount;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return BicycleSensorActivitySample.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return BicycleSensorActivitySample.Properties.DeviceId;
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
