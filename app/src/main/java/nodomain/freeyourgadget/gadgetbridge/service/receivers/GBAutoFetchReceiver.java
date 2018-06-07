package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.log;

public class GBAutoFetchReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("screen on!", 3, null);
            List<GBDevice> devices = new DeviceManager(context).getDevices();
            for (int i = 0; i < devices.size(); i++) {
                GBDevice device = devices.get(i);

                log(device.toString(), 3, null);
                // Will show that the device is not connected even when the device is connected
//                if (device.isConnected() && device.isInitialized()) {
                    DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                    log(device.getName() + " is connected and initialized!", 3, null);
                    if (coordinator.supportsActivityDataFetching() && !device.isBusy()) {
                        log("about to transfer activity!", 3, null);
                        GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
//                    } else {
//                        log("Device is not connected!", 3, null);
//                    }
                }
            }
        }
    }

