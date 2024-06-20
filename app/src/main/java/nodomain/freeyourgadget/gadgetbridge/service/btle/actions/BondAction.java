/*  Copyright (C) 2023-2024 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;

public class BondAction extends PlainAction implements BondingInterface {
    private String mMacAddress;
    private final BroadcastReceiver pairingReceiver = BondingUtil.getPairingReceiver(this);
    private final BroadcastReceiver bondingReceiver = BondingUtil.getBondingReceiver(this);

    @Override
    public void onBondingComplete(boolean success) {
        unregisterBroadcastReceivers();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return null;
    }

    @Override
    public String getMacAddress() {
        return mMacAddress;
    }

    @Override
    public boolean getAttemptToConnect() {
        return false;
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(GBApplication.getContext()), pairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(GBApplication.getContext(), bondingReceiver);
    }

    @Override
    public void registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(pairingReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
        ContextCompat.registerReceiver(getContext(), bondingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public Context getContext() {
        return GBApplication.getContext();
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        mMacAddress = gatt.getDevice().getAddress();
        BondingUtil.tryBondThenComplete(this, gatt.getDevice(), gatt.getDevice().getAddress());
        return true;
    }
}
