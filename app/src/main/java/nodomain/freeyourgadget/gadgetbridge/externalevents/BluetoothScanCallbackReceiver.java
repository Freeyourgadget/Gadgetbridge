/*  Copyright (C) 2016-2018 Andreas Shimokawa

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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class BluetoothScanCallbackReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(BluetoothScanCallbackReceiver.class);
    private List<String> mSeenScanCallbackUUIDs = new ArrayList<String>();

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(!action.equals("BluetoothDevice.ACTION_FOUND") || !intent.hasExtra("address") || !intent.hasExtra("uuid")) {
            return;
        }

        String wantedAddress = intent.getExtras().getString("address");
        String uuid = intent.getExtras().getString("uuid");
        //if (!action.equals(BluetoothDevice.ACTION_ACL_CONNECTED) || !intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
        //    return;
        //}

        int bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);
        if(bleCallbackType != -1) {
            //LOG.debug("Passive background scan callback type: " + bleCallbackType);
            ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
            for(ScanResult result: scanResults) {
                BluetoothDevice device = result.getDevice();
                if(device.getAddress().equals(wantedAddress) && !mSeenScanCallbackUUIDs.contains(uuid)) {
                    mSeenScanCallbackUUIDs.add(uuid);
                    LOG.info("ScanCallbackReceiver has found " + device.getAddress() + "(" + device.getName() + ")");
                    GBApplication.deviceService().connect();
                }
            }
        }
    }
}
