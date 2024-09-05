/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Pair;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.AbstractMoyoungDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungWeatherForecast;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungWeatherToday;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungBloodPressureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumDeviceVersion;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumMetricSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumTimeSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingEnum;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingRemindersToMove;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungBloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class MoyoungDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MoyoungDeviceSupport.class);
    private static final long IDLE_STEPS_INTERVAL = 5 * 60 * 1000;

    private final DeviceInfoProfile<MoyoungDeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<MoyoungDeviceSupport> batteryInfoProfile;
    private final HeartRateProfile<MoyoungDeviceSupport> heartRateProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = intent -> {
        String s = intent.getAction();
        if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
            handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
        }
        if (Objects.equals(s, BatteryInfoProfile.ACTION_BATTERY_INFO)) {
            handleBatteryInfo(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
        }
    };

    private final Handler idleUpdateHandler = new Handler();

    private int mtu = 20;
    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    private boolean realTimeHeartRate;
    private boolean findMyPhoneActive = false;

    public int getMtu() {
        return this.mtu;
    }

    public MoyoungDeviceSupport() {
        super(LOG);
        batteryCmd.level = ActivitySample.NOT_MEASURED;

        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(MoyoungConstants.UUID_SERVICE_MOYOUNG);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        heartRateProfile = new HeartRateProfile<>(this);
        heartRateProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
        addSupportedProfile(batteryInfoProfile);
        addSupportedProfile(heartRateProfile); // TODO: this profile doesn't seem to work...
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        mtu = ((AbstractMoyoungDeviceCoordinator) getDevice().getDeviceCoordinator()).getMtu();

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_DATA_IN), true);
        deviceInfoProfile.requestDeviceInfo(builder);
        setTime(builder);
        setMeasurementSystem(builder);
        sendSetting(builder, getSetting("USER_INFO"), new ActivityUser()); // these settings are write-only, so write them just in case because there is no way to know if they desynced somehow
        sendSetting(builder, getSetting("GOAL_STEP"), new ActivityUser().getStepsGoal());
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        heartRateProfile.enableNotify(builder, true);
        builder.notify(getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_STEPS), true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        // TODO: I would prefer this to be done when the alarms screen is open, not on initialization...
        sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_QUERY_ALARM_CLOCK, new byte[0]));

        return builder;
    }

    @Override
    public void dispose() {
        super.dispose();
        idleUpdateHandler.removeCallbacks(updateIdleStepsRunnable);
    }

    private BluetoothGattCharacteristic getTargetCharacteristicForPacketType(byte packetType)
    {
        if (packetType == 1)
            return getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_1);
        else if (packetType == 2)
            return getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_2);
        else
            return getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_DATA_OUT);
    }

    public void sendPacket(TransactionBuilder builder, byte[] packet)
    {
        MoyoungPacketOut packetOut = new MoyoungPacketOut(packet);

        byte[] fragment = new byte[Math.min(packet.length, mtu)];
        while(packetOut.getFragment(fragment))
        {
            builder.write(getTargetCharacteristicForPacketType(packet[4]), fragment.clone());
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID charUuid = characteristic.getUuid();
        if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_STEPS))
        {
            byte[] payload = characteristic.getValue();
            LOG.info("Update step count: " + Logging.formatBytes(characteristic.getValue()));
            handleStepsHistory(0, payload, true);
            return true;
        }
        if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_DATA_IN))
        {
            if (packetIn.putFragment(characteristic.getValue())) {
                Pair<Byte, byte[]> packet = MoyoungPacketIn.parsePacket(packetIn.getPacket());
                packetIn = new MoyoungPacketIn();
                if (packet != null) {
                    byte packetType = packet.first;
                    byte[] payload = packet.second;

                    LOG.info("Response for: " + Logging.formatBytes(new byte[]{packetType}));

                    if (handlePacket(packetType, payload))
                        return true;
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handlePacket(byte packetType, byte[] payload)
    {
        if (packetType == MoyoungConstants.CMD_TRIGGER_MEASURE_HEARTRATE)
        {
            int heartRate = payload[0];
            LOG.info("Measure heart rate finished: " + heartRate + " BPM");

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                MoyoungHeartRateSampleProvider sampleProvider = new MoyoungHeartRateSampleProvider(getDevice(), dbHandler.getDaoSession());
                Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

                MoyoungHeartRateSample sample = new MoyoungHeartRateSample();
                sample.setTimestamp(System.currentTimeMillis());
                sample.setHeartRate(heartRate);
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);

                sampleProvider.addSample(sample);
                broadcastSample(sample);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording heart rate samples", e);
            }

            if (realTimeHeartRate)
                onHeartRateTest();

            return true;
        }
        if (packetType == MoyoungConstants.CMD_TRIGGER_MEASURE_BLOOD_OXYGEN)
        {
            int percent = payload[0];
            LOG.info("Measure blood oxygen finished: " + percent + "%");

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                MoyoungSpo2SampleProvider sampleProvider = new MoyoungSpo2SampleProvider(getDevice(), dbHandler.getDaoSession());
                Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

                MoyoungSpo2Sample sample = new MoyoungSpo2Sample();
                sample.setTimestamp(System.currentTimeMillis());
                sample.setSpo2(percent);
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);

                sampleProvider.addSample(sample);
//                broadcastSample(sample);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording SpO2 samples", e);
            }

            return true;
        }
        if (packetType == MoyoungConstants.CMD_TRIGGER_MEASURE_BLOOD_PRESSURE)
        {
            int dataUnknown = payload[0];
            int data1 = payload[1];
            int data2 = payload[2];
            LOG.info("Measure blood pressure finished: " + data1 + "/" + data2 + " (" + dataUnknown + ")");

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                MoyoungBloodPressureSampleProvider sampleProvider = new MoyoungBloodPressureSampleProvider(getDevice(), dbHandler.getDaoSession());
                Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

                MoyoungBloodPressureSample sample = new MoyoungBloodPressureSample();
                sample.setTimestamp(System.currentTimeMillis());
                sample.setBpSystolic(data1);
                sample.setBpSystolic(data2);
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);

                sampleProvider.addSample(sample);
