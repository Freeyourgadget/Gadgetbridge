/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBandPairingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandPairingActivity.class);

    private static final int REQ_CODE_USER_SETTINGS = 52;
    private static final String STATE_DEVICE_CANDIDATE = "stateDeviceCandidate";
    private static final long DELAY_AFTER_BONDING = 1000; // 1s
    private TextView message;
    private boolean isPairing;
    private GBDeviceCandidate deviceCandidate;
    private String bondingMacAddress;

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.debug("pairing activity: device changed: " + device);
                if (deviceCandidate.getMacAddress().equals(device.getAddress())) {
                    if (device.isInitialized()) {
                        pairingFinished(true, deviceCandidate);
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
                LOG.info("Bond state changed: " + device + ", state: " + device.getBondState() + ", expected address: " + bondingMacAddress);
                if (bondingMacAddress != null && bondingMacAddress.equals(device.getAddress())) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        LOG.info("Bonded with " + device.getAddress());
                        bondingMacAddress = null;
                        attemptToConnect();
                    } else if (bondState == BluetoothDevice.BOND_BONDING) {
                        LOG.info("Bonding in progress with " + device.getAddress());
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        LOG.info("Not bonded with " + device.getAddress() + ", attempting to connect anyway.");
                        bondingMacAddress = null;
                        attemptToConnect();
                    } else {
                        LOG.warn("Unknown bond state for device " + device.getAddress() + ": " + bondState);
                        pairingFinished(false, deviceCandidate);
                    }
                }
            }
        }
    };

    private void attemptToConnect() {
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(new Runnable() {
            @Override
            public void run() {
                performApplicationLevelPair();
            }
        }, DELAY_AFTER_BONDING);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_band_pairing);

        message = (TextView) findViewById(R.id.miband_pair_message);
        Intent intent = getIntent();
        deviceCandidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }
        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        if (!MiBandCoordinator.hasValidUserInfo()) {
            Intent userSettingsIntent = new Intent(this, MiBandPreferencesActivity.class);
            startActivityForResult(userSettingsIntent, REQ_CODE_USER_SETTINGS, null);
            return;
        }

        // already valid user info available, use that and pair
        startPairing();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE_CANDIDATE, deviceCandidate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // start pairing immediately when we return from the user settings
        if (requestCode == REQ_CODE_USER_SETTINGS) {
            if (!MiBandCoordinator.hasValidUserInfo()) {
                GB.toast(this, getString(R.string.miband_pairing_using_dummy_userdata), Toast.LENGTH_LONG, GB.WARN);
            }
            startPairing();
        }
    }

    @Override
    protected void onDestroy() {
        // just to be sure, remove the receivers -- might actually be already unregistered
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), mPairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(this, mBondingReceiver);
        if (isPairing) {
            stopPairing();
        }
        super.onDestroy();
    }

    private void startPairing() {
        isPairing = true;
        message.setText(getString(R.string.pairing, deviceCandidate));

        IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPairingReceiver, filter);

        if (!shouldSetupBTLevelPairing()) {
            // there are connection problems on certain Galaxy S devices at least;
            // try to connect without BT pairing (bonding)
            attemptToConnect();
            return;
        }

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBondingReceiver, filter);

        performBluetoothPair(deviceCandidate);
    }

    private boolean shouldSetupBTLevelPairing() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getPreferences().getBoolean(MiBandConst.PREF_MIBAND_SETUP_BT_PAIRING, true);
    }

    private void pairingFinished(boolean pairedSuccessfully, GBDeviceCandidate candidate) {
        LOG.debug("pairingFinished: " + pairedSuccessfully);
        if (!isPairing) {
            // already gone?
            return;
        }

        isPairing = false;
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), mPairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(this, mBondingReceiver);

        if (pairedSuccessfully) {
            // remember the device since we do not necessarily pair... temporary -- we probably need
            // to query the db for available devices in ControlCenter. But only remember un-bonded
            // devices, as bonded devices are displayed anyway.
            String macAddress = deviceCandidate.getMacAddress();
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
            if (device != null && device.getBondState() == BluetoothDevice.BOND_NONE) {
                Prefs prefs = GBApplication.getPrefs();
                prefs.getPreferences().edit().putString(MiBandConst.PREF_MIBAND_ADDRESS, macAddress).apply();
            }
            Intent intent = new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void stopPairing() {
        // TODO
        isPairing = false;
    }

    protected void performBluetoothPair(GBDeviceCandidate deviceCandidate) {
        BluetoothDevice device = deviceCandidate.getDevice();

        int bondState = device.getBondState();
        if (bondState == BluetoothDevice.BOND_BONDED) {
            GB.toast(getString(R.string.pairing_already_bonded, device.getName(), device.getAddress()), Toast.LENGTH_SHORT, GB.INFO);
            performApplicationLevelPair();
            return;
        }

        bondingMacAddress = device.getAddress();
        if (bondState == BluetoothDevice.BOND_BONDING) {
            GB.toast(this, getString(R.string.pairing_in_progress, device.getName(), bondingMacAddress), Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        GB.toast(this, getString(R.string.pairing_creating_bond_with, device.getName(), bondingMacAddress), Toast.LENGTH_LONG, GB.INFO);
        if (!device.createBond()) {
            GB.toast(this, getString(R.string.pairing_unable_to_pair_with, device.getName(), bondingMacAddress), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void performApplicationLevelPair() {
        GBApplication.deviceService().disconnect(); // just to make sure...
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        if (device != null) {
            GBApplication.deviceService().connect(device, true);
        } else {
            GB.toast(this, "Unable to connect, can't recognize the device type: " + deviceCandidate, Toast.LENGTH_LONG, GB.ERROR);
        }
    }
}
