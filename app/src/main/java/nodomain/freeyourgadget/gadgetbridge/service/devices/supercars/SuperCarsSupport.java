package nodomain.freeyourgadget.gadgetbridge.service.devices.supercars;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.supercars.SuperCarsConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;

public class SuperCarsSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SuperCarsSupport.class);
    public static final String COMMAND_DRIVE_CONTROL = "nodomain.freeyourgadget.gadgetbridge.supercars.command.DRIVE_CONTROL";
    public static final String EXTRA_DIRECTION = "EXTRA_DIRECTION";
    public static final String EXTRA_MODE = "EXTRA_MODE";

    public SuperCarsSupport() {
        super(LOG);
        addSupportedService(SuperCarsConstants.SERVICE_UUID_FFF);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF4), true); //for battery

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_DRIVE_CONTROL);
        broadcastManager.registerReceiver(commandReceiver, filter);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        LOG.debug("name " + gbDevice.getName());

        return builder;
    }

    BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(COMMAND_DRIVE_CONTROL)) {
                SuperCarsSupport.this.setDirection(
                        (SuperCarsConstants.SpeedModes) intent.getSerializableExtra(EXTRA_MODE),
                        (SuperCarsConstants.Directions) intent.getSerializableExtra(EXTRA_DIRECTION)

                );
            }
        }
    };

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        byte[] data = characteristic.getValue();
        byte[] decodedData = new byte[0];
        try {
            decodedData = CryptoUtils.decryptAES(data, SuperCarsConstants.aes_key);
        } catch (Exception e) {
            LOG.error("Error while decoding received data");
        }

        if (decodedData.length == 16) {
            GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
            batteryEvent.state = BatteryState.BATTERY_NORMAL;
            batteryEvent.level = decodedData[4];
            evaluateGBDeviceEvent(batteryEvent);
        }
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

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
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void setDirection(SuperCarsConstants.SpeedModes speedModes, SuperCarsConstants.Directions direction) {

        byte[] command = SuperCarsConstants.get_directions_data(speedModes, direction);
        TransactionBuilder builder = new TransactionBuilder("setDirections");
        BluetoothGattCharacteristic writeCharacteristic = getCharacteristic(SuperCarsConstants.CHARACTERISTIC_UUID_FFF1);
        builder.write(writeCharacteristic, command);
        builder.queue(getQueue());
    }

    @Override
    public void dispose() {
        super.dispose();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
    }

}

