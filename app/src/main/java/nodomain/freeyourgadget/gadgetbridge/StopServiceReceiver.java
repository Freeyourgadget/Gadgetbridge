package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stopIntent = new Intent(context, BluetoothCommunicationService.class);
        context.stopService(stopIntent);

        Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);
        context.sendBroadcast(quitIntent);
    }
}
