package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A special action that checks for an abort-condition, and if met, the currently
 * executing transaction will be aborted by returning false.
 */
public abstract class AbortTransactionAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(AbortTransactionAction.class);

    public AbortTransactionAction() {
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        if (shouldAbort()) {
            LOG.info("Aborting transaction because abort criteria met.");
            return false;
        }
        return true;
    }

    protected abstract boolean shouldAbort();
}
