/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Felix Konstantin Maurer, Taavi Eomäe, Uwe Hermann, Yar

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.isRunningOreoOrLater;

public class GB {

    public static final String NOTIFICATION_CHANNEL_ID = "gadgetbridge";
    public static final String NOTIFICATION_CHANNEL_ID_TRANSFER = "gadgetbridge transfer";

    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_ID_INSTALL = 2;
    public static final int NOTIFICATION_ID_LOW_BATTERY = 3;
    public static final int NOTIFICATION_ID_TRANSFER = 4;
    public static final int NOTIFICATION_ID_EXPORT_FAILED = 5;

    private static final Logger LOG = LoggerFactory.getLogger(GB.class);
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static final String ACTION_DISPLAY_MESSAGE = "GB_Display_Message";
    public static final String DISPLAY_MESSAGE_MESSAGE = "message";
    public static final String DISPLAY_MESSAGE_DURATION = "duration";
    public static final String DISPLAY_MESSAGE_SEVERITY = "severity";

    private static PendingIntent getContentIntent(Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        return pendingIntent;
    }

    public static Notification createNotification(GBDevice device, Context context) {
        String deviceName = device.getName();
        String text = device.getStateString();
        if (device.getBatteryLevel() != GBDevice.BATTERY_UNKNOWN) {
            text += ": " + context.getString(R.string.battery) + " " + device.getBatteryLevel() + "%";
        }

        boolean connected = device.isInitialized();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(deviceName)
                .setTicker(deviceName + " - " + text)
                .setContentText(text)
                .setSmallIcon(connected ? R.drawable.ic_notification : R.drawable.ic_notification_disconnected)
                .setContentIntent(getContentIntent(context))
                .setColor(context.getResources().getColor(R.color.accent))
                .setOngoing(true);

        Intent deviceCommunicationServiceIntent = new Intent(context, DeviceCommunicationService.class);
        if (connected) {
            deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_DISCONNECT);
            PendingIntent disconnectPendingIntent = PendingIntent.getService(context, 0, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.drawable.ic_notification_disconnected, context.getString(R.string.controlcenter_disconnect), disconnectPendingIntent);
            if (GBApplication.isRunningLollipopOrLater() && DeviceHelper.getInstance().getCoordinator(device).supportsActivityDataFetching()) { //for some reason this fails on KK
                deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_FETCH_RECORDED_DATA);
                PendingIntent fetchPendingIntent = PendingIntent.getService(context, 1, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
                builder.addAction(R.drawable.ic_action_fetch_activity_data, context.getString(R.string.controlcenter_fetch_activity_data), fetchPendingIntent);
            }
        } else if (device.getState().equals(GBDevice.State.WAITING_FOR_RECONNECT) || device.getState().equals(GBDevice.State.NOT_CONNECTED)) {
            deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_CONNECT);
            deviceCommunicationServiceIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            PendingIntent reconnectPendingIntent = PendingIntent.getService(context, 2, deviceCommunicationServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_notification, context.getString(R.string.controlcenter_connect), reconnectPendingIntent);
        }
        if (GBApplication.isRunningLollipopOrLater()) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        if (GBApplication.minimizeNotification()) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }
        return builder.build();
    }

    public static Notification createNotification(String text, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setTicker(text)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_disconnected)
                .setContentIntent(getContentIntent(context))
                .setColor(context.getResources().getColor(R.color.accent))
                .setOngoing(true);
        if (GBApplication.getPrefs().getString("last_device_address", null) != null) {
            Intent deviceCommunicationServiceIntent = new Intent(context, DeviceCommunicationService.class);
            deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_CONNECT);
            PendingIntent reconnectPendingIntent = PendingIntent.getService(context, 2, deviceCommunicationServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.drawable.ic_notification, context.getString(R.string.controlcenter_connect), reconnectPendingIntent);
        }
        if (GBApplication.isRunningLollipopOrLater()) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        if (GBApplication.minimizeNotification()) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }
        return builder.build();
    }

    public static void updateNotification(GBDevice device, Context context) {
        Notification notification = createNotification(device, context);
        updateNotification(notification, NOTIFICATION_ID, context);
    }

    private static void updateNotification(@Nullable Notification notification, int id, Context context) {
        if (notification == null) {
            return;
        }
//      TODO: I believe it's better do always use the NMC instead of the old call, but old code works
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
//        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private static void removeNotification(int id, Context context) {
//      TODO: I believe it's better do always use the NMC instead of the old call, but old code works
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
//        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
            headerbuf.putInt(0); // size in bytes (uncompressed = 0)
            headerbuf.putInt(0); // reserved
            headerbuf.putInt(FILE_HEADER_SIZE + INFO_HEADER_SIZE + screenshot.clut.length);

            // info header
            headerbuf.putInt(INFO_HEADER_SIZE);
            headerbuf.putInt(screenshot.width);
            headerbuf.putInt(-screenshot.height);
            headerbuf.putShort((short) 1); // planes
            headerbuf.putShort((short) screenshot.bpp);
            headerbuf.putInt(0); // compression
            headerbuf.putInt(0); // length of pixeldata in bytes (uncompressed=0)
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
        log(message, severity, ex); // log immediately, not delayed
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Looper mainLooper = Looper.getMainLooper();
        if (Thread.currentThread() == mainLooper.getThread()) {
            Toast.makeText(context, message, displayTime).show();
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
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

    public static void log(String message, int severity, Throwable ex) {
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

    private static Notification createTransferNotification(String title, String text, boolean ongoing,
                                                           int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(isRunningOreoOrLater()) {
            NotificationChannel channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_TRANSFER);
            if(channel == null) {
                channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_TRANSFER,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(channel);
            }
        }
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_TRANSFER)
                .setTicker((title == null) ? context.getString(R.string.app_name) : title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle((title == null) ? context.getString(R.string.app_name) : title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
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

    public static void removeAllNotifications(Context context) {
        removeNotification(NOTIFICATION_ID_TRANSFER, context);
        removeNotification(NOTIFICATION_ID_INSTALL, context);
        removeNotification(NOTIFICATION_ID_LOW_BATTERY, context);
    }

    public static void updateTransferNotification(String title, String text, boolean ongoing, int percentage, Context context) {
        if (percentage == 100) {
            removeNotification(NOTIFICATION_ID_TRANSFER, context);
        } else {
            Notification notification = createTransferNotification(title, text, ongoing, percentage, context);
            updateNotification(notification, NOTIFICATION_ID_TRANSFER, context);
        }
    }

    private static Notification createInstallNotification(String text, boolean ongoing,
                                                          int percentage, Context context) {
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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
        Intent notificationIntent = new Intent(context, ControlCenterv2.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Notification notification = createBatteryNotification(text, bigText, context);
        updateNotification(notification, NOTIFICATION_ID_LOW_BATTERY, context);
    }

    public static void removeBatteryNotification(Context context) {
        removeNotification(NOTIFICATION_ID_LOW_BATTERY, context);
    }

    public static Notification createExportFailedNotification(String text, Context context) {
        Intent notificationIntent = new Intent(context, SettingsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notif_export_failed_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        return nb.build();
    }

    public static void updateExportFailedNotification(String text, Context context) {
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }
        Notification notification = createExportFailedNotification(text, context);
        updateNotification(notification, NOTIFICATION_ID_EXPORT_FAILED, context);
    }

    public static void removeExportFailedNotification(Context context) {
        removeNotification(NOTIFICATION_ID_EXPORT_FAILED, context);
    }


    public static void assertThat(boolean condition, String errorMessage) {
        if (!condition) {
            throw new AssertionError(errorMessage);
        }
    }
}
