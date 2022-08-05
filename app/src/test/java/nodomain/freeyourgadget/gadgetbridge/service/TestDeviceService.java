package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

/**
 * Extends GBDeviceServer so that communication with the service works
 * with Robolectric.
 */
class TestDeviceService extends GBDeviceService {
    private final ServiceController<DeviceCommunicationService> serviceController;
    private final DeviceCommunicationService service;

    TestDeviceService(Context context) {
        this(context, null);
    }

    TestDeviceService(final Context context, final GBDevice device) {
        super(context, device);

        serviceController = Robolectric.buildService(DeviceCommunicationService.class, createIntent());
        service = serviceController.create().get();
    }

    @Override
    public DeviceService forDevice(final GBDevice device) {
        return new TestDeviceService(mContext, device);
    }

    @Override
    protected void invokeService(Intent intent) {
        if (getDevice() != null) {
            intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
        }

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
