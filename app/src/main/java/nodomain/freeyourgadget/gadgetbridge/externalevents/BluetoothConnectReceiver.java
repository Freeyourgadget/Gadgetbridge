/*  Copyright (C) 2016-2024 Andreas Shimokawa, Daniel Dakhno, José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class BluetoothConnectReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothConnectReceiver.class);

    final DeviceCommunicationService service;

    public BluetoothConnectReceiver(final DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (!action.equals(BluetoothDevice.ACTION_ACL_CONNECTED) || !intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
            return;
        }

        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            LOG.error("Got no device for {}", action);
            return;
        }

        LOG.info("connection attempt detected from {}", device.getAddress());

        final GBDevice gbDevice = GBApplication.app().getDeviceManager().getDeviceByAddress(device.getAddress());
        if (gbDevice == null) {
            LOG.info("Connected device {} unknown", device.getAddress());
            return;
        }
        final SharedPreferences deviceSpecificPreferences = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        boolean reactToConnection = deviceSpecificPreferences.getBoolean(GBPrefs.DEVICE_CONNECT_BACK, false);
        reactToConnection |= gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT;
        reactToConnection |= gbDevice.getState() == GBDevice.State.WAITING_FOR_SCAN;
        if (!reactToConnection) {
            LOG.info("Ignoring connection attempt from {}", device.getAddress());
            return;
        }
        LOG.info("Will re-connect to {} ({})", gbDevice.getAddress(), gbDevice.getName());
        GBApplication.deviceService(gbDevice).connect();
    }
}
