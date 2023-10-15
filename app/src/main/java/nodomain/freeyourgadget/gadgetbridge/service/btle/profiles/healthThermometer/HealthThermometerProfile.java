/*  Copyright (C) 2023 Alicia Hormann

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.healthThermometer;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.ValueDecoder;

/***
 * This class handles the HealthThermometer as implemented on the Femometer Vinca II.
 * This might or might not be up to GATT standard.
 * @param <T>
 */
public class HealthThermometerProfile <T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(HealthThermometerProfile.class);

    private static final String ACTION_PREFIX = HealthThermometerProfile.class.getName() + "_";

    public static final String ACTION_TEMPERATURE_INFO = ACTION_PREFIX + "TEMPERATURE_INFO";
    public static final String EXTRA_TEMPERATURE_INFO = "TEMPERATURE_INFO";

    public static final UUID SERVICE_UUID = GattService.UUID_SERVICE_HEALTH_THERMOMETER;
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT = GattCharacteristic.UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT;
    public static final UUID UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL = GattCharacteristic.UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL;
    private final TemperatureInfo temperatureInfo = new TemperatureInfo();

    public HealthThermometerProfile(T support) {
        super(support);
    }

    public void requestMeasurementInterval(TransactionBuilder builder) {
        builder.read(getCharacteristic(UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL));
    }

    public void setMeasurementInterval(TransactionBuilder builder, byte[] value) {
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL), value);
    }

    @Override
    public void enableNotify(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT), enable);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL)) {
                handleMeasurementInterval(gatt, characteristic);
                return true;
            } else if (charUuid.equals(UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT)) {
                handleTemperatureMeasurement(gatt, characteristic);
                return true;
            } else {
                LOG.info("Unexpected onCharacteristicRead: " + GattCharacteristic.toString(characteristic));
            }
        } else {
            LOG.warn("error reading from characteristic:" + GattCharacteristic.toString(characteristic));
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return onCharacteristicRead(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);
    }


    private void handleMeasurementInterval(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // todo: not implemented
        LOG.debug("Health thermometer received Measurement Interval: " + ValueDecoder.decodeInt(characteristic));
    }

    private void handleTemperatureMeasurement(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        /*
         * This metadata contains as bits:
         * the unit (celsius (0) or fahrenheit (1)) (bit 7 (last bit))
         * if a timestamp is present (1) or not present (0) (bit 6)
         * if a temperature type is present (1) or not present (0) (bit 5)
         */
        byte metadata = characteristic.getValue()[0];
        // todo: evaluate this byte to enable support for devices without timestamp or temperature-type

        int year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
        int month = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);
        int day = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 8);
        int hour = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9);
        int minute = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 10);
        int second = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 11);

        Calendar c = GregorianCalendar.getInstance();
        c.set(year, month - 1, day, hour, minute, second);
        Date date = c.getTime();

        float temperature = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1); // bytes 1 - 4
        int temperature_type = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 12); // encodes where the measurement was taken

        LOG.debug("Received measurement of " + temperature + "° with Timestamp " + date + ", metadata is " + Integer.toBinaryString((metadata & 0xFF) + 0x100).substring(1));

        temperatureInfo.setTemperature(temperature);
        temperatureInfo.setTemperatureType(temperature_type);
        temperatureInfo.setTimestamp(date);
        notify(createIntent(temperatureInfo));
    }

    private Intent createIntent(TemperatureInfo temperatureInfo) {
        Intent intent = new Intent(ACTION_TEMPERATURE_INFO);
        intent.putExtra(EXTRA_TEMPERATURE_INFO, temperatureInfo);
        return intent;
    }
}
