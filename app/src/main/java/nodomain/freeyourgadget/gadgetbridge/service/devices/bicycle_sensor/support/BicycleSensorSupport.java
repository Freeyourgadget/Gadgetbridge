package nodomain.freeyourgadget.gadgetbridge.service.devices.bicycle_sensor.support;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class BicycleSensorSupport extends BicycleSensorBaseSupport{
    public final static UUID UUID_BICYCLE_SENSOR_SERVICE =
            UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BICYCLE_SENSOR_CSC_MEASUREMENT =
            UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private static final Logger logger = LoggerFactory.getLogger(BicycleSensorSupport.class);

    public BicycleSensorSupport() {
        super(logger);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        BluetoothGattCharacteristic measurementCharacteristic =
                getCharacteristic(UUID_BICYCLE_SENSOR_CSC_MEASUREMENT);

        builder.add(new NotifyAction(measurementCharacteristic, true));

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    private void handleMeasurementCharacteristic(BluetoothGattCharacteristic characteristic){
        logger.debug(characteristic.getUuid().toString());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(UUID_BICYCLE_SENSOR_CSC_MEASUREMENT)){
            handleMeasurementCharacteristic(characteristic);
            return true;
        }
        return false;
    }
}
