package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.externalevents.K9Receiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;

public class GB {
    public static final int NOTIFICATION_ID = 1;
    private static final Logger LOG = LoggerFactory.getLogger(GB.class);
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;

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
        LOG.info("Setting broadcast receivers to: " + enable);
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

    public static boolean supportsBluetoothLE() {
        return GBApplication.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String hexdump(byte[] buffer, int offset, int length) {
        if (length == -1) {
            length = buffer.length;
        }
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

    public static boolean writeScreenshot(GBDeviceEventScreenshot screenshot, String filename) {

        LOG.info("Will write screenshot: " + screenshot.width + "x" + screenshot.height + "x" + screenshot.bpp + "bpp");
        final int FILE_HEADER_SIZE = 14;
        final int INFO_HEADER_SIZE = 40;

        File dir = GBApplication.getContext().getExternalFilesDir(null);
        if (dir != null) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        try (FileOutputStream fos = new FileOutputStream(dir + "/" + filename)) {
            ByteBuffer headerbuf = ByteBuffer.allocate(FILE_HEADER_SIZE + INFO_HEADER_SIZE + screenshot.clut.length);
            headerbuf.order(ByteOrder.LITTLE_ENDIAN);

            // file header
            headerbuf.put((byte) 'B');
            headerbuf.put((byte) 'M');
            headerbuf.putInt(0); // size in bytes (unconpressed = 0)
            headerbuf.putInt(0); // reserved
            headerbuf.putInt(FILE_HEADER_SIZE + INFO_HEADER_SIZE + screenshot.clut.length);

            // info header
            headerbuf.putInt(INFO_HEADER_SIZE);
            headerbuf.putInt(screenshot.width);
            headerbuf.putInt(-screenshot.height);
            headerbuf.putShort((short) 1); // planes
            headerbuf.putShort((short) 1); // bit count
            headerbuf.putInt(0); // compression
            headerbuf.putInt(0); // length of pixeldata in byte (uncompressed=0)
            headerbuf.putInt(0); // pixels per meter (x)
            headerbuf.putInt(0); // pixels per meter (y)
            headerbuf.putInt(2); // number of colors in CLUT
            headerbuf.putInt(2); // numbers of used colors
            headerbuf.put(screenshot.clut);
            fos.write(headerbuf.array());
            int rowbytes = screenshot.width / 8;
            byte[] pad = new byte[rowbytes % 4];
            for (int i = 0; i < screenshot.height; i++) {
                fos.write(screenshot.data, rowbytes * i, rowbytes);
                fos.write(pad);
            }
        } catch (IOException e) {
            LOG.error("Error saving screenshot", e);
            return false;
        }

        return true;
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     * @param message the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity) {
        toast(GBApplication.getContext(), message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     * @param message the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity, Throwable ex) {
        toast(GBApplication.getContext(), message, displayTime, severity, ex);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     * @param context the context to use
     * @param message the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity either INFO, WARNING, ERROR
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity) {
       toast(context, message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     * @param context the context to use
     * @param message the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity either INFO, WARNING, ERROR
     * @param ex optional exception to be logged
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity, final Throwable ex) {
        Looper mainLooper = Looper.getMainLooper();
        if (Thread.currentThread() == mainLooper.getThread()) {
            log(message, severity, ex);
            Toast.makeText(context, message, displayTime).show();
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    log(message, severity, ex);
                    Toast.makeText(context, message, displayTime).show();
                }
            };

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(runnable);
            } else {
                new Handler(mainLooper).post(runnable);
            }
        }
    }

    private static void log(String message, int severity, Throwable ex) {
        switch (severity) {
            case INFO:
                LOG.info(message, ex);
                break;
            case WARN:
                LOG.warn(message, ex);
                break;
            case ERROR:
                LOG.error(message, ex);
                break;
        }
    }
}
