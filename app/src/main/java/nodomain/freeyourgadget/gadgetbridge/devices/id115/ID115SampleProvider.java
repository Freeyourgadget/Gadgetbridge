package nodomain.freeyourgadget.gadgetbridge.devices.id115;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ID115SampleProvider extends AbstractSampleProvider<ID115ActivitySample> {
    public ID115SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<ID115ActivitySample, ?> getSampleDao() {
        return getSession().getID115ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return ID115ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ID115ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ID115ActivitySampleDao.Properties.DeviceId;
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
        return rawIntensity;
    }

    @Override
    public ID115ActivitySample createActivitySample() {
        return new ID115ActivitySample();
    }
}
