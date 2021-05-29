/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Taavi Eomäe

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

import static nodomain.freeyourgadget.gadgetbridge.util.BondingUtil.STATE_DEVICE_CANDIDATE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBandPairingActivity extends AbstractGBActivity implements BondingInterface {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandPairingActivity.class);

    private static final int REQ_CODE_USER_SETTINGS = 52;

    private final BroadcastReceiver pairingReceiver = BondingUtil.getPairingReceiver(this);
    private final BroadcastReceiver bondingReceiver = BondingUtil.getBondingReceiver(this);
    private TextView message;
    private boolean isPairing;
    private GBDeviceCandidate deviceCandidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_band_pairing);

        message = findViewById(R.id.miband_pair_message);
        this.deviceCandidate = getIntent().getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            this.deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }

        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);

        if (coordinator.getSupportedDeviceSpecificSettings(device) != null) { // FIXME: this will no longer be sane in the future
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            String authKey = sharedPrefs.getString("authkey", null);
            if (authKey == null || authKey.isEmpty()) {
                SharedPreferences.Editor editor = sharedPrefs.edit();

                String randomAuthkey = RandomStringUtils.random(16, true, true);
                editor.putString("authkey", randomAuthkey);
                editor.apply();
            }
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

        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
    }


    private void startPairing() {
        isPairing = true;
        message.setText(getString(R.string.pairing, deviceCandidate));

        if (!BondingUtil.shouldUseBonding()) {
            BondingUtil.attemptToFirstConnect(getCurrentTarget().getDevice());
            return;
        }

        BondingUtil.tryBondThenComplete(this, deviceCandidate);
    }


    private void stopPairing() {
        isPairing = false;
        BondingUtil.stopBluetoothBonding(deviceCandidate.getDevice());
    }

    @Override
    public void onBondingComplete(boolean success) {
        LOG.debug("pairingFinished: " + success);
        if (!isPairing) {
            // already gone?
            return;
        } else {
            isPairing = false;
        }

        if (success) {
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

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceCandidate;
    }

    @Override
    protected void onResume() {
        registerBroadcastReceivers();
        super.onResume();
    }

    @Override
    protected void onStart() {
        registerBroadcastReceivers();
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopPairing();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopPairing();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        // WARN: Do not stop pairing or unregister receivers pause!
        // Bonding process can pause the activity and you might miss broadcasts
        super.onPause();
    }

    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), pairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bondingReceiver);
    }

    public void registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(pairingReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
        registerReceiver(bondingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    public Context getContext() {
        return this;
    }
}
