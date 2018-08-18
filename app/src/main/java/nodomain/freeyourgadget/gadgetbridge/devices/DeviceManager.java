/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Provides access to the list of devices managed by Gadgetbridge.
 * Changes to the devices (e.g. connection state) or the list of devices
 * are broadcasted via #ACTION_DEVICE_CHANGED
 */
public class DeviceManager {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);

    public static final String BLUETOOTH_DEVICE_ACTION_ALIAS_CHANGED = "android.bluetooth.device.action.ALIAS_CHANGED";
    /**
     * Intent action to notify that the list of devices has changed.
     */
    public static final String ACTION_DEVICES_CHANGED
            = "nodomain.freeyourgadget.gadgetbridge.devices.devicemanager.action.devices_changed";
    /**
     * Intent action to notify this class that the list of devices shall be refreshed.
     */
    public static final String ACTION_REFRESH_DEVICELIST
            = "nodomain.freeyourgadget.gadgetbridge.devices.devicemanager.action.set_version";
    private final Context context;
    /**
     * This list is final, it will never be recreated. Only its contents change.
     * This allows direct access to the list from ListAdapters.
     */
    private final List<GBDevice> deviceList = new ArrayList<>();
    private GBDevice selectedDevice = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_REFRESH_DEVICELIST: // fall through
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    refreshPairedDevices();
                    break;
                case BluetoothDevice.ACTION_NAME_CHANGED:
                case BLUETOOTH_DEVICE_ACTION_ALIAS_CHANGED:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String newName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    updateDeviceName(device, newName);
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev.getAddress() != null) {
                        int index = deviceList.indexOf(dev); // search by address
                        if (index >= 0) {
                            deviceList.set(index, dev);
                        } else {
                            deviceList.add(dev);
                        }
                        if (dev.isInitialized()) {
                            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                                DBHelper.getDevice(dev, dbHandler.getDaoSession()); // implicitly creates the device in database if not present, and updates device attributes
                            } catch (Exception ignore) {
                            }
                        }
                    }
                    updateSelectedDevice(dev);
                    refreshPairedDevices();
                    break;
            }
        }
    };

    public DeviceManager(Context context) {
        this.context = context;
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceManager.ACTION_REFRESH_DEVICELIST);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, filterLocal);

        IntentFilter filterGlobal = new IntentFilter();
        filterGlobal.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filterGlobal.addAction(BLUETOOTH_DEVICE_ACTION_ALIAS_CHANGED);
        filterGlobal.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mReceiver, filterGlobal);

        refreshPairedDevices();
    }

    private void updateDeviceName(BluetoothDevice device, String newName) {
        for (GBDevice dev : deviceList) {
            if (device.getAddress().equals(dev.getAddress())) {
                if (!dev.getName().equals(newName)) {
                    dev.setName(newName);
                    notifyDevicesChanged();
                    return;
                }
            }
        }
    }

    private void updateSelectedDevice(GBDevice dev) {
        if (selectedDevice == null) {
            selectedDevice = dev;
        } else {
            if (selectedDevice.equals(dev)) {
                selectedDevice = dev; // equality vs identity!
            } else {
                if (selectedDevice.isConnected() && dev.isConnected()) {
                    LOG.warn("multiple connected devices -- this is currently not really supported");
                    selectedDevice = dev; // use the last one that changed
                } else if (!selectedDevice.isConnected()) {
                    selectedDevice = dev; // use the last one that changed
                }
            }
        }
        GB.updateNotification(selectedDevice, context);

    }

    private void refreshPairedDevices() {
        Set<GBDevice> availableDevices = DeviceHelper.getInstance().getAvailableDevices(context);
        deviceList.retainAll(availableDevices);
        for (GBDevice availableDevice : availableDevices) {
            if (!deviceList.contains(availableDevice)) {
                deviceList.add(availableDevice);
            }
        }

        Collections.sort(deviceList, new Comparator<GBDevice>() {
            @Override
            public int compare(GBDevice lhs, GBDevice rhs) {
                if (rhs.getStateOrdinal() - lhs.getStateOrdinal() == 0) {
                    return Collator.getInstance().compare(lhs.getName(), rhs.getName());
                }
                return (rhs.getStateOrdinal() - lhs.getStateOrdinal());
            }
        });
        notifyDevicesChanged();
    }

    /**
     * The returned list is final, it will never be recreated. Only its contents change.
     * This allows direct access to the list from ListAdapters.
     */
    public List<GBDevice> getDevices() {
        return Collections.unmodifiableList(deviceList);
    }

    @Nullable
    public GBDevice getSelectedDevice() {
        return selectedDevice;
    }

    private void notifyDevicesChanged() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_DEVICES_CHANGED));
    }
}
