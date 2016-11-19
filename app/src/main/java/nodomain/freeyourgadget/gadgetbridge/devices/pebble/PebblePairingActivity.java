package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.GBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PebblePairingActivity extends GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingActivity.class);
    private TextView message;
    private boolean isPairing;
    private boolean isLEPebble;
    private String macAddress;

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.debug("pairing activity: device changed: " + device);
                if (macAddress.equals(device.getAddress())) {
                    if (device.isInitialized()) {
                        pairingFinished(true);
                    } else if (device.isConnecting() || device.isInitializing()) {
                        LOG.info("still connecting/initializing device...");
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mBondingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                LOG.info("Bond state changed: " + device + ", state: " + device.getBondState() + ", expected address: " + macAddress);
                if (macAddress != null && macAddress.equals(device.getAddress())) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        LOG.info("Bonded with " + device.getAddress());
                        if (!isLEPebble) {
                            performConnect(device);
                        }
                    } else if (bondState == BluetoothDevice.BOND_BONDING) {
                        LOG.info("Bonding in progress with " + device.getAddress());
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        LOG.info("Not bonded with " + device.getAddress() + ", attempting to connect anyway.");
                    } else {
                        LOG.warn("Unknown bond state for device " + device.getAddress() + ": " + bondState);
                        pairingFinished(false);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pebble_pairing);

        message = (TextView) findViewById(R.id.pebble_pair_message);
        Intent intent = getIntent();
        macAddress = intent.getStringExtra(DeviceCoordinator.EXTRA_DEVICE_MAC_ADDRESS);
        if (macAddress == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        startPairing();
    }

    @Override
    protected void onDestroy() {
        try {
            // just to be sure, remove the receivers -- might actually be already unregistered
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mPairingReceiver);
            unregisterReceiver(mBondingReceiver);
        } catch (IllegalArgumentException ex) {
            // already unregistered, ignore
        }
        if (isPairing) {
            stopPairing();
        }
        super.onDestroy();
    }

    private void startPairing() {
        isPairing = true;
        message.setText(getString(R.string.pairing, macAddress));

        IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPairingReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBondingReceiver, filter);

        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        if (device != null) {
            performPair(device);
        } else {
            GB.toast(this, "No such Bluetooth Device: " + macAddress, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void pairingFinished(boolean pairedSuccessfully) {
        LOG.debug("pairingFinished: " + pairedSuccessfully);
        if (!isPairing) {
            // already gone?
            return;
        }

        isPairing = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPairingReceiver);
        unregisterReceiver(mBondingReceiver);

        if (pairedSuccessfully) {
            Intent intent = new Intent(this, ControlCenter.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void stopPairing() {
        // TODO
        isPairing = false;
    }

    protected void performPair(BluetoothDevice device) {
        int bondState = device.getBondState();
        if (bondState == BluetoothDevice.BOND_BONDED) {
            GB.toast(getString(R.string.pairing_already_bonded, device.getName(), device.getAddress()), Toast.LENGTH_SHORT, GB.INFO);
            return;
        }

        if (bondState == BluetoothDevice.BOND_BONDING) {
            GB.toast(this, getString(R.string.pairing_in_progress, device.getName(), macAddress), Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        GB.toast(this, getString(R.string.pairing_creating_bond_with, device.getName(), macAddress), Toast.LENGTH_LONG, GB.INFO);
        GBApplication.deviceService().disconnect(); // just to make sure...
        if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            isLEPebble = true;
            performConnect(device);
        } else {
            isLEPebble = false;
            device.createBond();
        }
    }

    private void performConnect(BluetoothDevice device) {
        GBDevice gbDevice = new GBDevice(device.getAddress(), device.getName(), DeviceType.PEBBLE);
        GBApplication.deviceService().connect(gbDevice);
    }
}
