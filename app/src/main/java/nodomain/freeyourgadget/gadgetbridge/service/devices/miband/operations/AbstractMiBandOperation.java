package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import android.widget.Toast;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractMiBandOperation extends AbstractBTLEOperation<MiBandSupport> {
    protected AbstractMiBandOperation(MiBandSupport support) {
        super(support);
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        TransactionBuilder builder = performInitialized("disabling some notifications");
        enableOtherNotifications(builder, false);
        builder.queue(getQueue());
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            try {
                TransactionBuilder builder = performInitialized("reenabling disabled notifications");
                enableOtherNotifications(builder, true);
                builder.queue(getQueue());
            } catch (IOException ex) {
                GB.toast(getContext(), "Error enabling Mi Band notifications, you may need to connect and disconnect", Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }
    }

    /**
     * Enables or disables certain kinds of notifications that could interfere with this
     * operation. Call this method once initially to disable other notifications, and once
     * when this operation has finished.
     * @param builder
     * @param enable true to enable, false to disable the other notifications
     */
    protected void enableOtherNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);
    }
}
