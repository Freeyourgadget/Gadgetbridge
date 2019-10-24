package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapterFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ActivityPointGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.BatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.EraseFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.FileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetCountdownSettingsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GoalTrackingGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ListFilesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.OTAEnterRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.OTAEraseRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.UploadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.VibrateRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class QHybridSupport extends QHybridBaseSupport {
    public static final String QHYBRID_COMMAND_CONTROL = "qhybrid_command_control";
    public static final String QHYBRID_COMMAND_UNCONTROL = "qhybrid_command_uncontrol";
    public static final String QHYBRID_COMMAND_SET = "qhybrid_command_set";
    public static final String QHYBRID_COMMAND_VIBRATE = "qhybrid_command_vibrate";
    public static final String QHYBRID_COMMAND_UPDATE = "qhybrid_command_update";
    public static final String QHYBRID_COMMAND_NOTIFICATION = "qhybrid_command_notification";
    public static final String QHYBRID_COMMAND_UPDATE_SETTINGS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_SETTINGS";
    public static final String QHYBRID_COMMAND_OVERWRITE_BUTTONS = "nodomain.freeyourgadget.gadgetbridge.Q_OVERWRITE_BUTTONS";

    public static final String QHYBRID_ACTION_SET_ACTIVITY_HAND = "nodomain.freeyourgadget.gadgetbridge.Q_SET_ACTIVITY_HAND";

    public static final String QHYBRID_EVENT_SETTINGS_UPDATED = "nodomain.freeyourgadget.gadgetbridge.Q_SETTINGS_UPDATED";
    public static final String QHYBRID_EVENT_FILE_UPLOADED = "nodomain.freeyourgadget.gadgetbridge.Q_FILE_UPLOADED";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";

    public static final String ITEM_STEP_GOAL = "STEP_GOAL";
    public static final String ITEM_STEP_COUNT = "STEP_COUNT";
    public static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";
    public static final String ITEM_ACTIVITY_POINT = "ACTIVITY_POINT";
    public static final String ITEM_EXTENDED_VIBRATION_SUPPORT = "EXTENDED_VIBRATION";
    public static final String ITEM_HAS_ACTIVITY_HAND = "HAS_ACTIVITY_HAND";
    public static final String ITEM_USE_ACTIVITY_HAND = "USE_ACTIVITY_HAND";

    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    private PackageConfigHelper helper;

    private volatile boolean searchDevice = false;

    private long timeOffset;

    private String modelNumber;

    private boolean useActivityHand;

    WatchAdapter watchAdapter;

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
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_SETTINGS);
        commandFilter.addAction(QHYBRID_COMMAND_OVERWRITE_BUTTONS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);

        helper = new PackageConfigHelper(GBApplication.getContext());

        IntentFilter globalFilter = new IntentFilter();
        globalFilter.addAction(QHYBRID_ACTION_SET_ACTIVITY_HAND);
        GBApplication.getContext().registerReceiver(globalCommandReceiver, globalFilter);
    }

    private boolean supportsActivityHand() {
        switch (modelNumber) {
            case "HL.0.0":
                return false;
            case "HW.0.0":
                return true;
        }
        throw new UnsupportedOperationException();
    }

    private boolean supportsExtendedVibration() {
        switch (modelNumber) {
            case "HL.0.0":
                return false;
            case "HW.0.0":
                return true;
        }
        throw new UnsupportedOperationException();
    }

    private void getTimeOffset() {
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
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


        Request initialRequest = new GetStepGoalRequest();

        builder
                // .read(getCharacteristic(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")))
                // .read(getCharacteristic(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")))
        // .read(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")))
        // .notify(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")), true)
        // .write(getCharacteristic(initialRequest.getRequestUUID()), initialRequest.getRequestData())
        ;

        getTimeOffset();

        // builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        //TODO
        /* if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            requestQueue.add(new BatteryLevelRequest());
            requestQueue.add(new GetCurrentStepCountRequest());
            requestQueue.add(new ListFilesRequest());
            queueWrite(new ActivityPointGetRequest());
        } */
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        Log.d("Service", "notif from " + notificationSpec.sourceAppId + "  " + notificationSpec.sender + "   " + notificationSpec.phoneNumber);
        //new Exception().printStackTrace();
        String packageName = notificationSpec.sourceName;

        PackageConfig config = helper.getSetting(packageName);
        if (config == null) return;

        Log.d("Service", "handling notification");

        if (config.getRespectSilentMode()) {
            int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
            if (mode == AudioManager.RINGER_MODE_SILENT) return;
        }

        boolean enforceActivityHandNotification = config.getHour() == -1 && config.getMin() == -1;

        showNotificationsByAllActive(enforceActivityHandNotification);

        playNotification(config);
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        showNotificationsByAllActive(true);
    }

    private void showNotificationsByAllActive(boolean enforceByNotification) {
        if (!this.useActivityHand) ;
        double progress = calculateNotificationProgress();
        showNotificationCountOnActivityHand(progress);

        if (enforceByNotification) {
            watchAdapter.playNotification(new PackageConfig(
                    (short) -1,
                    (short) -1,
                    //TODO test activity hand
                    (short) (progress * 180),
                    PlayNotificationRequest.VibrationType.NO_VIBE
            ));
        }
    }


    private double calculateNotificationProgress() {
        HashMap<PackageConfig, Boolean> configs = new HashMap<>(0);
        for (PackageConfig config : helper.getSettings()) {
            configs.put(config, false);
        }

        double notificationProgress = 0;

        for (String notificationPackage : NotificationListener.notificationStack) {
            for (PackageConfig packageConfig : configs.keySet()) {
                if (configs.get(packageConfig)) continue;
                if (packageConfig.getPackageName().equals(notificationPackage)) {
                    notificationProgress += 0.25;
                    configs.put(packageConfig, true);
                }
            }
        }

        return notificationProgress;
    }

    private void showNotificationCountOnActivityHand(double progress) {
        if (useActivityHand) {
            watchAdapter.setActivityHand(progress);
        }
    }

    private void playNotification(PackageConfig config) {
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
            if (start && !supportsExtendedVibration()) {
                GB.toast("Device does not support brr brr", Toast.LENGTH_SHORT, GB.INFO);
                return;
            }
        } catch (UnsupportedOperationException e) {
            GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.INFO);
        }

        if (start && searchDevice) return;

        searchDevice = start;

        if (start) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    VibrateRequest request = new VibrateRequest(false, (short) 4, (short) 1);
                    BluetoothGattCharacteristic chara = getCharacteristic(request.getRequestUUID());
                    int i = 0;
                    while (searchDevice) {
                        new TransactionBuilder("findDevice#" + i++).write(chara, request.getRequestData()).queue(getQueue());
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

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        switch (characteristic.getUuid().toString()) {
            case "00002a26-0000-1000-8000-00805f9b34fb": {
                String firmwareVersion = characteristic.getStringValue(0);

                gbDevice.setFirmwareVersion(firmwareVersion);
                this.watchAdapter = new WatchAdapterFactory().createWatchAdapter(firmwareVersion, this);
                break;
            }
            case "00002a24-0000-1000-8000-00805f9b34fb": {
                modelNumber = characteristic.getStringValue(0);
                gbDevice.setModel(modelNumber);
                gbDevice.setName(getModelNameByModelNumber(modelNumber));
                try {
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, String.valueOf(supportsExtendedVibration())));
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_HAS_ACTIVITY_HAND, String.valueOf(supportsActivityHand())));
                } catch (UnsupportedOperationException e) {
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

    private String getModelNameByModelNumber(String modelNumber) {
        switch (modelNumber) {
            case "HW.0.0":
                return "Q Commuter";
            case "HL.0.0":
                return "Q Activist";
        }
        return "unknwon Q";
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
            characteristic) {
        return watchAdapter.onCharacteristicChanged(gatt, characteristic);
    }

    private final BroadcastReceiver globalCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case QHYBRID_ACTION_SET_ACTIVITY_HAND: {
                    try {
                        Object extra = intent.getExtras().get("EXTRA_PROGRESS");
                        float progress = (float) extra;
                        watchAdapter.setActivityHand(progress);

                        watchAdapter.playNotification(new PackageConfig(
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

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            PackageConfig config = extras == null ? null : (PackageConfig) intent.getExtras().get("CONFIG");
            switch (intent.getAction()) {
                case QHYBRID_COMMAND_CONTROL: {
                    Log.d("Service", "sending control request");
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
                    getTimeOffset();
                    onSetTime();
                    break;
                }
                case QHYBRID_COMMAND_UPDATE_SETTINGS: {
                    String newSetting = intent.getStringExtra("EXTRA_SETTING");
                    switch (newSetting) {
                        case ITEM_VIBRATION_STRENGTH: {
                            watchAdapter.setVibrationStrength(Short.parseShort(gbDevice.getDeviceInfo(ITEM_VIBRATION_STRENGTH).getDetails()));
                            // queueWrite(new VibrateRequest(false, (short)4, (short)1));
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
            }
        }
    };
}
