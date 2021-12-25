/*  Copyright (C) 2020-2021 Andreas Böhler, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.util;

import static androidx.core.app.ActivityCompat.startIntentSenderForResult;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;

public class BondingUtil {
    public static final String STATE_DEVICE_CANDIDATE = "stateDeviceCandidate";

    private static final int REQUEST_CODE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BondingUtil.class);
    private static final long DELAY_AFTER_BONDING = 1000; // 1s

    /**
     * Returns a BroadcastReceiver that handles Gadgetbridge's device changed broadcasts
     */
    public static BroadcastReceiver getPairingReceiver(final BondingInterface activity) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                    GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    LOG.debug("Pairing receiver: device changed: " + device);
                    if (activity.getCurrentTarget().getDevice().getAddress().equals(device.getAddress())) {
                        if (device.isInitialized()) {
                            LOG.info("Device is initialized, finish things up");
                            activity.onBondingComplete(true);
                        } else if (device.isConnecting() || device.isInitializing()) {
                            LOG.info("Still connecting/initializing device...");
                        }
                    }
                }
            }
        };
    }

    /**
     * Returns a BroadcastReceiver that handles Bluetooth chance broadcasts
     */
    public static BroadcastReceiver getBondingReceiver(final BondingInterface bondingInterface) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String bondingMacAddress = bondingInterface.getCurrentTarget().getDevice().getAddress();

                    LOG.info("Bond state changed: " + device + ", state: " + device.getBondState() + ", expected address: " + bondingMacAddress);
                    if (bondingMacAddress != null && bondingMacAddress.equals(device.getAddress())) {
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        switch (bondState) {
                            case BluetoothDevice.BOND_BONDED: {
                                LOG.info("Bonded with " + device.getAddress());
                                //noinspection StatementWithEmptyBody
                                if (isLePebble(device)) {
                                    // Do not initiate connection to LE Pebble!
                                } else {
                                    attemptToFirstConnect(bondingInterface.getCurrentTarget().getDevice());
                                }
                                return;
                            }
                            case BluetoothDevice.BOND_NONE: {
                                LOG.info("Not bonded with " + device.getAddress() + ", attempting to connect anyway.");
                                attemptToFirstConnect(bondingInterface.getCurrentTarget().getDevice());
                                return;
                            }
                            case BluetoothDevice.BOND_BONDING: {
                                LOG.info("Bonding in progress with " + device.getAddress());
                                return;
                            }
                            default: {
                                LOG.warn("Unknown bond state for device " + device.getAddress() + ": " + bondState);
                                bondingInterface.onBondingComplete(false);
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * Connect to candidate after a certain delay
     *
     * @param candidate the device to connect to
     */
    public static void attemptToFirstConnect(final BluetoothDevice candidate) {
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(new Runnable() {
            @Override
            public void run() {
                GBApplication.deviceService().disconnect();
                GBDevice device = DeviceHelper.getInstance().toSupportedDevice(candidate);
                connectToGBDevice(device);
            }
        }, DELAY_AFTER_BONDING);
    }

    /**
     * Just calls DeviceService connect with the "first time" flag
     */
    private static void connectToGBDevice(GBDevice device) {
        if (device != null) {
            GBApplication.deviceService().connect(device, true);
        } else {
            GB.toast("Unable to connect, can't recognize the device type", Toast.LENGTH_LONG, GB.ERROR);
        }
    }


    /**
     * Returns true if GB should pair
     */
    public static boolean shouldUseBonding() {
        // TODO: Migrate to generic "should even try bonding" preference key

        // There are connection problems on certain Galaxy S devices at least
        // try to connect without BT pairing (bonding)
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getPreferences().getBoolean(MiBandConst.PREF_MIBAND_SETUP_BT_PAIRING, true);
    }

    /**
     * Connects to the device and calls callback
     */
    public static void connectThenComplete(BondingInterface bondingInterface, GBDeviceCandidate deviceCandidate) {
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        connectThenComplete(bondingInterface, device);
    }

    /**
     * Connects to the device and calls callback
     */
    public static void connectThenComplete(BondingInterface bondingInterface, GBDevice device) {
        toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_trying_to_connect_to, device.getName()), Toast.LENGTH_SHORT, GB.INFO);
        // Disconnect when LE Pebble so that the user can manually initiate a connection
        GBApplication.deviceService().disconnect();
        GBApplication.deviceService().connect(device, true);
        bondingInterface.onBondingComplete(true);
    }

    /**
     * Checks the type of bonding needed for the device and continues accordingly
     */
    public static void initiateCorrectBonding(final BondingInterface bondingInterface, final GBDeviceCandidate deviceCandidate) {
        int bondingStyle = DeviceHelper.getInstance().getCoordinator(deviceCandidate).getBondingStyle();
        if (bondingStyle == DeviceCoordinator.BONDING_STYLE_NONE ||
            bondingStyle == DeviceCoordinator.BONDING_STYLE_LAZY ) {
            // Do nothing
            return;
        } else if (bondingStyle == DeviceCoordinator.BONDING_STYLE_ASK) {
            new AlertDialog.Builder(bondingInterface.getContext())
                    .setCancelable(true)
                    .setTitle(bondingInterface.getContext().getString(R.string.discovery_pair_title, deviceCandidate.getName()))
                    .setMessage(bondingInterface.getContext().getString(R.string.discovery_pair_question))
                    .setPositiveButton(bondingInterface.getContext().getString(R.string.discovery_yes_pair), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BondingUtil.tryBondThenComplete(bondingInterface, deviceCandidate);
                        }
                    })
                    .setNegativeButton(R.string.discovery_dont_pair, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BondingUtil.connectThenComplete(bondingInterface, deviceCandidate);
                        }
                    })
                    .show();
        } else {
            BondingUtil.tryBondThenComplete(bondingInterface, deviceCandidate);
        }
        LOG.debug("Bonding initiated");
    }

    /**
     * Tries to create a BluetoothDevice bond
     * Do not call directly, use createBond(Activity, GBDeviceCandidate) instead!
     */
    private static void bluetoothBond(BondingInterface context, GBDeviceCandidate candidate) {
        BluetoothDevice device = candidate.getDevice();
        if (device.createBond()) {
            // Async, results will be delivered via a broadcast
            LOG.info("Bonding in progress...");
        } else {
            LOG.error(String.format(Locale.getDefault(),
                    "Bonding failed immediately! %1$s (%2$s) %3$d",
                    device.getName(),
                    device.getAddress(),
                    device.getType())
            );

            BluetoothClass bluetoothClass = device.getBluetoothClass();
            if (bluetoothClass != null) {
                LOG.error(String.format(Locale.getDefault(),
                        "BluetoothClass: %1$s",
                        bluetoothClass.toString()));
            }

            // Theoretically we shouldn't be doing this
            // because this function shouldn't've been called
            // with an already bonded device
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                LOG.warn("For some reason the device is already bonded, but let's try first connect");
                attemptToFirstConnect(context.getCurrentTarget().getDevice());
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                LOG.warn("Device is still bonding after an error");
                // TODO: Not sure we can handle this better, it's weird already.
            } else {
                LOG.warn("Bonding failed immediately and no bond was made");
                toast(context.getContext(), context.getContext().getString(R.string.discovery_bonding_failed_immediately, device.getName()), Toast.LENGTH_SHORT, GB.ERROR);
            }
        }
    }

    /**
     * Handles the activity result and checks if there's anything CompanionDeviceManager-related going on
     */
    public static void handleActivityResult(BondingInterface bondingInterface, int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                requestCode == BondingUtil.REQUEST_CODE &&
                resultCode == Activity.RESULT_OK) {

            BluetoothDevice deviceToPair =
                    data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

            if (deviceToPair != null) {
                if (bondingInterface.getCurrentTarget().getDevice().getAddress().equals(deviceToPair.getAddress())) {
                    if (deviceToPair.getBondState() != BluetoothDevice.BOND_BONDED) {
                        BondingUtil.bluetoothBond(bondingInterface, bondingInterface.getCurrentTarget());
                    } else {
                        bondingInterface.onBondingComplete(true);
                    }
                } else {
                    bondingInterface.onBondingComplete(false);
                }
            }
        }
    }

    /**
     * Checks if device is LE Pebble
     */
    public static boolean isLePebble(BluetoothDevice device) {
        return (device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL || device.getType() == BluetoothDevice.DEVICE_TYPE_LE) &&
                (device.getName().startsWith("Pebble-LE ") || device.getName().startsWith("Pebble Time LE "));
    }

    /**
     * Uses the CompanionDeviceManager bonding method
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private static void companionDeviceManagerBond(BondingInterface bondingInterface,
                                                   final GBDeviceCandidate deviceCandidate) {
        BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                .setAddress(deviceCandidate.getMacAddress())
                .build();

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build();

        CompanionDeviceManager manager = (CompanionDeviceManager) bondingInterface.getContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
        LOG.debug(String.format("Searching for %s associations", deviceCandidate.getMacAddress()));
        for (String association : manager.getAssociations()) {
            LOG.debug(String.format("Already associated with: %s", association));
            if (association.equals(deviceCandidate.getMacAddress())) {
                LOG.info("The device has already been bonded through CompanionDeviceManager, using regular");
                // If it's already "associated", we should immediately pair
                // because the callback is never called (AFAIK?)
                BondingUtil.bluetoothBond(bondingInterface, deviceCandidate);
                return;
            }
        }

        LOG.debug("Starting association request");
        manager.associate(pairingRequest,
                getCompanionDeviceManagerCallback(bondingInterface),
                null);
    }

    /**
     * This is a bit hacky, but it does stop a bonding that might be otherwise stuck,
     * use with some caution
     */
    public static void stopBluetoothBonding(BluetoothDevice device) {
        try {
            //noinspection JavaReflectionMemberAccess
            device.getClass().getMethod("cancelBondProcess").invoke(device);
        } catch (Throwable ignore) {
        }
    }

    /**
     * Finalizes bonded device
     */
    public static void handleDeviceBonded(BondingInterface bondingInterface, GBDeviceCandidate deviceCandidate) {
        if (deviceCandidate == null) {
            LOG.error("deviceCandidate was null! Can't handle bonded device!");
            return;
        }

        toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_successfully_bonded, deviceCandidate.getName()), Toast.LENGTH_SHORT, GB.INFO);
        connectThenComplete(bondingInterface, deviceCandidate);
    }

    /**
     * Use this function to initiate bonding to a GBDeviceCandidate
     */
    public static void tryBondThenComplete(BondingInterface bondingInterface, GBDeviceCandidate deviceCandidate) {
        bondingInterface.registerBroadcastReceivers();
        BluetoothDevice device = deviceCandidate.getDevice();

        int bondState = device.getBondState();
        if (bondState == BluetoothDevice.BOND_BONDED) {
            GB.toast(bondingInterface.getContext().getString(R.string.pairing_already_bonded, device.getName(), device.getAddress()), Toast.LENGTH_SHORT, GB.INFO);
            //noinspection StatementWithEmptyBody
            if (GBApplication.getPrefs().getBoolean("enable_companiondevice_pairing", true) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // If CompanionDeviceManager is enabled, skip connection and go bond
                // TODO: It would theoretically be nice to check if it's already been granted,
                //  but re-bond works
            } else {
                attemptToFirstConnect(bondingInterface.getCurrentTarget().getDevice());
                return;
            }
        } else if (bondState == BluetoothDevice.BOND_BONDING) {
            GB.toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.pairing_in_progress, device.getName(), device.getAddress()), Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        GB.toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.pairing_creating_bond_with, device.getName(), device.getAddress()), Toast.LENGTH_LONG, GB.INFO);
        toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_attempting_to_pair, deviceCandidate.getName()), Toast.LENGTH_SHORT, GB.INFO);
        if (GBApplication.getPrefs().getBoolean("enable_companiondevice_pairing", true) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            companionDeviceManagerBond(bondingInterface, deviceCandidate);
        } else {
            bluetoothBond(bondingInterface, deviceCandidate);
        }
    }

    /**
     * Returns a callback for CompanionDeviceManager
     *
     * @param bondingInterface the activity that started the CDM bonding process
     * @return CompanionDeviceManager.Callback that handles the CompanionDeviceManager bonding process results
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private static CompanionDeviceManager.Callback getCompanionDeviceManagerCallback(final BondingInterface bondingInterface) {
        return new CompanionDeviceManager.Callback() {
            @Override
            public void onFailure(CharSequence error) {
                toast(bondingInterface.getContext(), bondingInterface.getContext().getString(R.string.discovery_bonding_failed_immediately), Toast.LENGTH_SHORT, GB.ERROR);
            }

            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                try {
                    startIntentSenderForResult((Activity) bondingInterface.getContext(),
                            chooserLauncher,
                            REQUEST_CODE,
                            null,
                            0,
                            0,
                            0,
                            null);
                } catch (IntentSender.SendIntentException e) {
                    LOG.error(e.toString());
                }
            }
        };
    }
}
