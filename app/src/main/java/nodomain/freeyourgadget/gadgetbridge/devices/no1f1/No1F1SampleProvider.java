package nodomain.freeyourgadget.gadgetbridge.devices.no1f1;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.No1F1ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.No1F1ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class No1F1SampleProvider extends AbstractSampleProvider<No1F1ActivitySample> {

    private GBDevice mDevice;
    private DaoSession mSession;

    public No1F1SampleProvider(GBDevice device, DaoSession session) {
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
        return rawIntensity / (float) 100.0;
    }

    @Override
    public No1F1ActivitySample createActivitySample() {
        return new No1F1ActivitySample();
    }

    @Override
    public AbstractDao<No1F1ActivitySample, ?> getSampleDao() {
        return getSession().getNo1F1ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return No1F1ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return No1F1ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return No1F1ActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public List<No1F1ActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        List<No1F1ActivitySample> samples = super.getGBActivitySamples(timestamp_from - 3599, timestamp_to, ActivityKind.TYPE_ALL);
        List<No1F1ActivitySample> outSamples = new ArrayList<>();

        for (No1F1ActivitySample sample : samples) {
            int timestamp = sample.getTimestamp();
            int steps = sample.getSteps() / 60;
            int leftover_steps = sample.getSteps() % 60;
            for (int i = 0; i < 60; i++) {
                if (i == 59)
                    steps = steps + leftover_steps; // simplest way to not lose steps from rounding error
                if (timestamp >= timestamp_from && timestamp <= timestamp_to) {
                    No1F1ActivitySample newSample = new No1F1ActivitySample(
                            timestamp,
                            sample.getDeviceId(),
                            sample.getUserId(),
                            steps,
                            sample.getRawKind()
                    );
                    newSample.setProvider(this);
                    outSamples.add(newSample);
                }
                timestamp += 60;
            }
        }

        detachFromSession();
        return outSamples;
    }
}
