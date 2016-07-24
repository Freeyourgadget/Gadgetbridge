package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;

public class ValueDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(ValueDecoder.class);

    public static int decodePercent(BluetoothGattCharacteristic characteristic) {
        int percent = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (percent > 100 || percent < 0) {
            LOG.warn("Unexpected percent value: " + percent + ": " + GattCharacteristic.toString(characteristic));
            percent = Math.max(100, Math.min(0, percent));
        }
        return percent;
    }
}
