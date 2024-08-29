/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

public class DeviceInfoProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceInfoProfile.class);

    private static final String ACTION_PREFIX = DeviceInfoProfile.class.getName() + "_";

    public static final String ACTION_DEVICE_INFO = ACTION_PREFIX + "DEVICE_INFO";
    public static final String EXTRA_DEVICE_INFO = "DEVICE_INFO";

    public static final UUID SERVICE_UUID = GattService.UUID_SERVICE_DEVICE_INFORMATION;

    public static final UUID UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING = GattCharacteristic.UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING;
    public static final UUID UUID_CHARACTERISTIC_MODEL_NUMBER_STRING = GattCharacteristic.UUID_CHARACTERISTIC_MODEL_NUMBER_STRING;
    public static final UUID UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING = GattCharacteristic.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING;
    public static final UUID UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING = GattCharacteristic.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING;
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING = GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING;
    public static final UUID UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING = GattCharacteristic.UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING;
    public static final UUID UUID_CHARACTERISTIC_SYSTEM_ID = GattCharacteristic.UUID_CHARACTERISTIC_SYSTEM_ID;
    public static final UUID UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST = GattCharacteristic.UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST;
    public static final UUID UUID_CHARACTERISTIC_PNP_ID = GattCharacteristic.UUID_CHARACTERISTIC_PNP_ID;
    private final DeviceInfo deviceInfo = new DeviceInfo();

    public DeviceInfoProfile(final T support) {
        super(support);
    }

    public void requestDeviceInfo(final TransactionBuilder builder) {
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_MODEL_NUMBER_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING))
                .read(getCharacteristic(UUID_CHARACTERISTIC_SYSTEM_ID))
                .read(getCharacteristic(UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST))
                .read(getCharacteristic(UUID_CHARACTERISTIC_PNP_ID));
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        final UUID charUuid = characteristic.getUuid();

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (charUuid.equals(UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING)) {
                handleManufacturerName(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_MODEL_NUMBER_STRING)) {
                handleModelNumber(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING)) {
                handleSerialNumber(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING)) {
                handleHardwareRevision(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING)) {
                handleFirmwareRevision(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING)) {
                handleSoftwareRevision(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_SYSTEM_ID)) {
                handleSystemId(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST)) {
                handleRegulatoryCertificationData(characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_PNP_ID)) {
                handlePnpId(characteristic);
                return true;
            }
        } else {
            if (charUuid.equals(UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_MODEL_NUMBER_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING) ||
                    charUuid.equals(UUID_CHARACTERISTIC_SYSTEM_ID) ||
                    charUuid.equals(UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST) ||
                    charUuid.equals(UUID_CHARACTERISTIC_PNP_ID)) {
                LOG.warn("error reading from characteristic: {}, status={}", GattCharacteristic.toString(characteristic), status);
            }
        }
        return false;
    }

    private void handleManufacturerName(final BluetoothGattCharacteristic characteristic) {
        String name = characteristic.getStringValue(0).trim();
        deviceInfo.setManufacturerName(name);
        notify(createIntent(deviceInfo));
    }

    private void handleModelNumber(final BluetoothGattCharacteristic characteristic) {
        String modelNumber = characteristic.getStringValue(0).trim();
        deviceInfo.setModelNumber(modelNumber);
        notify(createIntent(deviceInfo));
    }

    private void handleSerialNumber(final BluetoothGattCharacteristic characteristic) {
        String serialNumber = characteristic.getStringValue(0).trim();
        deviceInfo.setSerialNumber(serialNumber);
        notify(createIntent(deviceInfo));
    }

    private void handleHardwareRevision(final BluetoothGattCharacteristic characteristic) {
        String hardwareRevision = characteristic.getStringValue(0).trim();
        deviceInfo.setHardwareRevision(hardwareRevision);
        notify(createIntent(deviceInfo));
    }

    private void handleFirmwareRevision(final BluetoothGattCharacteristic characteristic) {
        String firmwareRevision = characteristic.getStringValue(0).trim();
        deviceInfo.setFirmwareRevision(firmwareRevision);
        notify(createIntent(deviceInfo));
    }

    private void handleSoftwareRevision(final BluetoothGattCharacteristic characteristic) {
        String softwareRevision = characteristic.getStringValue(0).trim();
        deviceInfo.setSoftwareRevision(softwareRevision);
        notify(createIntent(deviceInfo));
    }

    private void handleSystemId(final BluetoothGattCharacteristic characteristic) {
        String systemId = characteristic.getStringValue(0).trim();
        deviceInfo.setSystemId(systemId);
        notify(createIntent(deviceInfo));
    }

    private void handleRegulatoryCertificationData(final BluetoothGattCharacteristic characteristic) {
        // TODO: regulatory certification data list not supported yet
//        String regulatoryCertificationData = characteristic.getStringValue(0).trim();
//        deviceInfo.setRegulatoryCertificationDataList(regulatoryCertificationData);
//        notify(createIntent(deviceInfo));
    }

    private void handlePnpId(final BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length == 7) {
//            int vendorSource
//
//            deviceInfo.setPnpId(pnpId);
            notify(createIntent(deviceInfo));
        } else {
            // TODO: LOG warning
        }
    }

    private Intent createIntent(final DeviceInfo deviceInfo) {
        final Intent intent = new Intent(ACTION_DEVICE_INFO);
        intent.putExtra(EXTRA_DEVICE_INFO, deviceInfo); // TODO: broadcast a clone of the info
        return intent;
    }
}
