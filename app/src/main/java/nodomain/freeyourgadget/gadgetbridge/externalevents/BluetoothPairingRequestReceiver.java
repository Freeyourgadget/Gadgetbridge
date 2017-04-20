/*  Copyright (C) 2015-2017 João Paulo Barraca

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

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

/**
 * Created by jpbarraca on 13/04/2017.
 */

public class BluetoothPairingRequestReceiver extends BroadcastReceiver {


    private static final Logger LOG = LoggerFactory.getLogger(BluetoothConnectReceiver.class);

    final DeviceCommunicationService service;

    public BluetoothPairingRequestReceiver(DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (!action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
            return;
        }

        GBDevice gbDevice = service.getGBDevice();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (gbDevice == null || device == null)
            return;

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        try {
            if (coordinator.getBondingStyle(gbDevice) == DeviceCoordinator.BONDING_STYLE_NONE) {
                LOG.info("Aborting unwanted pairing request");
                abortBroadcast();
            }
        } catch (Exception e) {
            LOG.warn("Could not abort pairing request process");

        }
    }
}