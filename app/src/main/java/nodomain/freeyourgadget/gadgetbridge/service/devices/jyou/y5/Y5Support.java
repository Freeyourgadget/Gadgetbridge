package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.y5;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.JYouActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.RealtimeSamplesSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class Y5Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Y5Support.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

    private RealtimeSamplesSupport realtimeSamplesSupport;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public Y5Support() {
        super(LOG);
        addSupportedService(JYouConstants.UUID_SERVICE_JYOU);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(JYouConstants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(JYouConstants.UUID_CHARACTERISTIC_CONTROL);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        syncSettings(builder);

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
            case JYouConstants.RECEIVE_HISTORY_SLEEP_COUNT:
                LOG.info("onCharacteristicChanged: " + data[0]);
                return true;
            case JYouConstants.RECEIVE_BLOOD_PRESSURE:
                int heartRate = data[2];
                int bloodPressureHigh = data[3];
                int bloodPressureLow = data[4];
                int bloodOxygen = data[5];
                int Fatigue = data[6];
                LOG.info("RECEIVE_BLOOD_PRESSURE: Heart rate: " + heartRate + " Pressure high: " + bloodPressureHigh+ " pressure low: " + bloodPressureLow);
                return true;
            case JYouConstants.RECEIVE_DEVICE_INFO:
                int model = data[7];
                int fwVerNum = data[4] & 0xFF;
                versionCmd.fwVersion = (fwVerNum / 100) + "." + ((fwVerNum % 100) / 10) + "." + ((fwVerNum % 100) % 10);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case JYouConstants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[8];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case JYouConstants.RECEIVE_STEPS_DATA:
                int steps = ByteBuffer.wrap(data, 5, 4).getInt();
                LOG.info("Number of walked steps: " + steps);
                handleRealtimeSteps(steps);
                return true;
            case JYouConstants.RECEIVE_HEARTRATE:
                handleHeartrate(data[8]);
                return true;
            case JYouConstants.RECEIVE_WATCH_MAC:
                return true;
            case JYouConstants.RECEIVE_GET_PHOTO:
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                return true;
        }
    }

    private void handleRealtimeSteps(int value) {
        //todo Call on connect the device
        if (LOG.isDebugEnabled()) {
            LOG.debug("realtime steps: " + value);
        }
        getRealtimeSamplesSupport().setSteps(value);
    }

    private void handleHeartrate(int value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("heart rate: " + value);
        }
        RealtimeSamplesSupport realtimeSamplesSupport = getRealtimeSamplesSupport();
        realtimeSamplesSupport.setHeartrateBpm(value);
        if (!realtimeSamplesSupport.isRunning()) {
            // single shot measurement, manually invoke storage and result publishing
            realtimeSamplesSupport.triggerCurrentSample();
        }
    }

    public JYouActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        JYouActivitySample sample = new JYouActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);
        return sample;
    }

    private void enableRealtimeSamplesTimer(boolean enable) {
        if (enable) {
            getRealtimeSamplesSupport().start();
        } else {
            if (realtimeSamplesSupport != null) {
                realtimeSamplesSupport.stop();
            }
        }
    }

    private RealtimeSamplesSupport getRealtimeSamplesSupport() {
        if (realtimeSamplesSupport == null) {
            realtimeSamplesSupport = new RealtimeSamplesSupport(1000, 1000) {
                @Override
                public void doCurrentSample() {

                    try (DBHandler handler = GBApplication.acquireDB()) {
                        DaoSession session = handler.getDaoSession();
                        int ts = (int) (System.currentTimeMillis() / 1000);
                        JYouSampleProvider provider = new JYouSampleProvider(gbDevice, session);
                        JYouActivitySample sample = createActivitySample(DBHelper.getDevice(getDevice(), session), DBHelper.getUser(session), ts, provider);
                        sample.setHeartRate(getHeartrateBpm());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                        sample.setRawKind(JYouSampleProvider.TYPE_ACTIVITY); // to make it visible in the charts TODO: add a MANUAL kind for that?

                        provider.addGBActivitySample(sample);

                        // set the steps only afterwards, since realtime steps are also recorded
                        // in the regular samples and we must not count them twice
                        // Note: we know that the DAO sample is never committed again, so we simply
                        // change the value here in memory.
                        sample.setSteps(getSteps());
                        if(steps > 1){
                            LOG.debug("Have steps: " + getSteps());
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("realtime sample: " + sample);
                        }

                        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

                    } catch (Exception e) {
                        LOG.warn("Unable to acquire db for saving realtime samples", e);
                    }
                }
            };
        }
        return realtimeSamplesSupport;
    }

    private void syncDateAndTime(TransactionBuilder builder) {
        Calendar cal = Calendar.getInstance();
        String strYear = String.valueOf(cal.get(Calendar.YEAR));
        byte year1 = (byte)Integer.parseInt(strYear.substring(0, 2));
        byte year2 = (byte)Integer.parseInt(strYear.substring(2, 4));
        byte month = (byte)cal.get(Calendar.MONTH);
        byte day = (byte)cal.get(Calendar.DAY_OF_MONTH);
        byte hour = (byte)cal.get(Calendar.HOUR_OF_DAY);
        byte minute = (byte)cal.get(Calendar.MINUTE);
        byte second = (byte)cal.get(Calendar.SECOND);
        byte weekDay = (byte)cal.get(Calendar.DAY_OF_WEEK);

        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_DATE_AND_TIME,
                (year1 << 24) | (year2 << 16) | (month << 8) | day,
                (hour << 24) | (minute << 16) | (second << 8) | weekDay
        ));
    }

    private void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);
    }

    private void showNotification(byte icon, String title, String message) {
        try {
            TransactionBuilder builder = performInitialized("ShowNotification");

            byte[] titleBytes = stringToUTF8Bytes(title, 16);
            byte[] messageBytes = stringToUTF8Bytes(message, 80);

            for (int i = 1; i <= 7; i++)
            {
                byte[] currentPacket = new byte[20];
                currentPacket[0] = JYouConstants.CMD_ACTION_SHOW_NOTIFICATION;
                currentPacket[1] = 7;
                currentPacket[2] = (byte)i;
                switch(i) {
                    case 1:
                        currentPacket[4] = icon;
                        break;
                    case 2:
                        if (titleBytes != null) {
                            System.arraycopy(titleBytes, 0, currentPacket, 3, 6);
                            System.arraycopy(titleBytes, 6, currentPacket, 10, 10);
                        }
                        break;
                    default:
                        if (messageBytes != null) {
                            System.arraycopy(messageBytes, 16 * (i - 3), currentPacket, 3, 6);
                            System.arraycopy(messageBytes, 6 + 16 * (i - 3), currentPacket, 10, 10);
                        }
                        break;
                }
                builder.write(ctrlCharacteristic, currentPacket);
            }
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String notificationTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte icon;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = JYouConstants.ICON_SMS;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                icon = JYouConstants.ICON_FACEBOOK;
                break;
            case TWITTER:
                icon = JYouConstants.ICON_TWITTER;
                break;
            case WHATSAPP:
                icon = JYouConstants.ICON_WHATSAPP;
                break;
            default:
                icon = JYouConstants.ICON_LINE;
                break;
        }
        showNotification(icon, notificationTitle, notificationSpec.body);
    }

    @Override
    public void onDeleteNotification(int id) {
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("SetAlarms");

            for (int i = 0; i < alarms.size(); i++)
            {
                byte cmd;
                switch (i) {
                    case 0:
                        cmd = JYouConstants.CMD_SET_ALARM_1;
                        break;
                    case 1:
                        cmd = JYouConstants.CMD_SET_ALARM_2;
                        break;
                    case 2:
                        cmd = JYouConstants.CMD_SET_ALARM_3;
                        break;
                    default:
                        return;
                }
                Calendar cal = alarms.get(i).getAlarmCal();
                builder.write(ctrlCharacteristic, commandWithChecksum(
                        cmd,
                        alarms.get(i).isEnabled() ? cal.get(Calendar.HOUR_OF_DAY) : -1,
                        alarms.get(i).isEnabled() ? cal.get(Calendar.MINUTE) : -1
                ));
            }
            performConnected(builder.getTransaction());
            GB.toast(getContext(), "Alarm settings applied - do note that the current device does not support day specification", Toast.LENGTH_LONG, GB.INFO);
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("SetTime");
            syncDateAndTime(builder);
            performConnected(builder.getTransaction());
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(JYouConstants.ICON_CALL, callSpec.name, callSpec.number);
                break;
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
        onEnableRealtimeHeartRateMeasurement(enable);
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
    public void dispose() {
        LOG.info("Dispose");
        super.dispose();
    }

    @Override
    public void onReboot() {
        try {
            TransactionBuilder builder = performInitialized("Reboot");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_ACTION_REBOOT_DEVICE, 0, 0
            ));
            performConnected(builder.getTransaction());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_SET_HEARTRATE_AUTO, 0, 0

            ));
            performConnected(builder.getTransaction());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_ACTION_HEARTRATE_SWITCH, 0, enable ? 1 : 0
            ));
            performConnected(builder.getTransaction());
            enableRealtimeSamplesTimer(enable);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            showNotification(JYouConstants.ICON_QQ, "Gadgetbridge", "Bzzt! Bzzt!");
            GB.toast(getContext(), "As your device doesn't have sound, it will only vibrate 3 times consecutively", Toast.LENGTH_LONG, GB.INFO);
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

    private byte[] commandWithChecksum(byte cmd, int argSlot1, int argSlot2)
    {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put(cmd);
        buf.putInt(argSlot1);
        buf.putInt(argSlot2);

        byte[] bytesToWrite = buf.array();

        byte checksum = 0;
        for (byte b : bytesToWrite) {
            checksum += b;
        }

        bytesToWrite[9] = checksum;

        return bytesToWrite;
    }

    private byte[] stringToUTF8Bytes(String src, int byteCount) {
        try {
            if (src == null)
                return null;

            for (int i = src.length(); i > 0; i--) {
                String sub = src.substring(0, i);
                byte[] subUTF8 = sub.getBytes("UTF-8");

                if (subUTF8.length == byteCount) {
                    return subUTF8;
                }

                if (subUTF8.length < byteCount) {
                    byte[] largerSubUTF8 = new byte[byteCount];
                    System.arraycopy(subUTF8, 0, largerSubUTF8, 0, subUTF8.length);
                    return largerSubUTF8;
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOG.warn(e.getMessage());
        }
        return null;
    }
}
