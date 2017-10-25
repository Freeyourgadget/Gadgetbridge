/*  Copyright (C) 2016-2017 Andreas Shimokawa

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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

    private static final Logger LOG = LoggerFactory.getLogger(BluetoothConnectReceiver.class);

    final DeviceCommunicationService service;

    public BluetoothConnectReceiver(DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.equals(BluetoothDevice.ACTION_ACL_CONNECTED) || !intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        LOG.info("connection attempt detected from or to " + device.getAddress() + "(" + device.getName() + ")");

        GBDevice gbDevice = service.getGBDevice();
        if (gbDevice != null) {
            if (device.getAddress().equals(gbDevice.getAddress()) && gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                LOG.info("Will re-connect to " + gbDevice.getAddress() + "(" + gbDevice.getName() + ")");
                GBApplication.deviceService().connect();
            }
        }
    }
}
