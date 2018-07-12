package nodomain.freeyourgadget.gadgetbridge.database;

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

    private GBDevice dummyGBDevice;
    private ContentResolver mContentResolver;

    @Override
    public void setUp() throws Exception {
        LOG.debug("-------------------------------------");
        super.setUp();
        LOG.debug("++++++++++++++++++++++++++++++++++++++");
        ShadowLog.stream = System.out; // show logger’s output

    }

   @Test
   public void testDeviceManager() {
       DeviceManager manager = ((GBApplication) (this.getContext())).getDeviceManager();
       assertNotNull(((GBApplication) GBApplication.getContext()).getDeviceManager());
       LOG.debug(manager.toString());

   }
   @Test
   public void testDeviceManager2() {
       DeviceManager manager = ((GBApplication) (this.getContext())).getDeviceManager();
       assertNotNull(((GBApplication) GBApplication.getContext()).getDeviceManager());
       LOG.debug(manager.toString());

   }
}
