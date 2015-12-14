package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBCallControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBMusicControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

// TODO: support option for a single reminder notification when notifications could not be delivered?
// conditions: app was running and received notifications, but device was not connected.
// maybe need to check for "unread notifications" on device for that.

/**
 * Abstract implementation of DeviceSupport with some implementations for
 * common functionality. Still transport independent.
 */
public abstract class AbstractDeviceSupport implements DeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);
    private static final int NOTIFICATION_ID_SCREENSHOT = 8000;

    protected GBDevice gbDevice;
    private BluetoothAdapter btAdapter;
    private Context context;

    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    @Override
    public boolean isConnected() {
        return gbDevice.isConnected();
    }

    /**
     * Returns true if the device is not only connected, but also
     * initialized.
     *
     * @see GBDevice#isInitialized()
     */
    protected boolean isInitialized() {
        return gbDevice.isInitialized();
    }

    @Override
    public GBDevice getDevice() {
        return gbDevice;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof GBDeviceEventMusicControl) {
            handleGBDeviceEvent((GBDeviceEventMusicControl) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventCallControl) {
            handleGBDeviceEvent((GBDeviceEventCallControl) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventVersionInfo) {
            handleGBDeviceEvent((GBDeviceEventVersionInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventAppInfo) {
            handleGBDeviceEvent((GBDeviceEventAppInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventSleepMonitorResult) {
            handleGBDeviceEvent((GBDeviceEventSleepMonitorResult) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventScreenshot) {
            handleGBDeviceEvent((GBDeviceEventScreenshot) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventNotificationControl) {
            handleGBDeviceEvent((GBDeviceEventNotificationControl) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventBatteryInfo) {
            handleGBDeviceEvent((GBDeviceEventBatteryInfo) deviceEvent);
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventMusicControl musicEvent) {
        Context context = getContext();
        LOG.info("Got event for MUSIC_CONTROL");
        Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
        musicIntent.putExtra("event", musicEvent.event.ordinal());
        musicIntent.setPackage(context.getPackageName());
        context.sendBroadcast(musicIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventCallControl callEvent) {
        Context context = getContext();
        LOG.info("Got event for CALL_CONTROL");
        Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
        callIntent.putExtra("event", callEvent.event.ordinal());
        callIntent.setPackage(context.getPackageName());
        context.sendBroadcast(callIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventVersionInfo infoEvent) {
        Context context = getContext();
        LOG.info("Got event for VERSION_INFO");
        if (gbDevice == null) {
            return;
        }
        gbDevice.setFirmwareVersion(infoEvent.fwVersion);
        gbDevice.setHardwareVersion(infoEvent.hwVersion);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    public void handleGBDeviceEvent(GBDeviceEventAppInfo appInfoEvent) {
        Context context = getContext();
        LOG.info("Got event for APP_INFO");

        Intent appInfoIntent = new Intent(AppManagerActivity.ACTION_REFRESH_APPLIST);
        int appCount = appInfoEvent.apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (Integer i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i.toString(), appInfoEvent.apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i.toString(), appInfoEvent.apps[i].getCreator());
            appInfoIntent.putExtra("app_uuid" + i.toString(), appInfoEvent.apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i.toString(), appInfoEvent.apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }

    public void handleGBDeviceEvent(GBDeviceEventSleepMonitorResult sleepMonitorResult) {
        Context context = getContext();
        LOG.info("Got event for SLEEP_MONIOR_RES");
        Intent sleepMontiorIntent = new Intent(ChartsHost.REFRESH);
        sleepMontiorIntent.putExtra("smartalarm_from", sleepMonitorResult.smartalarm_from);
        sleepMontiorIntent.putExtra("smartalarm_to", sleepMonitorResult.smartalarm_to);
        sleepMontiorIntent.putExtra("recording_base_timestamp", sleepMonitorResult.recording_base_timestamp);
        sleepMontiorIntent.putExtra("alarm_gone_off", sleepMonitorResult.alarm_gone_off);

        LocalBroadcastManager.getInstance(context).sendBroadcast(sleepMontiorIntent);
    }

    private void handleGBDeviceEvent(GBDeviceEventScreenshot screenshot) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");
        String filename = "screenshot_" + dateFormat.format(new Date()) + ".bmp";

        try {
            String fullpath = GB.writeScreenshot(screenshot, filename);
            Bitmap bmp = BitmapFactory.decodeFile(fullpath);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(fullpath)), "image/*");

            PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(fullpath)));

            PendingIntent pendingShareIntent = PendingIntent.getActivity(context, 0, Intent.createChooser(shareIntent, "share screenshot"),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notif = new Notification.Builder(context)
                    .setContentTitle("Screenshot taken")
                    .setTicker("Screenshot taken")
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setStyle(new Notification.BigPictureStyle()
                            .bigPicture(bmp))
                    .setContentIntent(pIntent)
                    .addAction(android.R.drawable.ic_menu_share, "share", pendingShareIntent)
                    .build();


            notif.flags |= Notification.FLAG_AUTO_CANCEL;

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID_SCREENSHOT, notif);
        } catch (IOException ex) {
            LOG.error("Error writing screenshot", ex);
        }
    }

    private void handleGBDeviceEvent(GBDeviceEventNotificationControl deviceEvent) {
        Context context = getContext();
        LOG.info("Got NOTIFICATION CONTROL device event");
        String action = null;
        switch (deviceEvent.event) {
            case DISMISS:
                action = NotificationListener.ACTION_DISMISS;
                break;
            case DISMISS_ALL:
                action = NotificationListener.ACTION_DISMISS_ALL;
                break;
            case OPEN:
                action = NotificationListener.ACTION_OPEN;
                break;
            case MUTE:
                action = NotificationListener.ACTION_MUTE;
                break;
            case REPLY:
                String phoneNumber = GBApplication.getIDSenderLookup().lookup(deviceEvent.handle);
                if (phoneNumber != null) {
                    GB.toast(context, "got notfication reply for  " + phoneNumber + " : " + deviceEvent.reply, 2, GB.INFO);
                }
                break;
        }
        if (action != null) {
            Intent notificationListenerIntent = new Intent(action);
            notificationListenerIntent.putExtra("handle", deviceEvent.handle);
            LocalBroadcastManager.getInstance(context).sendBroadcast(notificationListenerIntent);
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventBatteryInfo deviceEvent) {
        Context context = getContext();
        LOG.info("Got BATTERY_INFO device event");
        gbDevice.setBatteryLevel(deviceEvent.level);
        gbDevice.setBatteryState(deviceEvent.state);

        //show the notification if the battery level is below threshold and only if not connected to charger
        if (deviceEvent.level <= gbDevice.getBatteryThresholdPercent() &&
                (BatteryState.BATTERY_LOW.equals(deviceEvent.state) ||
                        BatteryState.BATTERY_NORMAL.equals(deviceEvent.state))
                ) {
            GB.updateBatteryNotification(context.getString(R.string.notif_battery_low_percent, gbDevice.getName(), deviceEvent.level),
                    deviceEvent.extendedInfoAvailable() ?
                            context.getString(R.string.notif_battery_low_percent, gbDevice.getName(), deviceEvent.level) + "\n" +
                                    context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(deviceEvent.lastChargeTime.getTime())) +
                                    context.getString(R.string.notif_battery_low_bigtext_number_of_charges, deviceEvent.numCharges)
                            : ""
                    , context);
        }

        gbDevice.sendDeviceUpdateIntent(context);
    }

}
