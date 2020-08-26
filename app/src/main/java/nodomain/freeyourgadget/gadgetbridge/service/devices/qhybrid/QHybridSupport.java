/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapterFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class QHybridSupport extends QHybridBaseSupport {
    public static final String QHYBRID_COMMAND_CONTROL = "qhybrid_command_control";
    public static final String QHYBRID_COMMAND_UNCONTROL = "qhybrid_command_uncontrol";
    public static final String QHYBRID_COMMAND_SET = "qhybrid_command_set";
    public static final String QHYBRID_COMMAND_VIBRATE = "qhybrid_command_vibrate";
    public static final String QHYBRID_COMMAND_UPDATE = "qhybrid_command_update";
    public static final String QHYBRID_COMMAND_UPDATE_TIMEZONE = "qhybrid_command_update_timezone";
    public static final String QHYBRID_COMMAND_NOTIFICATION = "qhybrid_command_notification";
    public static final String QHYBRID_COMMAND_UPDATE_SETTINGS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_SETTINGS";
    public static final String QHYBRID_COMMAND_OVERWRITE_BUTTONS = "nodomain.freeyourgadget.gadgetbridge.Q_OVERWRITE_BUTTONS";
    public static final String QHYBRID_COMMAND_UPDATE_WIDGETS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_WIDGETS";
    public static final String QHYBRID_COMMAND_SET_MENU_MESSAGE = "nodomain.freeyourgadget.gadgetbridge.Q_SET_MENU_MESSAGE";
    public static final String QHYBRID_COMMAND_SEND_MENU_ITEMS = "nodomain.freeyourgadget.gadgetbridge.Q_SEND_MENU_ITEMS";
    public static final String QHYBRID_COMMAND_SET_WIDGET_CONTENT = "nodomain.freeyourgadget.gadgetbridge.Q_SET_WIDGET_CONTENT";
    public static final String QHYBRID_COMMAND_SET_BACKGROUND_IMAGE = "nodomain.freeyourgadget.gadgetbridge.Q_SET_BACKGROUND_IMAGE";

    private static final String QHYBRID_ACTION_SET_ACTIVITY_HAND = "nodomain.freeyourgadget.gadgetbridge.Q_SET_ACTIVITY_HAND";

    public static final String QHYBRID_EVENT_SETTINGS_UPDATED = "nodomain.freeyourgadget.gadgetbridge.Q_SETTINGS_UPDATED";
    public static final String QHYBRID_EVENT_FILE_UPLOADED = "nodomain.freeyourgadget.gadgetbridge.Q_FILE_UPLOADED";
    public static final String QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED = "nodomain.freeyourgadget.gadgetbridge.Q_NOTIFICATION_CONFIG_CHANGED";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";
    public static final String QHYBRID_EVENT_MULTI_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_MULTI_BUTTON_PRESSED";
    public static final String QHYBRID_EVENT_COMMUTE_MENU = "nodomain.freeyourgadget.gadgetbridge.Q_COMMUTE_MENU";

    public static final String ITEM_STEP_GOAL = "STEP_GOAL";
    public static final String ITEM_STEP_COUNT = "STEP_COUNT";
    public static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";
    public static final String ITEM_ACTIVITY_POINT = "ACTIVITY_POINT";
    public static final String ITEM_EXTENDED_VIBRATION_SUPPORT = "EXTENDED_VIBRATION";
    public static final String ITEM_HAS_ACTIVITY_HAND = "HAS_ACTIVITY_HAND";
    public static final String ITEM_USE_ACTIVITY_HAND = "USE_ACTIVITY_HAND";
    public static final String ITEM_LAST_HEARTBEAT = "LAST_HEARTBEAT";
    public static final String ITEM_TIMEZONE_OFFSET = "TIMEZONE_OFFSET_COUNT";
    public static final String ITEM_HEART_RATE_MEASUREMENT_MODE = "HEART_RATE_MEASUREMENT_MODE";

    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);
    private final BroadcastReceiver commandReceiver;
    private final BroadcastReceiver globalCommandReceiver;

    private final PackageConfigHelper helper;

    public volatile boolean searchDevice = false;

    private long timeOffset;

    private boolean useActivityHand;

    private WatchAdapter watchAdapter;

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        IntentFilter commandFilter = new IntentFilter(QHYBRID_COMMAND_CONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_UNCONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_SET);
        commandFilter.addAction(QHYBRID_COMMAND_VIBRATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_TIMEZONE);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_SETTINGS);
        commandFilter.addAction(QHYBRID_COMMAND_OVERWRITE_BUTTONS);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_WIDGETS);
        commandFilter.addAction(QHYBRID_COMMAND_SEND_MENU_ITEMS);
        commandFilter.addAction(QHYBRID_COMMAND_SET_BACKGROUND_IMAGE);
        commandReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                NotificationConfiguration config = extras == null ? null : (NotificationConfiguration) intent.getExtras().get("CONFIG");
                if (intent.getAction() == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case QHYBRID_COMMAND_CONTROL: {
                        log("sending control request");
                        watchAdapter.requestHandsControl();
                        if (config != null) {
                            watchAdapter.setHands(config.getHour(), config.getMin());
                        } else {
                            watchAdapter.setHands((short) 0, (short) 0);
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_UNCONTROL: {
                        watchAdapter.releaseHandsControl();
                        break;
                    }
                    case QHYBRID_COMMAND_SET: {
                        if (config != null) {
                            watchAdapter.setHands(config.getHour(), config.getMin());
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_VIBRATE: {
                        if (config != null) {
                            watchAdapter.vibrate(config.getVibration());
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION: {
                        if (config != null) {
                            watchAdapter.playNotification(config);
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE: {
                        loadTimeOffset();
                        onSetTime();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_TIMEZONE:{
                        loadTimezoneOffset();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_SETTINGS: {
                        String newSetting = intent.getStringExtra("EXTRA_SETTING");
                        switch (newSetting) {
                            case ITEM_VIBRATION_STRENGTH: {
                                watchAdapter.setVibrationStrength(Short.parseShort(gbDevice.getDeviceInfo(ITEM_VIBRATION_STRENGTH).getDetails()));
                                break;
                            }
                            case ITEM_STEP_GOAL: {
                                watchAdapter.setStepGoal(Integer.parseInt(gbDevice.getDeviceInfo(ITEM_STEP_GOAL).getDetails()));
                                break;
                            }
                            case ITEM_USE_ACTIVITY_HAND: {
                                QHybridSupport.this.useActivityHand = gbDevice.getDeviceInfo(ITEM_USE_ACTIVITY_HAND).getDetails().equals("true");
                                GBApplication.getPrefs().getPreferences().edit().putBoolean("QHYBRID_USE_ACTIVITY_HAND", useActivityHand).apply();
                                break;
                            }
                        }

                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(QHYBRID_EVENT_SETTINGS_UPDATED));
                        break;
                    }
                    case QHYBRID_COMMAND_OVERWRITE_BUTTONS: {
                        String buttonConfig = intent.getStringExtra(FossilWatchAdapter.ITEM_BUTTONS);
                        watchAdapter.overwriteButtons(buttonConfig);
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED: {
                        watchAdapter.syncNotificationSettings();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_WIDGETS: {
                        watchAdapter.updateWidgets();
                        break;
                    }
                    case QHYBRID_COMMAND_SET_BACKGROUND_IMAGE:{
                        byte[] pixels = intent.getByteArrayExtra("EXTRA_PIXELS_ENCODED");
                        watchAdapter.setBackgroundImage(pixels);
                        break;
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);

        helper = new PackageConfigHelper(GBApplication.getContext());

        IntentFilter globalFilter = new IntentFilter();
        globalFilter.addAction(QHYBRID_ACTION_SET_ACTIVITY_HAND);
        globalFilter.addAction(QHYBRID_COMMAND_SET_MENU_MESSAGE);
        globalFilter.addAction(QHYBRID_COMMAND_SET_WIDGET_CONTENT);
        globalCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (watchAdapter == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case QHYBRID_ACTION_SET_ACTIVITY_HAND: {
                        try {
                            String extra = String.valueOf(intent.getExtras().get("EXTRA_PROGRESS"));
                            float progress = Float.parseFloat(extra);
                            watchAdapter.setActivityHand(progress);

                            watchAdapter.playNotification(new NotificationConfiguration(
                                    (short) -1,
                                    (short) -1,
                                    (short) (progress * 180),
                                    PlayNotificationRequest.VibrationType.NO_VIBE
                            ));
                        } catch (Exception e) {
                            GB.log("wrong number format", GB.ERROR, e);
                            logger.debug("trash extra should be number 0.0-1.0");
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_SET_MENU_MESSAGE: {
                        String message = String.valueOf(intent.getExtras().get("EXTRA_MESSAGE"));
                        boolean finished = Boolean.valueOf(String.valueOf(intent.getExtras().get("EXTRA_FINISHED")));

                        watchAdapter.setCommuteMenuMessage(message, finished);

                        break;
                    }
                    case QHYBRID_COMMAND_SET_WIDGET_CONTENT: {
                        HashMap<String, String> widgetValues = new HashMap<>();

                        for(String key : intent.getExtras().keySet()){
                            if(key.matches("^EXTRA_WIDGET_ID_.*$")){
                                widgetValues.put(key.substring(16), String.valueOf(intent.getExtras().get(key)));
                            }
                        }
                        boolean render = intent.getBooleanExtra("EXTRA_RENDER", true);
                        if(widgetValues.size() > 0){
                            Iterator<String> valuesIterator = widgetValues.keySet().iterator();
                            valuesIterator.next();

                            while(valuesIterator.hasNext()){
                                String id = valuesIterator.next();
                                watchAdapter.setWidgetContent(id, widgetValues.get(id), false);
                            }

                            valuesIterator = widgetValues.keySet().iterator();
                            String id = valuesIterator.next();
                            watchAdapter.setWidgetContent(id, widgetValues.get(id), render);
                        }else {
                            String id = String.valueOf(intent.getExtras().get("EXTRA_WIDGET_ID"));
                            String content = String.valueOf(intent.getExtras().get("EXTRA_CONTENT"));
                            watchAdapter.setWidgetContent(id, content, render);
                        }
                        break;
                    }
                }
            }
        };
        GBApplication.getContext().registerReceiver(globalCommandReceiver, globalFilter);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        super.onSetCallState(callSpec);
        watchAdapter.onSetCallState(callSpec);
    }

    @Override
    public void dispose() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
        GBApplication.getContext().unregisterReceiver(globalCommandReceiver);
        super.dispose();
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        super.onSetAlarms(alarms);
        if(this.watchAdapter == null){
            GB.toast("watch not connected", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        this.watchAdapter.onSetAlarms(alarms);
    }

    @Override
    public void onSendConfiguration(String config) {
        if (watchAdapter != null) {
            watchAdapter.onSendConfiguration(config);
        }
    }
    private void loadTimeOffset() {
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
    }

    private void loadTimezoneOffset(){
        short offset = (short) getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIMEZONE_OFFSET", 0);

        this.watchAdapter.setTimezoneOffsetMinutes(offset);
    }

    public long getTimeOffset(){
        return this.timeOffset;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        this.useActivityHand = GBApplication.getPrefs().getBoolean("QHYBRID_USE_ACTIVITY_HAND", false);
        getDevice().addDeviceInfo(new GenericItem(ITEM_USE_ACTIVITY_HAND, String.valueOf(this.useActivityHand)));

        if (GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_USE_CUSTOM_DEVICEICON, true)) {
            getDevice().setNotificationIconConnected(R.drawable.ic_notification_qhybrid);
            getDevice().setNotificationIconDisconnected(R.drawable.ic_notification_disconnected_qhybrid);
        }

        for (int i = 2; i <= 7; i++)
            builder.notify(getCharacteristic(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d")), true);

        builder
                .read(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")))
                // .notify(getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")), true)
        ;


        loadTimeOffset();

        return builder;
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        super.onSetMusicInfo(musicSpec);

        try {
            watchAdapter.setMusicInfo(musicSpec);
        }catch (Exception e){
            GB.log("setMusicInfo error", GB.ERROR, e);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        super.onSetMusicState(stateSpec);

        watchAdapter.setMusicState(stateSpec);
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            this.watchAdapter.onFetchActivityData();
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        log("notif from " + notificationSpec.sourceAppId + "  " + notificationSpec.sender + "   " + notificationSpec.phoneNumber);
        //new Exception().printStackTrace();

        if(this.watchAdapter instanceof FossilHRWatchAdapter){
            if(((FossilHRWatchAdapter) watchAdapter).playRawNotification(notificationSpec)) return;
        }

        String packageName = notificationSpec.sourceName;

        NotificationConfiguration config = null;
        try {
            config = helper.getNotificationConfiguration(packageName);
        } catch (Exception e) {
            GB.toast("error getting notification configuration", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        if (config == null) return;

        log("handling notification");

        if (config.getRespectSilentMode()) {
            int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
            if (mode == AudioManager.RINGER_MODE_SILENT) return;
        }

        boolean enforceActivityHandNotification = config.getHour() == -1 && config.getMin() == -1;

        playNotification(config);

        showNotificationsByAllActive(enforceActivityHandNotification);
    }

    private void log(String message){
        logger.debug(message);
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        this.watchAdapter.onDeleteNotification(id);

        showNotificationsByAllActive(true);
    }

    private void showNotificationsByAllActive(boolean enforceByNotification) {
        if (!this.useActivityHand) return;
        double progress = calculateNotificationProgress();
        showNotificationCountOnActivityHand(progress);

        if (enforceByNotification) {
            watchAdapter.playNotification(new NotificationConfiguration(
                    (short) -1,
                    (short) -1,
                    (short) (progress * 180),
                    PlayNotificationRequest.VibrationType.NO_VIBE
            ));
        }
    }


    public double calculateNotificationProgress() {
        HashMap<NotificationConfiguration, Boolean> configs = new HashMap<>(0);
        try {
            for (NotificationConfiguration config : helper.getNotificationConfigurations()) {
                configs.put(config, false);
            }
        } catch (Exception e) {
            GB.toast("error getting notification configs", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        double notificationProgress = 0;

        for (String notificationPackage : NotificationListener.notificationStack) {
            for (NotificationConfiguration notificationConfiguration : configs.keySet()) {
                if (configs.get(notificationConfiguration)) continue;
                if (notificationConfiguration.getPackageName().equals(notificationPackage)) {
                    notificationProgress += 0.25;
                    configs.put(notificationConfiguration, true);
                }
            }
        }

        return notificationProgress;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        watchAdapter.onConnectionStateChange(gatt, status, newState);
    }

    //TODO toggle "Notifications when screen on" options on this check
    private void showNotificationCountOnActivityHand(double progress) {
        if (useActivityHand) {
            watchAdapter.setActivityHand(progress);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if(watchAdapter == null) return;
        watchAdapter.onMtuChanged(gatt, mtu, status);
    }

    private void playNotification(NotificationConfiguration config) {
        if (config.getMin() == -1 && config.getHour() == -1 && config.getVibration() == PlayNotificationRequest.VibrationType.NO_VIBE)
            return;
        watchAdapter.playNotification(config);
    }

    @Override
    public void onSetTime() {
        watchAdapter.setTime();
    }

    @Override
    public void onFindDevice(boolean start) {
        watchAdapter.onFindDevice(start);
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        watchAdapter.onSendWeather(weatherSpec);
    }

    @Override
    public void onTestNewFunction() {
        watchAdapter.onTestNewFunction();
    }

    @Override
    public void onInstallApp(Uri uri) {
        watchAdapter.onInstallApp(uri);
    }

    private void backupFile(DownloadFileRequest request) {
        try {
            File file = FileUtils.getExternalFile("qFiles/" + request.timeStamp);
            if (file.exists()) {
                throw new Exception("file " + file.getPath() + " exists");
            }
            logger.debug("Writing file " + file.getPath());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(request.file);
            }
            logger.debug("file written.");

            file = FileUtils.getExternalFile("qFiles/steps");
            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                fos.write(("file " + request.timeStamp + " cut\n\n").getBytes());
            }

            //TODO file stuff
            // queueWrite(new EraseFileRequest((short) request.fileHandle));
        } catch (Exception e) {
            GB.log("error", GB.ERROR, e);
            if (request.fileHandle > 257) {
                // queueWrite(new DownloadFileRequest((short) (request.fileHandle - 1)));
            }
        }
    }

    @Override
    public void handleGBDeviceEvent(GBDeviceEventBatteryInfo deviceEvent){
        super.handleGBDeviceEvent(deviceEvent);
    }

    public void notifiyException(Exception e){
        notifiyException("", e);
    }

    public void notifiyException(String requestName, Exception e) {
        if (!BuildConfig.DEBUG) {
            logger.error("Error: " + requestName, e);
            return;
        }
        GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.ERROR, e);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString();

        Notification.Builder notificationBuilder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(getContext(), GB.NOTIFICATION_CHANNEL_ID);
        } else {
            notificationBuilder = new Notification.Builder(getContext());
        }
        notificationBuilder
                .setContentTitle("Q Error " + requestName)
                .setSmallIcon(R.drawable.ic_notification_qhybrid)
                .setContentText(sStackTrace)
                .setStyle(new Notification.BigTextStyle())
                .build();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","dakhnod@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Exception Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's a crash from your stupid app: \n\n" + sStackTrace);

        PendingIntent intent = PendingIntent.getActivity(getContext(), 0, emailIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            notificationBuilder.addAction(new Notification.Action(0, "report", intent));
        }else{
            notificationBuilder.addAction(0, "report", intent);
        }

        ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        watchAdapter.onCharacteristicWrite(gatt, characteristic, status);
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        switch (characteristic.getUuid().toString()) {
            case "00002a26-0000-1000-8000-00805f9b34fb": {
                String firmwareVersion = characteristic.getStringValue(0);

                gbDevice.setFirmwareVersion(firmwareVersion);
                this.watchAdapter = new WatchAdapterFactory().createWatchAdapter(firmwareVersion, this);
                this.watchAdapter.initialize();
                showNotificationsByAllActive(false);
                break;
            }
            case "00002a24-0000-1000-8000-00805f9b34fb": {
                String modelNumber = characteristic.getStringValue(0);
                gbDevice.setModel(modelNumber);
                gbDevice.setName(watchAdapter.getModelName());
                try {
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, String.valueOf(watchAdapter.supportsExtendedVibration())));
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_HAS_ACTIVITY_HAND, String.valueOf(watchAdapter.supportsActivityHand())));
                } catch (UnsupportedOperationException e) {
                    notifiyException(e);
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, "false"));
                }
                break;
            }
            case "00002a19-0000-1000-8000-00805f9b34fb": {
                short level = characteristic.getValue()[0];
                gbDevice.setBatteryLevel(level);

                gbDevice.setBatteryThresholdPercent((short) 2);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = gbDevice.getBatteryLevel();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                handleGBDeviceEvent(batteryInfo);
                break;

            }
        }

        return true;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
            characteristic) {
        if(watchAdapter == null) return super.onCharacteristicChanged(gatt, characteristic);
        return watchAdapter.onCharacteristicChanged(gatt, characteristic);
    }

}
