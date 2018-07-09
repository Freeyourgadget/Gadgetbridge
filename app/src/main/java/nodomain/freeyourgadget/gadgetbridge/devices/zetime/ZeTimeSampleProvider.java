package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.ZeTimeActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ZeTimeActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ZeTimeSampleProvider extends AbstractSampleProvider<ZeTimeActivitySample> {

    private GBDevice mDevice;
    private DaoSession mSession;

    public ZeTimeSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);

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
        return rawIntensity;
    }

    @Override
    public ZeTimeActivitySample createActivitySample() {
        return new ZeTimeActivitySample();
    }

    @Override
    public AbstractDao<ZeTimeActivitySample, ?> getSampleDao() {
        return getSession().getZeTimeActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.DeviceId;
    }
}
