package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;


public class TimeChangeReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            Log.i(TAG, "Time or Timezone changed, syncing with device");
            Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
            startIntent.setAction(BluetoothCommunicationService.ACTION_SETTIME);
            context.startService(startIntent);
        }
    }
}