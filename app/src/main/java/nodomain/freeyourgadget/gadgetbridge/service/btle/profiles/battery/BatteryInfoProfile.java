package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery;

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
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.ValueDecoder;

public class BatteryInfoProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoProfile.class);

    private static final String ACTION_PREFIX = BatteryInfoProfile.class.getName() + "_";

    public static final String ACTION_BATTERY_INFO = ACTION_PREFIX + "BATTERY_INFO";
    public static final String EXTRA_BATTERY_INFO = "BATTERY_INFO";

    public static final UUID SERVICE_UUID = GattService.UUID_SERVICE_BATTERY_SERVICE;

    public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL;
    private BatteryInfo batteryInfo;

    public BatteryInfoProfile(T support) {
        super(support);
    }

    public void requestBatteryInfo(TransactionBuilder builder) {
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_BATTERY_LEVEL));
    }

    public void enableNotifiy() {
        // TODO: notification
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                handleBatteryLevel(gatt, characteristic);
                return true;
            } else {
                LOG.info("Unexpected onCharacteristicRead: " + GattCharacteristic.toString(characteristic));
            }
        } else {
            LOG.warn("error reading from characteristic:" + GattCharacteristic.toString(characteristic));
        }
        return false;
    }

    private void handleBatteryLevel(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        int percent = ValueDecoder.decodePercent(characteristic);
        batteryInfo.setPercentCharged(percent);

        notify(createIntent(batteryInfo));
    }

    private Intent createIntent(BatteryInfo batteryInfo) {
        Intent intent = new Intent(ACTION_BATTERY_INFO);
        intent.putExtra(EXTRA_BATTERY_INFO, batteryInfo);
        return intent;
    }
}
