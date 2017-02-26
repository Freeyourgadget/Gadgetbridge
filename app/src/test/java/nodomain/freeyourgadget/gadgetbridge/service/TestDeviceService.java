package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import org.robolectric.Robolectric;
import org.robolectric.util.ServiceController;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;

/**
 * Extends GBDeviceServer so that communication with the service works
 * with Robolectric.
 */
class TestDeviceService extends GBDeviceService {
    private final ServiceController<DeviceCommunicationService> serviceController;
    private final DeviceCommunicationService service;

    TestDeviceService(Context context) throws Exception {
        super(context);

        serviceController = Robolectric.buildService(DeviceCommunicationService.class, createIntent());
        service = serviceController.create().get();
    }

    @Override
    protected void invokeService(Intent intent) {
        // calling though to the service natively does not work with robolectric,
        // we have to use the ServiceController to do that
        service.onStartCommand(intent, Service.START_FLAG_REDELIVERY, (int) (Math.random() * 10000));
        super.invokeService(intent);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void stopService(Intent intent) {
        super.stopService(intent);
        serviceController.destroy();
    }

    @Override
    public Intent createIntent() {
        return super.createIntent();
    }
}
