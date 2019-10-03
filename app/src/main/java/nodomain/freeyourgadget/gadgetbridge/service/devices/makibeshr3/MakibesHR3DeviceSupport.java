// TODO: WearFit resets today's step count when it's used after GB.

// TODO: Battery level

// TODO: ALARM REMINDER REPETITION

// TODO: It'd be cool if we could change the language. There's no official way to do so, but the
// TODO: watch is sold as chinese/english.

package nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
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
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Constants.RPRT_SOFTWARE;

public class MakibesHR3DeviceSupport extends AbstractBTLEDeviceSupport implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(MakibesHR3DeviceSupport.class);

    private Vibrator mVibrator;

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic rprtCharacteristic = null;


    public MakibesHR3DeviceSupport() {
        super(LOG);

        addSupportedService(MakibesHR3Constants.UUID_SERVICE);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    public MakibesHR3ActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        MakibesHR3ActivitySample sample = new MakibesHR3ActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }


    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        TransactionBuilder transactionBuilder = this.createTransactionBuilder("onnotificaiton");

        byte sender;

        switch (notificationSpec.type) {
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_FACEBOOK;
                break;
            case LINE:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_LINE;
                break;
            case TELEGRAM:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_MESSAGE;
                break;
            case TWITTER:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_TWITTER;
                break;
            case WECHAT:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_WECHAT;
                break;
            case WHATSAPP:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_WHATSAPP;
                break;
            case KAKAO_TALK:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_KAKOTALK;
                break;

            default:
                sender = MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_MESSAGE;
                break;
        }

        this.sendNotification(transactionBuilder,
                sender, notificationSpec.title + ": " + notificationSpec.body);

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LoggerFactory.getLogger(this.getClass()).error("notification failed");
        }
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        TransactionBuilder transactionBuilder = this.createTransactionBuilder("settime");

        this.setDateTime(transactionBuilder);

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LoggerFactory.getLogger(this.getClass()).error("factory reset failed");
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

        TransactionBuilder transactionBuilder = this.createTransactionBuilder("setalarms");

        for (int i = 0; i < alarms.size(); ++i) {
            Alarm alarm = alarms.get(i);

            // Should we use @alarm.getPosition() rather than @i?
            this.setAlarmReminder(
                    transactionBuilder,
                    i,
                    alarm.getEnabled(),
                    alarm.getHour(),
                    alarm.getMinute(),
                    MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_CUSTOM);
        }

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LoggerFactory.getLogger(this.getClass()).error("setalarms failed");
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        TransactionBuilder transactionBuilder = this.createTransactionBuilder("callstate");
        LOG.debug("callSpec " + callSpec.command);
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            this.sendNotification(transactionBuilder, MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_CALL, callSpec.name);
        } else {
            this.sendNotification(transactionBuilder, MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_STOP_CALL, callSpec.name);
        }

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LoggerFactory.getLogger(this.getClass()).error("call state failed");
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

        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            TransactionBuilder transactionBuilder = this.createTransactionBuilder("reset");
            this.factoryReset(transactionBuilder);

            try {
                this.performConnected(transactionBuilder.getTransaction());
            } catch (Exception ex) {
                LoggerFactory.getLogger(this.getClass()).error("factory reset failed");
            }
        } else if ((flags & GBDeviceProtocol.RESET_FLAGS_REBOOT) != 0) {
            TransactionBuilder transactionBuilder = this.createTransactionBuilder("reboot");
            this.reboot(transactionBuilder);

            try {
                this.performConnected(transactionBuilder.getTransaction());
            } catch (Exception ex) {
                LoggerFactory.getLogger(this.getClass()).error("factory reset failed");
            }
        }
    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        TransactionBuilder transactionBuilder = this.createTransactionBuilder("finddevice");

        this.setEnableRealTimeHeartRate(transactionBuilder, enable);

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception e) {
            LOG.debug("ERROR");
        }
    }

    private void onReverseFindDevice(boolean start) {
        final long[] PATTERN = new long[]{
                100, 100,
                100, 100,
                100, 100,
                500
        };

        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mVibrator.vibrate(VibrationEffect.createWaveform(PATTERN, 0));
            } else {
                this.mVibrator.vibrate(PATTERN, 0);
            }
        } else {
            this.mVibrator.cancel();
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        if (!start) {
            return;
        }

        TransactionBuilder transactionBuilder = this.createTransactionBuilder("finddevice");

        this.findDevice(transactionBuilder);

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception e) {
            LOG.debug("ERROR");
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

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    private MakibesHR3DeviceSupport sendUserInfo(TransactionBuilder builder) {
        syncPreferences(builder);

        return this;
    }

    private MakibesHR3DeviceSupport syncPreferences(TransactionBuilder transaction) {

        this.setTimeMode(transaction);
        this.setDateTime(transaction);
        // setDayOfWeek(transaction);
        // setTimeMode(transaction);

        // setGender(transaction);
        // setAge(transaction);
        // setWeight(transaction);
        // setHeight(transaction);

        // setGoal(transaction);
        // setLanguage(transaction);
        // setScreenTime(transaction);
        // setUnit(transaction);
        // setAllDayHeart(transaction);

        return this;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOG.debug(key + " changed");
        TransactionBuilder transactionBuilder = this.createTransactionBuilder("onSharedPreferenceChanged");

        if (key.equals(PREF_TIMEFORMAT)) {
            this.setTimeMode(transactionBuilder);
        } else {
            return;
        }

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        this.ctrlCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_CONTROL);
        this.rprtCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_REPORT);

        this.mVibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);

        builder.notify(this.rprtCharacteristic, true);
        builder.setGattCallback(this);


        // Allow modifications
        builder.write(this.ctrlCharacteristic, new byte[]{0x01, 0x00});

        // Initialize device
        sendUserInfo(builder); //Sync preferences

        this.requestFitness(builder, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        SharedPreferences preferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());

        // TODO: Why doesn't this work?
        preferences.registerOnSharedPreferenceChangeListener(this);

        return builder;
    }

    private void broadcastActivity(Integer heartRate, Integer steps) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());

            MakibesHR3SampleProvider provider = new MakibesHR3SampleProvider(this.getDevice(), dbHandler.getDaoSession());

            int timeStamp = (int) (System.currentTimeMillis() / 1000);

            MakibesHR3ActivitySample sample = this.createActivitySample(device, user, timeStamp, provider);

            if (heartRate != null) {
                sample.setHeartRate(heartRate);
            }

            if (steps != null) {
                sample.setSteps(steps);
            }

            sample.setRawKind(-1);

            provider.addGBActivitySample(sample);

            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, timeStamp);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    private void onReceiveFitness(int steps) {
        LOG.info("steps: " + steps);

        this.broadcastActivity(null, steps);
    }

    private void onReceiveHeartRate(int heartRate) {
        LOG.info("heart rate: " + heartRate);

        this.broadcastActivity(heartRate, null);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        byte[] data = characteristic.getValue();
        if (data.length < 6)
            return true;

        UUID characteristicUuid = characteristic.getUuid();

        if (characteristicUuid.equals(rprtCharacteristic.getUuid())) {
            byte[] value = characteristic.getValue();
            byte[] arguments = new byte[value.length - 6];

            for (int i = 0; i < arguments.length; ++i) {
                arguments[i] = value[i + 6];
            }

            byte report = value[4];

            LOG.debug("report: " + Integer.toHexString((int) report));

            switch (report) {
                case MakibesHR3Constants.RPRT_FITNESS:
                    if (value.length == 17) {
                        this.onReceiveFitness(
                                (int) arguments[1] * 0xff + arguments[2]
                        );
                    }
                    break;
                case MakibesHR3Constants.RPRT_REVERSE_FIND_DEVICE:
                    this.onReverseFindDevice(arguments[0] == 0x01);
                    break;
                case MakibesHR3Constants.RPRT_HEARTRATE:
                    if (value.length == 7) {
                        this.onReceiveHeartRate((int) arguments[0]);
                    }
                    break;
                case RPRT_SOFTWARE:
                    if (arguments.length == 11) {
                        this.getDevice().setFirmwareVersion(((int) arguments[0]) + "." + ((int) arguments[1]));
                    }
                    break;
            }
        }

        return false;
    }

    /**
     * @param command
     * @param data
     * @return
     */
    private byte[] craftData(byte command, byte[] data) {
        byte[] result = new byte[MakibesHR3Constants.DATA_TEMPLATE.length + data.length];

        System.arraycopy(MakibesHR3Constants.DATA_TEMPLATE, 0, result, 0, MakibesHR3Constants.DATA_TEMPLATE.length);

        result[MakibesHR3Constants.DATA_ARGUMENT_COUNT_INDEX] = (byte) (data.length + 3);
        result[MakibesHR3Constants.DATA_COMMAND_INDEX] = command;

        System.arraycopy(data, 0, result, 6, data.length);

        return result;
    }


    private byte[] craftData(byte command) {
        return this.craftData(command, new byte[]{});
    }

    private void writeSafe(BluetoothGattCharacteristic characteristic, TransactionBuilder builder, byte[] data) {
        final int maxMessageLength = 20;

        // For every split, we need 1 byte extra.
        int extraBytes = (((data.length - maxMessageLength) / maxMessageLength) + 1);

        int totalDataLength = (data.length + extraBytes);

        int segmentCount = (((totalDataLength - 1) / maxMessageLength) + 1);

        byte[] indexedData = new byte[totalDataLength];

        int it = 0;
        int segmentIndex = 0;
        for (int i = 0; i < data.length; ++i) {
            if ((i != 0) && ((it % maxMessageLength) == 0)) {
                indexedData[it++] = (byte) segmentIndex++;
            }

            indexedData[it++] = data[i];
        }

        for (int i = 0; i < segmentCount; ++i) {
            int segmentStart = (i * maxMessageLength);
            int segmentLength;

            if (i == (segmentCount - 1)) {
                segmentLength = (indexedData.length - segmentStart);
            } else {
                segmentLength = maxMessageLength;
            }

            byte[] segment = new byte[segmentLength];

            System.arraycopy(indexedData, segmentStart, segment, 0, segmentLength);

            builder.write(characteristic, segment);
        }
    }

    private MakibesHR3DeviceSupport factoryReset(TransactionBuilder transaction) {
        transaction.write(this.ctrlCharacteristic, this.craftData(MakibesHR3Constants.CMD_FACTORY_RESET));

        return this.reboot(transaction);
    }

    private MakibesHR3DeviceSupport requestFitness(TransactionBuilder transaction,
                                                   int yearStart, int monthStart, int dayStart,
                                                   int a4, int a5,
                                                   int yearEnd, int monthEnd, int dayEnd,
                                                   int a9, int a10) {

        byte[] data = this.craftData(MakibesHR3Constants.CMD_REQUEST_FITNESS,
                new byte[]{
                        (byte) (yearStart - 2000),
                        (byte) monthStart,
                        (byte) dayStart,
                        (byte) a4,
                        (byte) a5,
                        (byte) (yearEnd - 2000),
                        (byte) monthEnd,
                        (byte) dayEnd,
                        (byte) a9,
                        (byte) a10
                });

        transaction.write(this.ctrlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport findDevice(TransactionBuilder transaction) {
        transaction.write(this.ctrlCharacteristic, this.craftData(MakibesHR3Constants.CMD_FIND_DEVICE));

        return this;
    }

    private MakibesHR3DeviceSupport sendNotification(TransactionBuilder transaction,
                                                     byte source, String message) {
        byte[] data = new byte[message.length() + 2];
        data[0] = source;
        data[1] = (byte) 0x02;

        for (int i = 0; i < message.length(); ++i) {
            data[i + 2] = (byte) message.charAt(i);
        }

        this.writeSafe(
                this.ctrlCharacteristic,
                transaction,
                this.craftData(MakibesHR3Constants.CMD_SEND_NOTIFICATION, data));

        return this;
    }

    private MakibesHR3DeviceSupport setAlarmReminder(TransactionBuilder transaction,
                                                     int id, boolean enable, int hour, int minute, byte repeat) {
        transaction.write(this.ctrlCharacteristic,
                this.craftData(MakibesHR3Constants.CMD_SET_ALARM_REMINDER, new byte[]{
                        (byte) id,
                        (byte) (enable ? 0x01 : 0x00),
                        (byte) hour,
                        (byte) minute,
                        repeat
                }));

        return this;
    }

    private MakibesHR3DeviceSupport setTimeMode(TransactionBuilder transaction) {
        byte value = MakibesHR3Coordinator.getTimeMode(getDevice().getAddress());

        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_TIMEMODE, new byte[]{value});

        transaction.write(this.ctrlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setEnableRealTimeHeartRate(TransactionBuilder transaction, boolean enable) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_REAL_TIME_HEART_RATE, new byte[]{(byte) (enable ? 0x01 : 0x00)});

        transaction.write(this.ctrlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setDateTime(TransactionBuilder transaction,
                                                int year,
                                                int month,
                                                int day,
                                                int hour,
                                                int minute,
                                                int second) {

        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_DATE_TIME,
                new byte[]{
                        (byte) 0x00,
                        (byte) (year & 0xff00),
                        (byte) (year & 0x00ff),
                        (byte) month,
                        (byte) day,
                        (byte) hour,
                        (byte) minute,
                        (byte) second
                });

        transaction.write(this.ctrlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setDateTime(TransactionBuilder transaction) {

        Calendar calendar = Calendar.getInstance();

        return this.setDateTime(transaction,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
    }

    private MakibesHR3DeviceSupport reboot(TransactionBuilder transaction) {
        transaction.write(this.ctrlCharacteristic, this.craftData(MakibesHR3Constants.CMD_REBOOT));

        return this;
    }
}
