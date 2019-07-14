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
import android.net.wifi.aware.Characteristics;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.BatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.EraseFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.FileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ListFilesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.OTAEnterRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.OTAEraseRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetCurrentTimeServiceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SettingsFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.UploadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.VibrateRequest;

public class QHybridSupport extends QHybridBaseSupport {
    public static final String QHYBRID_COMMAND_CONTROL = "qhybrid_command_control";
    public static final String QHYBRID_COMMAND_UNCONTROL = "qhybrid_command_uncontrol";
    public static final String QHYBRID_COMMAND_SET = "qhybrid_command_set";
    public static final String QHYBRID_COMMAND_VIBRATE = "qhybrid_command_vibrate";
    public static final String QHYBRID_COMMAND_UPDATE = "qhybrid_command_update";
    public static final String QHYBRID_COMMAND_NOTIFICATION = "qhybrid_command_notification";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";

    private static final String ITEM_STEP_GOAL = "STEP_GOAL";
    private static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";


    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    private PackageConfigHelper helper;

    private volatile boolean searchDevice = false;

    private int lastButtonIndex = -1;

    private final SparseArray<Request> responseFilters = new SparseArray<>();

    private OnVibrationStrengthListener vibrationStrengthListener;
    private OnGoalListener goalListener;
    private OnButtonOverwriteListener buttonOverwriteListener;

    private Request fileRequest = null;

    private boolean dumpInited = false;

    private long timeOffset;

    private UploadFileRequest uploadFileRequest;

