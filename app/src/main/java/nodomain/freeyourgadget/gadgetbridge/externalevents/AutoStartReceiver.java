package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class AutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = AutoStartReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (GBApplication.getGBPrefs().getAutoStart()) {
            Log.i(TAG, "Boot completed, starting Gadgetbridge");
            GBApplication.deviceService().start();
        }
    }
}
