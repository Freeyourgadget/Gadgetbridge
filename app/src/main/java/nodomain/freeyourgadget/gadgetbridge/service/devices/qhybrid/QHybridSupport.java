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
import android.util.Log;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapterFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
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

    private static final String QHYBRID_ACTION_SET_ACTIVITY_HAND = "nodomain.freeyourgadget.gadgetbridge.Q_SET_ACTIVITY_HAND";

    public static final String QHYBRID_EVENT_SETTINGS_UPDATED = "nodomain.freeyourgadget.gadgetbridge.Q_SETTINGS_UPDATED";
    public static final String QHYBRID_EVENT_FILE_UPLOADED = "nodomain.freeyourgadget.gadgetbridge.Q_FILE_UPLOADED";
    public static final String QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED = "nodomain.freeyourgadget.gadgetbridge.Q_NOTIFICATION_CONFIG_CHANGED";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";
    public static final String QHYBRID_EVENT_MULTI_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_MULTI_BUTTON_PRESSED";

    public static final String ITEM_STEP_GOAL = "STEP_GOAL";
    public static final String ITEM_STEP_COUNT = "STEP_COUNT";
    public static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";
    public static final String ITEM_ACTIVITY_POINT = "ACTIVITY_POINT";
    public static final String ITEM_EXTENDED_VIBRATION_SUPPORT = "EXTENDED_VIBRATION";
    public static final String ITEM_HAS_ACTIVITY_HAND = "HAS_ACTIVITY_HAND";
    public static final String ITEM_USE_ACTIVITY_HAND = "USE_ACTIVITY_HAND";
    public static final String ITEM_LAST_HEARTBEAT = "LAST_HEARTBEAT";
    public static final String ITEM_TIMEZONE_OFFSET = "STEPTIMEZONE_OFFSET_COUNT";

    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    private PackageConfigHelper helper;

    private volatile boolean searchDevice = false;

    private long timeOffset;

    private boolean useActivityHand;

    private WatchAdapter watchAdapter;

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        addSupportedService(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"));
        addSupportedService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
        addSupportedService(UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"));
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
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                NotificationConfiguration config = extras == null ? null : (NotificationConfiguration) intent.getExtras().get("CONFIG");
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
                        watchAdapter.setHands(config.getHour(), config.getMin());

                        break;
                    }
                    case QHYBRID_COMMAND_VIBRATE: {
                        watchAdapter.vibrate(config.getVibration());
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION: {
                        watchAdapter.playNotification(config);
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
                        watchAdapter.overwriteButtons();
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED: {
                        watchAdapter.syncNotificationSettings();
                        break;
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);

        try {
            helper = new PackageConfigHelper(GBApplication.getContext());
        } catch (GBException e) {
            e.printStackTrace();
            GB.toast("erroe getting database", Toast.LENGTH_SHORT, GB.ERROR, e);
            try {
                throw e;
            } catch (GBException ex) {
                ex.printStackTrace();
            }
        }

        IntentFilter globalFilter = new IntentFilter();
        globalFilter.addAction(QHYBRID_ACTION_SET_ACTIVITY_HAND);
        BroadcastReceiver globalCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //noinspection SwitchStatementWithTooFewBranches
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
                            e.printStackTrace();
                            logger.debug("trash extra should be number 0.0-1.0");
                        }
                        break;
                    }
                }
            }
        };
        GBApplication.getContext().registerReceiver(globalCommandReceiver, globalFilter);
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

        getDevice().setNotificationIconConnected(R.drawable.ic_notification_qhybrid);
        getDevice().setNotificationIconDisconnected(R.drawable.ic_notification_disconnected_qhybrid);

        for (int i = 2; i <= 7; i++)
            builder.notify(getCharacteristic(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d")), true);

        builder
                .read(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")))
        ;

        loadTimeOffset();

        return builder;
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
        String packageName = notificationSpec.sourceName;

        NotificationConfiguration config = null;
        try {
            config = helper.getNotificationConfiguration(packageName);
        } catch (GBException e) {
            e.printStackTrace();
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
        } catch (GBException e) {
            e.printStackTrace();
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
        try {
            if (watchAdapter.supportsExtendedVibration()) {
                GB.toast("Device does not support brr brr", Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (UnsupportedOperationException e) {
            notifiyException(e);
            GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.INFO);
        }

        if (start && searchDevice) return;

        searchDevice = start;

        if (start) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    while (searchDevice) {
                        QHybridSupport.this.watchAdapter.vibrateFindMyDevicePattern();
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    public void onTestNewFunction() {
        watchAdapter.onTestNewFunction();
    }

    private void backupFile(DownloadFileRequest request) {
        try {
            File f = new File("/sdcard/qFiles/");
            if (!f.exists()) f.mkdir();

            File file = new File("/sdcard/qFiles/" + request.timeStamp);
            if (file.exists()) {
                throw new Exception("file " + file.getPath() + " exists");
            }
            logger.debug("Writing file " + file.getPath());
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(request.file);
            fos.close();
            logger.debug("file written.");

            FileOutputStream fos2 = new FileOutputStream("/sdcard/qFiles/steps", true);
            fos2.write(("file " + request.timeStamp + " cut\n\n").getBytes());
            fos2.close();

            //TODO file stuff
            // queueWrite(new EraseFileRequest((short) request.fileHandle));
        } catch (Exception e) {
            e.printStackTrace();
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
                .setContentTitle("Q Error")
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
                    GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.INFO);
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
