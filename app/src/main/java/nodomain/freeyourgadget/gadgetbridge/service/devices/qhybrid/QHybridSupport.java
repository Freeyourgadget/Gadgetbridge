package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.Transaction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.BatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.EraseFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.FileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ListFilesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetCurrentTimeServiceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.VibrateRequest;

public class QHybridSupport extends AbstractBTLEDeviceSupport {
    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    private PackageConfigHelper helper;


    private volatile boolean searchDevice = false;

    public QHybridSupport(Logger logger) {
        super(logger);
    }

    private SparseArray<Request> responseFilters = new SparseArray<>();

    private OnVibrationStrengthListener vibrationStrengthListener;
    private OnGoalListener goalListener;

    public static final String commandControl = "qhybrid_command_control";
    public static final String commandUncontrol = "qhybrid_command_uncontrol";
    public static final String commandSet = "qhybrid_command_set";
    public static final String commandVibrate = "qhybrid_command_vibrate";
    public static final String commandUpdate = "qhybrid_command_update";
    public static final String commandNotification = "qhybrid_command_notification";

    private static final String ITEM_STEP_GOAL = "STEP_GOAL";
    private static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";

    private Request fileRequest = null;
    //int fileIndex = -1;

    private boolean dumpInited = false;

    private long timeOffset;

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        IntentFilter commandFilter = new IntentFilter(commandControl);
        commandFilter.addAction(commandUncontrol);
        commandFilter.addAction(commandSet);
        commandFilter.addAction(commandVibrate);
        commandFilter.addAction(commandUpdate);
        commandFilter.addAction(commandNotification);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);


        fillResponseList();

    }

    private void fillResponseList() {

        Class<? extends Request>[] classes = new Class[]{
                BatteryLevelRequest.class,
                GetStepGoalRequest.class,
                GetVibrationStrengthRequest.class
        };
        for (Class<? extends Request> c : classes) {
            try {
                c.getSuperclass().getDeclaredMethod("handleResponse", BluetoothGattCharacteristic.class);
                Request object = c.newInstance();
                byte[] sequence = object.getStartSequence();
                if (sequence.length > 1) {
                    responseFilters.put((int) object.getStartSequence()[1], object);
                    Log.d("Service", "response filter " + object.getStartSequence()[1] + ": " + c.getSimpleName());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                Log.d("Service", "skipping class " + c.getName());
                //e.printStackTrace();
            }
        }
    }

    private void getTimeOffset(){
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
        getContext().unregisterReceiver(dumpReceiver);
        ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).cancel(dumpIntent);
        dumpInited = false;
    }

    private void getBattery() {
        queueWrite(new BatteryLevelRequest());
    }

    public void getGoal(OnGoalListener listener) {
        this.goalListener = listener;
        queueWrite(new GetStepGoalRequest());
    }

    public void setGoal(int goal) {
        queueWrite(new SetStepGoalRequest(goal));
    }

    public void getVibrationStrength(OnVibrationStrengthListener listener) {
        this.vibrationStrengthListener = listener;
        queueWrite(new GetVibrationStrengthRequest());
    }

    public void setVibrationStrength(int strength) {
        queueWrite(new SetVibrationStrengthRequest((short) strength));
    }

    private void queueWrite(Request request) {
        new TransactionBuilder(request.getClass().getSimpleName()).write(getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getQueue());
        if(request instanceof FileRequest) this.fileRequest = request;
    }

    @Override
    public boolean connect() {
        logger.debug("connect attempt...");
        return super.connect();
    }

    private BroadcastReceiver dumpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Dump", "dumping...");
            downloadActivityFiles();
        }
    };
    private PendingIntent dumpIntent;

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        for (int i = 2; i <= 7; i++)
            builder.notify(getCharacteristic(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d")), true);
        /*
        builder.notify(getCharacteristic(UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d")), true);
        builder.notify(getCharacteristic(UUID.fromString("3dda0004-957f-7d4a-34a6-74696673696d")), true);
        builder.notify(getCharacteristic(UUID.fromString("3dda0005-957f-7d4a-34a6-74696673696d")), true);
        builder.notify(getCharacteristic(UUID.fromString("3dda0006-957f-7d4a-34a6-74696673696d")), true);
        builder.notify(getCharacteristic(UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d")), true);
        */

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));


        helper = new PackageConfigHelper(getContext());

        if (!dumpInited) {
            getContext().registerReceiver(dumpReceiver, new IntentFilter("dumpReceiver2"));
            dumpIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("dumpReceiver2"), PendingIntent.FLAG_UPDATE_CURRENT);
            ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, AlarmManager.INTERVAL_HOUR, dumpIntent);
            dumpInited = true;
        }
        getTimeOffset();
        return builder;
    }


    @Override
    public void performConnected(Transaction transaction) throws IOException {
        super.performConnected(transaction);
        logger.debug("performConnected()");
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        super.onServicesDiscovered(gatt);

        playAnimation();
        getBattery();

        logger.debug("onServicesDiscovered");
    }

    private void playAnimation() {
        queueWrite(new AnimationRequest());
    }


    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        Log.d("Service", "notif from " + notificationSpec.sourceAppId + "  " + notificationSpec.sender + "   " + notificationSpec.phoneNumber);
        //new Exception().printStackTrace();
        String packageName = notificationSpec.sourceName;

        PackageConfig config = helper.getSetting(packageName);
        if (config == null) return;

        Log.d("Service", "handling notification");

        int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
        if (mode == AudioManager.RINGER_MODE_SILENT && config.getRespectSilentMode()) return;


        queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();
        SetCurrentTimeServiceRequest request = new SetCurrentTimeServiceRequest(
                (int) (millis / 1000 + timeOffset * 60),
                (short) (millis % 1000),
                (short) ((zone.getRawOffset() + zone.getDSTSavings()) / 60000));
        queueWrite(request);
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        logger.debug("onFindDevice");
        if (start && searchDevice) return;

        searchDevice = start;

        if (start) {
            new Thread(() -> {
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
            }).start();
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {
        downloadActivityFiles();
    }

    private void downloadActivityFiles(){
        queueWrite(new ListFilesRequest());
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    private void backupFile(DownloadFileRequest request){
        try {
            File f = new File("/sdcard/qFiles/");
            if(!f.exists()) f.mkdir();

            File file = new File("/sdcard/qFiles/" + request.timeStamp);
            if(file.exists()){
                throw new Exception("file " + file.getPath() + " exists");
            }
            logger.debug("Writing file " + file.getPath());
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(request.file);
            fos.close();
            logger.debug("file written.");

            queueWrite(new EraseFileRequest((short)request.fileHandle));
        }catch (Exception e){
            e.printStackTrace();
            if(request.fileHandle > 257){
                queueWrite(new DownloadFileRequest((short)(request.fileHandle - 1)));
            }
        }

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Request request = null;
        byte[] values = characteristic.getValue();
        if (characteristic.getUuid().toString().equals("3dda0004-957f-7d4a-34a6-74696673696d") || characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")) {
            request = fileRequest;
            request.handleResponse(characteristic);
            if(request instanceof ListFilesRequest){
                if(((ListFilesRequest)request).completed) {
                    logger.debug("File count: " + ((ListFilesRequest) request).fileCount + "  size: " + ((ListFilesRequest) request).size);
                    if(((ListFilesRequest) request).fileCount == 0) return true;
                    queueWrite(new DownloadFileRequest((short)(256 + ((ListFilesRequest) request).fileCount)));
                }
            }else if(request instanceof DownloadFileRequest){
                if(((FileRequest) request).completed) {
                    logger.debug("file " + ((DownloadFileRequest)request).fileHandle + " completed: " + ((DownloadFileRequest)request).size);
                    backupFile((DownloadFileRequest)request);
                }
            }else if(request instanceof EraseFileRequest){
                if(((EraseFileRequest)request).fileHandle > 257){
                    queueWrite(new DownloadFileRequest((short)(((EraseFileRequest)request).fileHandle - 1)));
                }
            }
        } else if (characteristic.getUuid().toString().equals("3dda0002-957f-7d4a-34a6-74696673696d")) {
            request = resolveAnswer(characteristic);

            String valueString = String.valueOf(values[0]);
            for (int i = 1; i < characteristic.getValue().length; i++) {
                valueString += ", " + String.valueOf(values[i]);
            }
            if (request == null) {
                Log.d("Service", "unable to resolve " + characteristic.getUuid().toString() + ": " + valueString);
                return true;
            }
            Log.d("Service", "response: " + request.getClass().getSimpleName());
            request.handleResponse(characteristic);

            if (request instanceof BatteryLevelRequest) {
                gbDevice.setBatteryLevel(((BatteryLevelRequest) request).level);
            } else if (request instanceof GetStepGoalRequest) {
                if (this.goalListener != null) {
                    this.goalListener.onGoal(((GetStepGoalRequest) request).stepGoal);
                    this.goalListener = null;
                }
                gbDevice.addDeviceInfo(new GenericItem(ITEM_STEP_GOAL, String.valueOf(((GetStepGoalRequest) request).stepGoal)));
            } else if (request instanceof GetVibrationStrengthRequest) {
                if (this.vibrationStrengthListener != null) {
                    logger.debug("got vibration: " + ((GetVibrationStrengthRequest) request).strength);
                    this.vibrationStrengthListener.onVibrationStrength(((GetVibrationStrengthRequest) request).strength);
                    this.vibrationStrengthListener = null;
                }
                gbDevice.addDeviceInfo(new GenericItem(ITEM_VIBRATION_STRENGTH, String.valueOf(((GetVibrationStrengthRequest) request).strength)));
            } else if (fileRequest instanceof ListFilesRequest) {
                ListFilesRequest r = (ListFilesRequest) fileRequest;
                //if(r.fileCount != -1){
                if (r.completed) {
                    Log.d("Service", "FileCount: " + r.fileCount);
                    this.fileRequest = null;
                }
                //}

            }
        } else {
            Log.d("Service", "unknown shit on " + characteristic.getUuid().toString() + ":  " + characteristic.getValue()[1]);
            try {
                File charLog = new File("/sdcard/charLog.txt");
                if (!charLog.exists()) {
                    charLog.createNewFile();
                }

                FileOutputStream fos = new FileOutputStream(charLog, true);
                fos.write((new Date().toString() + ": " + characteristic.getUuid().toString() + ": " + arrayToString(characteristic.getValue())).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);

    }

    private String arrayToString(byte[] bytes){
        if(bytes.length == 0) return "";
        String s = "";
        final String chars = "0123456789ABCDEF";
        for(byte b : bytes){
            s += chars.charAt((b >> 4) & 0xF)
            + chars.charAt(b & 0xF)
            + " ";
        }
        return s.substring(0, s.length() - 1) + "\n";
    }

    private Request resolveAnswer(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        if (values[0] != 3) return null;
        return responseFilters.get(values[1]);
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }

    private void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1));
    }

    void vibrate(int vibration) {
        queueWrite(new PlayNotificationRequest(vibration, -1, -1));
    }

    private BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            PackageConfig config = extras == null ? null : (PackageConfig) intent.getExtras().get("CONFIG");
            switch (intent.getAction()) {
                case commandControl: {
                    Log.d("Service", "sending control request");
                    queueWrite(new RequestHandControlRequest());
                    if (config != null) {
                        setHands(config.getHour(), config.getMin());
                    } else {
                        setHands((short) 0, (short) 0);
                    }
                    break;
                }
                case commandUncontrol: {
                    queueWrite(new ReleaseHandsControlRequest());
                    break;
                }
                case commandSet: {
                    setHands(config.getHour(), config.getMin());

                    break;
                }
                case commandVibrate: {
                    vibrate(config.getVibration());
                    break;
                }
                case commandNotification:{
                    queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
                    break;
                }
                case commandUpdate: {
                    getTimeOffset();
                    onSetTime();
                    break;
                }
            }
        }
    };

    public interface OnVibrationStrengthListener {
        public void onVibrationStrength(int strength);
    }

    public interface OnGoalListener {
        public void onGoal(long goal);
    }
}
