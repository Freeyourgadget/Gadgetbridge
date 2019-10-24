package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class TaskerPluginReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String min = intent.getStringExtra(TaskerPluginActivity.key_minute);
        String hour = intent.getStringExtra(TaskerPluginActivity.key_hours);
        String vibration = intent.getStringExtra(TaskerPluginActivity.key_vibration);

        int minDegrees = (int)Float.parseFloat(min);
        int hourDegrees = (int)Float.parseFloat(hour);

        PackageConfig config = new PackageConfig(
                (short)minDegrees,
                (short)hourDegrees,
                null,
                null,
                false,
                PlayNotificationRequest.VibrationType.fromValue(Byte.parseByte(vibration))
        );

        Intent send = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION);
        send.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(context).sendBroadcast(send);
    }
}