//                broadcastSample(sample);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording blood pressure samples", e);
            }

            return true;
        }

        if (packetType == MoyoungConstants.CMD_QUERY_LAST_DYNAMIC_RATE)
        {
            // Training on the watch just finished and it wants us to fetch the details
            LOG.info("Starting training fetch");
            try {
                new TrainingFinishedDataOperation(this, payload).perform();
            } catch (IOException e) {
                LOG.error("TrainingFinishedDataOperation failed: ", e);
            }
        }

        if (packetType == MoyoungConstants.CMD_QUERY_PAST_HEART_RATE_1)
        {
            handleHeartRateHistory(payload);
            return true;
        }

        if (packetType == MoyoungConstants.CMD_NOTIFY_PHONE_OPERATION)
        {
            byte operation = payload[0];
            if (operation == MoyoungConstants.ARG_OPERATION_PLAY_PAUSE)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_PREV_SONG)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_NEXT_SONG)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.NEXT;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_DROP_INCOMING_CALL)
            {
                GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_VOLUME_UP)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_VOLUME_DOWN)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_PLAY)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PLAY;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == MoyoungConstants.ARG_OPERATION_PAUSE)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PAUSE;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }

        }

        if (packetType == MoyoungConstants.CMD_SWITCH_CAMERA_VIEW)
        {
            // TODO: trigger camera photo
            LOG.info("Camera shutter triggered from watch");
            return true;
        }

        if (packetType == MoyoungConstants.CMD_NOTIFY_WEATHER_CHANGE)
        {
            LOG.info("Will transmit cached weather (if any) since the watch asks for it");
            if (Weather.getInstance().getWeatherSpec() != null) {
                final ArrayList<WeatherSpec> specs = new ArrayList<>(Weather.getInstance().getWeatherSpecs());
                GBApplication.deviceService().onSendWeather(specs);
            }
            return true;
        }

        for (MoyoungSetting setting : queriedSettings)
        {
            if (setting.cmdQuery == packetType)
            {
                Object value = setting.decode(payload);
                onReadConfigurationDone(setting, value, payload);
                queriedSettings.remove(setting);
                return true;
            }
        }

        if (packetType == MoyoungConstants.CMD_QUERY_ALARM_CLOCK)
        {
            handleGetAlarmsResponse(payload);
            return true;
        }

        if (packetType == MoyoungConstants.CMD_QUERY_DISPLAY_WATCH_FACE)
        {
            LOG.info("Watchface changed on watch to nr {}", payload[0]);
            onReadConfigurationDone(getSetting("DISPLAY_WATCH_FACE"), payload[0], null);
            return true;
        }

        if (packetType == MoyoungConstants.CMD_QUERY_STOCKS)
        {
            LOG.info("Stocks queried from watch");
            return true;
        }

        if (packetType == MoyoungConstants.CMD_DAGPT)
        {
            LOG.info("Da GPT started on watch");
            return true;
        }

        if (packetType == MoyoungConstants.CMD_FIND_MY_PHONE)
        {
            LOG.info("Find my phone started on watch");
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            findPhoneEvent.event = findMyPhoneActive ? GBDeviceEventFindPhone.Event.STOP : GBDeviceEventFindPhone.Event.START;
            evaluateGBDeviceEvent(findPhoneEvent);
            findMyPhoneActive = !findMyPhoneActive;
            return true;
        }

        LOG.warn("Unhandled packet " + Logging.formatBytes(new byte[]{packetType}) + ": " + Logging.formatBytes(payload));
        return false;
    }

    private void broadcastSample(MoyoungActivitySample sample) {
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
            .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
            .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void broadcastSample(MoyoungHeartRateSample sample) {
        MoyoungActivitySample genericSample = new MoyoungActivitySample();
        genericSample.setTimestamp((int) (sample.getTimestamp() / 1000));
        genericSample.setHeartRate(sample.getHeartRate());
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, genericSample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, genericSample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void handleDeviceInfo(DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getSoftwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void handleBatteryInfo(BatteryInfo info) {
        LOG.warn("Battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private void sendNotification(byte type, String text)
    {
        try {
            TransactionBuilder builder = performInitialized("sendNotification");
            byte[] str = text.getBytes();
            byte[] payload = new byte[str.length + 1];
            payload[0] = type;
            System.arraycopy(str, 0, payload, 1, str.length);
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SEND_MESSAGE, payload));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending notification: ", e);
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        final String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        // Notifications are sent with both sender/title and message in 1 packet, separated by a ':',
        // so we have to make sure there is no ':' in the sender/title part
        String message = StringUtils.truncate(senderOrTitle, 32).replace(":", ";") + ":";
        if (notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 128) + "\n\n";
        }
        if (notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 512);
        }
        if (notificationSpec.body == null && notificationSpec.subject == null) {
            message += " ";
        }

        // The notification is split at first : into sender and text
        sendNotification(MoyoungConstants.notificationType(notificationSpec.type), message);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING)
            sendNotification(MoyoungConstants.NOTIFICATION_TYPE_CALL, NotificationUtils.getPreferredTextFor(callSpec));
        else
            sendNotification(MoyoungConstants.NOTIFICATION_TYPE_CALL_OFF_HOOK, "");
    }

    private void setMeasurementSystem(TransactionBuilder builder) {
        Prefs prefs = GBApplication.getPrefs();
        String unit = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));

        MoyoungEnumMetricSystem metricSystem = null;
        if (unit.equals(getContext().getString(R.string.p_unit_metric)))
            metricSystem = MoyoungEnumMetricSystem.METRIC_SYSTEM;
        else if (unit.equals(getContext().getString(R.string.p_unit_imperial)))
            metricSystem = MoyoungEnumMetricSystem.IMPERIAL_SYSTEM;
        else
            LOG.warn("Invalid unit preference: {}", unit);

        if (metricSystem != null) {
            if (builder == null)
                sendSetting(getSetting("METRIC_SYSTEM"), metricSystem);
            else
                sendSetting(builder, getSetting("METRIC_SYSTEM"), metricSystem);
        }
    }

    private void setTime(TransactionBuilder builder) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(MoyoungConstants.LocalTimeToWatchTime(new Date())); // The watch is hardcoded to GMT+8 internally...
        buffer.put((byte)8); // I guess this means GMT+8 but changing it has no effect at all (it was hardcoded in the original app too)
        sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SYNC_TIME, buffer.array()));
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("onSetTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error setting time: ", e);
        }
    }

    private void handleGetAlarmsResponse(byte[] payload)
    {
        if (payload.length % 8 != 0)
            throw new IllegalArgumentException();

        List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> alarms = DBHelper.getAlarms(gbDevice);
        int i = 0;
        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : alarms) {
            ByteBuffer buffer = ByteBuffer.wrap(payload, 8 * i, 8);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.get() != i)
                throw new IllegalArgumentException();
            if (alarm.getPosition() != i)
                throw new IllegalArgumentException();
            alarm.setEnabled(buffer.get() != 0);
            byte repetition = buffer.get();
            alarm.setRepetition(AlarmUtils.createRepetitionMask(
                (repetition & 2) != 0,
                (repetition & 4) != 0,
                (repetition & 8) != 0,
                (repetition & 16) != 0,
                (repetition & 32) != 0,
                (repetition & 64) != 0,
                (repetition & 1) != 0));
            alarm.setHour(buffer.get());
            alarm.setMinute(buffer.get());
            byte singleShotYearAndMonth = buffer.get();
            byte singleShotDay = buffer.get();
            byte repetitionEnabled = buffer.get(); // not sure why they store the same info in two places
            DBHelper.store(alarm);
            i++;
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("onSetAlarms");
            for(int i = 0; i < 3; i++) {
                Alarm alarm = alarms.get(i);

                ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)i);
                buffer.put(alarm.getEnabled() ? (byte)1 : (byte)0);
                byte repetition = 0;
                if (alarm.getRepetition(Alarm.ALARM_SUN))
                    repetition |= 1;
                if (alarm.getRepetition(Alarm.ALARM_MON))
                    repetition |= 2;
                if (alarm.getRepetition(Alarm.ALARM_TUE))
                    repetition |= 4;
                if (alarm.getRepetition(Alarm.ALARM_WED))
                    repetition |= 8;
                if (alarm.getRepetition(Alarm.ALARM_THU))
                    repetition |= 16;
                if (alarm.getRepetition(Alarm.ALARM_FRI))
                    repetition |= 32;
                if (alarm.getRepetition(Alarm.ALARM_SAT))
                    repetition |= 64;
                buffer.put(repetition);
                buffer.put((byte)alarm.getHour());
                buffer.put((byte)alarm.getMinute());
                if (repetition == 0)
                {
                    // TODO: it would be possible to set an "once" alarm on a set day, but Gadgetbridge does not seem to support that
                    Calendar calendar = AlarmUtils.toCalendar(alarm);
                    buffer.put((byte)(((calendar.get(Calendar.YEAR) - 2015) << 4) + calendar.get(Calendar.MONTH) + 1));
                    buffer.put((byte)calendar.get(Calendar.DAY_OF_MONTH));
                }
                else
                {
                    buffer.put((byte)0);
                    buffer.put((byte)0);
                }
                byte repeat;
                if (repetition == 0)
                    repeat = 0;
                else if (repetition == 127)
                    repeat = 1;
                else
                    repeat = 2;
                buffer.put(repeat);
                sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_ALARM_CLOCK, buffer.array()));
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error setting alarms: ", e);
        }
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        switch (seconds) {
            case 300:
                sendSetting(getSetting("HR_AUTO_INTERVAL"), MoyoungConstants.HR_INTERVAL_5MIN);
                break;
            case 600:
                sendSetting(getSetting("HR_AUTO_INTERVAL"), MoyoungConstants.HR_INTERVAL_10MIN);
                break;
            case 1200:
                sendSetting(getSetting("HR_AUTO_INTERVAL"), MoyoungConstants.HR_INTERVAL_20MIN);
                break;
            case 1800:
                sendSetting(getSetting("HR_AUTO_INTERVAL"), MoyoungConstants.HR_INTERVAL_30MIN);
                break;
            default:
                sendSetting(getSetting("HR_AUTO_INTERVAL"), MoyoungConstants.HR_INTERVAL_OFF);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        try {
            TransactionBuilder builder = performInitialized("sendMusicState");
            byte[] payload = new byte[]{(byte) (stateSpec.state == MusicStateSpec.STATE_PLAYING ? 0x01 : 0x00)};
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_MUSIC_STATE, payload));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending music state: ", e);
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        try {
            TransactionBuilder builder = performInitialized("sendMusicInfo");
            byte[] artistBytes = musicSpec.artist.getBytes();
            byte[] artistPayload = new byte[artistBytes.length + 1];
            artistPayload[0] = 1;
            System.arraycopy(artistBytes, 0, artistPayload, 1, artistBytes.length);
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_MUSIC_INFO, artistPayload));
            byte[] trackBytes = musicSpec.track.getBytes();
            byte[] trackPayload = new byte[trackBytes.length + 1];
            trackPayload[0] = 0;
            System.arraycopy(trackBytes, 0, trackPayload, 1, trackBytes.length);
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_MUSIC_INFO, trackPayload));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending music info: ", e);
        }
    }

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {
        // TODO
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        // TODO
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        // TODO
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0)
        {
            try {
                new FetchDataOperation(this).perform();
            } catch (IOException e) {
                LOG.error("Error fetching data: ", e);
            }
        }
    }

    private Runnable updateIdleStepsRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                updateIdleSteps();
            } finally {
                idleUpdateHandler.postDelayed(updateIdleStepsRunnable, IDLE_STEPS_INTERVAL);
            }
        }
    };

    private void updateIdleSteps()
    {
        // The steps value hasn't changed for a while, so the user is not moving
        // Store this information in the database to improve the averaging over long periods of time

        if (!getDevice().isConnected())
        {
            LOG.warn("updateIdleSteps but device not connected?!");
            return;
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

            MoyoungActivitySampleProvider provider = new MoyoungActivitySampleProvider(getDevice(), dbHandler.getDaoSession());

            int currentSampleTimestamp = (int)(Calendar.getInstance().getTimeInMillis() / 1000);

            MoyoungActivitySample sample = new MoyoungActivitySample();
            sample.setDevice(device);
            sample.setUser(user);
            sample.setProvider(provider);
            sample.setTimestamp(currentSampleTimestamp);

            sample.setRawKind(MoyoungActivitySampleProvider.ACTIVITY_NOT_MEASURED);
            sample.setDataSource(MoyoungActivitySampleProvider.SOURCE_STEPS_IDLE);

            sample.setSteps(0);
            sample.setDistanceMeters(0);
            sample.setCaloriesBurnt(0);

            sample.setHeartRate(ActivitySample.NOT_MEASURED);

//            provider.addGBActivitySample(sample);
//            broadcastSample(sample);

            LOG.info("Adding an idle sample: " + sample.toString());
        } catch (Exception ex) {
            LOG.error("Error saving samples: ", ex);
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    private void handleHeartRateHistory(byte[] data) {
        final int packetIndex = data[0];
        final int daysAgo = Math.floorDiv(packetIndex, 4);
        final int startHour = (packetIndex % 4) * 6;  // There are 6 hours of data in every packet
        final ArrayList<MoyoungHeartRateSample> hrSamples = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        cal.set(Calendar.SECOND, 0);
        LOG.info("Received HR history packet: index={}, daysAgo={}, startHour={}", packetIndex, daysAgo, startHour);
        int index = 1;
        for (int hour=startHour; hour<startHour+6; hour++) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            for (int minute=0; minute<60; minute+=5) {
                cal.set(Calendar.MINUTE, minute);
                int hr = data[index] & 0xff;
                if (HeartRateUtils.getInstance().isValidHeartRateValue(hr) && cal.getTimeInMillis() < System.currentTimeMillis()) {
                    MoyoungHeartRateSample sample = new MoyoungHeartRateSample();
                    sample.setTimestamp(cal.getTimeInMillis());
                    sample.setHeartRate(hr);
                    hrSamples.add(sample);
                    LOG.info("Parsed HR sample: {}", sample);
                }
                index++;
            }
        }
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            MoyoungHeartRateSampleProvider sampleProvider = new MoyoungHeartRateSampleProvider(getDevice(), dbHandler.getDaoSession());
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

            for (MoyoungHeartRateSample sample : hrSamples) {
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
            }

            sampleProvider.addSamples(hrSamples);
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording heart rate samples", e);
        }

        // Request next batch
        if (packetIndex >= 7) return;  // 8 packets = 2 days, the maximum
        try {
            TransactionBuilder builder = performInitialized("FetchHROperation");
            sendPacket(builder, MoyoungPacketOut.buildPacket(getMtu(), MoyoungConstants.CMD_QUERY_PAST_HEART_RATE_1, new byte[]{(byte) (packetIndex+1)}));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Failed sending HR history request packet: ", e);
        }
    }

    public void handleStepsHistory(int daysAgo, byte[] data, boolean isRealtime) {
        if (data.length != 9)
            throw new IllegalArgumentException();

        byte[] bArr2 = new byte[3];
        System.arraycopy(data, 0, bArr2, 0, 3);
        int steps = BLETypeConversions.toUint24(bArr2);
        System.arraycopy(data, 3, bArr2, 0, 3);
        int distance = BLETypeConversions.toUint24(bArr2);
        System.arraycopy(data, 6, bArr2, 0, 3);
        int calories = BLETypeConversions.toUint24(bArr2);

        LOG.info("steps[{}] steps={}, distance={}, calories={}", daysAgo, steps, distance, calories);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

            MoyoungActivitySampleProvider provider = new MoyoungActivitySampleProvider(getDevice(), dbHandler.getDaoSession());

            Calendar thisSample = Calendar.getInstance();
            if (daysAgo != 0)
            {
                thisSample.add(Calendar.DATE, -daysAgo);
                thisSample.set(Calendar.HOUR_OF_DAY, 23);
                thisSample.set(Calendar.MINUTE, 59);
                thisSample.set(Calendar.SECOND, 59);
                thisSample.set(Calendar.MILLISECOND, 999);
            }
            else
            {
                // no change needed - use current time
            }

            Calendar startOfDay = (Calendar) thisSample.clone();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            int startOfDayTimestamp = (int) (startOfDay.getTimeInMillis() / 1000);
            int thisSampleTimestamp = (int) (thisSample.getTimeInMillis() / 1000);

            int previousSteps = 0;
            int previousDistance = 0;
            int previousCalories = 0;
            for (MoyoungActivitySample sample : provider.getAllActivitySamples(startOfDayTimestamp, thisSampleTimestamp))
            {
                if (sample.getSteps() != ActivitySample.NOT_MEASURED)
                    previousSteps += sample.getSteps();
                if (sample.getDistanceMeters() != ActivitySample.NOT_MEASURED)
                    previousDistance += sample.getDistanceMeters();
                if (sample.getCaloriesBurnt() != ActivitySample.NOT_MEASURED)
                    previousCalories += sample.getCaloriesBurnt();
            }

            int newSteps = steps - previousSteps;
            int newDistance = distance - previousDistance;
            int newCalories = calories - previousCalories;

            if (newSteps < 0 || newDistance < 0 || newCalories < 0)
            {
                LOG.warn("Ignoring a sample that would generate negative values: steps += {}, distance +={}, calories += {}", newSteps, newDistance, newCalories);
            }
            else if (newSteps != 0 || newDistance != 0 || newCalories != 0 || daysAgo == 0)
            {
                MoyoungActivitySample sample = new MoyoungActivitySample();
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);
                sample.setTimestamp(thisSampleTimestamp);

//                sample.setRawKind(MoyoungActivitySampleProvider.ACTIVITY_NOT_MEASURED);
                sample.setDataSource(daysAgo == 0 ? MoyoungActivitySampleProvider.SOURCE_STEPS_REALTIME : MoyoungActivitySampleProvider.SOURCE_STEPS_SUMMARY);
                sample.setSteps(newSteps);
                sample.setDistanceMeters(newDistance);
                sample.setCaloriesBurnt(newCalories);

                provider.addGBActivitySample(sample);
                if (isRealtime)
                {
                    idleUpdateHandler.removeCallbacks(updateIdleStepsRunnable);
                    idleUpdateHandler.postDelayed(updateIdleStepsRunnable, IDLE_STEPS_INTERVAL);
                    broadcastSample(sample);
                }

                LOG.info("Adding a sample: {}", sample);
            }
        } catch (Exception ex) {
            LOG.error("Error saving samples: ", ex);
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    public void handleSleepHistory(int daysAgo, byte[] data)
    {
        if (data.length % 3 != 0)
            throw new IllegalArgumentException();

        int prevActivityType = MoyoungActivitySampleProvider.ACTIVITY_SLEEP_START;
        int prevSampleTimestamp = -1;

        for(int i = 0; i < data.length / 3; i++)
        {
            int type = data[3*i];
            int start_h = data[3*i + 1];
            int start_m = data[3*i + 2];

            LOG.info("sleep[" + daysAgo + "][" + i + "] type=" + type + ", start_h=" + start_h + ", start_m=" + start_m);

            // SleepAnalysis measures sleep fragment type by marking the END of the fragment.
            // The watch provides data by marking the START of the fragment.

            // Additionally, ActivityAnalysis (used by the weekly view...) does AVERAGING when
            // adjacent samples are not of the same type..

            // FIXME: The way Gadgetbridge does it seems kinda broken...

            // This means that we have to convert the data when importing. Each sample gets
            // converted to two samples - one marking the beginning of the segment, and another
            // marking the end.

            // Watch:           SLEEP_LIGHT       ...       SLEEP_DEEP       ...      SLEEP_LIGHT        ...       SLEEP_SOBER
            // Gadgetbridge: ANYTHING,SLEEP_LIGHT ... SLEEP_LIGHT,SLEEP_DEEP ... SLEEP_DEEP,SLEEP_LIGHT  ... SLEEP_LIGHT,ANYTHING
            //                       ^     ^- this is important, it MUST be sleep, to ensure proper detection
            //  Time since the last -|        of sleepStart, see SleepAnalysis.calculateSleepSessions
            //  sample must be 0
            //  (otherwise SleepAnalysis will include this fragment...)

            // This means that when inserting samples:
            // * every sample is converted to (previous_sample_type, current_sample_type) happening
            //   roughly at the same time (but in this order)
            // * the first sample is prefixed by unspecified activity
            // * the last sample (SOBER) is converted to unspecified activity

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                User user = DBHelper.getUser(dbHandler.getDaoSession());
                Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

                MoyoungActivitySampleProvider provider = new MoyoungActivitySampleProvider(getDevice(), dbHandler.getDaoSession());

                Calendar thisSample = Calendar.getInstance();
                thisSample.add(Calendar.HOUR_OF_DAY, 4); // the clock assumes the sleep day changes at 20:00, so move the time forward to make the day correct
                thisSample.set(Calendar.MINUTE, 0);
                thisSample.add(Calendar.DATE, -daysAgo);

                thisSample.set(Calendar.HOUR_OF_DAY, start_h);
                thisSample.set(Calendar.MINUTE, start_m);
                thisSample.set(Calendar.SECOND, 0);
                thisSample.set(Calendar.MILLISECOND, 0);
                int thisSampleTimestamp = (int) (thisSample.getTimeInMillis() / 1000);

                int activityType;
                if (type == MoyoungConstants.SLEEP_SOBER)
                    activityType = MoyoungActivitySampleProvider.ACTIVITY_SLEEP_END;
                else if (type == MoyoungConstants.SLEEP_LIGHT)
                    activityType = MoyoungActivitySampleProvider.ACTIVITY_SLEEP_LIGHT;
                else if (type == MoyoungConstants.SLEEP_RESTFUL)
                    activityType = MoyoungActivitySampleProvider.ACTIVITY_SLEEP_RESTFUL;
                else
                    throw new IllegalArgumentException("Invalid sleep type");

                // Insert the end of previous segment sample
                MoyoungActivitySample prevSegmentSample = new MoyoungActivitySample();
                prevSegmentSample.setDevice(device);
                prevSegmentSample.setUser(user);
                prevSegmentSample.setProvider(provider);
                prevSegmentSample.setTimestamp(thisSampleTimestamp - 1);

                prevSegmentSample.setRawKind(prevActivityType);
                prevSegmentSample.setDataSource(MoyoungActivitySampleProvider.SOURCE_SLEEP_SUMMARY);

//                prevSegmentSample.setBatteryLevel(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setSteps(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setDistanceMeters(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

                prevSegmentSample.setHeartRate(ActivitySample.NOT_MEASURED);
//                prevSegmentSample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
//                prevSegmentSample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
//                prevSegmentSample.setBloodOxidation(ActivitySample.NOT_MEASURED);

//                addGBActivitySampleIfNotExists(provider, prevSegmentSample);

                // Insert the start of new segment sample
                MoyoungActivitySample nextSegmentSample = new MoyoungActivitySample();
                nextSegmentSample.setDevice(device);
                nextSegmentSample.setUser(user);
                nextSegmentSample.setProvider(provider);
                nextSegmentSample.setTimestamp(thisSampleTimestamp);

                nextSegmentSample.setRawKind(activityType);
                nextSegmentSample.setDataSource(MoyoungActivitySampleProvider.SOURCE_SLEEP_SUMMARY);

//                nextSegmentSample.setBatteryLevel(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setSteps(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setDistanceMeters(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

                nextSegmentSample.setHeartRate(ActivitySample.NOT_MEASURED);
//                nextSegmentSample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
//                nextSegmentSample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
//                nextSegmentSample.setBloodOxidation(ActivitySample.NOT_MEASURED);

//                addGBActivitySampleIfNotExists(provider, nextSegmentSample);

                // Set the activity type on all samples in this time period
                if (prevActivityType != MoyoungActivitySampleProvider.ACTIVITY_SLEEP_START)
//                    provider.updateActivityInRange(prevSampleTimestamp, thisSampleTimestamp, prevActivityType);

                prevActivityType = activityType;
                if (prevActivityType == MoyoungActivitySampleProvider.ACTIVITY_SLEEP_END)
                    prevActivityType = MoyoungActivitySampleProvider.ACTIVITY_SLEEP_START;
                prevSampleTimestamp = thisSampleTimestamp;
            } catch (Exception ex) {
                LOG.error("Error saving samples: ", ex);
                GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
            }
        }
    }

    public void handleTrainingData(byte[] data)
    {
        if (data.length % 24 != 0)
            throw new IllegalArgumentException();

        for(int i = 0; i < data.length / 24; i++)
        {
            if (ArrayUtils.isAllZeros(data, 24*i, 24)) // no data recorded in this slot
                continue;

            ByteBuffer buffer = ByteBuffer.wrap(data, 24 * i, 24);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            Date startTime = MoyoungConstants.WatchTimeToLocalTime(buffer.getInt());
            Date endTime = MoyoungConstants.WatchTimeToLocalTime(buffer.getInt());
            int validTime = buffer.getShort();
            byte num = buffer.get(); // == i
            byte type = buffer.get();
            int steps = buffer.getInt();
            int distance = buffer.getInt();
            int calories = buffer.getShort();
            LOG.info("Training data: start=" + startTime + " end=" + endTime + " totalTimeWithoutPause=" + validTime + " num=" + num + " type=" + type + " steps=" + steps + " distance=" + distance + " calories=" + calories);

            // NOTE: We are ignoring the step/distance/calories data here
            // If we had the phone connected, the realtime data is already stored anyway, and I'm
            // too lazy to try to integrate this info into the main timeline without messing
            // something up or counting the steps twice

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                User user = DBHelper.getUser(dbHandler.getDaoSession());
                Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

                MoyoungActivitySampleProvider provider = new MoyoungActivitySampleProvider(getDevice(), dbHandler.getDaoSession());
                BaseActivitySummaryDao summaryDao = provider.getSession().getBaseActivitySummaryDao();

                QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
                qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(device.getId()))
                    .where(BaseActivitySummaryDao.Properties.StartTime.eq(startTime))
                    .where(BaseActivitySummaryDao.Properties.EndTime.eq(endTime));
                boolean alreadyHaveThisSample = qb.count() > 0;

                if (alreadyHaveThisSample)
                {
                    LOG.info("Already had this training sample, ignoring");
                }
                else
                {
                    BaseActivitySummary summary = new BaseActivitySummary();

                    summary.setDevice(device);
                    summary.setUser(user);

                    ActivityKind gbType = provider.normalizeType(type);
                    String name;
                    if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_ROPE)
                        name = "Rope";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_BADMINTON)
                        name = "Badminton";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_BASKETBALL)
                        name = "Basketball";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_FOOTBALL)
                        name = "Football";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_MOUNTAINEERING)
                        name = "Mountaineering";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_TENNIS)
                        name = "Tennis";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_RUGBY)
                        name = "Rugby";
                    else if (type == MoyoungActivitySampleProvider.ACTIVITY_TRAINING_GOLF)
                        name = "Golf";
                    else
                        name = gbType.name();
                    summary.setName(name);
                    summary.setActivityKind(gbType.getCode());

                    summary.setStartTime(startTime);
                    summary.setEndTime(endTime);

                    summaryDao.insert(summary);

                    // NOTE: The type format from device maps directly to the database format
                    provider.updateActivityInRange((int)(startTime.getTime() / 1000), (int)(endTime.getTime() / 1000), type);
                }
            } catch (Exception ex) {
                LOG.error("Error saving samples: ", ex);
                GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
            }
        }
    }

    @Override
    public void onReset(int flags) {
        // TODO: this shuts down the watch, rather than rebooting it - perhaps add a new operation type?
        // (reboot is not supported, btw)

        try {
            TransactionBuilder builder = performInitialized("shutdown");
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SHUTDOWN, new byte[] { -1 }));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending reset command: ", e);
        }
    }

    private void triggerHeartRateTest(boolean start)
    {
        try {
            TransactionBuilder builder = performInitialized("onHeartRateTest");
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_TRIGGER_MEASURE_HEARTRATE, new byte[] { start ? (byte)0 : (byte)-1 }));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending heart rate test command: ", e);
        }
    }

    @Override
    public void onHeartRateTest() {
        triggerHeartRateTest(true);
    }

    public void onAbortHeartRateTest() {
        triggerHeartRateTest(false);
    }

    // TODO: starting other tests

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        // enabled all the time :D that's the only way to get more than a daily sum from this watch...
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (realTimeHeartRate == enable)
            return;
        realTimeHeartRate = enable; // will do another measurement immediately
        if (realTimeHeartRate)
            onHeartRateTest();
        else
            onAbortHeartRateTest();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start)
        {
            try {
                TransactionBuilder builder = performInitialized("onFindDevice");
                sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_FIND_MY_WATCH, new byte[0]));
                builder.queue(getQueue());
            } catch (IOException e) {
                LOG.error("Error while finding device: ", e);
            }
        }
        else
        {
            // Not supported - the device vibrates three times and then stops automatically
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends MoyoungSetting> T getSetting(String id) {
        AbstractMoyoungDeviceCoordinator coordinator = (AbstractMoyoungDeviceCoordinator) getDevice().getDeviceCoordinator();
        for(MoyoungSetting setting : coordinator.getSupportedSettings())
        {
            if (setting.name.equals(id))
                return (T) setting;
        }
        throw new IllegalArgumentException("No such setting: " + id);
    }

    private static Calendar getTimePref(Prefs prefs, String key, String defaultValue) {
        String timePref = prefs.getString(key, defaultValue);
        Date time = null;
        try {
            time = new SimpleDateFormat("HH:mm").parse(timePref);
        } catch (ParseException e) {
            LOG.error("Error parsing time: ", e);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return cal;
    }

    private <T> void sendSetting(TransactionBuilder builder, MoyoungSetting<T> setting, T newValue)
    {
        sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, setting.cmdSet, setting.encode(newValue)));
    }

    private <T> void sendSetting(MoyoungSetting<T> setting, T newValue)
    {
        try {
            TransactionBuilder builder = performInitialized("sendSetting");
            sendSetting(builder, setting, newValue);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending setting: ", e);
        }
    }

    private Set<MoyoungSetting> queriedSettings = new HashSet<>();

    private void querySetting(MoyoungSetting setting)
    {
        if (queriedSettings.contains(setting))
            return;

        try {
            TransactionBuilder builder = performInitialized("querySetting");
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, setting.cmdQuery, new byte[0]));
            builder.queue(getQueue());
            queriedSettings.add(setting);
        } catch (IOException e) {
            LOG.error("Error querying setting: ", e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        LOG.info("Send configuration: " + config);

        Prefs prefs = getDevicePrefs();
        switch (config) {
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_DATE_OF_BIRTH:
            case ActivityUser.PREF_USER_GENDER:
                sendSetting(getSetting("USER_INFO"), new ActivityUser());
                break;

            case ActivityUser.PREF_USER_STEPS_GOAL:
                sendSetting(getSetting("GOAL_STEP"), new ActivityUser().getStepsGoal());
                break;

            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                String timeSystemPref = prefs.getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));

                MoyoungEnumTimeSystem timeSystem;
                if (timeSystemPref.equals(getContext().getString(R.string.p_timeformat_24h)))
                    timeSystem = MoyoungEnumTimeSystem.TIME_SYSTEM_24;
                else if (timeSystemPref.equals(getContext().getString(R.string.p_timeformat_am_pm)))
                    timeSystem = MoyoungEnumTimeSystem.TIME_SYSTEM_12;
                else
                    throw new IllegalArgumentException();

                sendSetting(getSetting("TIME_SYSTEM"), timeSystem);
                break;

            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setMeasurementSystem(null);
                break;

            case MoyoungConstants.PREF_MOYOUNG_WATCH_FACE:
                String watchFacePref = prefs.getString(MoyoungConstants.PREF_MOYOUNG_WATCH_FACE, String.valueOf(1));
                byte watchFace = Byte.valueOf(watchFacePref);
                sendSetting(getSetting("DISPLAY_WATCH_FACE"), watchFace);
                break;

            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                String languagePref = prefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, "en_US");
                byte languageCode;
                switch (languagePref.substring(0, 2)) {
                    case "zh":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_CHINESE.value();
                        break;
                    case "it":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_ITALIAN.value();
                        break;
                    case "cs":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_CZECH.value();
                        break;
                    case "ru":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_RUSSIAN.value();
                        break;
                    case "pl":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_POLISH.value();
                        break;
                    case "nl":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_DUTCH.value();
                        break;
                    case "fr":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_FRENCH.value();
                        break;
                    case "es":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_SPANISH.value();
                        break;
                    case "de":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_GERMAN.value();
                        break;
                    case "pt":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_PORTUGUESE.value();
                        break;
                    case "jp":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_JAPANESE.value();
                        break;
                    case "ko":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_KOREAN.value();
                        break;
                    case "ar":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_ARABIC.value();
                        break;
                    case "uk":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_UKRAINIAN.value();
                        break;
                    case "hu":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_HUNGARIAN.value();
                        break;
                    case "ro":
                        languageCode = MoyoungEnumLanguage.LANGUAGE_ROMANIAN.value();
                        break;
                    default:
                        languageCode = MoyoungEnumLanguage.LANGUAGE_ENGLISH.value();
                }
                MoyoungSettingEnum<MoyoungEnumLanguage> languageSetting = getSetting("DEVICE_LANGUAGE");
                sendSetting(languageSetting, languageSetting.findByValue(languageCode));
                break;

            case MoyoungConstants.PREF_MOYOUNG_DEVICE_VERSION:
                String versionPref = prefs.getString(MoyoungConstants.PREF_MOYOUNG_DEVICE_VERSION,
                    String.valueOf(MoyoungEnumDeviceVersion.INTERNATIONAL_EDITION.value()));
                byte versionNum = Byte.valueOf(versionPref);
                MoyoungSettingEnum<MoyoungEnumDeviceVersion> versionSetting = getSetting("DEVICE_VERSION");
                sendSetting(versionSetting, versionSetting.findByValue(versionNum));
                break;

