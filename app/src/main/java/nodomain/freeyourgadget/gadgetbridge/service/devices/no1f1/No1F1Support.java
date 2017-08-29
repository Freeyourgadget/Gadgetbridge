package nodomain.freeyourgadget.gadgetbridge.service.devices.no1f1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.no1f1.No1F1Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class No1F1Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(No1F1Support.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public No1F1Support() {
        super(LOG);
        addSupportedService(No1F1Constants.UUID_SERVICE_NO1);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(No1F1Constants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(No1F1Constants.UUID_CHARACTERISTIC_CONTROL);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_FIRMWARE_VERSION});
        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_BATTERY});

        ActivityUser activityUser = new ActivityUser();
        byte[] msg = new byte[]{
                No1F1Constants.CMD_USER_DATA,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        msg[2]=(byte) Math.round(activityUser.getHeightCm() * 0.43); // step length in cm
        msg[4]=(byte) activityUser.getWeightKg();
        msg[5]=5; // screen on time
        msg[8]=(byte) (activityUser.getStepsGoal()/256);
        msg[9]=(byte) (activityUser.getStepsGoal()%256);
        msg[10]=1; // unknown
        msg[11]=(byte)0xff; // unknown
        msg[13]=(byte) activityUser.getAge();
        if (activityUser.getGender() == ActivityUser.GENDER_FEMALE)
            msg[14]=2; // female
        else
            msg[14]=1; // male

        builder.write(ctrlCharacteristic, msg);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case No1F1Constants.CMD_FIRMWARE_VERSION:
                versionCmd.fwVersion=new String(Arrays.copyOfRange(data,1,data.length));
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case No1F1Constants.CMD_BATTERY:
                batteryCmd.level = data[1];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + data[1]);
                return true;
            case No1F1Constants.CMD_DATETIME:
                LOG.info("Time is set to: " + (data[1] * 256 + ((int)data[2] & 0xff)) + "-" + data[3] + "-" + data[4] + " " + data[5] + ":" + data[6] + ":" + data[7]);
                return true;
            case No1F1Constants.CMD_USER_DATA:
                LOG.info("User data updated");
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + Arrays.toString(data));
                return true;
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("time");
            Calendar c = GregorianCalendar.getInstance();
            int year = c.get(Calendar.YEAR);
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_DATETIME,
                    (byte) ((year / 256) & 0xff),
                    (byte) (year % 256),
                    (byte) (c.get(Calendar.MONTH)+1),
                    (byte) c.get(Calendar.DAY_OF_MONTH),
                    (byte) c.get(Calendar.HOUR),
                    (byte) c.get(Calendar.MINUTE),
                    (byte) c.get(Calendar.SECOND)
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        }catch(IOException e){

        }
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
    public void onAppConfiguration(UUID appUuid, String config) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchActivityData() {

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
        try {
            TransactionBuilder builder = performInitialized("findMe");
            byte[] msg = new byte[]{No1F1Constants.CMD_ALARM, 0, 0, 0, 0, 0, 2, 1};
            if (start)
            {
                msg[4]=1; // vibrate 10 times with duration of 1 second
                msg[5]=10; // sending zeroes stops vibration immediately
            }
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
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
    public boolean useAutoConnect() {
        return true;
    }
}
