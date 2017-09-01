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

import static org.apache.commons.lang3.math.NumberUtils.min;

public class No1F1Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(No1F1Support.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

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

        sendSettings(builder);

        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_FIRMWARE_VERSION});
        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_BATTERY});

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
                versionCmd.fwVersion = new String(Arrays.copyOfRange(data, 1, data.length));
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case No1F1Constants.CMD_BATTERY:
                batteryCmd.level = data[1];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + data[1]);
                return true;
            case No1F1Constants.CMD_DATETIME:
                LOG.info("Time is set to: " + (data[1] * 256 + ((int) data[2] & 0xff)) + "-" + data[3] + "-" + data[4] + " " + data[5] + ":" + data[6] + ":" + data[7]);
                return true;
            case No1F1Constants.CMD_USER_DATA:
                LOG.info("User data updated");
                return true;
            case No1F1Constants.CMD_NOTIFICATION:
            case No1F1Constants.CMD_ICON:
                return true;
            default:
                LOG.warn("Unhandled characteristic change: " + characteristicUUID + " code: " + Arrays.toString(data));
                return true;
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                showNotification(No1F1Constants.NOTIFICATION_SMS, notificationSpec.phoneNumber, notificationSpec.body);
                setVibration(1, 3);
                break;
            default:
                showIcon(No1F1Constants.ICON_WECHAT);
                setVibration(1, 2);
                break;
        }
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
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            showNotification(No1F1Constants.NOTIFICATION_CALL, callSpec.name, callSpec.number);
            setVibration(3, 5);
        } else {
            stopNotification();
            setVibration(0, 0);
        }
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
        if (start)
            setVibration(3, 10);
        else
            setVibration(0, 0);
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

    private void sendSettings(TransactionBuilder builder) {
        // TODO Create custom settings page for changing hardcoded values

        // set date and time
        Calendar c = GregorianCalendar.getInstance();
        byte[] datetimeBytes = new byte[]{
                No1F1Constants.CMD_DATETIME,
                (byte) ((c.get(Calendar.YEAR) / 256) & 0xff),
                (byte) (c.get(Calendar.YEAR) % 256),
                (byte) (c.get(Calendar.MONTH) + 1),
                (byte) c.get(Calendar.DAY_OF_MONTH),
                (byte) c.get(Calendar.HOUR_OF_DAY),
                (byte) c.get(Calendar.MINUTE),
                (byte) c.get(Calendar.SECOND)
        };
        builder.write(ctrlCharacteristic, datetimeBytes);

        // set user data
        ActivityUser activityUser = new ActivityUser();
        byte[] userBytes = new byte[]{
                No1F1Constants.CMD_USER_DATA,
                0,
                (byte) Math.round(activityUser.getHeightCm() * 0.43), // step length in cm
                0,
                (byte) activityUser.getWeightKg(),
                5, // screen on time
                0,
                0,
                (byte) (activityUser.getStepsGoal() / 256),
                (byte) (activityUser.getStepsGoal() % 256),
                1, // unknown
                (byte) 0xff, // unknown
                0,
                (byte) activityUser.getAge(),
                0
        };
        if (activityUser.getGender() == ActivityUser.GENDER_FEMALE)
            userBytes[14] = 2; // female
        else
            userBytes[14] = 1; // male
        builder.write(ctrlCharacteristic, userBytes);
    }

    private void setVibration(int duration, int count) {
        try {
            TransactionBuilder builder = performInitialized("vibrate");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_ALARM,
                    0,
                    0,
                    0,
                    (byte) duration,
                    (byte) count,
                    2,
                    1
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
        }
    }

    private void showIcon(int iconId) {
        try {
            TransactionBuilder builder = performInitialized("showIcon");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_ICON,
                    (byte) iconId
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
        }
    }

    private void showNotification(int type, String header, String body) {
        try {
            // TODO Add transliteration.
            TransactionBuilder builder = performInitialized("showNotification");
            int length;
            byte[] bytes;
            byte[] msg;

            // send header
            bytes = header.toString().getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = No1F1Constants.CMD_NOTIFICATION;
            msg[1] = No1F1Constants.NOTIFICATION_HEADER;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            // send body
            bytes = header.toString().getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = No1F1Constants.CMD_NOTIFICATION;
            msg[1] = (byte) type;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            performConnected(builder.getTransaction());
        } catch (IOException e) {
        }
    }

    private void stopNotification() {
        try {
            TransactionBuilder builder = performInitialized("clearNotification");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_NOTIFICATION,
                    No1F1Constants.NOTIFICATION_STOP
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
        }
    }
}
