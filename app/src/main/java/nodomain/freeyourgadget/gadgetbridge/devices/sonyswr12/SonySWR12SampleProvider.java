package nodomain.freeyourgadget.gadgetbridge.devices.sonyswr12;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.SonySWR12Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.SonySWR12SampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.SonySWR12Constants;

public class SonySWR12SampleProvider extends AbstractSampleProvider<SonySWR12Sample> {
    public SonySWR12SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<SonySWR12Sample, ?> getSampleDao() {
        return getSession().getSonySWR12SampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return SonySWR12SampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return SonySWR12SampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return SonySWR12SampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
            case SonySWR12Constants.TYPE_ACTIVITY:
                return ActivityKind.TYPE_ACTIVITY;
            case SonySWR12Constants.TYPE_LIGHT:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            case SonySWR12Constants.TYPE_DEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case SonySWR12Constants.TYPE_NOT_WORN:
                return ActivityKind.TYPE_NOT_WORN;
        }
        return ActivityKind.TYPE_UNKNOWN;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_ACTIVITY:
                return SonySWR12Constants.TYPE_ACTIVITY;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return SonySWR12Constants.TYPE_LIGHT;
            case ActivityKind.TYPE_DEEP_SLEEP:
                return SonySWR12Constants.TYPE_DEEP;
            case ActivityKind.TYPE_NOT_WORN:
                return SonySWR12Constants.TYPE_NOT_WORN;
        }
        return SonySWR12Constants.TYPE_ACTIVITY;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public SonySWR12Sample createActivitySample() {
        return new SonySWR12Sample();
    }
}
