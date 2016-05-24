package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class BluetoothConnectReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);

    final DeviceCommunicationService service;

    public BluetoothConnectReceiver(DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            return;
        }
        LOG.info("got connection attempt");
        GBDevice gbDevice = service.getGBDevice();
        if (gbDevice != null) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getAddress().equals(gbDevice.getAddress())) {
                LOG.info("will connect to " + gbDevice.getName());
                GBApplication.deviceService().connect();
            } else {
                LOG.info("won't connect to " + device.getAddress() + "(" + device.getName() + ")");
            }
        }
    }
}
