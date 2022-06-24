package nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Support;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.um25.Activity.DataActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data.CaptureGroup;
import nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data.MeasurementData;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class UM25Support extends UM25BaseSupport {
    public static final String UUID_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHAR    = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public static final String ACTION_MEASUREMENT_TAKEN = "com.nodomain.gadgetbridge.um25.MEASUREMENT_TAKEN";
    public static final String ACTION_RESET_STATS = "com.nodomain.gadgetbridge.um25.RESET_STATS";
    public static final String EXTRA_KEY_MEASUREMENT_DATA = "EXTRA_MEASUREMENT_DATA";
    public static final int LOOP_DELAY = 500;

    private final byte[] COMMAND_UPDATE = new byte[]{(byte) 0xF0};
    private final byte[] COMMAND_RESET_STATS = new byte[]{(byte) 0xF4};
    private final int PAYLOAD_LENGTH = 130;

    private ByteBuffer buffer = ByteBuffer.allocate(PAYLOAD_LENGTH);

    private static final  Logger logger = LoggerFactory.getLogger(UM25Support.class);

    SharedPreferences preferences;

    private boolean notifyOnCurrentThreshold;
    private int notificationCurrentThreshold;
    private boolean wasOverNotificationCurrent = false;
    private long lastOverThresholdTimestamp = 0;

    public UM25Support() {
        super(logger);
        addSupportedService(UUID.fromString(UUID_SERVICE));
        this.buffer.mark();
    }

    BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!ACTION_RESET_STATS.equals(intent.getAction())){
                return;
            }
            new TransactionBuilder("reset stats")
                    .write(getCharacteristic(UUID.fromString(UUID_CHAR)), COMMAND_RESET_STATS)
                    .queue(getQueue());
        }
    };

    void readPreferences(){
        notifyOnCurrentThreshold = preferences.getBoolean(DeviceSettingsPreferenceConst.PREF_UM25_SHOW_THRESHOLD_NOTIFICATION, false);
        notificationCurrentThreshold = Integer.parseInt(preferences.getString(DeviceSettingsPreferenceConst.PREF_UM25_SHOW_THRESHOLD, "100"));
    }

    @Override
    public void onSendConfiguration(String config) {
        readPreferences();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        preferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        readPreferences();

        getDevice().setFirmwareVersion("1.0");

        return builder
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()))
                .notify(getCharacteristic(UUID.fromString(UUID_CHAR)), true)
                .add(new BtLEAction(null) {
                    @Override
                    public boolean expectsResult() {
                        return false;
                    }

                    @Override
                    public boolean run(BluetoothGatt gatt) {
                        logger.debug("initialized, starting timers");
                        LocalBroadcastManager.getInstance(getContext())
                                .registerReceiver(
                                        resetReceiver,
                                        new IntentFilter(ACTION_RESET_STATS)
                                );
                        startLoop();
                        return true;
                    }
                })
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(resetReceiver);
    }

    private void startLoop(){
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                sendReadCommand();
            }
        }, 0, LOOP_DELAY, TimeUnit.MILLISECONDS);
    }

    private void sendReadCommand(){
        logger.debug("sending read command");
        buffer.reset();
        new TransactionBuilder("send read command")
                .write(getCharacteristic(UUID.fromString(UUID_CHAR)), COMMAND_UPDATE)
                .queue(getQueue());
        logger.debug("sent command");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(!characteristic.getUuid().toString().equals(UUID_CHAR)) return false;

        try{
            buffer.put(characteristic.getValue());

            if(buffer.position() == PAYLOAD_LENGTH){
                handlePayload(buffer);
            }
        }catch (BufferOverflowException e){
            logger.error("buffer overflow");
        }

        return true;
    }

    private void handleCurrentNotification(int currentMa){
        logger.debug("current: " + currentMa);

        if(!notifyOnCurrentThreshold){
            return;
        }

        boolean isOverNotificationCurrent = currentMa > notificationCurrentThreshold;

        long now = System.currentTimeMillis();

        if(isOverNotificationCurrent){
            lastOverThresholdTimestamp = now;
            wasOverNotificationCurrent = true;
            return;
        }
        long deltaSinceOverThreshold = now - lastOverThresholdTimestamp;

        if(deltaSinceOverThreshold < 5000){
            // must be below threshold for over certain time before triggering notification
            return;
        }

        if(wasOverNotificationCurrent){
            // handle change from over threshold to below threshold
            wasOverNotificationCurrent = false;
            Intent activityIntent = new Intent(getContext(), DataActivity.class);
            Notification notification = new NotificationCompat.Builder(getContext(), GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                    .setSmallIcon(R.drawable.ic_notification_low_battery)
                    .setContentTitle("USB current")
                    .setContentText("USB current below threshold")
                    .setContentIntent(PendingIntent.getActivity(getContext(), 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();

            GB.notify(
                    GB.NOTIFICATION_ID_LOW_BATTERY,
                    notification,
                    getContext()
            );
        }
    }

    private void handlePayload(ByteBuffer payload){
        String payloadString = StringUtils.bytesToHex(payload.array());
        payloadString = payloadString.replaceAll("(..)", "$1 ");
        logger.debug("payload: " + payloadString);
        payload.order(ByteOrder.BIG_ENDIAN);
        int voltage = payload.getShort(2);
        int current = payload.getShort(4);
        int wattage = payload.getShort(8);
        int temperatureCelsius = payload.getShort(10);
        int temperatureFahrenheit = payload.getShort(12);

        final int STORAGE_START = 16;

        CaptureGroup[] groups = new CaptureGroup[10];

        for(int i = 0; i < 10; i++){
            groups[i] = new CaptureGroup(
                    i,
                    payload.getInt(STORAGE_START + i * 4 + 0),
                    payload.getInt(STORAGE_START + i * 4 + 4)
            );
        }

        int voltagePositive = payload.getShort(96);
        int voltageNegative = payload.getShort(98);
        int chargedCurrent = payload.getInt(102);
        int chargedWattage = payload.getInt(106);
        int thresholdCurrent = payload.get(111);
        int chargingSeconds = payload.getInt(112);
        int cableResistance = payload.getInt(122);

        MeasurementData data = new MeasurementData(
                voltage,
                current,
                wattage,
                temperatureCelsius,
                temperatureFahrenheit,
                groups,
                voltagePositive,
                voltageNegative,
                chargedCurrent,
                chargedWattage,
                thresholdCurrent,
                chargingSeconds,
                cableResistance
        );

        Intent measurementIntent = new Intent(ACTION_MEASUREMENT_TAKEN);

        measurementIntent.putExtra(EXTRA_KEY_MEASUREMENT_DATA, data);

        handleCurrentNotification(current / 10);

        LocalBroadcastManager.getInstance(getContext())
                .sendBroadcast(measurementIntent);
    }
}
