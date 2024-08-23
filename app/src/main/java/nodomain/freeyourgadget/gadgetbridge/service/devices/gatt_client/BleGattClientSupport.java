package nodomain.freeyourgadget.gadgetbridge.service.devices.gatt_client;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class BleGattClientSupport extends AbstractBTLEDeviceSupport {
    public static final Logger logger = LoggerFactory.getLogger(BleGattClientSupport.class);

    public BleGattClientSupport() {
        super(logger);

        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
            batteryInfo.level = characteristic.getValue()[0];
            handleGBDeviceEvent(batteryInfo);
        }else if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING)) {
            String firmwareVersion = characteristic.getStringValue(0);
            getDevice().setFirmwareVersion(firmwareVersion);
            getDevice().sendDeviceUpdateIntent(getContext());
        }
        return super.onCharacteristicRead(gatt, characteristic, status);
    }

    void readCharacteristicIfAvailable(UUID characteristicUUID, TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(characteristicUUID);
        if(characteristic == null) {
            return;
        }

        logger.debug("found characteristic {}", characteristicUUID);
        builder.read(characteristic);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        readCharacteristicIfAvailable(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL, builder);
        readCharacteristicIfAvailable(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING, builder);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }
}
