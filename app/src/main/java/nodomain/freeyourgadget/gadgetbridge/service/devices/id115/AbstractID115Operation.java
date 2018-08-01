package nodomain.freeyourgadget.gadgetbridge.service.devices.id115;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractID115Operation extends AbstractBTLEOperation<ID115Support> {
    protected BluetoothGattCharacteristic controlCharacteristic = null;
    protected BluetoothGattCharacteristic notifyCharacteristic = null;

    protected AbstractID115Operation(ID115Support support) {
        super(support);

        if (isHealthOperation()) {
            controlCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_HEALTH);
            notifyCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_HEALTH);
        } else {
            controlCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_WRITE_NORMAL);
            notifyCharacteristic = getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL);
        }
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask("AbstractID115Operation starting..."); // mark as busy quickly to avoid interruptions from the outside
        TransactionBuilder builder = performInitialized("disabling some notifications");
        enableNotifications(builder, true);
        builder.queue(getQueue());
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            unsetBusy();
            try {
                TransactionBuilder builder = performInitialized("reenabling disabled notifications");
                enableNotifications(builder, false);
                builder.setGattCallback(null); // unset ourselves from being the queue's gatt callback
                builder.queue(getQueue());
            } catch (IOException ex) {
                GB.toast(getContext(), "Error enabling ID115 notifications, you may need to connect and disconnect", Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (notifyCharacteristic.getUuid().equals(characteristicUUID)) {
            handleResponse(characteristic.getValue());
            return true;
        } else {
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    void enableNotifications(TransactionBuilder builder, boolean enable) {
        if (isHealthOperation()) {
            builder.notify(getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_HEALTH), enable);
        } else {
            builder.notify(getCharacteristic(ID115Constants.UUID_CHARACTERISTIC_NOTIFY_NORMAL), enable);
        }
    }

    abstract boolean isHealthOperation();

    abstract void handleResponse(byte[] data);
}
