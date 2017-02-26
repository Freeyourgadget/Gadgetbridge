package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;

public abstract class StopNotificationAction extends AbortTransactionAction {

    private final BluetoothGattCharacteristic alertLevelCharacteristic;

    public StopNotificationAction(BluetoothGattCharacteristic alertLevelCharacteristic) {
        this.alertLevelCharacteristic = alertLevelCharacteristic;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        if (!super.run(gatt)) {
            // send a signal to stop the vibration
            alertLevelCharacteristic.setValue(new byte[]{MiBand2Service.ALERT_LEVEL_NONE});
            gatt.writeCharacteristic(alertLevelCharacteristic);
            return false;
        }
        return true;
    }
};

