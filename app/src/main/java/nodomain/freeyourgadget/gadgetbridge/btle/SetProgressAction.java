package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GB;

public class SetProgressAction extends PlainAction {

    private String text;
    private boolean ongoing;
    private int percentage;
    private final Context context;

    /**
     * When run, will update the progress notification.
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
        GB.updateInstallNotification(this.text, this.ongoing, this.percentage, this.context);
        return true;
    }
}
