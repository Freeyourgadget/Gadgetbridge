package nodomain.freeyourgadget.gadgetbridge.service.btle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;

/**
 * A special action that is executed at the very front of the initialization
 * sequence (transaction). It will abort the entire initialization sequence
 * by returning false, when the device is already initialized.
 */
public class CheckInitializedAction extends AbortTransactionAction {
    private static final Logger LOG = LoggerFactory.getLogger(CheckInitializedAction.class);

    private final GBDevice device;

    public CheckInitializedAction(GBDevice gbDevice) {
        device = gbDevice;
    }

    @Override
    protected boolean shouldAbort() {
        boolean abort = device.isInitialized();
        if (abort) {
            LOG.info("Aborting device initialization, because already initialized: " + device);
        }
        return abort;
    }
}