    private PendingIntent dumpIntent;
    private PendingIntent stepIntent;

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        IntentFilter commandFilter = new IntentFilter(QHYBRID_COMMAND_CONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_UNCONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_SET);
        commandFilter.addAction(QHYBRID_COMMAND_VIBRATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);
        fillResponseList();
    }

    private void fillResponseList() {

        Class<? extends Request>[] classes = new Class[]{
                BatteryLevelRequest.class,
                GetStepGoalRequest.class,
                GetVibrationStrengthRequest.class,
                GetCurrentStepCountRequest.class,
                OTAEnterRequest.class
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
            }
        }
    }

    private void getTimeOffset() {
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
        getContext().unregisterReceiver(dumpReceiver);
        ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).cancel(dumpIntent);
        getContext().unregisterReceiver(stepReceiver);
        ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).cancel(stepIntent);
        dumpInited = false;
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
        if (request instanceof FileRequest) this.fileRequest = request;
    }

    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            queueWrite(new GetCurrentStepCountRequest());
        }
    };

    private final BroadcastReceiver dumpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Dump", "dumping...");
            downloadActivityFiles();
        }
    };

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        for (int i = 2; i <= 7; i++)
            builder.notify(getCharacteristic(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d")), true);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        helper = new PackageConfigHelper(getContext());

        if (!dumpInited) {
            getContext().registerReceiver(dumpReceiver, new IntentFilter("dumpReceiver2"));
            getContext().registerReceiver(stepReceiver, new IntentFilter("stepDumpReceiver"));
            dumpIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("dumpReceiver2"), PendingIntent.FLAG_UPDATE_CURRENT);
            stepIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("stepDumpReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);
            ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, AlarmManager.INTERVAL_HOUR, dumpIntent);
            ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, AlarmManager.INTERVAL_HOUR / 60, stepIntent);
            dumpInited = true;
        }
        getTimeOffset();
        return builder;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        super.onServicesDiscovered(gatt);

        playAnimation();
        queueWrite(new BatteryLevelRequest());

        logger.debug("onServicesDiscovered");
    }

    private void playAnimation() {
        queueWrite(new AnimationRequest());
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

        playNotification(config);
    }

    public void playNotification(PackageConfig config){
        queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
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
    public void onFindDevice(boolean start) {
        logger.debug("onFindDevice");
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
        //downloadActivityFiles();
        //queueWrite(new GetCurrentStepCountRequest());
        // queueWrite(new EventStreamRequest((short)4));
        // queueWrite(new OTAEraseRequest(0));
        // queueWrite(new OTAResetRequest());
        new UploadFileRequest((short)00, new byte[]{0x01, 0x00, 0x08, 0x01, 0x01, 0x0C, 0x00, (byte)0xBD, 0x01, 0x30, 0x71, (byte)0xFF, 0x05, 0x00, 0x01, 0x00});
    }

    public void overwriteButtons(OnButtonOverwriteListener listener){
        uploadFileRequest = new UploadFileRequest((short) 0x0800, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01,
                (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00
        });
        this.buttonOverwriteListener = listener;
        queueWrite(uploadFileRequest);
    }

    private void downloadActivityFiles() {
        queueWrite(new ListFilesRequest());
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

            queueWrite(new EraseFileRequest((short) request.fileHandle));
        } catch (Exception e) {
            e.printStackTrace();
            if (request.fileHandle > 257) {
                queueWrite(new DownloadFileRequest((short) (request.fileHandle - 1)));
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()){
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d":{
                return handleFileDownloadCharacteristic(characteristic);
            }
            case "3dda0007-957f-7d4a-34a6-74696673696d":{
                return handleFileUploadCharacteristic(characteristic);
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d":{
                return handleBasicCharacteristic(characteristic);
            }
            case "3dda0006-957f-7d4a-34a6-74696673696d":{
                return handleButtonCharacteristic(characteristic);
            }
            default:{
                Log.d("Service", "unknown shit on " + characteristic.getUuid().toString() + ":  " + arrayToString(characteristic.getValue()));
                try {
                    File charLog = new File("/sdcard/qFiles/charLog.txt");
                    if (!charLog.exists()) {
                        charLog.createNewFile();
                    }

                    FileOutputStream fos = new FileOutputStream(charLog, true);
                    fos.write((new Date().toString() + ": " + characteristic.getUuid().toString() + ": " + arrayToString(characteristic.getValue())).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handleFileUploadCharacteristic(BluetoothGattCharacteristic characteristic) {
        uploadFileRequest.handleResponse(characteristic);

        switch (uploadFileRequest.state){
            case ERROR:
                buttonOverwriteListener.OnButtonOverwrite(false);
                break;
            case UPLOAD:
                for(byte[] packet : this.uploadFileRequest.packets){
                    new TransactionBuilder("File upload").write(characteristic, packet).queue(getQueue());
                }
                break;
            case UPLOADED:
                buttonOverwriteListener.OnButtonOverwrite(true);
                break;
        }
        return true;
    }

    private boolean handleButtonCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length != 11) {
            logger.debug("wrong button message");
            return true;
        }
        int index = value[6] & 0xFF;
        int button = value[8] >> 4 & 0xFF;

        if (index != this.lastButtonIndex) {
            lastButtonIndex = index;
            logger.debug("Button press on button " + button);

            Intent i = new Intent(QHYBRID_EVENT_BUTTON_PRESS);
            i.putExtra("BUTTON", button);

            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.put(new byte[]{0x01, 0x00, 0x08});
            buffer.put(value, 2, 8);
            buffer.put(new byte[]{(byte)0xFF, 0x05, 0x00, 0x01, 0x00});

            UploadFileRequest request = new UploadFileRequest((short)0, buffer.array());
            for(byte[] packet : request.packets){
                new TransactionBuilder("File upload").write(getCharacteristic(UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d")), packet).queue(getQueue());
            }

            getContext().sendBroadcast(i);
        }
        return true;
    }

    private boolean handleBasicCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        Request request;
        request = resolveAnswer(characteristic);

        StringBuilder valueString = new StringBuilder(String.valueOf(values[0]));
        for (int i = 1; i < characteristic.getValue().length; i++) {
            valueString.append(", ").append(values[i]);
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
        } else if (request instanceof GetCurrentStepCountRequest) {
            int steps = ((GetCurrentStepCountRequest) request).steps;
            logger.debug("get current steps: " + steps);
            try {
                File f = new File("/sdcard/qFiles/");
                if (!f.exists()) f.mkdir();

                File file = new File("/sdcard/qFiles/steps");
                if (!file.exists()) {
                    file.createNewFile();
                }
                logger.debug("Writing file " + file.getPath());
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write((System.currentTimeMillis() + ": " + steps + "\n").getBytes());
                fos.close();
                logger.debug("file written.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request instanceof OTAEnterRequest) {
            if (((OTAEnterRequest) request).success) {
                fileRequest = new OTAEraseRequest(1024 << 16);
                queueWrite(fileRequest);
            }
        }
        return true;
    }

    private boolean handleFileDownloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        Request request;
        request = fileRequest;
        request.handleResponse(characteristic);
        if (request instanceof ListFilesRequest) {
            if (((ListFilesRequest) request).completed) {
                logger.debug("File count: " + ((ListFilesRequest) request).fileCount + "  size: " + ((ListFilesRequest) request).size);
                if (((ListFilesRequest) request).fileCount == 0) return true;
                queueWrite(new DownloadFileRequest((short) (256 + ((ListFilesRequest) request).fileCount)));
            }
        } else if (request instanceof DownloadFileRequest) {
            if (((FileRequest) request).completed) {
                logger.debug("file " + ((DownloadFileRequest) request).fileHandle + " completed: " + ((DownloadFileRequest) request).size);
                backupFile((DownloadFileRequest) request);
            }
        } else if (request instanceof EraseFileRequest) {
            if (((EraseFileRequest) request).fileHandle > 257) {
                queueWrite(new DownloadFileRequest((short) (((EraseFileRequest) request).fileHandle - 1)));
            }
        }
        return true;
    }

    private String arrayToString(byte[] bytes) {
        if (bytes.length == 0) return "";
        StringBuilder s = new StringBuilder();
        final String chars = "0123456789ABCDEF";
        for (byte b : bytes) {
            s.append(chars.charAt((b >> 4) & 0xF)).append(chars.charAt(b & 0xF)).append(" ");
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

    private void vibrate(int vibration) {
        queueWrite(new PlayNotificationRequest(vibration, -1, -1));
    }

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            PackageConfig config = extras == null ? null : (PackageConfig) intent.getExtras().get("CONFIG");
            switch (intent.getAction()) {
                case QHYBRID_COMMAND_CONTROL: {
                    Log.d("Service", "sending control request");
                    queueWrite(new RequestHandControlRequest());
                    if (config != null) {
                        setHands(config.getHour(), config.getMin());
                    } else {
                        setHands((short) 0, (short) 0);
                    }
                    break;
                }
                case QHYBRID_COMMAND_UNCONTROL: {
                    queueWrite(new ReleaseHandsControlRequest());
                    break;
                }
                case QHYBRID_COMMAND_SET: {
                    setHands(config.getHour(), config.getMin());

                    break;
                }
                case QHYBRID_COMMAND_VIBRATE: {
                    vibrate(config.getVibration());
                    break;
                }
                case QHYBRID_COMMAND_NOTIFICATION: {
                    queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
                    break;
                }
                case QHYBRID_COMMAND_UPDATE: {
                    getTimeOffset();
                    onSetTime();
                    break;
                }
            }
        }
    };

    public interface OnVibrationStrengthListener {
        void onVibrationStrength(int strength);
    }

    public interface OnGoalListener {
        void onGoal(long goal);
    }

    public interface OnButtonOverwriteListener{
        void OnButtonOverwrite(boolean success);
    }
}
