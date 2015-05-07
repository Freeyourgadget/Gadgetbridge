package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.externalevents.K9Receiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;

public class GB {
    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = "GB";

    public static final String PREF_DEVELOPMENT_MIBAND_ADDRESS = "development_miaddr";

    public static Notification createNotification(String text, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        return new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setTicker(text)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
    }

    public static void updateNotification(String text, Context context) {
        Notification notification = createNotification(text, context);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    public static void setReceiversEnableState(boolean enable, Context context) {
        Log.i(TAG, "Setting broadcast receivers to: " + enable);
        final Class<?>[] receiverClasses = {
                PhoneCallReceiver.class,
                SMSReceiver.class,
                K9Receiver.class,
                PebbleReceiver.class,
                MusicPlaybackReceiver.class,
                TimeChangeReceiver.class,
                //NotificationListener.class, // disabling this leads to loss of permission to read notifications
        };

        int newState;

        if (enable) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }

        PackageManager pm = context.getPackageManager();

        for (Class<?> receiverClass : receiverClasses) {
            ComponentName compName = new ComponentName(context, receiverClass);

            pm.setComponentEnabledSetting(compName, newState, PackageManager.DONT_KILL_APP);
        }
    }

    static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static String hexdump(byte[] buffer, int offset, int length) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = buffer[i + offset] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String formatRssi(short rssi) {
        return String.valueOf(rssi);
    }
}
