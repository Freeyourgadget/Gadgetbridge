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
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGBX100DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.BcdUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_KEY_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WEARLOCATION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.GENDER_MALE;

public class SetConfigurationOperation  extends AbstractBTLEOperation<CasioGBX100DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(GetConfigurationOperation.class);
    private final CasioGBX100DeviceSupport support;
    private final CasioConstants.ConfigurationOption option;

    public SetConfigurationOperation(CasioGBX100DeviceSupport support, CasioConstants.ConfigurationOption option) {
        super(support);
        this.support = support;
        this.option = option;
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask("SetConfigurationOperation starting..."); // mark as busy quickly to avoid interruptions from the outside
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
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            byte[] oldData = new byte[data.length];
            System.arraycopy(data, 0, oldData, 0, data.length);

            if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE")) {

                ActivityUser user = new ActivityUser();
                boolean all = (option == CasioConstants.ConfigurationOption.OPTION_ALL);
                if (option == CasioConstants.ConfigurationOption.OPTION_GENDER || all) {
                    if (user.getGender() == GENDER_MALE) {
                        data[1] = (byte) (data[1] & ~0x01);
                    } else {
                        data[1] = (byte) (data[1] | 0x01);
                    }
                }

                for(int i=2; i<data.length; i++) {
                    data[i] = (byte)~data[i];
                }

                if (option == CasioConstants.ConfigurationOption.OPTION_HEIGHT || all) {
                    int height = user.getHeightCm();
                    data[2] = BcdUtil.toBcd8(height % 100);
                    data[3] = BcdUtil.toBcd8((height - (height % 100)) / 100);
                }
                if (option == CasioConstants.ConfigurationOption.OPTION_WEIGHT || all) {
                    int weight = user.getWeightKg();
                    data[4] = BcdUtil.toBcd8(weight % 100);
                    data[5] = BcdUtil.toBcd8((weight - (weight % 100)) / 100);
                }
                if (option == CasioConstants.ConfigurationOption.OPTION_WRIST || all) {
                    String location = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getString(PREF_WEARLOCATION, "left");
                    if (location == "right") {
                        data[1] = (byte) (data[1] | 0x02);
                    } else {
                        data[1] = (byte) (data[1] & ~0x02);
                    }
                }
                if(option == CasioConstants.ConfigurationOption.OPTION_BIRTHDAY || all) {
                    int year = user.getYearOfBirth();
                    // Month and Day are not configured in Gadgetbridge!
                    int month = 1;
                    int day = 1;
                    data[6] = BcdUtil.toBcd8(year % 100);
                    data[7] = BcdUtil.toBcd8((year - (year % 100)) / 100);
                    data[8] = BcdUtil.toBcd8(month);
                    data[9] = BcdUtil.toBcd8(day);
                }

                for(int i=2; i<data.length; i++) {
                    data[i] = (byte)~data[i];
                }

                // This allows changing month and day of birth on the watch
                // without having it overwritten on every sync
                boolean match = true;
                for(int i=0; i<8; i++) {
                    if(data[i] != oldData[i]) {
                        match = false;
                        break;
                    }
                }
                if(match) {
                    LOG.info("No configuration update required");
                    requestTargetSettings();
                } else {
                    // Target settings will be requested in write callback
                    try {
                        TransactionBuilder builder = performInitialized("setConfiguration");
                        builder.setGattCallback(this);
                        support.writeAllFeatures(builder, data);
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.info("Error writing configuration to Casio watch");
                    }
                }
                return true;
            } else if (data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE")) {
                ActivityUser user = new ActivityUser();
                boolean all = (option == CasioConstants.ConfigurationOption.OPTION_ALL);

                if(option == CasioConstants.ConfigurationOption.OPTION_STEP_GOAL || all) {
                    int steps = user.getStepsGoal();
                    data[1] = (byte)(steps & 0xff);
                    data[2] = (byte)((steps >> 8) & 0xff);
                }

                if(option == CasioConstants.ConfigurationOption.OPTION_DISTANCE_GOAL || all) {
                    // The watch requires a monthly goal, so we multiply that with 30
                    // and divide it by 100 because the value is set in 100m units
                    int distance = user.getDistanceMeters() * 30;
                    distance = distance / 100;
                    data[6] = (byte)(distance & 0xff);
                    data[7] = (byte)((distance >> 8) & 0xff);
                }

                if(option == CasioConstants.ConfigurationOption.OPTION_ACTIVITY_GOAL || all) {
                    // The watch requires a monthly goal, so we multiply that with 30
                    int time = user.getActiveTimeMinutes() * 30;
                    data[9] = (byte)(time & 0xff);
                    data[10] = (byte)((time >> 8) & 0xff);
                }

                if(Arrays.equals(oldData, data)) {
                    LOG.info("No configuration update required");
                    requestBasicSettings();
                } else {
                    // Basic settings will be requested in Gatt callback
                    try {
                        TransactionBuilder builder = performInitialized("setConfiguration");
                        builder.setGattCallback(this);
                        support.writeAllFeatures(builder, data);
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.info("Error writing configuration to Casio watch");
                    }
                }
                return true;
            } else if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BASIC")) {
                SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                GBPrefs gbPrefs = new GBPrefs(new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress())));

                String timeformat = gbPrefs.getTimeFormat();

                if(timeformat.equals(getContext().getString(R.string.p_timeformat_24h))) {
                    data[1]  |= 0x01;
                } else {
                    data[1] &= ~0x01;
                }

                boolean autolight = sharedPreferences.getBoolean(PREF_AUTOLIGHT, false);
                if(autolight) {
                    data[1] &= ~0x04;
                } else {
                    data[1] |= 0x04;
                }

                boolean key_vibration = sharedPreferences.getBoolean(PREF_KEY_VIBRATION, true);
                if (key_vibration) {
                    data[10] = 1;
                } else {
                    data[10] = 0;
                }

                boolean operating_sounds = sharedPreferences.getBoolean(PREF_OPERATING_SOUNDS, false);
                if(operating_sounds) {
                    data[1] &= ~0x02;
                } else {
                    data[1] |= 0x02;
                }

                if(Arrays.equals(oldData, data)) {
                    LOG.info("No configuration update required");
                    operationFinished();
                } else {
                    // Operation will be finished in Gatt callback
                    try {
                        TransactionBuilder builder = performInitialized("setConfiguration");
                        builder.setGattCallback(this);
                        support.writeAllFeatures(builder, data);
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.info("Error writing configuration to Casio watch");
                    }
                }
                return true;
            }

        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    protected void operationFinished() {
        LOG.info("SetConfigurationOperation finished");

        unsetBusy();
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("finished operation");
                builder.setGattCallback(null); // unset ourselves from being the queue's gatt callback
                builder.wait(0);
                builder.queue(getQueue());
            } catch (IOException ex) {
                LOG.info("Error resetting Gatt callback: " + ex.getMessage());
            }
        }
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

    private void requestTargetSettings() {
        byte[] command = new byte[1];
        command[0] = CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE");
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
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_USER_PROFILE")) {
                requestTargetSettings();
                return true;
            }
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_TARGET_VALUE")) {
                requestBasicSettings();
                return true;
            }
            if(data[0] == CasioConstants.characteristicToByte.get("CASIO_SETTING_FOR_BASIC")) {
                operationFinished();
                return true;
            }
        }
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }
}