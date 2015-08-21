package nodomain.freeyourgadget.gadgetbridge.service;

import android.test.ServiceTestCase;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import nodomain.freeyourgadget.gadgetbridge.test.TestDeviceSupport;

public class DeviceCommunicationServiceTestCase extends AbstractServiceTestCase<DeviceCommunicationService> {
    private static final java.lang.String TEST_DEVICE_ADDRESS = TestDeviceSupport.class.getName();
    
    private TestDeviceService mDeviceService;

    public DeviceCommunicationServiceTestCase() {
        super(DeviceCommunicationService.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mDeviceService = new TestDeviceService(this);
    }

    @Test
    public void testStart() {
        mDeviceService.start();
    }

    @Test
    public void testConnect() {
        mDeviceService.connect(TEST_DEVICE_ADDRESS);
    }
}
