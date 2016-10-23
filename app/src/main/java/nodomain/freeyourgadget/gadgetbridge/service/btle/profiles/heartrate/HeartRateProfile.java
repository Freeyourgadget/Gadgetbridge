package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.heart_rate.xml
 */
public class HeartRateProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(HeartRateProfile.class);

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
        writeToControlPoint(new byte[] { value }, builder);
    }

    protected void writeToControlPoint(byte[] value, TransactionBuilder builder) {
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), value);
    }

    public void requestBodySensorLocation(TransactionBuilder builder) {

    }

    // TODO: I didn't find anything in the spec to request heart rate readings, so probably this
    // should be done in a device specific way.
    public void requestHeartRateMeasurement(TransactionBuilder builder) {
        writeToControlPoint(new byte[] { 0x15, 0x02, 0x01}, builder);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            GB.toast(getContext(), "Heart rate: " + heartRate, Toast.LENGTH_LONG, GB.INFO);
        }
        return false;
    }
}
