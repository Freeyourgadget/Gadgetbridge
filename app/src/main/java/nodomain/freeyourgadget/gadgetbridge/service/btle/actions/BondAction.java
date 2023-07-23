package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

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
        getContext().registerReceiver(bondingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
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
