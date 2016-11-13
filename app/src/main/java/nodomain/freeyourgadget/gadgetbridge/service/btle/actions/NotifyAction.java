package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.GattDescriptor.UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION;

/**
 * Enables or disables notifications for a given GATT characteristic.
 * The result will be made available asynchronously through the
 * {@link BluetoothGattCallback}.
 */
public class NotifyAction extends BtLEAction {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyAction.class);
    protected final boolean enableFlag;
    private boolean hasWrittenDescriptor = true;

    public NotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        super(characteristic);
        enableFlag = enable;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        boolean result = gatt.setCharacteristicNotification(getCharacteristic(), enableFlag);
        if (result) {
            BluetoothGattDescriptor notifyDescriptor = getCharacteristic().getDescriptor(UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION);
            if (notifyDescriptor != null) {
                int properties = getCharacteristic().getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    LOG.debug("use NOTIFICATION");
                    notifyDescriptor.setValue(enableFlag ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    result = gatt.writeDescriptor(notifyDescriptor);
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    LOG.debug("use INDICATION");
                    notifyDescriptor.setValue(enableFlag ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    result = gatt.writeDescriptor(notifyDescriptor);
                    hasWrittenDescriptor = true;
                } else {
                    hasWrittenDescriptor = false;
                }
            } else {
                LOG.warn("Descriptor CLIENT_CHARACTERISTIC_CONFIGURATION for characteristic " + getCharacteristic().getUuid() + " is null");
                hasWrittenDescriptor = false;
            }
        } else {
            hasWrittenDescriptor = false;
            LOG.error("Unable to enable notification for " + getCharacteristic().getUuid());
        }
        return result;
    }

    @Override
    public boolean expectsResult() {
        return hasWrittenDescriptor;
    }
}
