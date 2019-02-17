/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PebblePairingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingActivity.class);
    private TextView message;
    private boolean isPairing;
    private boolean isLEPebble;
    private String macAddress;
    private BluetoothDevice mBtDevice;

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.debug("pairing activity: device changed: " + device);
                if (macAddress.equals(device.getAddress()) || macAddress.equals(device.getVolatileAddress())) {
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
                            performConnect(null);
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
        GBDeviceCandidate candidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (candidate != null) {
            macAddress = candidate.getMacAddress();
        }
        if (macAddress == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            returnToPairingActivity();
            return;
        }

        mBtDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        if (mBtDevice == null) {
            GB.toast(this, "No such Bluetooth Device: " + macAddress, Toast.LENGTH_LONG, GB.ERROR);
            returnToPairingActivity();
            return;
        }

        isLEPebble = mBtDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE;

        GBDevice gbDevice = null;
        if (isLEPebble) {
            if (mBtDevice.getName().startsWith("Pebble-LE ") || mBtDevice.getName().startsWith("Pebble Time LE ")) {
                if (!GBApplication.getPrefs().getBoolean("pebble_force_le", false)) {
                    GB.toast(this, "Please switch on \"Always prefer BLE\" option in Pebble settings before pairing you Pebble LE", Toast.LENGTH_LONG, GB.ERROR);
                    returnToPairingActivity();
                    return;
                }
                gbDevice = getMatchingParentDeviceFromDB(mBtDevice);
                if (gbDevice == null) {
                    return;
                }
            }
        }
        startPairing(gbDevice);
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

    private void startPairing(GBDevice gbDevice) {
        isPairing = true;
        message.setText(getString(R.string.pairing, macAddress));

        IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPairingReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBondingReceiver, filter);

        performPair(gbDevice);
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
            Intent intent = new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void stopPairing() {
        // TODO
        isPairing = false;
    }

    protected void performPair(GBDevice gbDevice) {
        int bondState = mBtDevice.getBondState();
        if (bondState == BluetoothDevice.BOND_BONDED) {
            GB.toast(getString(R.string.pairing_already_bonded, mBtDevice.getName(), mBtDevice.getAddress()), Toast.LENGTH_SHORT, GB.INFO);
            return;
        }

        if (bondState == BluetoothDevice.BOND_BONDING) {
            GB.toast(this, getString(R.string.pairing_in_progress, mBtDevice.getName(), macAddress), Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        GB.toast(this, getString(R.string.pairing_creating_bond_with, mBtDevice.getName(), macAddress), Toast.LENGTH_LONG, GB.INFO);
        GBApplication.deviceService().disconnect(); // just to make sure...

        if (isLEPebble) {
            performConnect(gbDevice);
        } else {
            mBtDevice.createBond();
        }
    }

    private void performConnect(GBDevice gbDevice) {
        if (gbDevice == null) {
            gbDevice = new GBDevice(mBtDevice.getAddress(), mBtDevice.getName(), DeviceType.PEBBLE);
        }
        GBApplication.deviceService().connect(gbDevice);
    }

    private void returnToPairingActivity() {
        startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private GBDevice getMatchingParentDeviceFromDB(BluetoothDevice btDevice) {
        String expectedSuffix = btDevice.getName();
        expectedSuffix = expectedSuffix.replace("Pebble-LE ", "");
        expectedSuffix = expectedSuffix.replace("Pebble Time LE ", "");
        expectedSuffix = expectedSuffix.substring(0, 2) + ":" + expectedSuffix.substring(2);
        LOG.info("will try to find a Pebble with BT address suffix " + expectedSuffix);
        GBDevice gbDevice = null;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            DeviceDao deviceDao = session.getDeviceDao();
            Query<Device> query = deviceDao.queryBuilder().where(DeviceDao.Properties.Type.eq(1), DeviceDao.Properties.Identifier.like("%" + expectedSuffix)).build();
            List<Device> devices = query.list();
            if (devices.size() == 0) {
                GB.toast("Please pair your non-LE Pebble before pairing the LE one", Toast.LENGTH_SHORT, GB.INFO);
                returnToPairingActivity();
                return null;
            } else if (devices.size() > 1) {
                GB.toast("Can not match this Pebble LE to a unique device", Toast.LENGTH_SHORT, GB.INFO);
                returnToPairingActivity();
                return null;
            }
            DeviceHelper deviceHelper = DeviceHelper.getInstance();
            gbDevice = deviceHelper.toGBDevice(devices.get(0));
            gbDevice.setVolatileAddress(btDevice.getAddress());
        } catch (Exception e) {
            GB.toast("Error retrieving devices from database", Toast.LENGTH_SHORT, GB.ERROR);
            returnToPairingActivity();
            return null;
        }
        return gbDevice;
    }
}
