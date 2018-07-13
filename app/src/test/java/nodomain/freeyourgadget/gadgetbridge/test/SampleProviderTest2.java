package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.ContentResolver;
import android.util.Log;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.contentprovider.HRContentProvider;
import nodomain.freeyourgadget.gadgetbridge.contentprovider.HRContentProviderContract;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertNotNull;

import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SampleProviderTest2 extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(SampleProviderTest2.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

   @Test
   public void testDeviceManager() {
       DeviceManager manager = ((GBApplication) (app)).getDeviceManager();
       assertNotNull(manager);
       LOG.debug(manager.toString());

   }
   @Test
   public void testDeviceManager2() {
       DeviceManager manager = ((GBApplication) (app)).getDeviceManager();
       assertNotNull(manager);
       LOG.debug(manager.toString());

   }
}
