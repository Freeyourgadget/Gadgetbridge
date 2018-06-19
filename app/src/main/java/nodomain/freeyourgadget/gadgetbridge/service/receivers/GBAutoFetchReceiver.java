package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;


public class GBAutoFetchReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBAutoFetchReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        //LOG.info("User is present!");
        GBApplication application = (GBApplication) context;
        List<GBDevice> devices = application.getDeviceManager().getDevices();
        for (int i = 0; i < devices.size(); i++) {
            GBDevice device = devices.get(i);
            // Will show that the device is not connected even when the device is connected
            if (device.isInitialized()) {
                DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                if (coordinator.supportsActivityDataFetching() && !device.isBusy()) {
                    application.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
                }
            }
        }
    }
}

