/*  Copyright (C) 2020-2021 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGBX100DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.BcdUtil;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_KEY_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WEARLOCATION;

public class GetConfigurationOperation extends AbstractBTLEOperation<CasioGBX100DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(GetConfigurationOperation.class);
    private final CasioGBX100DeviceSupport support;
    private final boolean mFirstConnect;

    public GetConfigurationOperation(CasioGBX100DeviceSupport support, boolean firstconnect) {
        super(support);
        this.support = support;
        this.mFirstConnect = firstconnect;
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask("GetConfigurationOperation starting..."); // mark as busy quickly to avoid interruptions from the outside
    }

    @Override
    protected void doPerform() throws IOException {
        byte[] command = new byte[1];
        command[0] = CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE");
        TransactionBuilder builder = performInitialized("getConfiguration");
        builder.setGattCallback(this);
        support.writeAllFeaturesRequest(builder, command);
        builder.queue(getQueue());
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        unsetBusy();
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("finished operation");
                builder.wait(0);
                builder.setGattCallback(null); // unset ourselves from being the queue's gatt callback
                builder.queue(getQueue());
            } catch (IOException ex) {
                LOG.info("Error resetting Gatt callback: " + ex.getMessage());
            }
        }
        support.onGetConfigurationFinished();
    }

    private void requestBasicSettings() {
        byte[] command = new byte[1];
        command[0] = CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BASIC");
        try {
            TransactionBuilder builder = performInitialized("getConfiguration");
            builder.setGattCallback(this);
            support.writeAllFeaturesRequest(builder, command);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.info("Error requesting Casio configuration");
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE")) {
                boolean female = ((data[1] & 0x01) == 0x01);
                boolean right = ((data[1] & 0x02) == 0x02);
                byte[] compData = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    compData[i] = (byte) (~data[i]);
                }
                int height = BcdUtil.fromBcd8(compData[2]) + BcdUtil.fromBcd8(compData[3]) * 100;
                int weight = BcdUtil.fromBcd8(compData[4]) + BcdUtil.fromBcd8(compData[5]) * 100;
                int year = BcdUtil.fromBcd8(compData[6]) + BcdUtil.fromBcd8(compData[7]) * 100;
                int month = BcdUtil.fromBcd8(compData[8]);
                int day = BcdUtil.fromBcd8(compData[9]);

                // Store only the device-specific settings on first-connect
                SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString(PREF_WEARLOCATION, right ? "right" : "left");
                editor.apply();

                requestBasicSettings();

                return true;
            } else if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BASIC")) {
                boolean timeformat = ((data[1] & 0x01) == 0x01);
                boolean autolight = ((data[1] & 0x04) == 0x00);
                boolean key_vibration = (data[10] == 0x01);
                boolean operating_sounds = ((data[1] & 0x02) == 0x00);

                // Store only the device-specific settings on first-connect
                SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                SharedPreferences.Editor editor = prefs.edit();

                editor.putBoolean(PREF_AUTOLIGHT, autolight);
                editor.putBoolean(PREF_KEY_VIBRATION, key_vibration);
                editor.putBoolean(PREF_OPERATING_SOUNDS, operating_sounds);
                editor.apply();



                LOG.info("GetConfigurationOperation finished");
                operationFinished();

                // Retrieve all settings from the watch, this overwrites the profile
                // on first connect, overwrite the watch settings
                if (!mFirstConnect) {

                } else {
                    support.syncProfile();
                }
                return true;
            }
        }

        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {

        return super.onCharacteristicRead(gatt, characteristic, status);
    }
}
