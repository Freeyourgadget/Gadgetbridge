package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;

class MibandIoThread extends GBDeviceIoThread {
    public MibandIoThread(GBDevice gbDevice, Context context) {
        super(gbDevice, context);
    }

    // implement connect() run() write() and quit() here
}