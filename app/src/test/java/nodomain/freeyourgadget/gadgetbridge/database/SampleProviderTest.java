package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.contentprovider.HRContentProvider;
import nodomain.freeyourgadget.gadgetbridge.contentprovider.HRContentProviderContract;
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

import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SampleProviderTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(SampleProviderTest.class);

    private GBDevice dummyGBDevice;
    private ContentResolver mContentResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ShadowLog.stream = System.out; // show logger’s output

        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
        mContentResolver = app.getContentResolver();

        HRContentProvider provider = new HRContentProvider();

        // Stuff context into provider
        provider.attachInfo(app.getApplicationContext(), null);

        ShadowContentResolver.registerProviderInternal(HRContentProviderContract.AUTHORITY, provider);
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

    private void generateSampleStream(MiBandSampleProvider sampleProvider) {
        final User user = DBHelper.getUser(daoSession);
        final Device device = DBHelper.getDevice(dummyGBDevice, daoSession);

        for (int i = 0; i < 10; i++) {
            MiBandActivitySample sample = createSample(sampleProvider, MiBandSampleProvider.TYPE_ACTIVITY, 100 + i * 50, 10, 60 + i * 5, 1000 * i, user, device);
            //LOG.debug("Sending sample " + sample.getHeartRate());
            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(intent);
        }
    }


    //@Ignore
    @Test
    public void testContentProvider() {

        dummyGBDevice.setState(GBDevice.State.CONNECTED);
        final MiBandSampleProvider sampleProvider = new MiBandSampleProvider(dummyGBDevice, daoSession);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(app);
        sharedPreferences.edit().putString(MiBandConst.PREF_MIBAND_ADDRESS, dummyGBDevice.getAddress()).commit();

        // Refresh the device list
        dummyGBDevice.sendDeviceUpdateIntent(app);

        assertNotNull("The ContentResolver may not be null", mContentResolver);


        Cursor cursor;
        /*
         * Test the device uri
         */
        cursor = mContentResolver.query(HRContentProviderContract.DEVICES_URI, null, null, null, null);

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

        /*
         * Test the activity start uri
         */
        cursor = mContentResolver.query(HRContentProviderContract.ACTIVITY_START_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(0);
                String message = cursor.getString(1);
                assertEquals("OK", status);
                assertEquals("Connected", message);

            } while (cursor.moveToNext());
        }

        /*
         * Test the activity stop uri
         */
        cursor = mContentResolver.query(HRContentProviderContract.ACTIVITY_STOP_URI, null, null, null, null);
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
                Cursor cursor = mContentResolver.query(HRContentProviderContract.REALTIME_URI, null, null, null, null);
                if (cursor.moveToFirst()) {
                    do {
                        String status = cursor.getString(0);
                        int heartRate = cursor.getInt(1);

                        LOG.info("HeartRate " + heartRate);
                        assertEquals("OK", status);
                        assertEquals(60 + 5*numObserved, heartRate);
                    } while (cursor.moveToNext());
                }
                numObserved++;
            }
        }
        A1 a1 = new A1();

        mContentResolver.registerContentObserver(HRContentProviderContract.REALTIME_URI, false, a1);
        generateSampleStream(sampleProvider);

        assertEquals(a1.numObserved, 10);

    }

   @Test
   public void testDeviceManager() {
       DeviceManager manager = ((GBApplication) (this.getContext())).getDeviceManager();
       Log.d("---------------", "-----------------------------------");

       System.out.println("-----------------------------------------");
       assertNotNull(((GBApplication) GBApplication.getContext()).getDeviceManager());
       LOG.debug(manager.toString());

   }
}
