package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.heart_rate.xml
 */
public class HeartRateProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    /**
     * Returned when a request to the heart rate control point is not supported by the device
     */
    public static final int ERR_CONTROL_POINT_NOT_SUPPORTED = 0x80;

    public HeartRateProfile(T support) {
        super(support);
    }

    public void resetEnergyExpended(TransactionBuilder builder) {
        writeToControlPoint((byte) 0x01, builder);
    }

    protected void writeToControlPoint(byte value, TransactionBuilder builder) {
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), new byte[] { value });
    }

    public void requestBodySensorLocation(TransactionBuilder builder) {

    }

    public void requestHeartRateMeasurement(TransactionBuilder builder) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            int flag = characteristic.getProperties();
//            int format = -1;
//            if ((flag & 0x01) != 0) {
//                format = BluetoothGattCharacteristic.FORMAT_UINT16;
//            } else {
//                format = BluetoothGattCharacteristic.FORMAT_UINT8;
//            }
//            final int heartRate = characteristic.getIntValue(format, 1);
//        }
        return false;
    }
}
