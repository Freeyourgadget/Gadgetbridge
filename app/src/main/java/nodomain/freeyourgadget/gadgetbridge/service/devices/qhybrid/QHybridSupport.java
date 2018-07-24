package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.misfit.ble.obfuscated.ew;
import com.misfit.ble.setting.qmotion.QMotionEnum;
import com.misfit.ble.setting.sam.HandControl;
import com.misfit.ble.setting.sam.HandSettings;
import com.misfit.ble.setting.sam.SAMEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import d.d.qhook.requests.displayPairAnimationRequest;
import d.d.qhook.requests.fileListRequest;
import d.d.qhook.requests.getBatteryRequest;
import d.d.qhook.requests.getGoalInStepsRequest;
import d.d.qhook.requests.getVibeStrengthRequest;
import d.d.qhook.requests.playNotificationFilterVibrationRequest;
import d.d.qhook.requests.sendNotificationHandControlRequest;
import d.d.qhook.requests.setCurrentTimeServiceRequest;
import d.d.qhook.requests.setGoalInStepsRequest;
import d.d.qhook.requests.setMovingHandsRequest;
import d.d.qhook.requests.setReleaseHandsControlRequest;
import d.d.qhook.requests.setRequestHandsControlRequest;
import d.d.qhook.requests.setVibeStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.Transaction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class QHybridSupport extends AbstractBTLEDeviceSupport {
    static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    PackageConfigHelper helper;


    private volatile boolean searchDevice = false;

    public QHybridSupport(Logger logger) {
        super(logger);
    }

    //ArrayList<ew> requests = new ArrayList<>();
    List<ResponseFilter> responseFilters;

    OnVibrationStrengthListener vibrationStrengthListener;
    OnGoalListener goalListener;

    public static final String commandControl = "qhybrid_command_control";
    public static final String commandUncontrol = "qhybrid_command_uncontrol";
    public static final String commandSet = "qhybrid_command_set";
    public static final String commandVibrate = "qhybrid_command_vibrate";
    public static final String commandUpdate = "qhybrid_command_update";

    class ResponseFilter {
        int responseDataLength;
        byte[] responseDataMask;
        Class<? extends com.misfit.ble.obfuscated.ew> responseClass;

        public ResponseFilter(Class<? extends ew> responseClass, int responseDataLanegth, byte... responseDataMask) {
            this.responseDataLength = responseDataLanegth;
            this.responseDataMask = responseDataMask;
            this.responseClass = responseClass;
        }
    }


    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        IntentFilter commandFilter = new IntentFilter(commandControl);
        commandFilter.addAction(commandUncontrol);
        commandFilter.addAction(commandSet);
        commandFilter.addAction(commandVibrate);
        commandFilter.addAction(commandUpdate);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);
        //addSupportedService(UUID.fromString("3dda0002-957f-7d4a-34a6-74696673696d"));

        responseFilters = Arrays.asList(
                new ResponseFilter(getGoalInStepsRequest.class, 6, (byte) 3, (byte) 16),
                new ResponseFilter(getVibeStrengthRequest.class, 4, (byte) 3, (byte) 15, (byte) 8),
                new ResponseFilter(getBatteryRequest.class, 3, (byte) 3, (byte) 8)
        );
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
    }

    private void getBattery() {
        d.d.qhook.requests.getBatteryRequest request = new getBatteryRequest();
        request.cb();
        queueWrite(request);
    }

    public void getGoal(OnGoalListener listener) {
        this.goalListener = listener;
        d.d.qhook.requests.getGoalInStepsRequest request = new getGoalInStepsRequest();
        request.cb();
        queueWrite(request);
    }

    public void setGoal(long goal) {
        d.d.qhook.requests.setGoalInStepsRequest request = new setGoalInStepsRequest();
        request.f(goal);
        queueWrite(request);
    }

    public void getVibrationStrength(OnVibrationStrengthListener listener) {
        this.vibrationStrengthListener = listener;
        d.d.qhook.requests.getVibeStrengthRequest request = new getVibeStrengthRequest();
        request.cb();
        queueWrite(request);
    }

    public void setVibrationStrength(int strength) {
        d.d.qhook.requests.setVibeStrengthRequest request = new setVibeStrengthRequest();
        request.c((short) strength);
        queueWrite(request);
    }

    private void getXY() {
        d.d.qhook.requests.fileListRequest request = new fileListRequest();
        request.cb();

        queueWrite(request);
    }

    private void queueWrite(com.misfit.ble.obfuscated.ew request) {
        new TransactionBuilder(request.getRequestName()).write(getCharacteristic(UUID.fromString(request.getCharacteristicUUID())), request.mRequestData).queue(getQueue());
    }

    @Override
    public boolean connect() {
        logger.debug("connect attempt...");
        return super.connect();
    }

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

        playPairingAnimation();
        getBattery();
        //getXY();

        logger.debug("onServicesDiscovered");
    }

    private void playPairingAnimation() {
        d.d.qhook.requests.displayPairAnimationRequest request = new displayPairAnimationRequest();
        request.cb();
        queueWrite(request);
    }


    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        Log.d("Service", "notif from " + notificationSpec.sourceAppId);
        new Exception().printStackTrace();
        String packageName = notificationSpec.sourceName;

        PackageConfig config = helper.getSetting(packageName);
        if (config == null) return;

        Log.d("Service", "handling notification");

        int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
        if (mode == AudioManager.RINGER_MODE_SILENT && config.getRespectSilentMode()) return;


        ArrayList<HandControl> controls = new ArrayList<>(2);
        if (config.getHour() != -1)
            controls.add(new HandControl(SAMEnum.HandID.HOUR, (short) config.getHour()));
        if (config.getMin() != -1)
            controls.add(new HandControl(SAMEnum.HandID.MINUTE, (short) config.getMin()));

        SAMEnum.VibeEnum vibe = getVine(config.getVibration());

        sendNotificationHandControlRequest request = new sendNotificationHandControlRequest();
        request.a(QMotionEnum.LEDColor.BLUE, (byte) 1, vibe, 0, controls);
        queueWrite(request);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        d.d.qhook.requests.setCurrentTimeServiceRequest request = new setCurrentTimeServiceRequest();
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();
        request.a(
                (long) (millis / 1000),
                (int) (millis % 1000),
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
    public void onReboot() {

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
                d.d.qhook.requests.playNotificationFilterVibrationRequest request = new playNotificationFilterVibrationRequest();
                request.a(false, (short) 4, 1);
                BluetoothGattCharacteristic chara = getCharacteristic(UUID.fromString(request.getCharacteristicUUID()));
                int i = 0;
                while (searchDevice) {
                    new TransactionBuilder("findDevice#" + i++).write(chara, request.mRequestData).queue(getQueue());
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
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*if (requests.get(0).getRequestName().equals("fileList")) {
                if (characteristic.getUuid().toString().equals("3dda0004-957f-7d4a-34a6-74696673696d")) {
                    requests.get(0).handleResponse(characteristic.getUuid().toString(), characteristic.getValue());
                    return true;
                }
            }*/
        //if(characteristic.getValue().length == 3 && characteristic.getValue()[0] == 1 && characteristic.getValue()[1] == 15 && characteristic.getValue()[2] == 8) return true;
        com.misfit.ble.obfuscated.ew request = resolveAnswer(characteristic);
        String values = String.valueOf(characteristic.getValue()[0]);
        for (int i = 1; i < characteristic.getValue().length; i++) {
            values += ", " + String.valueOf(characteristic.getValue()[i]);
        }
        if (request == null) {
            Log.d("Service", "unable to resolve " + characteristic.getUuid().toString() + ": " + values);
            return true;
        }
        request.handleResponse(characteristic.getUuid().toString(), characteristic.getValue());

        logger.debug(request.getRequestName() + " response: " + request.getResponseDescriptionJSON().toString() + "   " + values);

        switch (request.getRequestName()) {
            case "getBattery": {
                gbDevice.setBatteryLevel(((getBatteryRequest.a) request.getResponse()).ly);
                logger.debug("battery level: " + gbDevice.getBatteryLevel());
                break;
            }
            case "getVibeStrength": {
                if (this.vibrationStrengthListener != null) {
                    this.vibrationStrengthListener.onVibrationStrength(((getVibeStrengthRequest) request).dm().nH);
                    this.vibrationStrengthListener = null;
                }
                break;
            }
            case "getGoalInSteps": {
                if (this.goalListener != null) {
                    this.goalListener.onGoal(((getGoalInStepsRequest) request).cI().mb);
                    this.goalListener = null;
                }
                break;
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);

    }

    private com.misfit.ble.obfuscated.ew resolveAnswer(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        for (ResponseFilter filter : responseFilters) {
            if (filter.responseDataLength != values.length) continue;
            for (int i = 0; i < filter.responseDataMask.length; i++) {
                if (values[i] != filter.responseDataMask[i]) continue;
            }
            try {
                return filter.responseClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        logger.debug("char written: " + status);
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }

    private void setHands(int hour, int minute) {
        ArrayList<HandSettings> controls = new ArrayList<>();
        if (hour > -1) {
            controls.add(new HandSettings(SAMEnum.HandID.HOUR, hour, SAMEnum.HandMovingDirection.SHORTEST_PATH, SAMEnum.HandMovingSpeed.FULL));
        }
        if (minute > -1) {
            controls.add(new HandSettings(SAMEnum.HandID.MINUTE, minute, SAMEnum.HandMovingDirection.SHORTEST_PATH, SAMEnum.HandMovingSpeed.FULL));
        }
        d.d.qhook.requests.setMovingHandsRequest request = new setMovingHandsRequest();
        request.a(SAMEnum.HandMovingType.POSITION, controls);
        queueWrite(request);
    }

    void vibrate(int vibration) {
        sendNotificationHandControlRequest request = new sendNotificationHandControlRequest();
        request.a(QMotionEnum.LEDColor.BLUE, (byte) 1, getVine(vibration), 0, Collections.emptyList());
        queueWrite(request);
    }

    private SAMEnum.VibeEnum getVine(int vibration) {
        for (SAMEnum.VibeEnum v : SAMEnum.VibeEnum.values()) {
            if (v.getId() == vibration) {
                return v;
            }
        }
        return SAMEnum.VibeEnum.SINGLE_SHORT_VIBE;
    }

    BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            PackageConfig config = extras == null ? null : (PackageConfig) intent.getExtras().get("CONFIG");
            switch (intent.getAction()) {
                case commandControl: {
                    Log.d("Service", "sending control request");
                    d.d.qhook.requests.setRequestHandsControlRequest request = new setRequestHandsControlRequest();
                    request.a((byte) 1, false, false);
                    queueWrite(request);
                    setHands(0, 0);
                    break;
                }
                case commandUncontrol: {
                    d.d.qhook.requests.setReleaseHandsControlRequest request = new setReleaseHandsControlRequest();
                    request.i(0);
                    queueWrite(request);
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
                case commandUpdate: {

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
