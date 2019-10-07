// TODO: WearFit sometimes resets today's step count when it's used after GB.
// TODO: Where can I view today's steps in GB?
// TODO: GB accumulates all step samples, even if they're part of the same day.

// TODO: Activity history download progress.
// TODO: Remove downloaded history from the device.

// TODO: Request and handle step history from the device.

// TODO: All the commands that aren't supported by GB should be added to device specific settings.

// TODO: It'd be cool if we could change the language. There's no official way to do so, but the
// TODO: watch is sold as chinese/english. Screen on time would be nice too.

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

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
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

public class MakibesHR3DeviceSupport extends AbstractBTLEDeviceSupport implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(MakibesHR3DeviceSupport.class);

    private Vibrator mVibrator;

    private BluetoothGattCharacteristic mControlCharacteristic = null;
    private BluetoothGattCharacteristic mReportCharacteristic = null;


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

            byte repetition = 0x00;

            switch (alarm.getRepetition()) {
                case Alarm.ALARM_ONCE:
                    repetition = MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_ONE_TIME;
                    break;

                case Alarm.ALARM_MON:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_MONDAY;
                case Alarm.ALARM_TUE:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_TUESDAY;
                case Alarm.ALARM_WED:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_WEDNESDAY;
                case Alarm.ALARM_THU:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_THURSDAY;
                case Alarm.ALARM_FRI:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_FRIDAY;
                case Alarm.ALARM_SAT:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_SATURDAY;
                case Alarm.ALARM_SUN:
                    repetition |= MakibesHR3Constants.ARG_SET_ALARM_REMINDER_REPEAT_SUNDAY;
                    break;

                default:
                    LOG.warn("invalid alarm repetition " + alarm.getRepetition());
                    break;
            }

            // Should we use @alarm.getPosition() rather than @i?
            this.setAlarmReminder(
                    transactionBuilder,
                    i,
                    alarm.getEnabled(),
                    alarm.getHour(),
                    alarm.getMinute(),
                    repetition);
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
        // what is this?
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
        this.setTimeMode(transaction);

        ActivityUser activityUser = new ActivityUser();

        this.setPersonalInformation(transaction,
                (byte) Math.round(activityUser.getHeightCm() * 0.43), // Thanks no1f1
                activityUser.getAge(),
                activityUser.getHeightCm(),
                activityUser.getWeightKg(),
                activityUser.getStepsGoal() / 1000);

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

        this.mControlCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_CONTROL);
        this.mReportCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_REPORT);

        this.mVibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);

        builder.notify(this.mReportCharacteristic, true);
        builder.setGattCallback(this);


        // Allow modifications
        builder.write(this.mControlCharacteristic, new byte[]{0x01, 0x00});

        // Initialize device
        sendUserInfo(builder); //Sync preferences

        // TODO: arguments
        this.requestFitness(builder, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        SharedPreferences preferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());

        preferences.registerOnSharedPreferenceChangeListener(this);

        return builder;
    }

    private void addGBActivitySamples(MakibesHR3ActivitySample[] samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());

            MakibesHR3SampleProvider provider = new MakibesHR3SampleProvider(this.getDevice(), dbHandler.getDaoSession());

            for (MakibesHR3ActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);

                sample.setRawIntensity(ActivitySample.NOT_MEASURED);

                provider.addGBActivitySample(sample);
            }

        } catch (Exception ex) {
            // Why is this a toast? The user doesn't care about the error.
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());

            LOG.error(ex.getMessage());
        }
    }

    private void addGBActivitySample(MakibesHR3ActivitySample sample) {
        this.addGBActivitySamples(new MakibesHR3ActivitySample[]{ sample });
    }

    /**
     * Should only be called after the sample has been populated by
     * {@link MakibesHR3DeviceSupport#addGBActivitySample} or
     * {@link MakibesHR3DeviceSupport#addGBActivitySamples}
     * @param sample
     */
    private void broadcastSample(MakibesHR3ActivitySample sample) {
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
         LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void onReceiveFitness(int steps) {
        LOG.info("steps: " + steps);

        MakibesHR3ActivitySample sample = new MakibesHR3ActivitySample();

        sample.setSteps(steps);
        sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

        sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
        this.addGBActivitySample(sample);
    }

    private void onReceiveHeartRate(int heartRate) {
        LOG.info("heart rate: " + heartRate);

        MakibesHR3ActivitySample sample = new MakibesHR3ActivitySample();

        if (heartRate > 0) {
            sample.setHeartRate(heartRate);
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
        } else {
            if (heartRate == MakibesHR3Constants.ARG_HEARTRATE_NO_TARGET) {
                sample.setRawKind(ActivityKind.TYPE_NOT_WORN);
            } else if (heartRate == MakibesHR3Constants.ARG_HEARTRATE_NO_READING) {
                sample.setRawKind(ActivityKind.TYPE_NOT_MEASURED);
            } else {
                LOG.warn("invalid heart rate reading: " + heartRate);
                return;
            }
        }

        this.addGBActivitySample(sample);
        this.broadcastSample(sample);
    }

    private void onReceiveHeartRateSample(int year, int month, int day, int hour, int minute, int heartRate) {
        LOG.debug("received heart rate sample " + year + "-" + month + "-" + day + " " + hour + ":" + minute + " " + heartRate);

        MakibesHR3ActivitySample sample = new MakibesHR3ActivitySample();

        Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute);

        int timeStamp = (int) (calendar.getTimeInMillis() / 1000);

        sample.setHeartRate(heartRate);
        sample.setTimestamp(timeStamp);

        sample.setRawKind(ActivityKind.TYPE_ACTIVITY);

        this.addGBActivitySample(sample);
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

        if (characteristicUuid.equals(mReportCharacteristic.getUuid())) {
            byte[] value = characteristic.getValue();
            byte[] arguments = new byte[value.length - 6];

            for (int i = 0; i < arguments.length; ++i) {
                arguments[i] = value[i + 6];
            }

            byte[] report = new byte[]{ value[4], value[5] };

            switch (report[0]) {
                case MakibesHR3Constants.RPRT_REVERSE_FIND_DEVICE:
                    this.onReverseFindDevice(arguments[0] == 0x01);
                    break;
                case MakibesHR3Constants.RPRT_HEARTRATE:
                    if (value.length == 7) {
                        this.onReceiveHeartRate((int) arguments[0]);
                    }
                    break;
                case MakibesHR3Constants.RPRT_BATTERY:
                    if (arguments.length == 2) {
                        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();

                        batteryInfo.level = (short) arguments[1];
                        batteryInfo.state = ((arguments[0] == 0x01) ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL);

                        this.handleGBDeviceEvent(batteryInfo);
                    }
                    break;
                case MakibesHR3Constants.RPRT_SOFTWARE:
                    if (arguments.length == 11) {
                        this.getDevice().setFirmwareVersion(((int) arguments[0]) + "." + ((int) arguments[1]));
                    }
                    break;
                default: // Non-80 reports
                    if (Arrays.equals(report, MakibesHR3Constants.RPRT_FITNESS)) {
                            this.onReceiveFitness(
                                    (int) arguments[1] * 0xff + arguments[2]
                            );
                    } else if (Arrays.equals(report, MakibesHR3Constants.RPRT_HEART_RATE_SAMPLE)) {
                        this.onReceiveHeartRateSample(
                                arguments[0] + 2000, arguments[1], arguments[2],
                                arguments[3], arguments[4],
                                arguments[5]);
                    }
                    break;
            }
        }

        return false;
    }

    private byte[] craftData(byte[] command, byte[] data) {
        byte[] result = new byte[MakibesHR3Constants.DATA_TEMPLATE.length + data.length];

        System.arraycopy(MakibesHR3Constants.DATA_TEMPLATE, 0, result, 0, MakibesHR3Constants.DATA_TEMPLATE.length);

        result[MakibesHR3Constants.DATA_ARGUMENT_COUNT_INDEX] = (byte) (data.length + 3);

        for (int i = 0; i < command.length; ++i) {
            result[MakibesHR3Constants.DATA_COMMAND_INDEX + i] = command[i];
        }

        System.arraycopy(data, 0, result, 6, data.length);

        return result;
    }

    private byte[] craftData(byte command, byte[] data) {
        return this.craftData(new byte[]{command}, data);
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
        transaction.write(this.mControlCharacteristic, this.craftData(MakibesHR3Constants.CMD_FACTORY_RESET));

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

        transaction.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport findDevice(TransactionBuilder transaction) {
        transaction.write(this.mControlCharacteristic, this.craftData(MakibesHR3Constants.CMD_FIND_DEVICE));

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
                this.mControlCharacteristic,
                transaction,
                this.craftData(MakibesHR3Constants.CMD_SEND_NOTIFICATION, data));

        return this;
    }

    private MakibesHR3DeviceSupport setAlarmReminder(TransactionBuilder transaction,
                                                     int id, boolean enable, int hour, int minute, byte repeat) {
        transaction.write(this.mControlCharacteristic,
                this.craftData(MakibesHR3Constants.CMD_SET_ALARM_REMINDER, new byte[]{
                        (byte) id,
                        (byte) (enable ? 0x01 : 0x00),
                        (byte) hour,
                        (byte) minute,
                        repeat
                }));

        return this;
    }

    /**
     * @param transactionBuilder
     * @param stepLength         cm
     * @param age                years
     * @param height             cm
     * @param weight             kg
     * @param stepGoal           kilo
     */
    private MakibesHR3DeviceSupport setPersonalInformation(TransactionBuilder transactionBuilder,
                                                           int stepLength, int age, int height, int weight, int stepGoal) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_PERSONAL_INFORMATION,
                new byte[]{
                        (byte) stepLength,
                        (byte) age,
                        (byte) height,
                        (byte) weight,
                        MakibesHR3Constants.ARG_SET_PERSONAL_INFORMATION_UNIT_DISTANCE_KILOMETERS,
                        (byte) stepGoal,
                        (byte) 0x5a,
                        (byte) 0x82,
                        (byte) 0x3c,
                        (byte) 0x5a,
                        (byte) 0x28,
                        (byte) 0xb4,
                        (byte) 0x5d,
                        (byte) 0x64,

                });

        transactionBuilder.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setTimeMode(TransactionBuilder transaction) {
        byte value = MakibesHR3Coordinator.getTimeMode(getDevice().getAddress());

        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_TIMEMODE, new byte[]{value});

        transaction.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setEnableRealTimeHeartRate(TransactionBuilder transaction, boolean enable) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_REAL_TIME_HEART_RATE, new byte[]{(byte) (enable ? 0x01 : 0x00)});

        transaction.write(this.mControlCharacteristic, data);

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
                        (byte) ((year & 0xff00) >> 8),
                        (byte) (year & 0x00ff),
                        (byte) month,
                        (byte) day,
                        (byte) hour,
                        (byte) minute,
                        (byte) second
                });

        transaction.write(this.mControlCharacteristic, data);

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
        transaction.write(this.mControlCharacteristic, this.craftData(MakibesHR3Constants.CMD_REBOOT));

        return this;
    }
}
