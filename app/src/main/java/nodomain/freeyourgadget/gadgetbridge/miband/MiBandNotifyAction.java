package nodomain.freeyourgadget.gadgetbridge.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.btle.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.miband.MiBandService;

/**
 * Enables or disables notifications for a given GATT characteristic.
 * The result will be made available asynchronously through the
 * {@link BluetoothGattCallback}.
 *
 * This class is Mi Band specific.
 */
public class MiBandNotifyAction extends NotifyAction {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);
    private boolean hasWrittenDescriptor = true;

    public MiBandNotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        super(characteristic, enable);
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        boolean result = super.run(gatt);
        if (result) {
            BluetoothGattDescriptor notifyDescriptor = getCharacteristic().getDescriptor(MiBandService.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION);
            if (notifyDescriptor != null) {
                int properties = getCharacteristic().getProperties();
                if ((properties & 0x10) > 0) {
                    LOG.debug("properties & 0x10 > 0");
                    notifyDescriptor.setValue(enableFlag ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    result = gatt.writeDescriptor(notifyDescriptor);
                } else if ((properties & 0x20) > 0){
                    LOG.debug("properties & 0x20 > 0");
                    notifyDescriptor.setValue(enableFlag ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    result = gatt.writeDescriptor(notifyDescriptor);
                    hasWrittenDescriptor = true;
                } else {
                    hasWrittenDescriptor = false;
                }
            } else {
                LOG.warn("sleep descriptor null");
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
