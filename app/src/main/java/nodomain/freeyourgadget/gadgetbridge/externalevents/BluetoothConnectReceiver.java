/*  Copyright (C) 2016-2020 Andreas Shimokawa

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
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

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
        LOG.info("connection attempt detected from " + device.getAddress() + "(" + device.getName() + ")");

        GBDevice gbDevice = getKnownDeviceByAddressOrNull(device.getAddress());
        if(gbDevice == null){
            LOG.info("connected device {} unknown", device.getAddress());
            return;
        }
        SharedPreferences deviceSpecificPreferences = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        boolean reactToConnection = deviceSpecificPreferences.getBoolean(GBPrefs.DEVICE_CONNECT_BACK, false);
        reactToConnection |= gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT;
        if(!reactToConnection){
            return;
        }
        LOG.info("Will re-connect to " + gbDevice.getAddress() + "(" + gbDevice.getName() + ")");
        GBApplication.deviceService().connect(gbDevice);
    }

    private GBDevice getKnownDeviceByAddressOrNull(String deviceAddress){
        List<GBDevice> knownDevices = GBApplication.app().getDeviceManager().getDevices();
        for(GBDevice device : knownDevices){
            if(device.getAddress().equals(deviceAddress)){
                return device;
            }
        }
        return null;
    }
}
