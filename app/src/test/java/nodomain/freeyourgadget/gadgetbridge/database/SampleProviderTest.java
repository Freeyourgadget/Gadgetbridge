package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.tools.ant.types.resources.comparators.Content;
import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.contentprovider.HRContentProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;


public class SampleProviderTest extends TestBase {

    private GBDevice dummyGBDevice;
    private ContentResolver mContentResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
        mContentResolver = app.getContentResolver();

        HRContentProvider provider = new HRContentProvider();
        // Stuff context into provider
        provider.attachInfo(app.getApplicationContext(), null);

        ShadowContentResolver.registerProviderInternal("com.gadgetbridge.heartrate.provider", provider);
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
        int type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_ACTIVITY);
        assertEquals(ActivityKind.TYPE_ACTIVITY, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_DEEP_SLEEP);
        assertEquals(ActivityKind.TYPE_DEEP_SLEEP, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_LIGHT_SLEEP);
        assertEquals(ActivityKind.TYPE_LIGHT_SLEEP, type);

        type = sampleProvider.normalizeType(MiBandSampleProvider.TYPE_NONWEAR);
        assertEquals(ActivityKind.TYPE_NOT_WORN, type);
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

        // and sleep
        samples = sampleProvider.getSleepSamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getSleepSamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getSleepSamples(1, -1);
        assertEquals(0, samples.size());
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
        sampleProvider.addGBActivitySamples(new MiBandActivitySample[]{s3, s4});

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

        // and sleep
        samples = sampleProvider.getSleepSamples(0, 0);
        assertEquals(0, samples.size());

        samples = sampleProvider.getSleepSamples(-1, 1);
        assertEquals(0, samples.size());

        samples = sampleProvider.getSleepSamples(1, -1);
        assertEquals(0, samples.size());

        // finally checks for existing timestamps
        List<MiBandActivitySample> allSamples = sampleProvider.getAllActivitySamples(0, 10000);
        assertEquals(4, allSamples.size());
        List<MiBandActivitySample> activitySamples = sampleProvider.getActivitySamples(0, 10000);
        assertEquals(2, activitySamples.size());
        List<MiBandActivitySample> sleepSamples = sampleProvider.getSleepSamples(0, 10000);
        assertEquals(2, sleepSamples.size());

        // now with more strict time ranges
        allSamples = sampleProvider.getAllActivitySamples(0, 1300);
        assertEquals(3, allSamples.size());
        activitySamples = sampleProvider.getActivitySamples(10, 150);
        assertEquals(1, activitySamples.size());
        sleepSamples = sampleProvider.getSleepSamples(1500, 2500);
        assertEquals(1, sleepSamples.size());
    }

    private void generateSampleStream(MiBandSampleProvider sampleProvider) {
        final User user = DBHelper.getUser(daoSession);
        final Device device = DBHelper.getDevice(dummyGBDevice, daoSession);

        for (int i = 0; i < 10; i++) {
            MiBandActivitySample sample = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 100 + i * 50, 10, 60 + i * 5, 1000 * i, user, device);
            //Log.d(SampleProviderTest.class.getName(), "Sending sample " + sample.toString());
            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
            app.sendBroadcast(intent);
        }
    }


    @Test
    public void testContentProvider() {

        dummyGBDevice.setState(GBDevice.State.CONNECTED);
        final MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(app);
        sharedPreferences.edit().putString(MiBandConst.PREF_MIBAND_ADDRESS, dummyGBDevice.getAddress()).commit();

        // Refresh the device list
        dummyGBDevice.sendDeviceUpdateIntent(app);

        assertNotNull("The ContentResolver may not be null", mContentResolver);
        //assertNotNull(((GBApplication) GBApplication.getContext()).getDeviceManager());

        Cursor cursor;
        /*
         * Test the device uri
        Cursor cursor = mContentResolver.query(HRContentProvider.DEVICES_URI, null, null, null, null);

        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                String deviceName = cursor.getString(0);
                String deviceAddress = cursor.getString(2);

                assertEquals(dummyGBDevice.getName(), deviceName);
                assertEquals(dummyGBDevice.getAddress(), deviceAddress);
            } while (cursor.moveToNext());
        }
         */

        /*
         * Test the activity start uri
         */
        cursor = mContentResolver.query(HRContentProvider.ACTIVITY_START_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(0);
                String message = cursor.getString(1);
                assertEquals("OK", status);
                assertEquals("No error", message);

            } while (cursor.moveToNext());
        }

        /*
         * Test the activity stop uri
         */
        cursor = mContentResolver.query(HRContentProvider.ACTIVITY_STOP_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(0);
                String message = cursor.getString(1);
                assertEquals("OK", status);
                assertEquals("No error", message);
            } while (cursor.moveToNext());
        }


        /*
         * Test realtime data and content observers
         */
        class A1 extends ContentObserver {
            public int numObserved = 0;

            A1() {
                super(null);
            }
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                //Log.e(SampleProviderTest.class.getName(), "Changed " + uri.toString());
                Cursor cursor = mContentResolver.query(HRContentProvider.REALTIME_URI, null, null, null, null);
                if (cursor.moveToFirst()) {
                    do {
                        String status = cursor.getString(0);
                        int heartRate = cursor.getInt(1);
                        assertEquals("OK", status);
                        assertEquals(60 + 5*numObserved, heartRate);
                        Log.i("test", "HeartRate " + heartRate);
                    } while (cursor.moveToNext());
                }
                numObserved++;
            }
        };
        A1 a1 = new A1();

        mContentResolver.registerContentObserver(HRContentProvider.REALTIME_URI, false, a1);

        generateSampleStream(sampleProvider);

        List<Intent> l = shadowOf(RuntimeEnvironment.application).getBroadcastIntents();

        assertEquals(10, l.size());
        for (Intent i : l)
            assertEquals(i.getAction(), DeviceService.ACTION_REALTIME_SAMPLES);

        List<BroadcastReceiver> r = shadowOf(RuntimeEnvironment.application).getReceiversForIntent(l.get(0));

        assertEquals(1, r.size());

        assertEquals(a1.numObserved, 10);

    }
}