//            case MiBandConst.PREF_DO_NOT_DISTURB:
//            case MiBandConst.PREF_DO_NOT_DISTURB_START:
//            case MiBandConst.PREF_DO_NOT_DISTURB_END:
//                String doNotDisturbPref = prefs.getString(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
//                boolean doNotDisturbEnabled = !MiBandConst.PREF_DO_NOT_DISTURB_OFF.equals(doNotDisturbPref);
//
//                Calendar doNotDisturbStart = getTimePref(prefs, MiBandConst.PREF_DO_NOT_DISTURB_START, "01:00");
//                Calendar doNotDisturbEnd = getTimePref(prefs, MiBandConst.PREF_DO_NOT_DISTURB_END, "06:00");
//
//                MoyoungSettingTimeRange.TimeRange doNotDisturb;
//                if (doNotDisturbEnabled)
//                    doNotDisturb = new MoyoungSettingTimeRange.TimeRange(
//                        (byte) doNotDisturbStart.get(Calendar.HOUR_OF_DAY), (byte) doNotDisturbStart.get(Calendar.MINUTE),
//                        (byte) doNotDisturbEnd.get(Calendar.HOUR_OF_DAY), (byte) doNotDisturbEnd.get(Calendar.MINUTE));
//                else
//                    doNotDisturb = new MoyoungSettingTimeRange.TimeRange((byte)0, (byte)0, (byte)0, (byte)0);
//
//                sendSetting(getSetting("DO_NOT_DISTURB_TIME"), doNotDisturb);
//                break;

