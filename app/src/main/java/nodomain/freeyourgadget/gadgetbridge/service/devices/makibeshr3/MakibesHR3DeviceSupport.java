/*  Copyright (C) 2019-2020 Andreas Shimokawa, Cre3per

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
// TODO: All the commands that aren't supported by GB should be added to device specific settings.

// TODO: It'd be cool if we could change the language. There's no official way to do so, but the
// TODO: watch is sold as chinese/english. Screen-on-time would be nice too.

// TODO: Firmware upgrades. WearFit tries to connect to Wake up Technology at
// TODO: http://47.112.119.52/app.php/Api/hardUpdate/type/55
// TODO: But that server resets the connection.
// TODO: The host is supposed to be www.iwhop.com, but that domain no longer exists.
// TODO: I think /app.php is missing a closing php tag.

package nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
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

public class MakibesHR3DeviceSupport extends AbstractBTLEDeviceSupport implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(MakibesHR3DeviceSupport.class);

    // The delay must be at least as long as it takes the watch to respond.
    // Reordering the requests could maybe reduce the delay, but this works fine too.
    private final CountDownTimer mFetchCountDown = new CountDownTimer(2000, 2000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            LOG.debug("download finished");
            GB.updateTransferNotification(null, "", false, 100, getContext());
        }
    };

    private final Handler mFindPhoneHandler = new Handler();

    private BluetoothGattCharacteristic mControlCharacteristic = null;
    private BluetoothGattCharacteristic mReportCharacteristic = null;


    public MakibesHR3DeviceSupport() {
        super(LOG);

        addSupportedService(MakibesHR3Constants.UUID_SERVICE);
    }

    /**
     * Called whenever data is received to postpone the removing of the progress notification.
     *
     * @param start Start showing the notification
     */
    private void fetch(boolean start) {
        if (start) {
            // We don't know how long the watch is going to take to reply. Keep progress at 0.
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 0, getContext());
        }

        this.mFetchCountDown.cancel();
        this.mFetchCountDown.start();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    /**
     * @param timeStamp seconds
     */
    private void getDayStartEnd(int timeStamp, Calendar start, Calendar end) {
        final int DAY = (24 * 60 * 60);

        int timeStampStart = ((timeStamp / DAY) * DAY);
        int timeStampEnd = (timeStampStart + DAY);

        start.setTimeInMillis(timeStampStart * 1000L);
        end.setTimeInMillis(timeStampEnd * 1000L);
    }

    /**
     * @param timeStamp Time stamp at some point during the requested day.
     */
    private int getStepsOnDay(int timeStamp) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            Calendar dayStart = new GregorianCalendar();
            Calendar dayEnd = new GregorianCalendar();

            this.getDayStartEnd(timeStamp, dayStart, dayEnd);

            MakibesHR3SampleProvider provider = new MakibesHR3SampleProvider(this.getDevice(), dbHandler.getDaoSession());

            List<MakibesHR3ActivitySample> samples = provider.getAllActivitySamples(
                    (int) (dayStart.getTimeInMillis() / 1000L),
                    (int) (dayEnd.getTimeInMillis() / 1000L));

            int totalSteps = 0;

            for (MakibesHR3ActivitySample sample : samples) {
                totalSteps += sample.getSteps();
            }

            return totalSteps;

        } catch (Exception ex) {
            LOG.error(ex.getMessage());

            return 0;
        }
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

        String message = "";

        if (notificationSpec.title != null) {
            message += (notificationSpec.title + ": ");
        }

        message += notificationSpec.body;

        this.sendNotification(transactionBuilder,
                sender, message);

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
            this.sendNotification(transactionBuilder, MakibesHR3Constants.ARG_SEND_NOTIFICATION_SOURCE_STOP_CALL, "");
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
        if (start) {
            SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(
                    this.getDevice().getAddress());

            int findPhone = MakibesHR3Coordinator.getFindPhone(sharedPreferences);

            if (findPhone != MakibesHR3Coordinator.FindPhone_OFF) {
                GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;

                evaluateGBDeviceEvent(findPhoneEvent);

                if (findPhone > 0) {
                    this.mFindPhoneHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onReverseFindDevice(false);
                        }
                    }, findPhone * 1000);
                }
            }
        } else {
            // Always send stop, ignore preferences.
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;

            evaluateGBDeviceEvent(findPhoneEvent);
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

    private void syncPreferences(TransactionBuilder transaction) {

        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());

        this.setTimeMode(transaction, sharedPreferences);
        this.setDateTime(transaction);
        this.setQuiteHours(transaction, sharedPreferences);

        this.setHeadsUpScreen(transaction, sharedPreferences);
        this.setLostReminder(transaction, sharedPreferences);

        ActivityUser activityUser = new ActivityUser();

        this.setPersonalInformation(transaction,
                (byte) Math.round(activityUser.getHeightCm() * 0.43), // Thanks no1f1
                activityUser.getAge(),
                activityUser.getHeightCm(),
                activityUser.getWeightKg(),
                activityUser.getStepsGoal() / 1000);

        this.fetch(true);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOG.debug(key + " changed");

        if (!this.isConnected()) {
            LOG.debug("ignoring change, we're disconnected");
            return;
        }

        TransactionBuilder transactionBuilder = this.createTransactionBuilder("onSharedPreferenceChanged");

        if (key.equals(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT)) {
            this.setTimeMode(transactionBuilder, sharedPreferences);
        } else if (key.equals(MakibesHR3Constants.PREF_HEADS_UP_SCREEN)) {
            this.setHeadsUpScreen(transactionBuilder, sharedPreferences);
        } else if (key.equals(MakibesHR3Constants.PREF_LOST_REMINDER)) {
            this.setLostReminder(transactionBuilder, sharedPreferences);
        } else if (key.equals(MakibesHR3Constants.PREF_DO_NOT_DISTURB) ||
                key.equals(MakibesHR3Constants.PREF_DO_NOT_DISTURB_START) ||
                key.equals(MakibesHR3Constants.PREF_DO_NOT_DISTURB_END)) {
            this.setQuiteHours(transactionBuilder, sharedPreferences);
        } else if (key.equals(MakibesHR3Constants.PREF_FIND_PHONE) ||
                key.equals(MakibesHR3Constants.PREF_FIND_PHONE_DURATION)) {
            // No action, we check the shared preferences when the device tries to ring the phone.
        } else {
            return;
        }

        try {
            this.performConnected(transactionBuilder.getTransaction());
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    /**
     * Use to show the battery icon in the device card.
     * If the icon shows up later, the user might be trying to tap one thing but the battery icon
     * will shift everything.
     * This is hacky. There should be a "supportsBattery" function in the coordinator that displays
     * the battery icon before the battery level is received.
     */
    private void fakeBattery() {
        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();

        batteryInfo.level = 100;
        batteryInfo.state = BatteryState.UNKNOWN;

        this.handleGBDeviceEvent(batteryInfo);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        this.fakeBattery();

        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 0, getContext());

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        this.mControlCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_CONTROL);
        this.mReportCharacteristic = getCharacteristic(MakibesHR3Constants.UUID_CHARACTERISTIC_REPORT);

        builder.notify(this.mReportCharacteristic, true);
        builder.setGattCallback(this);


        // Allow modifications
        builder.write(this.mControlCharacteristic, new byte[]{0x01, 0x00});

        // Initialize device
        this.syncPreferences(builder);

        this.requestFitness(builder);

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
        this.addGBActivitySamples(new MakibesHR3ActivitySample[]{sample});
    }

    /**
     * Should only be called after the sample has been populated by
     * {@link MakibesHR3DeviceSupport#addGBActivitySample} or
     * {@link MakibesHR3DeviceSupport#addGBActivitySamples}
     */
    private void broadcastSample(MakibesHR3ActivitySample sample) {
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void onReceiveFitness(int steps) {
        LOG.info("steps: " + steps);

        this.onReceiveStepsSample(steps);
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

    private void onReceiveStepsSample(int timeStamp, int steps) {
        MakibesHR3ActivitySample sample = new MakibesHR3ActivitySample();

        // We need to subtract the day's total step count thus far.
        int dayStepCount = this.getStepsOnDay(timeStamp);

        int newSteps = (steps - dayStepCount);

        if (newSteps > 0) {
            LOG.debug("adding " + newSteps + " steps");

            sample.setSteps(steps - dayStepCount);
            sample.setTimestamp(timeStamp);

            sample.setRawKind(ActivityKind.TYPE_ACTIVITY);

            this.addGBActivitySample(sample);
        }
    }

    /**
     * The time is the start of the measurement. Each measurement lasts 1h.
     */
    private void onReceiveStepsSample(int year, int month, int day, int hour, int minute, int steps) {
        LOG.debug("received steps sample " + year + "-" + month + "-" + day + " " + hour + ":" + minute + " " + steps);

        Calendar calendar = new GregorianCalendar(year, month - 1, day, hour + 1, minute);

        int timeStamp = (int) (calendar.getTimeInMillis() / 1000);

        this.onReceiveStepsSample(timeStamp, steps);
    }

    private void onReceiveStepsSample(int steps) {
        this.onReceiveStepsSample((int) (Calendar.getInstance().getTimeInMillis() / 1000l), steps);
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

        this.fetch(false);

        UUID characteristicUuid = characteristic.getUuid();

        if (characteristicUuid.equals(mReportCharacteristic.getUuid())) {
            byte[] value = characteristic.getValue();
            byte[] arguments = new byte[value.length - 6];

            if (arguments.length >= 0) {
                System.arraycopy(value, 6, arguments, 0, arguments.length);
            }

            byte[] report = new byte[]{value[4], value[5]};

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

                        batteryInfo.level = (short) (arguments[1] & 0xff);
                        batteryInfo.state = ((arguments[0] == 0x01) ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL);

                        this.handleGBDeviceEvent(batteryInfo);
                    }
                    break;
                case MakibesHR3Constants.RPRT_SOFTWARE:
                    if (arguments.length == 11) {
                        this.getDevice().setFirmwareVersion(((int) (arguments[0] & 0xff)) + "." + (arguments[1] & 0xff));
                    }
                    break;
                default: // Non-80 reports
                    if (Arrays.equals(report, MakibesHR3Constants.RPRT_FITNESS)) {
                        int steps = (arguments[1] & 0xff) * 0x100 + (arguments[2] & 0xff);
                        this.onReceiveFitness(
                                steps
                        );
                    } else if (Arrays.equals(report, MakibesHR3Constants.RPRT_HEART_RATE_SAMPLE)) {
                        this.onReceiveHeartRateSample(
                                (arguments[0] & 0xff) + 2000, (arguments[1] & 0xff), (arguments[2] & 0xff),
                                (arguments[3] & 0xff), (arguments[4] & 0xff),
                                (arguments[5] & 0xff));
                    } else if (Arrays.equals(report, MakibesHR3Constants.RPRT_STEPS_SAMPLE)) {
                        this.onReceiveStepsSample(
                                (arguments[0] & 0xff) + 2000, (arguments[1] & 0xff), (arguments[2] & 0xff),
                                (arguments[3] & 0xff), 0,
                                ((arguments[5] & 0xff) * 0x100) + (arguments[6] & 0xff));
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

        System.arraycopy(command, 0, result, 4, command.length);

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
        int extraBytes = 0;

        if (data.length > 20) {
            extraBytes = (((data.length - maxMessageLength) / maxMessageLength) + 1);
        }

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

    /**
     * Ugly because I don't like Date.
     * All non-zero records after the given times will be returned via
     * {@link MakibesHR3Constants#RPRT_HEART_RATE_SAMPLE},
     * {@link MakibesHR3Constants#RPRT_STEPS_SAMPLE},
     * {@link MakibesHR3Constants#RPRT_FITNESS}
     */
    private MakibesHR3DeviceSupport requestFitness(TransactionBuilder transaction,
                                                   int yearStepsAfter, int monthStepsAfter, int dayStepsAfter,
                                                   int hourStepsAfter, int minuteStepsAfter,
                                                   int yearHeartRateAfter, int monthHeartRateAfter, int dayHeartRateAfter,
                                                   int hourHeartRateAfter, int minuteHeartRateAfter) {

        byte[] data = this.craftData(MakibesHR3Constants.CMD_REQUEST_FITNESS,
                new byte[]{
                        (byte) 0x00,
                        (byte) (yearStepsAfter - 2000),
                        (byte) monthStepsAfter,
                        (byte) dayStepsAfter,
                        (byte) hourStepsAfter,
                        (byte) minuteStepsAfter,
                        (byte) (yearHeartRateAfter - 2000),
                        (byte) monthHeartRateAfter,
                        (byte) dayHeartRateAfter,
                        (byte) hourHeartRateAfter,
                        (byte) minuteHeartRateAfter
                });

        transaction.write(this.mControlCharacteristic, data);

        this.fetch(true);

        return this;
    }

    /**
     * Ugly because I don't like Date.
     * All non-zero records after the given times will be returned via
     * {@link MakibesHR3Constants#RPRT_HEART_RATE_SAMPLE},
     * {@link MakibesHR3Constants#RPRT_STEPS_SAMPLE},
     * {@link MakibesHR3Constants#RPRT_FITNESS}
     */
    private MakibesHR3DeviceSupport requestFitness(TransactionBuilder transaction) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            MakibesHR3SampleProvider provider = new MakibesHR3SampleProvider(this.getDevice(), dbHandler.getDaoSession());

            MakibesHR3ActivitySample latestSample = provider.getLatestActivitySample();

            if (latestSample == null) {
                this.requestFitness(transaction,
                        2000, 0, 0, 0, 0,
                        2000, 0, 0, 0, 0);
            } else {
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(new Date(latestSample.getTimestamp() * 1000l));

                int year = calendar.get(Calendar.YEAR);
                int month = (calendar.get(Calendar.MONTH) + 1);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                this.requestFitness(transaction,
                        year, month, day, hour, minute,
                        year, month, day, hour, minute);
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        }

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

    private MakibesHR3DeviceSupport setHeadsUpScreen(TransactionBuilder transactionBuilder, boolean enable) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_HEADS_UP_SCREEN,
                new byte[]{(byte) (enable ? 0x01 : 0x00)});

        transactionBuilder.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setQuiteHours(TransactionBuilder transactionBuilder,
                                                  boolean enable,
                                                  int hourStart, int minuteStart,
                                                  int hourEnd, int minuteEnd) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_QUITE_HOURS, new byte[]{
                (byte) (enable ? 0x01 : 0x00),
                (byte) hourStart, (byte) minuteStart,
                (byte) hourEnd, (byte) minuteEnd
        });

        transactionBuilder.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setQuiteHours(TransactionBuilder transactionBuilder,
                                                  SharedPreferences sharedPreferences) {

        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        boolean enable = MakibesHR3Coordinator.getQuiteHours(sharedPreferences, start, end);

        return this.setQuiteHours(transactionBuilder, enable,
                start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
    }

    private MakibesHR3DeviceSupport setHeadsUpScreen(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setHeadsUpScreen(transactionBuilder,
                MakibesHR3Coordinator.shouldEnableHeadsUpScreen(sharedPreferences));
    }

    private MakibesHR3DeviceSupport setLostReminder(TransactionBuilder transactionBuilder, boolean enable) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_LOST_REMINDER,
                new byte[]{(byte) (enable ? 0x01 : 0x00)});

        transactionBuilder.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setLostReminder(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setLostReminder(transactionBuilder,
                MakibesHR3Coordinator.shouldEnableLostReminder(sharedPreferences));
    }

    private MakibesHR3DeviceSupport setTimeMode(TransactionBuilder transactionBuilder, byte timeMode) {
        byte[] data = this.craftData(MakibesHR3Constants.CMD_SET_TIMEMODE, new byte[]{timeMode});

        transactionBuilder.write(this.mControlCharacteristic, data);

        return this;
    }

    private MakibesHR3DeviceSupport setTimeMode(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setTimeMode(transactionBuilder,
                MakibesHR3Coordinator.getTimeMode(sharedPreferences));
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
