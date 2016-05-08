package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import de.greenrobot.dao.AbstractDao;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;

public class MisfitSampleProvider extends AbstractSampleProvider<PebbleActivitySample> {

    protected final float movementDivisor = 300f;

    protected MisfitSampleProvider(DaoSession session) {
        super(session);
    }

    @Override
    public int normalizeType(int rawType) {
        return (int) rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return (byte) activityKind;
    }


    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }


    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }

    @Override
    protected AbstractDao<PebbleActivitySample, ?> getSampleDao() {
        return getmSession().getPebbleActivitySampleDao();
    }
}
