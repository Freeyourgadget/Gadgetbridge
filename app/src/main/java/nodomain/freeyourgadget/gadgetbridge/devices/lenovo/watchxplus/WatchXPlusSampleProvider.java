package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class WatchXPlusSampleProvider extends AbstractSampleProvider<WatchXPlusActivitySample> {

    public WatchXPlusSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);

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
    public WatchXPlusActivitySample createActivitySample() {
        return new WatchXPlusActivitySample();
    }

    @Override
    public AbstractDao<WatchXPlusActivitySample, ?> getSampleDao() {
        return getSession().getWatchXPlusActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return WatchXPlusActivitySampleDao.Properties.DeviceId;
    }
}
