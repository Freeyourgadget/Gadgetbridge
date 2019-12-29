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
package nodomain.freeyourgadget.gadgetbridge.service.devices.dafit;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventConfigurationRead;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitWeatherForecast;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitWeatherToday;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumDeviceVersion;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumMetricSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumTimeSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingEnum;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingRemindersToMove;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingTimeRange;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DaFitActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class DaFitDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DaFitDeviceSupport.class);
    private static final long IDLE_STEPS_INTERVAL = 5 * 60 * 1000;

    private final DeviceInfoProfile<DaFitDeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<DaFitDeviceSupport> batteryInfoProfile;
    private final HeartRateProfile<DaFitDeviceSupport> heartRateProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String s = intent.getAction();
            if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
            if (Objects.equals(s, BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                handleBatteryInfo((BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
            }
        }
    };

    private Handler idleUpdateHandler = new Handler();

    public static final int MTU = 20; // TODO: there seems to be some way to change this value...?
    private DaFitPacketIn packetIn = new DaFitPacketIn();

    private boolean realTimeHeartRate;

    public DaFitDeviceSupport() {
        super(LOG);
        batteryCmd.level = ActivitySample.NOT_MEASURED;

        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(DaFitConstants.UUID_SERVICE_DAFIT);

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
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_IN), true);
        deviceInfoProfile.requestDeviceInfo(builder);
        setTime(builder);
        sendSetting(builder, getSetting("USER_INFO"), new ActivityUser()); // these settings are write-only, so write them just in case because there is no way to know if they desynced somehow
        sendSetting(builder, getSetting("GOAL_STEP"), new ActivityUser().getStepsGoal());
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder);
        heartRateProfile.enableNotify(builder);
        builder.notify(getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_STEPS), true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

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
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_1);
        else if (packetType == 2)
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_2);
        else
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_OUT);
    }

    public void sendPacket(TransactionBuilder builder, byte[] packet)
    {
        DaFitPacketOut packetOut = new DaFitPacketOut(packet);

        byte[] fragment = new byte[MTU];
        while(packetOut.getFragment(fragment))
        {
            builder.write(getTargetCharacteristicForPacketType(packet[4]), fragment.clone());
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID charUuid = characteristic.getUuid();
        if (charUuid.equals(DaFitConstants.UUID_CHARACTERISTIC_STEPS))
        {
            byte[] payload = characteristic.getValue();
            Log.i("AAAAAAAAAAAAAAAA", "Update step count: " + Logging.formatBytes(characteristic.getValue()));
            handleStepsHistory(0, payload, true);
            return true;
        }
        if (charUuid.equals(DaFitConstants.UUID_CHARACTERISTIC_DATA_IN))
        {
            if (packetIn.putFragment(characteristic.getValue())) {
                Pair<Byte, byte[]> packet = DaFitPacketIn.parsePacket(packetIn.getPacket());
                packetIn = new DaFitPacketIn();
                if (packet != null) {
                    byte packetType = packet.first;
                    byte[] payload = packet.second;

                    Log.i("AAAAAAAAAAAAAAAA", "Response for: " + packetType);

                    if (handlePacket(packetType, payload))
                        return true;
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handlePacket(byte packetType, byte[] payload)
    {
        if (packetType == DaFitConstants.CMD_TRIGGER_MEASURE_HEARTRATE)
        {
            int heartRate = payload[0];
            Log.i("XXXXXXXX", "Measure heart rate finished: " + heartRate + " BPM");

            DaFitActivitySample sample = new DaFitActivitySample();
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

            sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
            sample.setDataSource(DaFitSampleProvider.SOURCE_SINGLE_MEASURE);

            sample.setBatteryLevel(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setDistanceMeters(ActivitySample.NOT_MEASURED);
            sample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

            sample.setHeartRate(heartRate);
            sample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
            sample.setBloodOxidation(ActivitySample.NOT_MEASURED);

            addGBActivitySample(sample);
            broadcastSample(sample);

            if (realTimeHeartRate)
                onHeartRateTest();

            return true;
        }
        if (packetType == DaFitConstants.CMD_TRIGGER_MEASURE_BLOOD_OXYGEN)
        {
            int percent = payload[0];
            Log.i("XXXXXXXX", "Measure blood oxygen finished: " + percent + "%");

            DaFitActivitySample sample = new DaFitActivitySample();
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

            sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
            sample.setDataSource(DaFitSampleProvider.SOURCE_SINGLE_MEASURE);

            sample.setBatteryLevel(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setDistanceMeters(ActivitySample.NOT_MEASURED);
            sample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
            sample.setBloodOxidation(percent);

            addGBActivitySample(sample);
            broadcastSample(sample);

            return true;
        }
        if (packetType == DaFitConstants.CMD_TRIGGER_MEASURE_BLOOD_PRESSURE)
        {
            int dataUnknown = payload[0];
            int data1 = payload[1];
            int data2 = payload[2];
            Log.i("XXXXXXXX", "Measure blood pressure finished: " + data1 + "/" + data2 + " (" + dataUnknown + ")");


            DaFitActivitySample sample = new DaFitActivitySample();
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

            sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
            sample.setDataSource(DaFitSampleProvider.SOURCE_SINGLE_MEASURE);

            sample.setBatteryLevel(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setDistanceMeters(ActivitySample.NOT_MEASURED);
            sample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureSystolic(data1);
            sample.setBloodPressureDiastolic(data2);
            sample.setBloodOxidation(ActivitySample.NOT_MEASURED);

            addGBActivitySample(sample);
            broadcastSample(sample);

            return true;
        }

        if (packetType == DaFitConstants.CMD_QUERY_LAST_DYNAMIC_RATE)
        {
            // Training on the watch just finished and it wants us to fetch the details
            LOG.info("Starting training fetch");
            try {
                new TrainingFinishedDataOperation(this, payload).perform();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (packetType == DaFitConstants.CMD_NOTIFY_PHONE_OPERATION)
        {
            byte operation = payload[0];
            if (operation == DaFitConstants.ARG_OPERATION_PLAY_PAUSE)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == DaFitConstants.ARG_OPERATION_PREV_SONG)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == DaFitConstants.ARG_OPERATION_NEXT_SONG)
            {
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                musicCmd.event = GBDeviceEventMusicControl.Event.NEXT;
                evaluateGBDeviceEvent(musicCmd);
                return true;
            }
            if (operation == DaFitConstants.ARG_OPERATION_DROP_INCOMING_CALL)
            {
                GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                return true;
            }
        }

        if (packetType == DaFitConstants.CMD_SWITCH_CAMERA_VIEW)
        {
            // TODO: trigger camera photo
            return true;
        }

        if (packetType == DaFitConstants.CMD_NOTIFY_WEATHER_CHANGE)
        {
            LOG.info("The watch really wants us to transmit the weather data for some reason...");
            // TODO: transmit weather
            return true;
        }

        for (DaFitSetting setting : queriedSettings)
        {
            if (setting.cmdQuery == packetType)
            {
                Object value = setting.decode(payload);
                onReadConfigurationDone(setting, value, payload);
                queriedSettings.remove(setting);
                return true;
            }
        }

        LOG.warn("Unhandled packet " + packetType + ": " + Logging.formatBytes(payload));
        return false;
    }

    private void addGBActivitySample(DaFitActivitySample sample) {
        addGBActivitySamples(new DaFitActivitySample[] { sample });
    }

    private void addGBActivitySamples(DaFitActivitySample[] samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

            DaFitSampleProvider provider = new DaFitSampleProvider(getDevice(), dbHandler.getDaoSession());

            for (DaFitActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);
                provider.addGBActivitySample(sample);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    private void broadcastSample(DaFitActivitySample sample) {
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
            .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
            .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
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

        DaFitActivitySample sample = new DaFitActivitySample();
        sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

        sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
        sample.setDataSource(DaFitSampleProvider.SOURCE_BATTERY);

        sample.setBatteryLevel(batteryCmd.level);
        sample.setSteps(ActivitySample.NOT_MEASURED);
        sample.setDistanceMeters(ActivitySample.NOT_MEASURED);
        sample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

        sample.setHeartRate(ActivitySample.NOT_MEASURED);
        sample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
        sample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
        sample.setBloodOxidation(ActivitySample.NOT_MEASURED);

        addGBActivitySample(sample);
        broadcastSample(sample);
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
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SEND_MESSAGE, payload));
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String sender = StringUtils.getFirstOf(notificationSpec.sender, StringUtils.getFirstOf(notificationSpec.sourceName, notificationSpec.sourceAppId));
        if (sender.isEmpty())
            sender = "(unknown)";

        String text = NotificationUtils.getPreferredTextFor(notificationSpec, 0, 75, getContext());
        if (text.isEmpty())
            text = StringUtils.getFirstOf(StringUtils.getFirstOf(notificationSpec.title, notificationSpec.subject), notificationSpec.body);

        // The notification is split at first : into sender and text
        sendNotification(DaFitConstants.notificationType(notificationSpec.type), sender + ":" + text);
    }

    @Override
    public void onDeleteNotification(int id) {
        // not supported :(
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING)
            sendNotification(DaFitConstants.NOTIFICATION_TYPE_CALL, NotificationUtils.getPreferredTextFor(callSpec));
        else
            sendNotification(DaFitConstants.NOTIFICATION_TYPE_CALL_OFF_HOOK, "");
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        // not supported :(
    }

    private void setTime(TransactionBuilder builder) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(DaFitConstants.LocalTimeToWatchTime(new Date())); // The watch is hardcoded to GMT+8 internally...
        buffer.put((byte)8); // I guess this means GMT+8 but changing it has no effect at all (it was hardcoded in the original app too)
        sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SYNC_TIME, buffer.array()));
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("onSetTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        // TODO: set alarms
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        // not supported :(
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        // not supported :(
    }

    @Override
    public void onInstallApp(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppInfoReq() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppDelete(UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0)
        {
            try {
                new FetchDataOperation(this).perform();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static int BytesToInt24(byte[] bArr) {
        if (bArr.length != 3)
            throw new IllegalArgumentException();
        return ((bArr[2] << 24) >>> 8) | ((bArr[1] << 8) & 0xFF00) | (bArr[0] & 0xFF);
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

            DaFitSampleProvider provider = new DaFitSampleProvider(getDevice(), dbHandler.getDaoSession());

            int currentSampleTimestamp = (int)(Calendar.getInstance().getTimeInMillis() / 1000);

            DaFitActivitySample sample = new DaFitActivitySample();
            sample.setDevice(device);
            sample.setUser(user);
            sample.setProvider(provider);
            sample.setTimestamp(currentSampleTimestamp);

            sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
            sample.setDataSource(DaFitSampleProvider.SOURCE_STEPS_IDLE);

            sample.setBatteryLevel(batteryCmd.level);
            sample.setSteps(0);
            sample.setDistanceMeters(0);
            sample.setCaloriesBurnt(0);

            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
            sample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
            sample.setBloodOxidation(ActivitySample.NOT_MEASURED);

            provider.addGBActivitySample(sample);
            broadcastSample(sample);

            LOG.info("Adding an idle sample: " + sample.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    public void handleStepsHistory(int daysAgo, byte[] data, boolean isRealtime)
    {
        if (data.length != 9)
            throw new IllegalArgumentException();

        byte[] bArr2 = new byte[3];
        System.arraycopy(data, 0, bArr2, 0, 3);
        int steps = BytesToInt24(bArr2);
        System.arraycopy(data, 3, bArr2, 0, 3);
        int distance = BytesToInt24(bArr2);
        System.arraycopy(data, 6, bArr2, 0, 3);
        int calories = BytesToInt24(bArr2);

        Log.i("steps[" + daysAgo + "]", "steps=" + steps + ", distance=" + distance + ", calories=" + calories);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

            DaFitSampleProvider provider = new DaFitSampleProvider(getDevice(), dbHandler.getDaoSession());

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
            for (DaFitActivitySample sample : provider.getAllActivitySamples(startOfDayTimestamp, thisSampleTimestamp))
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
                LOG.warn("Ignoring a sample that would generate negative values: steps += " + newSteps + ", distance +=" + newDistance + ", calories += " + newCalories);
            }
            else if (newSteps != 0 || newDistance != 0 || newCalories != 0 || daysAgo == 0)
            {
                DaFitActivitySample sample = new DaFitActivitySample();
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);
                sample.setTimestamp(thisSampleTimestamp);

                sample.setRawKind(DaFitSampleProvider.ACTIVITY_NOT_MEASURED);
                sample.setDataSource(daysAgo == 0 ? DaFitSampleProvider.SOURCE_STEPS_REALTIME : DaFitSampleProvider.SOURCE_STEPS_SUMMARY);

                sample.setBatteryLevel(ActivitySample.NOT_MEASURED);
                sample.setSteps(newSteps);
                sample.setDistanceMeters(newDistance);
                sample.setCaloriesBurnt(newCalories);

                sample.setHeartRate(ActivitySample.NOT_MEASURED);
                sample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
                sample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
                sample.setBloodOxidation(ActivitySample.NOT_MEASURED);

                provider.addGBActivitySample(sample);
                if (isRealtime)
                {
                    idleUpdateHandler.removeCallbacks(updateIdleStepsRunnable);
                    idleUpdateHandler.postDelayed(updateIdleStepsRunnable, IDLE_STEPS_INTERVAL);
                    broadcastSample(sample);
                }

                LOG.info("Adding a sample: " + sample.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
        }
    }

    public void handleSleepHistory(int daysAgo, byte[] data)
    {
        if (data.length % 3 != 0)
            throw new IllegalArgumentException();

        int prevActivityType = DaFitSampleProvider.ACTIVITY_SLEEP_START;
        int prevSampleTimestamp = -1;

        for(int i = 0; i < data.length / 3; i++)
        {
            int type = data[3*i];
            int start_h = data[3*i + 1];
            int start_m = data[3*i + 2];

            Log.i("sleep[" + daysAgo + "][" + i + "]", "type=" + type + ", start_h=" + start_h + ", start_m=" + start_m);

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

                DaFitSampleProvider provider = new DaFitSampleProvider(getDevice(), dbHandler.getDaoSession());

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
                if (type == DaFitConstants.SLEEP_SOBER)
                    activityType = DaFitSampleProvider.ACTIVITY_SLEEP_END;
                else if (type == DaFitConstants.SLEEP_LIGHT)
                    activityType = DaFitSampleProvider.ACTIVITY_SLEEP_LIGHT;
                else if (type == DaFitConstants.SLEEP_RESTFUL)
                    activityType = DaFitSampleProvider.ACTIVITY_SLEEP_RESTFUL;
                else
                    throw new IllegalArgumentException("Invalid sleep type");

                // Insert the end of previous segment sample
                DaFitActivitySample prevSegmentSample = new DaFitActivitySample();
                prevSegmentSample.setDevice(device);
                prevSegmentSample.setUser(user);
                prevSegmentSample.setProvider(provider);
                prevSegmentSample.setTimestamp(thisSampleTimestamp - 1);

                prevSegmentSample.setRawKind(prevActivityType);
                prevSegmentSample.setDataSource(DaFitSampleProvider.SOURCE_SLEEP_SUMMARY);

                prevSegmentSample.setBatteryLevel(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setSteps(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setDistanceMeters(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

                prevSegmentSample.setHeartRate(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
                prevSegmentSample.setBloodOxidation(ActivitySample.NOT_MEASURED);

                addGBActivitySampleIfNotExists(provider, prevSegmentSample);

                // Insert the start of new segment sample
                DaFitActivitySample nextSegmentSample = new DaFitActivitySample();
                nextSegmentSample.setDevice(device);
                nextSegmentSample.setUser(user);
                nextSegmentSample.setProvider(provider);
                nextSegmentSample.setTimestamp(thisSampleTimestamp);

                nextSegmentSample.setRawKind(activityType);
                nextSegmentSample.setDataSource(DaFitSampleProvider.SOURCE_SLEEP_SUMMARY);

                nextSegmentSample.setBatteryLevel(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setSteps(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setDistanceMeters(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);

                nextSegmentSample.setHeartRate(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setBloodPressureSystolic(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setBloodPressureDiastolic(ActivitySample.NOT_MEASURED);
                nextSegmentSample.setBloodOxidation(ActivitySample.NOT_MEASURED);

                addGBActivitySampleIfNotExists(provider, nextSegmentSample);

                // Set the activity type on all samples in this time period
                if (prevActivityType != DaFitSampleProvider.ACTIVITY_SLEEP_START)
                    provider.updateActivityInRange(prevSampleTimestamp, thisSampleTimestamp, prevActivityType);

                prevActivityType = activityType;
                if (prevActivityType == DaFitSampleProvider.ACTIVITY_SLEEP_END)
                    prevActivityType = DaFitSampleProvider.ACTIVITY_SLEEP_START;
                prevSampleTimestamp = thisSampleTimestamp;
            } catch (Exception ex) {
                ex.printStackTrace();
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
            Date startTime = DaFitConstants.WatchTimeToLocalTime(buffer.getInt());
            Date endTime = DaFitConstants.WatchTimeToLocalTime(buffer.getInt());
            int validTime = buffer.getShort();
            byte num = buffer.get(); // == i
            byte type = buffer.get();
            int steps = buffer.getInt();
            int distance = buffer.getInt();
            int calories = buffer.getShort();
            Log.i("Training data", "start=" + startTime + " end=" + endTime + " totalTimeWithoutPause=" + validTime + " num=" + num + " type=" + type + " steps=" + steps + " distance=" + distance + " calories=" + calories);

            // NOTE: We are ignoring the step/distance/calories data here
            // If we had the phone connected, the realtime data is already stored anyway, and I'm
            // too lazy to try to integrate this info into the main timeline without messing
            // something up or counting the steps twice

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                User user = DBHelper.getUser(dbHandler.getDaoSession());
                Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());

                DaFitSampleProvider provider = new DaFitSampleProvider(getDevice(), dbHandler.getDaoSession());
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

                    int gbType = provider.normalizeType(type);
                    String name;
                    if (type == DaFitSampleProvider.ACTIVITY_TRAINING_ROPE)
                        name = "Rope";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_BADMINTON)
                        name = "Badminton";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_BASKETBALL)
                        name = "Basketball";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_FOOTBALL)
                        name = "Football";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_MOUNTAINEERING)
                        name = "Mountaineering";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_TENNIS)
                        name = "Tennis";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_RUGBY)
                        name = "Rugby";
                    else if (type == DaFitSampleProvider.ACTIVITY_TRAINING_GOLF)
                        name = "Golf";
                    else
                        name = ActivityKind.asString(gbType, getContext());
                    summary.setName(name);
                    summary.setActivityKind(gbType);

                    summary.setStartTime(startTime);
                    summary.setEndTime(endTime);

                    summaryDao.insert(summary);

                    // NOTE: The type format from device maps directly to the database format
                    provider.updateActivityInRange((int)(startTime.getTime() / 1000), (int)(endTime.getTime() / 1000), type);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
            }
        }
    }

    private void addGBActivitySampleIfNotExists(DaFitSampleProvider provider, DaFitActivitySample sample)
    {
        boolean alreadyHaveThisSample = false;
        for (DaFitActivitySample sample2 : provider.getAllActivitySamples(sample.getTimestamp() - 1, sample.getTimestamp() + 1))
        {
            if (sample2.getTimestamp() == sample2.getTimestamp() && sample2.getRawKind() == sample.getRawKind())
                alreadyHaveThisSample = true;
        }

        if (!alreadyHaveThisSample)
        {
            provider.addGBActivitySample(sample);
            LOG.info("Adding a sample: " + sample.toString());
        }
    }

    @Override
    public void onReset(int flags) {
        // TODO: this shuts down the watch, rather than rebooting it - perhaps add a new operation type?
        // (reboot is not supported, btw)

        try {
            TransactionBuilder builder = performInitialized("shutdown");
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SHUTDOWN, new byte[] { -1 }));
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void triggerHeartRateTest(boolean start)
    {
        try {
            TransactionBuilder builder = performInitialized("onHeartRateTest");
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_TRIGGER_MEASURE_HEARTRATE, new byte[] { start ? (byte)0 : (byte)-1 }));
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
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
    public void onSetHeartRateMeasurementInterval(int seconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start)
        {
            try {
                TransactionBuilder builder = performInitialized("onFindDevice");
                sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_FIND_MY_WATCH, new byte[0]));
                builder.queue(getQueue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            // Not supported - the device vibrates three times and then stops automatically
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onScreenshotReq() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private <T extends DaFitSetting> T getSetting(String id) {
        DaFitDeviceCoordinator coordinator = (DaFitDeviceCoordinator) DeviceHelper.getInstance().getCoordinator(getDevice());
        for(DaFitSetting setting : coordinator.getSupportedSettings())
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
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return cal;
    }

    private <T> void sendSetting(TransactionBuilder builder, DaFitSetting<T> setting, T newValue)
    {
        sendPacket(builder, DaFitPacketOut.buildPacket(setting.cmdSet, setting.encode(newValue)));
    }

    private <T> void sendSetting(DaFitSetting<T> setting, T newValue)
    {
        try {
            TransactionBuilder builder = performInitialized("sendSetting");
            sendSetting(builder, setting, newValue);
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<DaFitSetting> queriedSettings = new HashSet<>();

    private void querySetting(DaFitSetting setting)
    {
        if (queriedSettings.contains(setting))
            return;

        try {
            TransactionBuilder builder = performInitialized("querySetting");
            sendPacket(builder, DaFitPacketOut.buildPacket(setting.cmdQuery, new byte[0]));
            builder.queue(getQueue());
            queriedSettings.add(setting);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        Log.i("OOOOOOOOOOOOOOOOsend", config);

        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        switch (config) {
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
            case ActivityUser.PREF_USER_GENDER:
                sendSetting(getSetting("USER_INFO"), new ActivityUser());
                break;

            case ActivityUser.PREF_USER_STEPS_GOAL:
                sendSetting(getSetting("GOAL_STEP"), new ActivityUser().getStepsGoal());
                break;

            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                String timeSystemPref = prefs.getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));

                DaFitEnumTimeSystem timeSystem;
                if (timeSystemPref.equals(getContext().getString(R.string.p_timeformat_24h)))
                    timeSystem = DaFitEnumTimeSystem.TIME_SYSTEM_24;
                else if (timeSystemPref.equals(getContext().getString(R.string.p_timeformat_am_pm)))
                    timeSystem = DaFitEnumTimeSystem.TIME_SYSTEM_12;
                else
                    throw new IllegalArgumentException();

                sendSetting(getSetting("TIME_SYSTEM"), timeSystem);
                break;

            case DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM:
                String metricSystemPref = prefs.getString(DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM, getContext().getString(R.string.p_unit_metric));

                DaFitEnumMetricSystem metricSystem;
                if (metricSystemPref.equals(getContext().getString(R.string.p_unit_metric)))
                    metricSystem = DaFitEnumMetricSystem.METRIC_SYSTEM;
                else if (metricSystemPref.equals(getContext().getString(R.string.p_unit_imperial)))
                    metricSystem = DaFitEnumMetricSystem.IMPERIAL_SYSTEM;
                else
                    throw new IllegalArgumentException();

                sendSetting(getSetting("METRIC_SYSTEM"), metricSystem);
                break;

            case DaFitConstants.PREF_WATCH_FACE:
                String watchFacePref = prefs.getString(DaFitConstants.PREF_WATCH_FACE, String.valueOf(1));
                byte watchFace = Byte.valueOf(watchFacePref);
                sendSetting(getSetting("DISPLAY_WATCH_FACE"), watchFace);
                break;

            case DaFitConstants.PREF_LANGUAGE:
                String languagePref = prefs.getString(DaFitConstants.PREF_LANGUAGE,
                    String.valueOf(DaFitEnumLanguage.LANGUAGE_ENGLISH.value()));
                byte languageNum = Byte.valueOf(languagePref);
                DaFitSettingEnum<DaFitEnumLanguage> languageSetting = getSetting("DEVICE_LANGUAGE");
                sendSetting(languageSetting, languageSetting.findByValue(languageNum));
                break;

            case DaFitConstants.PREF_DEVICE_VERSION:
                String versionPref = prefs.getString(DaFitConstants.PREF_DEVICE_VERSION,
                    String.valueOf(DaFitEnumDeviceVersion.INTERNATIONAL_EDITION.value()));
                byte versionNum = Byte.valueOf(versionPref);
                DaFitSettingEnum<DaFitEnumDeviceVersion> versionSetting = getSetting("DEVICE_VERSION");
                sendSetting(versionSetting, versionSetting.findByValue(versionNum));
                break;

            case MiBandConst.PREF_DO_NOT_DISTURB:
            case MiBandConst.PREF_DO_NOT_DISTURB_START:
            case MiBandConst.PREF_DO_NOT_DISTURB_END:
                String doNotDisturbPref = prefs.getString(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
                boolean doNotDisturbEnabled = !MiBandConst.PREF_DO_NOT_DISTURB_OFF.equals(doNotDisturbPref);

                Calendar doNotDisturbStart = getTimePref(prefs, MiBandConst.PREF_DO_NOT_DISTURB_START, "01:00");
                Calendar doNotDisturbEnd = getTimePref(prefs, MiBandConst.PREF_DO_NOT_DISTURB_END, "06:00");

                DaFitSettingTimeRange.TimeRange doNotDisturb;
                if (doNotDisturbEnabled)
                    doNotDisturb = new DaFitSettingTimeRange.TimeRange(
                        (byte) doNotDisturbStart.get(Calendar.HOUR_OF_DAY), (byte) doNotDisturbStart.get(Calendar.MINUTE),
                        (byte) doNotDisturbEnd.get(Calendar.HOUR_OF_DAY), (byte) doNotDisturbEnd.get(Calendar.MINUTE));
                else
                    doNotDisturb = new DaFitSettingTimeRange.TimeRange((byte)0, (byte)0, (byte)0, (byte)0);

                sendSetting(getSetting("DO_NOT_DISTURB_TIME"), doNotDisturb);
                break;

            case HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
            case HuamiConst.PREF_DISPLAY_ON_LIFT_START:
            case HuamiConst.PREF_DISPLAY_ON_LIFT_END:
                String quickViewPref = prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
                boolean quickViewEnabled = !quickViewPref.equals(getContext().getString(R.string.p_off));
                boolean quickViewScheduled = quickViewPref.equals(getContext().getString(R.string.p_scheduled));

                Calendar quickViewStart = getTimePref(prefs, HuamiConst.PREF_DISPLAY_ON_LIFT_START, "00:00");
                Calendar quickViewEnd = getTimePref(prefs, HuamiConst.PREF_DISPLAY_ON_LIFT_END, "00:00");

                DaFitSettingTimeRange.TimeRange quickViewTime;
                if (quickViewEnabled && quickViewScheduled)
                    quickViewTime = new DaFitSettingTimeRange.TimeRange(
                        (byte) quickViewStart.get(Calendar.HOUR_OF_DAY), (byte) quickViewStart.get(Calendar.MINUTE),
                        (byte) quickViewEnd.get(Calendar.HOUR_OF_DAY), (byte) quickViewEnd.get(Calendar.MINUTE));
                else
                    quickViewTime = new DaFitSettingTimeRange.TimeRange((byte)0, (byte)0, (byte)0, (byte)0);

                sendSetting(getSetting("QUICK_VIEW"), quickViewEnabled);
                sendSetting(getSetting("QUICK_VIEW_TIME"), quickViewTime);
                break;

            case DaFitConstants.PREF_SEDENTARY_REMINDER:
                String sedentaryReminderPref = prefs.getString(DaFitConstants.PREF_SEDENTARY_REMINDER, "off");
                boolean sedentaryReminderEnabled = !sedentaryReminderPref.equals("off");
                sendSetting(getSetting("SEDENTARY_REMINDER"), sedentaryReminderEnabled);
                break;

            case DaFitConstants.PREF_SEDENTARY_REMINDER_PERIOD:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_STEPS:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_START:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_END:
                byte sedentaryPeriod = (byte) prefs.getInt(DaFitConstants.PREF_SEDENTARY_REMINDER_PERIOD, 30);
                byte sedentarySteps = (byte) prefs.getInt(DaFitConstants.PREF_SEDENTARY_REMINDER_STEPS, 100);
                byte sedentaryStart = (byte) prefs.getInt(DaFitConstants.PREF_SEDENTARY_REMINDER_START, 10);
                byte sedentaryEnd = (byte) prefs.getInt(DaFitConstants.PREF_SEDENTARY_REMINDER_END, 22);
                sendSetting(getSetting("REMINDERS_TO_MOVE_PERIOD"),
                    new DaFitSettingRemindersToMove.RemindersToMove(sedentaryPeriod, sedentarySteps, sedentaryStart, sedentaryEnd));
                break;
        }

        // Query the setting to make sure the configuration got actually applied
        // TODO: breaks sedentary
        //onReadConfiguration(config);
    }

    @Override
    public void onReadConfiguration(String config) {
        Log.i("OOOOOOOOOOOOOOOOread", config);

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

            case DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM:
                querySetting(getSetting("METRIC_SYSTEM"));
                break;

            case DaFitConstants.PREF_WATCH_FACE:
                querySetting(getSetting("DISPLAY_WATCH_FACE"));
                break;

            case DaFitConstants.PREF_LANGUAGE:
                querySetting(getSetting("DEVICE_LANGUAGE"));
                break;

            case DaFitConstants.PREF_DEVICE_VERSION:
                querySetting(getSetting("DEVICE_VERSION"));
                break;

            case MiBandConst.PREF_DO_NOT_DISTURB:
            case MiBandConst.PREF_DO_NOT_DISTURB_START:
            case MiBandConst.PREF_DO_NOT_DISTURB_END:
                querySetting(getSetting("DO_NOT_DISTURB_TIME"));
                break;

            case HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
            case HuamiConst.PREF_DISPLAY_ON_LIFT_START:
            case HuamiConst.PREF_DISPLAY_ON_LIFT_END:
                querySetting(getSetting("QUICK_VIEW"));
                querySetting(getSetting("QUICK_VIEW_TIME"));
                break;

            case DaFitConstants.PREF_SEDENTARY_REMINDER:
                querySetting(getSetting("SEDENTARY_REMINDER"));
                break;

            case DaFitConstants.PREF_SEDENTARY_REMINDER_PERIOD:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_STEPS:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_START:
            case DaFitConstants.PREF_SEDENTARY_REMINDER_END:
                querySetting(getSetting("REMINDERS_TO_MOVE_PERIOD"));
                break;
            default:
                return;
        }

        GBDeviceEventConfigurationRead configReadEvent = new GBDeviceEventConfigurationRead();
        configReadEvent.config = config;
        configReadEvent.event = GBDeviceEventConfigurationRead.Event.IN_PROGRESS;
        evaluateGBDeviceEvent(configReadEvent);
    }

    public void onReadConfigurationDone(DaFitSetting setting, Object value, byte[] data)
    {
        Log.i("CONFIG", setting.name + " = " + value);
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        Map<String, String> changedProperties = new ArrayMap<>();
        SharedPreferences.Editor prefsEditor = prefs.getPreferences().edit();
        switch (setting.name) {
            case "TIME_SYSTEM":
                DaFitEnumTimeSystem timeSystem = (DaFitEnumTimeSystem) value;
                if (timeSystem == DaFitEnumTimeSystem.TIME_SYSTEM_24)
                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));
                else if (timeSystem == DaFitEnumTimeSystem.TIME_SYSTEM_12)
                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_am_pm));
                else
                    throw new IllegalArgumentException("Invalid value");
                break;

            case "METRIC_SYSTEM":
                DaFitEnumMetricSystem metricSystem = (DaFitEnumMetricSystem) value;
                if (metricSystem == DaFitEnumMetricSystem.METRIC_SYSTEM)
                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM, getContext().getString(R.string.p_unit_metric));
                else if (metricSystem == DaFitEnumMetricSystem.IMPERIAL_SYSTEM)
                    changedProperties.put(DeviceSettingsPreferenceConst.PREF_MEASUREMENTSYSTEM, getContext().getString(R.string.p_unit_imperial));
                else
                    throw new IllegalArgumentException("Invalid value");
                break;

            case "DISPLAY_WATCH_FACE":
                byte watchFace = (Byte) value;
                changedProperties.put(DaFitConstants.PREF_WATCH_FACE, String.valueOf(watchFace));
                break;

            case "DEVICE_LANGUAGE":
                DaFitEnumLanguage language = (DaFitEnumLanguage) value;
                changedProperties.put(DaFitConstants.PREF_LANGUAGE, String.valueOf(language.value()));
                DaFitEnumLanguage[] supportedLanguages = ((DaFitSettingLanguage) setting).decodeSupportedValues(data);
                Set<String> supportedLanguagesList = new HashSet<>();
                for(DaFitEnumLanguage supportedLanguage : supportedLanguages)
                    supportedLanguagesList.add(String.valueOf(supportedLanguage.value()));
                prefsEditor.putStringSet(DaFitConstants.PREF_LANGUAGE_SUPPORT, supportedLanguagesList);
                break;

            case "DEVICE_VERSION":
                DaFitEnumDeviceVersion deviceVersion = (DaFitEnumDeviceVersion) value;
                changedProperties.put(DaFitConstants.PREF_DEVICE_VERSION, String.valueOf(deviceVersion.value()));
                break;

            case "DO_NOT_DISTURB_TIME":
                DaFitSettingTimeRange.TimeRange doNotDisturb = (DaFitSettingTimeRange.TimeRange) value;
                if (doNotDisturb.start_h == 0 && doNotDisturb.start_m == 0 &&
                    doNotDisturb.end_h == 0 && doNotDisturb.end_m == 0)
                    changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_OFF);
                else
                    changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB, MiBandConst.PREF_DO_NOT_DISTURB_SCHEDULED);
                changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB_START, String.format(Locale.ROOT, "%02d:%02d", doNotDisturb.start_h, doNotDisturb.start_m));
                changedProperties.put(MiBandConst.PREF_DO_NOT_DISTURB_END, String.format(Locale.ROOT, "%02d:%02d", doNotDisturb.end_h, doNotDisturb.end_m));
                break;

            case "QUICK_VIEW":
                boolean quickViewEnabled = (Boolean) value;
                boolean quickViewScheduled = prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_off)).equals(getContext().getString(R.string.p_scheduled));
                changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, quickViewEnabled ? (quickViewScheduled ? getContext().getString(R.string.p_scheduled) : getContext().getString(R.string.p_on)) : getContext().getString(R.string.p_off));
                break;

            case "QUICK_VIEW_TIME":
                boolean quickViewEnabled2 = !prefs.getString(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_off)).equals(getContext().getString(R.string.p_off));
                DaFitSettingTimeRange.TimeRange quickViewTime = (DaFitSettingTimeRange.TimeRange) value;
                if (quickViewEnabled2)
                {
                    if (quickViewTime.start_h == 0 && quickViewTime.start_m == 0 &&
                        quickViewTime.end_h == 0 && quickViewTime.end_m == 0)
                        changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_on));
                    else
                        changedProperties.put(HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, getContext().getString(R.string.p_scheduled));
                }
                changedProperties.put(HuamiConst.PREF_DISPLAY_ON_LIFT_START, String.format(Locale.ROOT, "%02d:%02d", quickViewTime.start_h, quickViewTime.start_m));
                changedProperties.put(HuamiConst.PREF_DISPLAY_ON_LIFT_END, String.format(Locale.ROOT, "%02d:%02d", quickViewTime.end_h, quickViewTime.end_m));
                break;

            case "SEDENTARY_REMINDER":
                boolean sedentaryReminderEnabled = (Boolean) value;
                changedProperties.put(DaFitConstants.PREF_SEDENTARY_REMINDER, sedentaryReminderEnabled ? "on": "off");
                break;

            case "REMINDERS_TO_MOVE_PERIOD":
                DaFitSettingRemindersToMove.RemindersToMove remindersToMove = (DaFitSettingRemindersToMove.RemindersToMove) value;
                changedProperties.put(DaFitConstants.PREF_SEDENTARY_REMINDER_PERIOD, String.valueOf(remindersToMove.period));
                changedProperties.put(DaFitConstants.PREF_SEDENTARY_REMINDER_STEPS, String.valueOf(remindersToMove.steps));
                changedProperties.put(DaFitConstants.PREF_SEDENTARY_REMINDER_START, String.valueOf(remindersToMove.start_h));
                changedProperties.put(DaFitConstants.PREF_SEDENTARY_REMINDER_END, String.valueOf(remindersToMove.end_h));
                break;
        }
        for (Map.Entry<String, String> property : changedProperties.entrySet())
            prefsEditor.putString(property.getKey(), property.getValue());
        prefsEditor.apply();
        for (Map.Entry<String, String> property : changedProperties.entrySet())
        {
            GBDeviceEventConfigurationRead configReadEvent = new GBDeviceEventConfigurationRead();
            configReadEvent.config = property.getKey();
            configReadEvent.event = GBDeviceEventConfigurationRead.Event.SUCCESS;
            evaluateGBDeviceEvent(configReadEvent);
        }
    }

    @Override
    public void onTestNewFunction() {
        try {
            new QuerySettingsOperation(this).perform();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        try {
            TransactionBuilder builder = performInitialized("onSendWeather");

            DaFitWeatherToday weatherToday = new DaFitWeatherToday(weatherSpec);
            ByteBuffer packetWeatherToday = ByteBuffer.allocate(weatherToday.pm25 != null ? 21 : 19);
            packetWeatherToday.put(weatherToday.pm25 != null ? (byte)1 : (byte)0);
            packetWeatherToday.put(weatherToday.conditionId);
            packetWeatherToday.put(weatherToday.currentTemp);
            if (weatherToday.pm25 != null)
                packetWeatherToday.putShort(weatherToday.pm25);
            packetWeatherToday.put(weatherToday.lunar_or_festival.getBytes("unicodebigunmarked"));
            packetWeatherToday.put(weatherToday.city.getBytes("unicodebigunmarked"));
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SET_WEATHER_TODAY, packetWeatherToday.array()));

            ByteBuffer packetWeatherForecast = ByteBuffer.allocate(7 * 3);
            for(int i = 0; i < 7; i++)
            {
                DaFitWeatherForecast forecast;
                if (weatherSpec.forecasts.size() > i)
                    forecast = new DaFitWeatherForecast(weatherSpec.forecasts.get(i));
                else
                    forecast = new DaFitWeatherForecast(DaFitConstants.WEATHER_HAZE, (byte)-100, (byte)-100); // I don't think there is a way to send less (my watch shows only tomorrow anyway...)
                packetWeatherForecast.put(forecast.conditionId);
                packetWeatherForecast.put(forecast.minTemp);
                packetWeatherForecast.put(forecast.maxTemp);
            }
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SET_WEATHER_FUTURE, packetWeatherForecast.array()));

            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetFmFrequency(float frequency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSetLedColor(int color) {
        throw new UnsupportedOperationException();
    }
}
