package nodomain.freeyourgadget.gadgetbridge.database;

import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SampleProviderTest extends TestBase {

    private GBDevice dummyGBDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
    }

    @Test
    public void testBasics() {
        MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);
        assertNotNull(sampleProvider.getDevice());
        assertSame(dummyGBDevice, sampleProvider.getDevice());
        assertNotNull(sampleProvider.getSampleDao());
        assertNotNull(sampleProvider.createActivitySample());
        float intensity = sampleProvider.normalizeIntensity(50);
        assertTrue(intensity > 0);
        assertTrue(intensity < 1);
    }

    @Test
    public void testActivityKind() {
        MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);
        ActivityKind type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_ACTIVITY);
        assertEquals(ActivityKind.ACTIVITY, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_DEEP_SLEEP);
        assertEquals(ActivityKind.DEEP_SLEEP, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_LIGHT_SLEEP);
        assertEquals(ActivityKind.LIGHT_SLEEP, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_NONWEAR);
        assertEquals(ActivityKind.NOT_WORN, type);
    }

    @Test
    public void testNoSamples() {
        MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);
        List<MiBandActivitySample> samples = sampleProvider.getAllActivitySamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getAllActivitySamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getAllActivitySamples(1, -1);
        assertEquals(0, samples.size());

        // now specific activity kinds
        samples = sampleProvider.getActivitySamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getActivitySamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getActivitySamples(1, -1);
        assertEquals(0, samples.size());

        // Now high res data
        samples = sampleProvider.getAllActivitySamplesHighRes(0, 0);
        assertTrue(samples.isEmpty());

        samples = sampleProvider.getAllActivitySamplesHighRes(-1, 1);
        assertTrue(samples.isEmpty());

        samples = sampleProvider.getAllActivitySamplesHighRes(1, -1);
        assertTrue(samples.isEmpty());
    }

    private <T extends AbstractActivitySample> T createSample(SampleProvider<T> sampleProvider, int rawKind, int timestamp, int rawIntensity, int heartRate, int steps, User user, Device device) {
        T sample = sampleProvider.createActivitySample();
        sample.setProvider(sampleProvider);
        sample.setRawKind(rawKind);
        sample.setTimestamp(timestamp);
        sample.setRawIntensity(rawIntensity);
        sample.setHeartRate(heartRate);
        sample.setSteps(steps);
        sample.setUserId(user.getId());
        sample.setDeviceId(device.getId());

        return sample;
    }

    @Test
    public void testSamples() {
        MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);
        User user = DBHelper.getUser(daoSession);
        assertNotNull(user);
        assertNotNull(user.getId());
        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(device);

        MiBandActivitySample s1 = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 100, 10, 70, 1000, user, device);
        sampleProvider.addGBActivitySample(s1);
        sampleProvider.addGBActivitySample(s1); // add again, should not throw or fail

        MiBandActivitySample s2 = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 200, 20, 80, 1030, user, device);
        sampleProvider.addGBActivitySample(s2);

        MiBandActivitySample s3 = createSample(sampleProvider, MiBandSampleProvider.TYPE_DEEP_SLEEP, 1200, 10, 62, 4030, user, device);
        MiBandActivitySample s4 = createSample(sampleProvider, MiBandSampleProvider.TYPE_LIGHT_SLEEP, 2000, 10, 60, 4030, user, device);
        sampleProvider.addGBActivitySamples(new MiBandActivitySample[] { s3, s4 });

        // first checks for irrelevant timestamps => no samples
        List<MiBandActivitySample> samples = sampleProvider.getAllActivitySamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getAllActivitySamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getAllActivitySamples(1, -1);
        assertEquals(0, samples.size());

        // now specific activity kinds
        samples = sampleProvider.getActivitySamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getActivitySamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getActivitySamples(1, -1);
        assertEquals(0, samples.size());

        // Now high res data
        samples = sampleProvider.getAllActivitySamplesHighRes(0, 0);
        assertTrue(samples.isEmpty());

        samples = sampleProvider.getAllActivitySamplesHighRes(-1, 1);
        assertTrue(samples.isEmpty());

        samples = sampleProvider.getAllActivitySamplesHighRes(1, -1);
        assertTrue(samples.isEmpty());

        // finally checks for existing timestamps
        List<MiBandActivitySample> allSamples = sampleProvider.getAllActivitySamples(0, 10000);
        assertEquals(4, allSamples.size());
        // FIXME List<MiBandActivitySample> activitySamples = sampleProvider.getActivitySamples(0, 10000);
        // FIXME assertEquals(2, activitySamples.size());

        // now with more strict time ranges
        allSamples = sampleProvider.getAllActivitySamples(0, 1300);
        assertEquals(3, allSamples.size());
        // FIXME activitySamples = sampleProvider.getActivitySamples(10, 150);
        // FIXME assertEquals(1, activitySamples.size());
    }

    @Test
    public void testHighResSamples() {
        // Mi Band sample provider does not support this at the moment, so we use the Huawei sample provider
        HuaweiSampleProvider sampleProvider = new HuaweiSampleProvider(dummyGBDevice, daoSession);
        User user = DBHelper.getUser(daoSession);
        assertNotNull(user);
        assertNotNull(user.getId());
        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(device);

        HuaweiActivitySample s1 = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 100, 10, 70, 1000, user, device);
        s1.setOtherTimestamp(110); // Necessary for Huawei samples
        sampleProvider.addGBActivitySample(s1);
        sampleProvider.addGBActivitySample(s1); // add again, should not throw or fail

        HuaweiActivitySample s2 = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 110, 20, 80, 1030, user, device);
        s2.setOtherTimestamp(120);
        sampleProvider.addGBActivitySample(s2);

        HuaweiActivitySample s3 = createSample(sampleProvider, MiBandSampleProvider.TYPE_DEEP_SLEEP, 120, 10, 62, 4030, user, device);
        s3.setOtherTimestamp(200);
        HuaweiActivitySample s4 = createSample(sampleProvider, MiBandSampleProvider.TYPE_LIGHT_SLEEP, 200, 10, 60, 4030, user, device);
        s4.setOtherTimestamp(220);
        sampleProvider.addGBActivitySamples(new HuaweiActivitySample[] { s3, s4 });

        List<HuaweiActivitySample> samples = sampleProvider.getAllActivitySamples(0, 1);
        assertEquals(1, samples.size()); // It generates a sample for every 60 seconds that is requested

        samples = sampleProvider.getAllActivitySamplesHighRes(0, 1);
        assertTrue(samples.isEmpty());

        samples = sampleProvider.getAllActivitySamples(100, 150);
        assertEquals(1, samples.size());
        assertEquals(100, samples.get(0).getTimestamp());

        samples = sampleProvider.getAllActivitySamplesHighRes(100, 115);
        assertEquals(2, samples.size());

        samples = sampleProvider.getAllActivitySamples(100, 200);
        assertEquals(2, samples.size()); // First three are combined

        samples = sampleProvider.getAllActivitySamplesHighRes(100, 200);
        assertEquals(4, samples.size()); // No combining takes place for the high res
    }
}
