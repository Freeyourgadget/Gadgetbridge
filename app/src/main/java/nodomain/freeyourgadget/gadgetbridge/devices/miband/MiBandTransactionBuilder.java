package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class MiBandTransactionBuilder extends TransactionBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandTransactionBuilder.class);

    public MiBandTransactionBuilder(String taskName) {
        super(taskName);
    }

    @Override
    protected NotifyAction createNotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        return new NotifyAction(characteristic, enable);
    }
}
