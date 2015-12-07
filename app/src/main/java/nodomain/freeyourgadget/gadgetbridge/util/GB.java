package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;

public class GB {
    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_ID_INSTALL = 2;
    public static final int NOTIFICATION_ID_LOW_BATTERY = 3;
    public static final int NOTIFICATION_ID_TRANSFER = 4;

    private static final Logger LOG = LoggerFactory.getLogger(GB.class);
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static GBEnvironment environment;

    public static Notification createNotification(String text, Context context) {
        if (env().isLocalTest()) {
            return null;
        }
        Intent notificationIntent = new Intent(context, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(context.getString(R.string.app_name))
                .setTicker(text)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        if (GBApplication.isRunningLollipopOrLater()) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        return builder.build();
    }

    public static void updateNotification(String text, Context context) {
        Notification notification = createNotification(text, context);
        updateNotification(notification, NOTIFICATION_ID, context);
    }

    private static void updateNotification(@Nullable Notification notification, int id, Context context) {
        if (notification == null) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private static void removeNotification(int id, Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static boolean supportsBluetoothLE() {
        return GBApplication.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String hexdump(byte[] buffer, int offset, int length) {
        if (length == -1) {
            length = buffer.length - offset;
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

    public static String writeScreenshot(GBDeviceEventScreenshot screenshot, String filename) throws IOException {

        LOG.info("Will write screenshot: " + screenshot.width + "x" + screenshot.height + "x" + screenshot.bpp + "bpp");
        final int FILE_HEADER_SIZE = 14;
        final int INFO_HEADER_SIZE = 40;

        File dir = FileUtils.getExternalFilesDir();
        File outputFile = new File(dir, filename);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
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
            headerbuf.putShort((short) screenshot.bpp);
            headerbuf.putInt(0); // compression
            headerbuf.putInt(0); // length of pixeldata in byte (uncompressed=0)
            headerbuf.putInt(0); // pixels per meter (x)
            headerbuf.putInt(0); // pixels per meter (y)
            headerbuf.putInt(screenshot.clut.length / 4); // number of colors in CLUT
            headerbuf.putInt(0); // numbers of used colors
            headerbuf.put(screenshot.clut);
            fos.write(headerbuf.array());
            int rowbytes = (screenshot.width * screenshot.bpp) / 8;
            byte[] pad = new byte[rowbytes % 4];
            for (int i = 0; i < screenshot.height; i++) {
                fos.write(screenshot.data, rowbytes * i, rowbytes);
                fos.write(pad);
            }
        }
        return outputFile.getAbsolutePath();
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     *
     * @param message     the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity) {
        toast(GBApplication.getContext(), message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context.
     * Additionally the toast is logged using the provided severity.
     * Can be called from any thread.
     *
     * @param message     the message to display.
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(String message, int displayTime, int severity, Throwable ex) {
        toast(GBApplication.getContext(), message, displayTime, severity, ex);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     *
     * @param context     the context to use
     * @param message     the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity) {
        toast(context, message, displayTime, severity, null);
    }

    /**
     * Creates and display a Toast message using the application context
     * Can be called from any thread.
     *
     * @param context     the context to use
     * @param message     the message to display
     * @param displayTime something like Toast.LENGTH_SHORT
     * @param severity    either INFO, WARNING, ERROR
     * @param ex          optional exception to be logged
     */
    public static void toast(final Context context, final String message, final int displayTime, final int severity, final Throwable ex) {
        if (env().isLocalTest()) {
            return;
        }
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

    private static Notification createTransferNotification(String text, boolean ongoing,
                                                           int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);

        if (ongoing) {
            nb.setProgress(100, percentage, percentage == 0);
            nb.setSmallIcon(android.R.drawable.stat_sys_download);
        } else {
            nb.setProgress(0, 0, false);
            nb.setSmallIcon(android.R.drawable.stat_sys_download_done);
        }

        return nb.build();
    }

    public static void updateTransferNotification(String text, boolean ongoing, int percentage, Context context) {
        if (percentage == 100) {
            removeNotification(NOTIFICATION_ID_TRANSFER, context);
        } else {
            Notification notification = createTransferNotification(text, ongoing, percentage, context);
            updateNotification(notification, NOTIFICATION_ID_TRANSFER, context);
        }
    }

    private static Notification createInstallNotification(String text, boolean ongoing,
                                                          int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setTicker(text)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);

        if (ongoing) {
            nb.setProgress(100, percentage, percentage == 0);
            nb.setSmallIcon(android.R.drawable.stat_sys_upload);

        } else {
            nb.setSmallIcon(android.R.drawable.stat_sys_upload_done);
        }

        return nb.build();
    }

    public static void updateInstallNotification(String text, boolean ongoing, int percentage, Context context) {
        Notification notification = createInstallNotification(text, ongoing, percentage, context);
        updateNotification(notification, NOTIFICATION_ID_INSTALL, context);
    }

    private static Notification createBatteryNotification(String text, String bigText, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.notif_battery_low_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_low_battery)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        if (bigText != null) {
            nb.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        }

        return nb.build();
    }

    public static void updateBatteryNotification(String text, String bigText, Context context) {
        if (env().isLocalTest()) {
            return;
        }
        Notification notification = createBatteryNotification(text, bigText, context);
        updateNotification(notification, NOTIFICATION_ID_LOW_BATTERY, context);
    }

    public static GBEnvironment env() {
        return environment;
    }

    public static void assertThat(boolean condition, String errorMessage) {
        if (!condition) {
            throw new AssertionError(errorMessage);
        }
    }
}
