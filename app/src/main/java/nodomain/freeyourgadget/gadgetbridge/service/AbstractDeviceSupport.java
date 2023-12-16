/*  Copyright (C) 2015-2023 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Daniele Gobbetti, José Rebelo, Pauli Salmenrinne, Sebastian Kranz,
    Taavi Eomäe, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.FindPhoneActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFmFrequency;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepStateDetection;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSilentMode;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventLEDColor;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventWearState;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevel;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.SleepState;
import nodomain.freeyourgadget.gadgetbridge.model.WearingState;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBCallControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBMusicControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.SilentMode;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;

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
    private boolean autoReconnect;



    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    /**
     * Default implementation just calls #connect()
     */
    @Override
    public boolean connectFirstTime() {
        return connect();
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
    public void setAutoReconnect(boolean enable) {
        autoReconnect = enable;
    }

    @Override
    public boolean getAutoReconnect() {
        return autoReconnect;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
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
        } else if (deviceEvent instanceof GBDeviceEventScreenshot) {
            handleGBDeviceEvent((GBDeviceEventScreenshot) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventNotificationControl) {
            handleGBDeviceEvent((GBDeviceEventNotificationControl) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventBatteryInfo) {
            handleGBDeviceEvent((GBDeviceEventBatteryInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventFindPhone) {
            handleGBDeviceEvent((GBDeviceEventFindPhone) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventLEDColor) {
            handleGBDeviceEvent((GBDeviceEventLEDColor) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventUpdateDeviceInfo) {
            handleGBDeviceEvent((GBDeviceEventUpdateDeviceInfo) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventUpdatePreferences) {
            handleGBDeviceEvent((GBDeviceEventUpdatePreferences) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventUpdateDeviceState) {
            handleGBDeviceEvent((GBDeviceEventUpdateDeviceState) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventSilentMode) {
            handleGBDeviceEvent((GBDeviceEventSilentMode) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventFmFrequency) {
            handleGBDeviceEvent((GBDeviceEventFmFrequency) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventWearState) {
            handleGBDeviceEvent((GBDeviceEventWearState) deviceEvent);
        } else if (deviceEvent instanceof GBDeviceEventSleepStateDetection) {
            handleGBDeviceEvent((GBDeviceEventSleepStateDetection) deviceEvent);
        }
    }

    private void handleGBDeviceEvent(GBDeviceEventSilentMode deviceEvent) {
        LOG.info("Got GBDeviceEventSilentMode: enabled = {}", deviceEvent.isEnabled());

        SilentMode.setPhoneSilentMode(getDevice().getAddress(), deviceEvent.isEnabled());
    }

    private void handleGBDeviceEvent(final GBDeviceEventFindPhone deviceEvent) {
        final Context context = getContext();
        LOG.info("Got GBDeviceEventFindPhone: {}", deviceEvent.event);
        switch (deviceEvent.event) {
            case START:
                handleGBDeviceEventFindPhoneStart(true);
                break;
            case START_VIBRATE:
                handleGBDeviceEventFindPhoneStart(false);
                break;
            case VIBRATE:
                final Intent intentVibrate = new Intent(FindPhoneActivity.ACTION_VIBRATE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentVibrate);
                break;
            case RING:
                final Intent intentRing = new Intent(FindPhoneActivity.ACTION_RING);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentRing);
                break;
            case STOP:
                final Intent intentStop = new Intent(FindPhoneActivity.ACTION_FOUND);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentStop);
                break;
            default:
                LOG.warn("unknown GBDeviceEventFindPhone");
        }
    }

    private void handleGBDeviceEventFindPhoneStart(final boolean ring) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // this could be used if app in foreground // TODO: Below Q?
            Intent startIntent = new Intent(getContext(), FindPhoneActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra(FindPhoneActivity.EXTRA_RING, ring);
            context.startActivity(startIntent);
        } else {
            handleGBDeviceEventFindPhoneStartNotification(ring);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private void handleGBDeviceEventFindPhoneStartNotification(final boolean ring) {
        LOG.info("Got handleGBDeviceEventFindPhoneStartNotification");
        Intent intent = new Intent(context, FindPhoneActivity.class);
        intent.putExtra(FindPhoneActivity.EXTRA_RING, ring);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntentUtils.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID )
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(false)
                .setFullScreenIntent(pi, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentTitle(  context.getString( R.string.find_my_phone_notification ) );

        notification.setGroup("BackgroundService");

        CompanionDeviceManager manager = (CompanionDeviceManager) context.getSystemService(Context.COMPANION_DEVICE_SERVICE);
        if (manager.getAssociations().size() > 0) {
            GB.notify(GB.NOTIFICATION_ID_PHONE_FIND, notification.build(), context);
            context.startActivity(intent);
            LOG.debug("CompanionDeviceManager associations were found, starting intent");
        } else {
            GB.notify(GB.NOTIFICATION_ID_PHONE_FIND, notification.build(), context);
            LOG.warn("CompanionDeviceManager associations were not found, can't start intent");
        }
    }


    private void handleGBDeviceEvent(GBDeviceEventMusicControl musicEvent) {
        Context context = getContext();
        LOG.info("Got event for MUSIC_CONTROL");
        Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
        musicIntent.putExtra("event", musicEvent.event.ordinal());
        musicIntent.setPackage(context.getPackageName());
        context.sendBroadcast(musicIntent);
    }

    private void handleGBDeviceEvent(GBDeviceEventCallControl callEvent) {
        Context context = getContext();
        LOG.info("Got event for CALL_CONTROL");
        if(callEvent.event == GBDeviceEventCallControl.Event.IGNORE) {
            LOG.info("Sending intent for mute");
            Intent broadcastIntent = new Intent(context.getPackageName() + ".MUTE_CALL");
            broadcastIntent.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastIntent);
            return;
        }
        Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
        callIntent.putExtra("event", callEvent.event.ordinal());
        callIntent.setPackage(context.getPackageName());
        context.sendBroadcast(callIntent);
    }

    protected void handleGBDeviceEvent(GBDeviceEventVersionInfo infoEvent) {
        Context context = getContext();
        LOG.info("Got event for VERSION_INFO: " + infoEvent);
        if (gbDevice == null) {
            return;
        }
        if (infoEvent.fwVersion != null) {
            gbDevice.setFirmwareVersion(infoEvent.fwVersion);
        }
        if (infoEvent.fwVersion2 != null) {
            gbDevice.setFirmwareVersion2(infoEvent.fwVersion2);
        }
        gbDevice.setModel(infoEvent.hwVersion);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    protected void handleGBDeviceEvent(GBDeviceEventLEDColor colorEvent) {
        Context context = getContext();
        LOG.info("Got event for LED Color: #" + Integer.toHexString(colorEvent.color).toUpperCase(Locale.ROOT));
        if (gbDevice == null) {
            return;
        }
        gbDevice.setExtraInfo("led_color", colorEvent.color);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    protected void handleGBDeviceEvent(GBDeviceEventUpdateDeviceInfo itemEvent) {
        if (gbDevice == null) {
            return;
        }

        gbDevice.addDeviceInfo(itemEvent.item);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    protected void handleGBDeviceEvent(GBDeviceEventUpdatePreferences savePreferencesEvent) {
        if (gbDevice == null) {
            return;
        }

        savePreferencesEvent.update(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        gbDevice.sendDeviceUpdateIntent(context);
    }

    protected void handleGBDeviceEvent(GBDeviceEventUpdateDeviceState updateDeviceState) {
        if (gbDevice == null) {
            return;
        }

        gbDevice.setState(updateDeviceState.state);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    protected void handleGBDeviceEvent(GBDeviceEventFmFrequency frequencyEvent) {
        Context context = getContext();
        LOG.info("Got event for FM Frequency");
        if (gbDevice == null) {
            return;
        }
        gbDevice.setExtraInfo("fm_frequency", frequencyEvent.frequency);
        gbDevice.sendDeviceUpdateIntent(context);
    }

    private void handleGBDeviceEvent(GBDeviceEventAppInfo appInfoEvent) {
        Context context = getContext();
        LOG.info("Got event for APP_INFO");

        Intent appInfoIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
        int appCount = appInfoEvent.apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (int i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i, appInfoEvent.apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i, appInfoEvent.apps[i].getCreator());
            appInfoIntent.putExtra("app_version" + i, appInfoEvent.apps[i].getVersion());
            appInfoIntent.putExtra("app_uuid" + i, appInfoEvent.apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i, appInfoEvent.apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }

    private void handleGBDeviceEvent(GBDeviceEventScreenshot screenshot) {
        if (screenshot.getData() == null) {
            LOG.warn("Screnshot data is null");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.US);
        String filename = "screenshot_" + dateFormat.format(new Date()) + ".bmp";

        try {
            String fullpath = GB.writeScreenshot(screenshot, filename);
            Bitmap bmp = BitmapFactory.decodeFile(fullpath);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            Uri screenshotURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".screenshot_provider", new File(fullpath));
            intent.setDataAndType(screenshotURI, "image/*");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            PendingIntent pIntent = PendingIntentUtils.getActivity(context, 0, intent, 0, false);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, screenshotURI);

            PendingIntent pendingShareIntent = PendingIntentUtils.getActivity(context, 0, Intent.createChooser(shareIntent, context.getString(R.string.share_screenshot)),
                    PendingIntent.FLAG_UPDATE_CURRENT, false);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_share, context.getString(R.string.share), pendingShareIntent).build();

            Notification notif = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                    .setContentTitle(context.getString(R.string.screenshot_taken))
                    .setTicker(context.getString(R.string.screenshot_taken))
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bmp))
                    .setContentIntent(pIntent)
                    .addAction(action)
                    .setAutoCancel(true)
                    .build();

            GB.notify(NOTIFICATION_ID_SCREENSHOT, notif, context);
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
                if (deviceEvent.phoneNumber == null) {
                    deviceEvent.phoneNumber = GBApplication.getIDSenderLookup().lookup((int) (deviceEvent.handle >> 4));
                }
                if (deviceEvent.phoneNumber != null) {
                    LOG.info("Got notification reply for SMS from " + deviceEvent.phoneNumber + " : " + deviceEvent.reply);
                    SmsManager.getDefault().sendTextMessage(deviceEvent.phoneNumber, null, deviceEvent.reply, null, null);
                } else {
                    LOG.info("Got notification reply for notification id " + deviceEvent.handle + " : " + deviceEvent.reply);
                    action = NotificationListener.ACTION_REPLY;
                }
                break;
        }
        if (action != null) {
            Intent notificationListenerIntent = new Intent(action);
            notificationListenerIntent.putExtra("handle", deviceEvent.handle);
            notificationListenerIntent.putExtra("title", deviceEvent.title);
            if (deviceEvent.reply != null) {
                SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
                String suffix = prefs.getString("canned_reply_suffix", null);
                if (suffix != null && !Objects.equals(suffix, "")) {
                    deviceEvent.reply += suffix;
                }
                notificationListenerIntent.putExtra("reply", deviceEvent.reply);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(notificationListenerIntent);
        }
    }

    protected void handleGBDeviceEvent(GBDeviceEventBatteryInfo deviceEvent) {
        Context context = getContext();
        LOG.info("Got BATTERY_INFO device event");
        gbDevice.setBatteryLevel(deviceEvent.level, deviceEvent.batteryIndex);
        gbDevice.setBatteryState(deviceEvent.state);
        gbDevice.setBatteryVoltage(deviceEvent.voltage, deviceEvent.batteryIndex);

        if (deviceEvent.level == GBDevice.BATTERY_UNKNOWN) {
            // no level available, just "high" or "low"
            if (BatteryState.BATTERY_LOW.equals(deviceEvent.state)) {
                GB.updateBatteryNotification(context.getString(R.string.notif_battery_low, gbDevice.getAliasOrName()),
                        deviceEvent.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_extended, gbDevice.getAliasOrName(),
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(deviceEvent.lastChargeTime.getTime())) +
                                        context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(deviceEvent.numCharges)))
                                : ""
                        , context);
            } else {
                GB.removeBatteryNotification(context);
            }
        } else {
            createStoreTask("Storing battery data", context, deviceEvent).execute();

            //show the notification if the battery level is below threshold and only if not connected to charger
            if (deviceEvent.level <= gbDevice.getBatteryThresholdPercent() &&
                    (BatteryState.BATTERY_LOW.equals(deviceEvent.state) ||
                            BatteryState.BATTERY_NORMAL.equals(deviceEvent.state))
                    ) {
                GB.updateBatteryNotification(context.getString(R.string.notif_battery_low_percent, gbDevice.getAliasOrName(), String.valueOf(deviceEvent.level)),
                        deviceEvent.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_percent, gbDevice.getAliasOrName(), String.valueOf(deviceEvent.level)) + "\n" +
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(deviceEvent.lastChargeTime.getTime())) +
                                        context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(deviceEvent.numCharges))
                                : ""
                        , context);
            } else {
                GB.removeBatteryNotification(context);
            }
        }

        gbDevice.sendDeviceUpdateIntent(context);
    }

    /**
     * Helper method to run specific actions configured in the device preferences, upon wear state
     * or awake/asleep events.
     *
     * @param actions
     * @param message
     */
    private void handleDeviceAction(Set<String> actions, String message) {
        if (actions.isEmpty()) {
            return;
        }

        LOG.debug("Handing device actions: {}", TextUtils.join(",", actions));

        final String actionBroadcast = getContext().getString(R.string.pref_device_action_broadcast_value);
        final String actionFitnessControlStart = getContext().getString(R.string.pref_device_action_fitness_app_control_start_value);
        final String actionFitnessControlStop = getContext().getString(R.string.pref_device_action_fitness_app_control_stop_value);
        final String actionFitnessControlToggle = getContext().getString(R.string.pref_device_action_fitness_app_control_toggle_value);
        final String actionMediaPlay = getContext().getString(R.string.pref_media_play_value);
        final String actionMediaPause = getContext().getString(R.string.pref_media_pause_value);
        final String actionMediaPlayPause = getContext().getString(R.string.pref_media_playpause_value);
        final String actionDndOff = getContext().getString(R.string.pref_device_action_dnd_off_value);
        final String actionDndpriority = getContext().getString(R.string.pref_device_action_dnd_priority_value);
        final String actionDndAlarms = getContext().getString(R.string.pref_device_action_dnd_alarms_value);
        final String actionDndOn = getContext().getString(R.string.pref_device_action_dnd_on_value);

        if (actions.contains(actionBroadcast)) {
            if (message != null) {
                Intent in = new Intent();
                in.setAction(message);
                LOG.info("Sending broadcast {}", message);
                getContext().getApplicationContext().sendBroadcast(in);
            }
        }

        if (actions.contains(actionFitnessControlStart)) {
            OpenTracksController.startRecording(getContext());
        } else if (actions.contains(actionFitnessControlStop)) {
            OpenTracksController.stopRecording(getContext());
        } else if (actions.contains(actionFitnessControlToggle)) {
            OpenTracksController.toggleRecording(getContext());
        }

        final String mediaAction;
        if (actions.contains(actionMediaPlayPause)) {
            mediaAction = actionMediaPlayPause;
        } else if (actions.contains(actionMediaPause)) {
            mediaAction = actionMediaPause;
        } else if (actions.contains(actionMediaPlay)) {
            mediaAction = actionMediaPlay;
        } else {
            mediaAction = null;
        }

        if (mediaAction != null) {
            GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.valueOf(mediaAction);
            evaluateGBDeviceEvent(deviceEventMusicControl);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final int interruptionFilter;
            if (actions.contains(actionDndOff)) {
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
            } else if (actions.contains(actionDndpriority)) {
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY;
            } else if (actions.contains(actionDndAlarms)) {
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALARMS;
            } else if (actions.contains(actionDndOn)) {
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE;
            } else {
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
            }

            if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                LOG.debug("Setting do not disturb to {}", interruptionFilter);

                if (!notificationManager.isNotificationPolicyAccessGranted()) {
                    LOG.warn("Do not disturb permissions not granted");
                }

                notificationManager.setInterruptionFilter(interruptionFilter);
            }
        }
    }

    private void handleGBDeviceEvent(GBDeviceEventSleepStateDetection event) {
        LOG.debug("Got SLEEP_STATE_DETECTION device event, detected sleep state = {}", event.sleepState);

        if (event.sleepState == SleepState.UNKNOWN) {
            return;
        }

        String actionPreferenceKey, messagePreferenceKey;
        int defaultBroadcastMessageResource;

        switch (event.sleepState) {
            case AWAKE:
                actionPreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS;
                messagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST;
                defaultBroadcastMessageResource = R.string.prefs_events_forwarding_wokeup_broadcast_default_value;
                break;
            case ASLEEP:
                actionPreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS;
                messagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST;
                defaultBroadcastMessageResource = R.string.prefs_events_forwarding_fellsleep_broadcast_default_value;
                break;
            default:
                LOG.warn("Unable to deduce action and broadcast message preference key for sleep state {}", event.sleepState);
                return;
        }

        Set<String> actions = getDevicePrefs().getStringSet(actionPreferenceKey, Collections.emptySet());

        if (actions.isEmpty()) {
            return;
        }

        String broadcastMessage = getDevicePrefs().getString(messagePreferenceKey, context.getString(defaultBroadcastMessageResource));
        handleDeviceAction(actions, broadcastMessage);
    }

    private void handleGBDeviceEvent(GBDeviceEventWearState event) {
        LOG.debug("Got WEAR_STATE device event, wearingState = {}", event.wearingState);

        if (event.wearingState == WearingState.UNKNOWN) {
            LOG.warn("WEAR_STATE state is UNKNOWN, aborting further evaluation");
            return;
        }

        if (event.wearingState != WearingState.NOT_WEARING) {
            LOG.debug("WEAR_STATE state is not NOT_WEARING, aborting further evaluation");
        }

        Set<String> actionOnUnwear = getDevicePrefs().getStringSet(
                DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS,
                Collections.emptySet()
        );

        // check if an action is set
        if (actionOnUnwear.isEmpty()) {
            return;
        }

        String broadcastMessage = getDevicePrefs().getString(
                DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST,
                getContext().getString(R.string.prefs_events_forwarding_startnonwear_broadcast_default_value)
        );

        handleDeviceAction(actionOnUnwear, broadcastMessage);
    }

    private StoreDataTask createStoreTask(String task, Context context, GBDeviceEventBatteryInfo deviceEvent) {
        return new StoreDataTask(task, context, deviceEvent);
    }

    public class StoreDataTask extends DBAccess {
        GBDeviceEventBatteryInfo deviceEvent;

        public StoreDataTask(String task, Context context, GBDeviceEventBatteryInfo deviceEvent) {
            super(task, context);
            this.deviceEvent = deviceEvent;
        }

        @Override
        protected void doInBackground(DBHandler handler) {
            DaoSession daoSession = handler.getDaoSession();
            Device device = DBHelper.getDevice(gbDevice, daoSession);
            int ts = (int) (System.currentTimeMillis() / 1000);
            BatteryLevel batteryLevel = new BatteryLevel();
            batteryLevel.setTimestamp(ts);
            batteryLevel.setBatteryIndex(deviceEvent.batteryIndex);
            batteryLevel.setDevice(device);
            batteryLevel.setLevel(deviceEvent.level);
            handler.getDaoSession().getBatteryLevelDao().insert(batteryLevel);
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventDisplayMessage message) {
        GB.log(message.message, message.severity, null);

        Intent messageIntent = new Intent(GB.ACTION_DISPLAY_MESSAGE);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_MESSAGE, message.message);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_DURATION, message.duration);
        messageIntent.putExtra(GB.DISPLAY_MESSAGE_SEVERITY, message.severity);

        LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
    }

    protected Prefs getDevicePrefs() {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
    }

    @Override
    public String customStringFilter(String inputString) {
        return inputString;
    }


    // Empty functions following, leaving optional implementation up to child classes

    /**
     * If the device supports a "find phone" functionality, this method can
     * be overridden and implemented by the device support class.
     * @param start true if starting the search, false if stopping
     */
    @Override
    public void onFindPhone(boolean start) {

    }

    /**
     * If the device supports a "find device" functionality, this method can
     * be overridden and implemented by the device support class.
     * @param start true if starting the search, false if stopping
     */
    @Override
    public void onFindDevice(boolean start) {

    }

    /**
     * If the device supports a "set FM frequency" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param frequency the FM frequency to set
     */
    @Override
    public void onSetFmFrequency(float frequency) {

    }

    /**
     * If the device supports a "set LED color" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param color the new color, in ARGB, with alpha = 255
     */
    @Override
    public void onSetLedColor(int color) {

    }

    /**
     * If the device can be turned off by sending a command, this method
     * can be overridden and implemented by the device support class.
     */
    @Override
    public void onPowerOff() {

    }

    /**
     * If the device has a functionality to set the phone volume, this method
     * can be overridden and implemented by the device support class.
     * @param volume the volume percentage (0 to 100).
     */
    @Override
    public void onSetPhoneVolume(final float volume) {

    }

    /**
     * Called when the phone's interruption filter or ringer mode is changed.
     * @param ringerMode as per {@link android.media.AudioManager#getRingerMode()}
     */
    @Override
    public void onChangePhoneSilentMode(int ringerMode) {

    }

    /**
     * If the device can receive the GPS location from the phone, this method
     * can be overridden and implemented by the device support class.
     * @param location {@link android.location.Location} object containing the current GPS coordinates
     */
    @Override
    public void onSetGpsLocation(Location location) {

    }

    /**
     * If reminders can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param reminders {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Reminder} instances
     */
    @Override
    public void onSetReminders(ArrayList<? extends Reminder> reminders) {

    }

    /**
     * If loyalty cards can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param cards {@link java.util.ArrayList} containing {@link LoyaltyCard} instances
     */
    @Override
    public void onSetLoyaltyCards(ArrayList<LoyaltyCard> cards) {

    }

    /**
     * If world clocks can be configured on the device, this method can be
     * overridden and implemented by the device support class.
     * @param clocks {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.WorldClock} instances
     */
    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {

    }

    /**
     * If contacts can be configured on the device, this method can be
     * overridden and implemented by the device support class.
     * @param contacts {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Contact} instances
     */
    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {

    }

    /**
     * If the device can receive and display notifications, this method
     * can be overridden and implemented by the device support class.
     * @param notificationSpec notification details
     */
    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    /**
     * If notifications can be deleted from the device, this method can be
     * overridden and implemented by the device support class.
     * @param id the unique notification identifier
     */
    @Override
    public void onDeleteNotification(int id) {

    }

    /**
     * If the time can be set on the device, this method can be
     * overridden and implemented by the device support class.
     */
    @Override
    public void onSetTime() {

    }

    /**
     * If alarms can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param alarms {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Alarm} instances
     */
    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    /**
     * If the device can receive and show or handle phone call details, this
     * method can be overridden and implemented by the device support class.
     * @param callSpec the call state details
     */
    @Override
    public void onSetCallState(CallSpec callSpec) {

    }

    /**
     * If the device has a "canned messages" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param cannedMessagesSpec the canned messages to send to the device
     */
    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    /**
     * If the music play state can be set on the device, this method
     * can be overridden and implemented by the device support class.
     * @param stateSpec the current state of music playback
     */
    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    /**
     * If the music information can be shown on the device, this method can be
     * overridden and implemented by the device support class.
     * @param musicSpec the current music information, like track name and artist
     */
    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    /**
     * If apps can be installed on the device, this method can be
     * overridden and implemented by the device support class.
     * @param uri reference to a watch app file
     */
    @Override
    public void onInstallApp(Uri uri) {

    }

    /**
     * If the list of apps on the device can be retrieved, this method
     * can be overridden and implemented by the device support class.
     */
    @Override
    public void onAppInfoReq() {

    }

    /**
     * If the device supports starting an app with a command, this method
     * can be overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     * @param start true to start, false to stop the app (if supported)
     */
    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    /**
     * If apps can be downloaded from the device, this method can be
     * overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     */
    @Override
    public void onAppDownload(UUID uuid) {

    }

    /**
     * If apps on the device can be deleted with a command, this method
     * can be overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     */
    @Override
    public void onAppDelete(UUID uuid) {

    }

    /**
     * If apps on the device can be configured, this method can be
     * overridden and implemented by the device support class.
     * @param appUuid the Gadgetbridge internal UUID of the app
     * @param config the configuration of the app
     * @param id
     */
    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    /**
     * If apps on the device can be reordered, this method can be
     * overridden and implemented by the device support class.
     * @param uuids array of Gadgetbridge internal UUIDs of the apps
     */
    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    /**
     * If recorded data can be fetched from the device, this method
     * can be overridden and implemented by the device support class.
     * @param dataTypes which data types to fetch
     */
    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    /**
     * If a device can be reset with a command, this method can be
     * overridden and implemented by the device support class.
     * @param flags can be used to pass flags with the reset command
     */
    @Override
    public void onReset(int flags) {

    }

    /**
     * If the device can perform a heart rate measurement on request, this
     * method can be overridden and implemented by the device support class.
     */
    @Override
    public void onHeartRateTest() {

    }

    /**
     * If the device has the functionality to enable/disable realtime heart rate measurement,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable realtime heart rate measurement
     */
    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    /**
     * If the device has the functionality to enable/disable realtime steps information,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable realtime steps
     */
    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    /**
     * If the device has a functionality to enable constant vibration, this
     * method can be overridden and implemented by the device support class.
     * @param integer the vibration intensity
     */
    @Override
    public void onSetConstantVibration(int integer) {

    }

    /**
     * If the device supports taking screenshots of the screen, this method can
     * be overridden and implemented by the device support class.
     */
    @Override
    public void onScreenshotReq() {

    }

    /**
     * If the device has a toggle to enable the use of heart rate for sleep detection,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable using heart rate for sleep detection
     */
    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    /**
     * If the heart rate measurement interval can be changed on the device,
     * this method can be overridden and implemented by the device support class.
     * @param seconds the interval to configure on the device
     */
    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    /**
     * If calendar events can be sent to the device, this method can be
     * overridden and implemented by the device support class.
     * @param calendarEventSpec calendar event details
     */
    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    /**
     * If calendar events can be deleted from the device, this method can
     * be overridden and implemented by the device support class.
     * @param type type of calendar event
     * @param id id of calendar event
     */
    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    /**
     * If configuration options can be set on the device, this method
     * can be overridden and implemented by the device support class.
     * @param config the device specific option to set on the device
     */
    @Override
    public void onSendConfiguration(String config) {

    }

    /**
     * If the configuration can be retrieved from the device, this method
     * can be overridden and implemented by the device support class.
     * @param config the device specific option to get from the device
     */
    @Override
    public void onReadConfiguration(String config) {

    }

    /**
     * If the device can receive weather information, this method can be
     * overridden and implemented by the device support class.
     * @param weatherSpec weather information
     */
    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    /**
     * For testing new features, this method can be overridden and
     * implemented by the device support class.
     * It's called by clicking the "test new functionality" button
     * in the Debug menu.
     */
    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {

    }
}
