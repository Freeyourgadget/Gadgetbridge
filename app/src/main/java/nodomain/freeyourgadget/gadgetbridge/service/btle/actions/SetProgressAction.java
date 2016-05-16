package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SetProgressAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetProgressAction.class);

    private final String text;
    private final boolean ongoing;
    private final int percentage;
    private final Context context;

    /**
     * When run, will update the progress notification.
     *
     * @param text
     * @param ongoing
     * @param percentage
     * @param context
     */

    public SetProgressAction(String text, boolean ongoing, int percentage, Context context) {
        this.text = text;
        this.ongoing = ongoing;
        this.percentage = percentage;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        LOG.info(toString());
        GB.updateInstallNotification(this.text, this.ongoing, this.percentage, this.context);
        return true;
    }

    @Override
    public String toString() {
        return getCreationTime() + ": " + getClass().getSimpleName() + ": " + text + "; " + percentage + "%";
    }
}
