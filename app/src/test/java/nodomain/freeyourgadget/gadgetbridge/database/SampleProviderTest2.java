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


public class SampleProviderTest2 extends TestBase {
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

   @Test
   public void testDeviceManager() {
       DeviceManager manager = ((GBApplication) (this.getContext())).getDeviceManager();
       Log.d("---------------", "-----------------------------------");

       System.out.println("-----------------------------------------");
       assertNotNull(((GBApplication) GBApplication.getContext()).getDeviceManager());
       LOG.debug(manager.toString());

   }
}