//            case HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
//            case HuamiConst.PREF_DISPLAY_ON_LIFT_START:
//            case HuamiConst.PREF_DISPLAY_ON_LIFT_END:
//                String quickViewPref = prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
//                boolean quickViewEnabled = !quickViewPref.equals(getContext().getString(R.string.p_off));
//                boolean quickViewScheduled = quickViewPref.equals(getContext().getString(R.string.p_scheduled));
//
//                Calendar quickViewStart = getTimePref(prefs, HuamiConst.PREF_DISPLAY_ON_LIFT_START, "00:00");
//                Calendar quickViewEnd = getTimePref(prefs, HuamiConst.PREF_DISPLAY_ON_LIFT_END, "00:00");
//
//                MoyoungSettingTimeRange.TimeRange quickViewTime;
//                if (quickViewEnabled && quickViewScheduled)
//                    quickViewTime = new MoyoungSettingTimeRange.TimeRange(
//                        (byte) quickViewStart.get(Calendar.HOUR_OF_DAY), (byte) quickViewStart.get(Calendar.MINUTE),
//                        (byte) quickViewEnd.get(Calendar.HOUR_OF_DAY), (byte) quickViewEnd.get(Calendar.MINUTE));
//                else
//                    quickViewTime = new MoyoungSettingTimeRange.TimeRange((byte)0, (byte)0, (byte)0, (byte)0);
//
//                sendSetting(getSetting("QUICK_VIEW"), quickViewEnabled);
//                sendSetting(getSetting("QUICK_VIEW_TIME"), quickViewTime);
//                break;

            case MoyoungConstants.PREF_SEDENTARY_REMINDER:
                String sedentaryReminderPref = prefs.getString(MoyoungConstants.PREF_SEDENTARY_REMINDER, "off");
                boolean sedentaryReminderEnabled = !sedentaryReminderPref.equals("off");
                sendSetting(getSetting("SEDENTARY_REMINDER"), sedentaryReminderEnabled);
                break;

            case MoyoungConstants.PREF_SEDENTARY_REMINDER_PERIOD:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_STEPS:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_START:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_END:
                byte sedentaryPeriod = (byte) prefs.getInt(MoyoungConstants.PREF_SEDENTARY_REMINDER_PERIOD, 30);
                byte sedentarySteps = (byte) prefs.getInt(MoyoungConstants.PREF_SEDENTARY_REMINDER_STEPS, 100);
                byte sedentaryStart = (byte) prefs.getInt(MoyoungConstants.PREF_SEDENTARY_REMINDER_START, 10);
                byte sedentaryEnd = (byte) prefs.getInt(MoyoungConstants.PREF_SEDENTARY_REMINDER_END, 22);
                sendSetting(getSetting("REMINDERS_TO_MOVE_PERIOD"),
                    new MoyoungSettingRemindersToMove.RemindersToMove(sedentaryPeriod, sedentarySteps, sedentaryStart, sedentaryEnd));
                break;
        }

        // Query the setting to make sure the configuration got actually applied
        // TODO: breaks sedentary
        //onReadConfiguration(config);
    }

    @Override
    public void onReadConfiguration(String config) {
        LOG.info("Read configuration: " + config);

        switch (config) {
            /* These use the global Gadgetbridge configuration and are always forced on device upon connection
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
            case ActivityUser.PREF_USER_GENDER:
                querySetting(getSetting("USER_INFO"));
                break;

            case ActivityUser.PREF_USER_STEPS_GOAL:
                querySetting(getSetting("GOAL_STEP"));
                break;*/

            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                querySetting(getSetting("TIME_SYSTEM"));
                break;

//            case DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM:
//                querySetting(getSetting("METRIC_SYSTEM"));
//                break;

            case MoyoungConstants.PREF_MOYOUNG_WATCH_FACE:
                querySetting(getSetting("DISPLAY_WATCH_FACE"));
                break;

            case MoyoungConstants.PREF_LANGUAGE:
                querySetting(getSetting("DEVICE_LANGUAGE"));
                break;

            case MoyoungConstants.PREF_MOYOUNG_DEVICE_VERSION:
                querySetting(getSetting("DEVICE_VERSION"));
                break;

//            case MiBandConst.PREF_DO_NOT_DISTURB:
//            case MiBandConst.PREF_DO_NOT_DISTURB_START:
//            case MiBandConst.PREF_DO_NOT_DISTURB_END:
//                querySetting(getSetting("DO_NOT_DISTURB_TIME"));
//                break;

//            case HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
//            case HuamiConst.PREF_DISPLAY_ON_LIFT_START:
//            case HuamiConst.PREF_DISPLAY_ON_LIFT_END:
//                querySetting(getSetting("QUICK_VIEW"));
//                querySetting(getSetting("QUICK_VIEW_TIME"));
//                break;

            case MoyoungConstants.PREF_SEDENTARY_REMINDER:
                querySetting(getSetting("SEDENTARY_REMINDER"));
                break;

            case MoyoungConstants.PREF_SEDENTARY_REMINDER_PERIOD:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_STEPS:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_START:
            case MoyoungConstants.PREF_SEDENTARY_REMINDER_END:
                querySetting(getSetting("REMINDERS_TO_MOVE_PERIOD"));
                break;
            default:
                return;
        }

//        GBDeviceEventConfigurationRead configReadEvent = new GBDeviceEventConfigurationRead();
//        configReadEvent.config = config;
//        configReadEvent.event = GBDeviceEventConfigurationRead.Event.IN_PROGRESS;
//        evaluateGBDeviceEvent(configReadEvent);
    }

    public void onReadConfigurationDone(MoyoungSetting setting, Object value, byte[] data)
    {
        LOG.info("CONFIG " + setting.name + " = " + value);
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
//        Prefs prefs = getDevicePrefs();
//        Map<String, String> changedProperties = new ArrayMap<>();
//        SharedPreferences.Editor prefsEditor = prefs.getPreferences().edit();
        switch (setting.name) {
            case "TIME_SYSTEM":
                MoyoungEnumTimeSystem timeSystem = (MoyoungEnumTimeSystem) value;
//                if (timeSystem == MoyoungEnumTimeSystem.TIME_SYSTEM_24)
//                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));
//                else if (timeSystem == MoyoungEnumTimeSystem.TIME_SYSTEM_12)
//                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_am_pm));
//                else
//                    throw new IllegalArgumentException("Invalid value");
                break;

//            case "METRIC_SYSTEM":
//                MoyoungEnumMetricSystem metricSystem = (MoyoungEnumMetricSystem) value;
//                if (metricSystem == MoyoungEnumMetricSystem.METRIC_SYSTEM)
//                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM, getContext().getString(R.string.p_unit_metric));
//                else if (metricSystem == MoyoungEnumMetricSystem.IMPERIAL_SYSTEM)
//                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM, getContext().getString(R.string.p_unit_imperial));
//                else
//                    throw new IllegalArgumentException("Invalid value");
//                break;

            case "DISPLAY_WATCH_FACE":
//                byte watchFace = (Byte) value;
//                changedProperties.put(MoyoungConstants.PREF_MOYOUNG_WATCH_FACE, String.valueOf(watchFace));
                eventUpdatePreferences.withPreference(
                        MoyoungConstants.PREF_MOYOUNG_WATCH_FACE,
                        String.valueOf((byte) value)
                );
                evaluateGBDeviceEvent(eventUpdatePreferences);
                break;

            case "DEVICE_LANGUAGE":
                MoyoungEnumLanguage language = (MoyoungEnumLanguage) value;
//                changedProperties.put(MoyoungConstants.PREF_LANGUAGE, String.valueOf(language.value()));
//                MoyoungEnumLanguage[] supportedLanguages = ((MoyoungSettingLanguage) setting).decodeSupportedValues(data);
//                Set<String> supportedLanguagesList = new HashSet<>();
//                for(MoyoungEnumLanguage supportedLanguage : supportedLanguages)
//                    supportedLanguagesList.add(String.valueOf(supportedLanguage.value()));
//                prefsEditor.putStringSet(MoyoungConstants.PREF_LANGUAGE_SUPPORT, supportedLanguagesList);
                break;

            case "DEVICE_VERSION":
                MoyoungEnumDeviceVersion deviceVersion = (MoyoungEnumDeviceVersion) value;
//                changedProperties.put(MoyoungConstants.PREF_MOYOUNG_DEVICE_VERSION, String.valueOf(deviceVersion.value()));
                break;

//            case "DO_NOT_DISTURB_TIME":
//                MoyoungSettingTimeRange.TimeRange doNotDisturb = (MoyoungSettingTimeRange.TimeRange) value;
//                if (doNotDisturb.start_h == 0 && doNotDisturb.start_m == 0 &&
//                    doNotDisturb.end_h == 0 && doNotDisturb.end_m == 0)
//                    changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
//                else
//                    changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_SCHEDULED);
//                changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB_START, String.format(Locale.ROOT, "%02d:%02d", doNotDisturb.start_h, doNotDisturb.start_m));
//                changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB_END, String.format(Locale.ROOT, "%02d:%02d", doNotDisturb.end_h, doNotDisturb.end_m));
//                break;

//            case "QUICK_VIEW":
//                boolean quickViewEnabled = (Boolean) value;
//                boolean quickViewScheduled = prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_off)).equals(getContext().getString(R.string.p_scheduled));
//                changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, quickViewEnabled ? (quickViewScheduled ? getContext().getString(R.string.p_scheduled) : getContext().getString(R.string.p_on)) : getContext().getString(R.string.p_off));
//                break;

//            case "QUICK_VIEW_TIME":
//                boolean quickViewEnabled2 = !prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_off)).equals(getContext().getString(R.string.p_off));
//                MoyoungSettingTimeRange.TimeRange quickViewTime = (MoyoungSettingTimeRange.TimeRange) value;
//                if (quickViewEnabled2)
//                {
//                    if (quickViewTime.start_h == 0 && quickViewTime.start_m == 0 &&
//                        quickViewTime.end_h == 0 && quickViewTime.end_m == 0)
//                        changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_on));
//                    else
//                        changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_scheduled));
//                }
//                changedProperties.put(HuamiConst.PREF_DISPLAY_ON_LIFT_START, String.format(Locale.ROOT, "%02d:%02d", quickViewTime.start_h, quickViewTime.start_m));
//                changedProperties.put(HuamiConst.PREF_DISPLAY_ON_LIFT_END, String.format(Locale.ROOT, "%02d:%02d", quickViewTime.end_h, quickViewTime.end_m));
//                break;

            case "SEDENTARY_REMINDER":
                boolean sedentaryReminderEnabled = (Boolean) value;
//                changedProperties.put(MoyoungConstants.PREF_SEDENTARY_REMINDER, sedentaryReminderEnabled ? "on": "off");
                break;

            case "REMINDERS_TO_MOVE_PERIOD":
                MoyoungSettingRemindersToMove.RemindersToMove remindersToMove = (MoyoungSettingRemindersToMove.RemindersToMove) value;
//                changedProperties.put(MoyoungConstants.PREF_SEDENTARY_REMINDER_PERIOD, String.valueOf(remindersToMove.period));
//                changedProperties.put(MoyoungConstants.PREF_SEDENTARY_REMINDER_STEPS, String.valueOf(remindersToMove.steps));
//                changedProperties.put(MoyoungConstants.PREF_SEDENTARY_REMINDER_START, String.valueOf(remindersToMove.start_h));
//                changedProperties.put(MoyoungConstants.PREF_SEDENTARY_REMINDER_END, String.valueOf(remindersToMove.end_h));
                break;
        }
//        for (Map.Entry<String, String> property : changedProperties.entrySet())
//            prefsEditor.putString(property.getKey(), property.getValue());
//        prefsEditor.apply();
//        for (Map.Entry<String, String> property : changedProperties.entrySet())
//        {
//            GBDeviceEventConfigurationRead configReadEvent = new GBDeviceEventConfigurationRead();
//            configReadEvent.config = property.getKey();
//            configReadEvent.event = GBDeviceEventConfigurationRead.Event.SUCCESS;
//            evaluateGBDeviceEvent(configReadEvent);
//        }
    }

    @Override
    public void onTestNewFunction() {
        try {
            new QuerySettingsOperation(this).perform();
        } catch (IOException e) {
            LOG.debug("Error while testing new function: ", e);
        }
    }

    @Override
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        try {
            WeatherSpec weatherSpec = weatherSpecs.get(0);
            TransactionBuilder builder = performInitialized("onSendWeather");

            MoyoungWeatherToday weatherToday = new MoyoungWeatherToday(weatherSpec);
            ByteBuffer packetWeatherToday = ByteBuffer.allocate(weatherToday.pm25 != null ? 21 : 19);
            packetWeatherToday.put(weatherToday.pm25 != null ? (byte)1 : (byte)0);
            packetWeatherToday.put(weatherToday.conditionId);
            packetWeatherToday.put(weatherToday.currentTemp);
            if (weatherToday.pm25 != null)
                packetWeatherToday.putShort(weatherToday.pm25);
            packetWeatherToday.put(weatherToday.lunar_or_festival.getBytes("unicodebigunmarked"));
            packetWeatherToday.put(weatherToday.city.getBytes("unicodebigunmarked"));
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_WEATHER_TODAY, packetWeatherToday.array()));

            ByteBuffer packetWeatherForecast = ByteBuffer.allocate(8 * 3);
            packetWeatherForecast.put(weatherToday.conditionId);
            packetWeatherForecast.put(weatherToday.currentTemp);
            packetWeatherForecast.put(weatherToday.currentTemp);
            for(int i = 0; i < 7; i++)
            {
                MoyoungWeatherForecast forecast;
                if (weatherSpec.forecasts.size() > i)
                    forecast = new MoyoungWeatherForecast(weatherSpec.forecasts.get(i));
                else
                    forecast = new MoyoungWeatherForecast(MoyoungConstants.WEATHER_HAZE, (byte)-100, (byte)-100); // I don't think there is a way to send less (my watch shows only tomorrow anyway...)
                packetWeatherForecast.put(forecast.conditionId);
                packetWeatherForecast.put(forecast.minTemp);
                packetWeatherForecast.put(forecast.maxTemp);
            }
            sendPacket(builder, MoyoungPacketOut.buildPacket(mtu, MoyoungConstants.CMD_SET_WEATHER_FUTURE, packetWeatherForecast.array()));

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error sending weather: ", e);
        }
    }
}
