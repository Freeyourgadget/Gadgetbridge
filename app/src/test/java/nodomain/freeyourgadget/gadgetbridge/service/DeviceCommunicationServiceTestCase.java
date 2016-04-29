package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.test.GBMockApplication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeviceCommunicationServiceTestCase extends AbstractServiceTestCase<DeviceCommunicationService> {
    private static final java.lang.String TEST_DEVICE_ADDRESS = TestDeviceSupport.class.getName();

    /**
     * Factory that always returns the mockSupport instance
     */
    private class TestDeviceSupportFactory extends DeviceSupportFactory {
        public TestDeviceSupportFactory(Context context) {
            super(context);
        }

        @Override
        public synchronized DeviceSupport createDeviceSupport(GBDevice device) throws GBException {
            return mockSupport;
        }
    }

    private TestDeviceService mDeviceService;
    @Mock
    private TestDeviceSupport realSupport;
    private TestDeviceSupport mockSupport;

    public DeviceCommunicationServiceTestCase() {
        super(DeviceCommunicationService.class);
    }

    @Override
    protected DeviceCommunicationService createService(Class<DeviceCommunicationService> serviceClass, GBMockApplication application, NotificationManager notificationManager) throws Exception {
        DeviceCommunicationService service = getmMockHelper().createDeviceCommunicationService(serviceClass, application);
        getmMockHelper().addSystemServiceTo(service, Context.NOTIFICATION_SERVICE, notificationManager);
        return service;
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockSupport = null;
        realSupport = new TestDeviceSupport();
        realSupport.setContext(new GBDevice(TEST_DEVICE_ADDRESS, "Test Device", DeviceType.TEST), null, getContext());
        mockSupport = Mockito.spy(realSupport);
        getServiceInstance().setDeviceSupportFactory(new TestDeviceSupportFactory(getContext()));

        mDeviceService = new TestDeviceService(this);
    }

    @Test
    public void testStart() {
        assertFalse("Service was already", getServiceInstance().isStarted());
        mDeviceService.start();
        assertTrue("Service should be started", getServiceInstance().isStarted());
    }

    @Test
    public void ensureConnected() {
        mDeviceService.connect(realSupport.getDevice());
        Mockito.verify(mockSupport, Mockito.times(1)).connect();
        assertTrue(realSupport.getDevice().isInitialized());
    }

    @Test
    public void testFindDevice() {
        ensureConnected();

        InOrder inOrder = Mockito.inOrder(mockSupport);
        mDeviceService.onFindDevice(true);
        mDeviceService.onFindDevice(false);
        inOrder.verify(mockSupport, Mockito.times(1)).onFindDevice(true);
        inOrder.verify(mockSupport, Mockito.times(1)).onFindDevice(false);
        inOrder.verifyNoMoreInteractions();
    }
}
