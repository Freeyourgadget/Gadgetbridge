/*  Copyright (C) 2023 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3ActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3BehaviorSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3CaloriesSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3EnergySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3HeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3SettingKeys;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3StressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3Vo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.database.AppSpecificNotificationSettingsRepository;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3CaloriesSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3EnergySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSetting;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3StressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3Vo2Sample;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.ActivitySyncPacketProcessor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.BehaviorPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.CaloriesPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.EnergyPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.HeartRatePacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.StepsPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.StressPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers.Vo2MaxPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncDataPacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncStartPacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncTimePacketTypeA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncTimePacketTypeB;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.calendar.CalendarEntry;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.NotificationArrival;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.NotificationRemoval;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.LedColor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.NotificationFlags;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.NotificationKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationOptions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.AlarmListSettings;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.AutoPowerOffSettings;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.BodyPropertiesSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.CalendarNotificationEnableSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.CameraAppTypeSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.DayStartHourSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.DeviceButtonActionSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.DisplaySetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.DoNotDisturbSettings;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.GoalStepsSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.HomeIconOrderSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.MenuIconSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.SingleAlarmSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.StatusPageOrderSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.TimeSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.TimeZoneSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.VibrationSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.DeviceButtonActionId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.DisplayDesign;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.DisplayOrientation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.FontSize;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.GenderSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.HomeIconId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.Language;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.MenuIconId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.StatusPageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.VibrationStrength;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.BatteryLevelInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.MusicInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.NotificationServiceStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.StatusRequestType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.WeatherDay;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status.WeatherReport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SonyWena3DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final int INCOMING_CALL_ID = 3939;
    private static final Logger LOG = LoggerFactory.getLogger(SonyWena3DeviceSupport.class);
    private String lastMusicInfo = null;
    private MusicStateSpec lastMusicState = null;
    private final List<CalendarEventSpec> calendarEvents = new ArrayList<>();
    private final ActivitySyncPacketProcessor activitySyncHandler = new ActivitySyncPacketProcessor();
    private AppSpecificNotificationSettingsRepository perAppNotificationSettingsRepository = null;

    public SonyWena3DeviceSupport() {
        super(LoggerFactory.getLogger(SonyWena3DeviceSupport.class));
        addSupportedService(SonyWena3Constants.COMMON_SERVICE_UUID);
        addSupportedService(SonyWena3Constants.NOTIFICATION_SERVICE_UUID);
        addSupportedService(SonyWena3Constants.ACTIVITY_LOG_SERVICE_UUID);

        activitySyncHandler.registerParser(new StepsPacketParser());
        activitySyncHandler.registerParser(new HeartRatePacketParser());
        activitySyncHandler.registerParser(new StressPacketParser());
        activitySyncHandler.registerParser(new EnergyPacketParser());
        activitySyncHandler.registerParser(new BehaviorPacketParser());
        activitySyncHandler.registerParser(new CaloriesPacketParser());
        activitySyncHandler.registerParser(new Vo2MaxPacketParser());
    }
    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        if(perAppNotificationSettingsRepository == null) {
            perAppNotificationSettingsRepository = new AppSpecificNotificationSettingsRepository(getDevice());
        }
        getDevice().setFirmwareVersion("...");
        getDevice().setFirmwareVersion2("...");

        sendAllSettings(builder);
        sendAllCalendarEvents(builder);

        // Get battery state
        builder.read(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_STATE_UUID));

        // Subscribe to updates
        builder.notify(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_STATE_UUID), true);
        builder.notify(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID), true);
        builder.notify(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_INFO_UUID), true);
        builder.notify(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_MODE_UUID), true);
        builder.notify(getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID), true);
        builder.notify(getCharacteristic(SonyWena3Constants.ACTIVITY_LOG_CHARACTERISTIC_UUID), true);

        // Get serial number and firmware version
        builder.read(getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_INFO_UUID));

        // Finally, sync activity data
        requestActivityDataDownload(builder, false);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        CalendarReceiver.forceSync();
        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_STATE_UUID)) {
            BatteryLevelInfo stateInfo = new BatteryLevelInfo(characteristic.getValue());
            handleGBDeviceEvent(stateInfo.toDeviceEvent());
            return true;
        }
        else if (characteristic.getUuid().equals(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID)) {
            NotificationServiceStatusRequest request = new NotificationServiceStatusRequest(characteristic.getValue());
            if(request.requestType == StatusRequestType.MUSIC_INFO_FETCH.value) {
                LOG.debug("Request for music info received");
                if(lastMusicState != null && lastMusicState.state == MusicStateSpec.STATE_PLAYING && lastMusicInfo != null) {
                    sendMusicInfo(lastMusicInfo);
                }
                return true;
            }
            else if(request.requestType == StatusRequestType.LOCATE_PHONE.value) {
                LOG.debug("Request for find phone received");
                GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                evaluateGBDeviceEvent(findPhoneEvent);
                return true;
            }
            else if(request.requestType == StatusRequestType.BACKGROUND_SYNC_REQUEST.value) {
                Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
                boolean enableSync = prefs.getBoolean(SonyWena3SettingKeys.BACKGROUND_SYNC, true);
                if(enableSync) {
                    LOG.info("Request for background activity sync received");
                    requestActivityDataDownload(null, false);
                }
                return true;
            }
            else if(request.requestType == StatusRequestType.GET_CALENDAR.value) {
                CalendarReceiver.forceSync();
                sendAllCalendarEvents(null);
            }
            else {
                LOG.warn("Unknown NotificationServiceStatusRequest " + request.requestType);
            }
        } else if(characteristic.getUuid().equals(SonyWena3Constants.ACTIVITY_LOG_CHARACTERISTIC_UUID)) {
            ActivitySyncDataPacket asdp = new ActivitySyncDataPacket(characteristic.getValue());
            activitySyncHandler.receivePacket(asdp, getDevice());
        }
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if(characteristic.getUuid().equals(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_STATE_UUID)) {
            BatteryLevelInfo stateInfo = new BatteryLevelInfo(characteristic.getValue());
            handleGBDeviceEvent(stateInfo.toDeviceEvent());
            return true;
        } else if(characteristic.getUuid().equals(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_INFO_UUID)) {
            DeviceInfo deviceInfo = new DeviceInfo(characteristic.getValue());
            handleGBDeviceEvent(deviceInfo.toDeviceEvent());
            return true;
        }
        return false;
    }

    @Override
    public boolean onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        return super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
    }


    private void sendCurrentTime(@Nullable TransactionBuilder b) {
        try {
            TransactionBuilder builder = b == null ? performInitialized("updateDateTime") : b;

            TimeZone tz = TimeZone.getDefault();
            Date currentTime = new Date();

            builder.write(
                    getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                    new TimeSetting(currentTime).toByteArray()
            );

            builder.write(
                    getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                    new TimeZoneSetting(tz, currentTime).toByteArray()
            );

            if(b == null) performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send current time", e);
        }
    }

    private void sendMusicInfo(@Nullable String musicInfo) {
        try {
            TransactionBuilder builder = performInitialized("updateMusic");

            builder.write(
                    getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                    new MusicInfo(musicInfo != null ? musicInfo: "").toByteArray()
            );

            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send music info", e);
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        requestActivityDataDownload(null, false);
    }

    private void requestActivityDataDownload(@Nullable TransactionBuilder b, boolean syncAll) {
        try {
            TransactionBuilder builder = b == null ? performInitialized("activitySync") : b;
            BluetoothGattCharacteristic sportsCharacteristic = getCharacteristic(SonyWena3Constants.ACTIVITY_LOG_CHARACTERISTIC_UUID);

            Date stepLastSyncTime = null;
            Date heartLastSyncTime = null;
            Date behaviorLastSyncTime = null;
            Date vo2LastSyncTime = null;
            Date stressLastSyncTime = null;
            Date energyLastSyncTime = null;
            Date caloriesLastSyncTime = null;
            Date eventsLastSyncTime = null; // TODO: find out what this is

            if(!syncAll) {
                try (DBHandler db = GBApplication.acquireDB()) {
                    Wena3HeartRateSample heartSample = new SonyWena3HeartRateSampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(heartSample != null) {
                        heartLastSyncTime = new Date(heartSample.getTimestamp());
                    }

                    Wena3StressSample stressSample = new SonyWena3StressSampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(stressSample != null) {
                        stressLastSyncTime = new Date(stressSample.getTimestamp());
                    }

                    Wena3ActivitySample stepsSample = new SonyWena3ActivitySampleProvider(getDevice(), db.getDaoSession()).getLatestActivitySample();
                    if(stepsSample != null) {
                        stepLastSyncTime = new Date(stepsSample.getTimestamp() * 1000L);
                    }

                    Wena3BehaviorSample behaviorSample = new SonyWena3BehaviorSampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(behaviorSample != null) {
                        behaviorLastSyncTime = new Date(behaviorSample.getTimestamp());
                    }

                    Wena3Vo2Sample vo2Sample = new SonyWena3Vo2SampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(vo2Sample != null) {
                        vo2LastSyncTime = new Date(vo2Sample.getTimestamp());
                    }

                    Wena3EnergySample energySample = new SonyWena3EnergySampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(energySample != null) {
                        energyLastSyncTime = new Date(energySample.getTimestamp());
                    }

                    Wena3CaloriesSample caloriesSample = new SonyWena3CaloriesSampleProvider(getDevice(), db.getDaoSession()).getLatestSample();
                    if(caloriesSample != null) {
                        caloriesLastSyncTime = new Date(caloriesSample.getTimestamp());
                    }

                    LOG.info(
                            "Found last sync dates: " +
                            "\nsteps = " + stepLastSyncTime +
                            "\nheart = " + heartLastSyncTime +
                            "\nbehavior = " + behaviorLastSyncTime +
                            "\nvo2 = " + vo2LastSyncTime +
                            "\nstress = " + stressLastSyncTime +
                            "\nenergy = " + energyLastSyncTime +
                            "\ncalories = " + caloriesLastSyncTime +
                            "\nevents = " + eventsLastSyncTime
                    );
                } catch (Exception e) {
                    LOG.warn("Failed to communicate with DB to find last sync timestamps -- syncing everything:", e);
                }
            }

            builder.write(
                    sportsCharacteristic,
                    new ActivitySyncTimePacketTypeA(stepLastSyncTime, heartLastSyncTime, behaviorLastSyncTime, vo2LastSyncTime).toByteArray()
            );

            builder.write(
                    sportsCharacteristic,
                    new ActivitySyncTimePacketTypeB(stressLastSyncTime, energyLastSyncTime, caloriesLastSyncTime, eventsLastSyncTime).toByteArray()
            );

            builder.write(sportsCharacteristic, new ActivitySyncStartPacket().toByteArray());

            if(b == null) performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to force request a sync", e);
        }
    }

    private void sendWeatherInfo(WeatherReport weather, @Nullable TransactionBuilder b) {
        try {
            TransactionBuilder builder = b == null ? performInitialized("updateWeather") : b;

            builder.write(
                    getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                    weather.toByteArray()
            );

            if(b == null) performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send current weather", e);
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        StringBuilder sb = new StringBuilder();
        boolean hasTrackName = musicSpec.track != null && musicSpec.track.trim().length() > 0;
        boolean hasArtistName = musicSpec.artist != null && musicSpec.artist.trim().length() > 0;

        if(hasTrackName) {
            sb.append(musicSpec.track.trim());
        }
        if(hasArtistName && hasTrackName) {
            sb.append(" / ");
        }
        if(hasArtistName) {
            sb.append(musicSpec.artist.trim());
        }

        lastMusicInfo = sb.toString();
        sendMusicInfo(lastMusicInfo);
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if(stateSpec.state == MusicStateSpec.STATE_PLAYING && lastMusicInfo != null) {
            sendMusicInfo(lastMusicInfo);
        } else if (stateSpec.state == MusicStateSpec.STATE_STOPPED || stateSpec.state == MusicStateSpec.STATE_PAUSED) {
            sendMusicInfo("");
        }
        lastMusicState = stateSpec;
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        boolean enableNotifications = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        boolean enableCalls = prefs.getBoolean(SonyWena3SettingKeys.RECEIVE_CALLS, true);
        if(!enableCalls || !enableNotifications) {
            LOG.info("Calls are disabled, ignoring");
            return;
        }

        try {
            TransactionBuilder builder = performInitialized("sendCall");

            if(callSpec.command == CallSpec.CALL_INCOMING) {
                LedColor led = LedColor.valueOf(prefs.getString(SonyWena3SettingKeys.DEFAULT_CALL_LED_COLOR, LedColor.WHITE.name()).toUpperCase());
                VibrationKind vibra = VibrationKind.valueOf(prefs.getString(SonyWena3SettingKeys.DEFAULT_CALL_VIBRATION_PATTERN, VibrationKind.CONTINUOUS.name()).toUpperCase());
                boolean vibraContinuous = false;
                int vibraRepeats = prefs.getInt(SonyWena3SettingKeys.DEFAULT_CALL_VIBRATION_REPETITION, 0);
                if(vibraRepeats == 0) {
                    vibraContinuous = true;
                }

                builder.write(
                        getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                        new NotificationArrival(
                                NotificationKind.CALL,
                                INCOMING_CALL_ID,
                                callSpec.number,
                                callSpec.name,
                                "",
                                new Date(),
                                new VibrationOptions(vibra, vibraRepeats, vibraContinuous),
                                led,
                                NotificationFlags.NONE
                        ).toByteArray()
                );
            } else {
                builder.write(
                        getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                        new NotificationRemoval(NotificationKind.CALL, INCOMING_CALL_ID).toByteArray()
                );
            }

            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send call", e);
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        boolean enableNotifications = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        if(!enableNotifications) return;

        try {
            TransactionBuilder builder = performInitialized("sendNotify");

            StringBuilder bodyBuilder = new StringBuilder();

            if(notificationSpec.sender != null && notificationSpec.sender.length() > 0) {
                bodyBuilder.append(notificationSpec.sender);
                bodyBuilder.append(":");
            }

            if(notificationSpec.title != null && notificationSpec.title.length() > 0) {
                if(bodyBuilder.length() > 0) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append(notificationSpec.title);
                bodyBuilder.append(":");
            }

            if(notificationSpec.subject != null && notificationSpec.subject.length() > 0) {
                if(bodyBuilder.length() > 0) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append("- ");
                bodyBuilder.append(notificationSpec.subject);
            }

            if(notificationSpec.body != null) {
                if(bodyBuilder.length() > 0) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append(notificationSpec.body);
            }

            String actionLabel = notificationSpec.attachedActions.isEmpty() ? "" :
                    notificationSpec.attachedActions.get(0).title;

            boolean hasAction = !notificationSpec.attachedActions.isEmpty();

            NotificationFlags flags = NotificationFlags.NONE;
            // TODO: Figure out how actions work

            LedColor led = LedColor.valueOf(prefs.getString(SonyWena3SettingKeys.DEFAULT_LED_COLOR, LedColor.BLUE.name()).toUpperCase());
            VibrationKind vibra = VibrationKind.valueOf(prefs.getString(SonyWena3SettingKeys.DEFAULT_VIBRATION_PATTERN, VibrationKind.BASIC.name()).toUpperCase());
            boolean vibraContinuous = false;
            int vibraRepeats = prefs.getInt(SonyWena3SettingKeys.DEFAULT_VIBRATION_REPETITION, 1);

            if(notificationSpec.sourceAppId != null) {
                AppSpecificNotificationSetting appSpecificSetting = perAppNotificationSettingsRepository.getSettingsForAppId(notificationSpec.sourceAppId);
                if(appSpecificSetting != null) {
                    if(appSpecificSetting.getLedPattern() != null) {
                        led = LedColor.valueOf(appSpecificSetting.getLedPattern().toUpperCase());
                    }

                    if(appSpecificSetting.getVibrationPattern() != null) {
                        vibra = VibrationKind.valueOf(appSpecificSetting.getVibrationPattern().toUpperCase());
                    }

                    if(appSpecificSetting.getVibrationRepetition() != null) {
                        vibraRepeats = Integer.valueOf(appSpecificSetting.getVibrationRepetition());
                    }
                }
            }

            if(vibraRepeats == 0) {
                vibraContinuous = true;
            }

            builder.write(
                    getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                    new NotificationArrival(
                            NotificationKind.APP,
                            notificationSpec.getId(),
                            notificationSpec.sourceName,
                            bodyBuilder.toString(),
                            actionLabel,
                            new Date(notificationSpec.when),
                            new VibrationOptions(vibra, vibraRepeats, vibraContinuous),
                            led,
                            flags
                    ).toByteArray()
            );

            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send notification", e);
        }
    }

    @Override
    public void onDeleteNotification(int id) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        boolean enableNotifications = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        if(!enableNotifications) return;

        try {
            TransactionBuilder builder = performInitialized("delNotify");

            builder.write(
                    getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                    new NotificationRemoval(NotificationKind.APP, id).toByteArray()
            );

            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send notification", e);
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        if(weatherSpec.forecasts.size() < 4) return;

        ArrayList<WeatherDay> days = new ArrayList<>();
        // Add today
        days.add(
                new WeatherDay(
                        Weather.fromOpenWeatherMap(weatherSpec.currentConditionCode),
                        Weather.fromOpenWeatherMap(weatherSpec.currentConditionCode),
                        weatherSpec.todayMaxTemp,
                        weatherSpec.todayMinTemp
                )
        );

        // Add other days
        for(int i = 0; i < 4; i++) {
            days.add(WeatherDay.fromSpec(weatherSpec.forecasts.get(i)));
        }

        WeatherReport report = new WeatherReport(days);
        sendWeatherInfo(report, null);
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
            TransactionBuilder builder = performInitialized("alarmSetting");

            assert alarms.size() <= SonyWena3Constants.ALARM_SLOTS;

            int wakeupMargin = prefs.getInt(SonyWena3SettingKeys.SMART_WAKEUP_MARGIN_MINUTES,
                    SonyWena3Constants.ALARM_DEFAULT_SMART_WAKEUP_MARGIN_MINUTES);

            for(
                int i = 0;
                i < SonyWena3Constants.ALARM_SLOTS;
                i += AlarmListSettings.MAX_ALARMS_IN_PACKET
            ) {
                AlarmListSettings pkt = new AlarmListSettings(new ArrayList<>(), i);

                for(int j = 0; j < AlarmListSettings.MAX_ALARMS_IN_PACKET; j++) {
                    if(i + j < alarms.size()) {
                        Alarm alarm = alarms.get(i + j);
                        SingleAlarmSetting sas = new SingleAlarmSetting(
                                alarm.getEnabled(),
                                (byte) alarm.getRepetition(),
                                alarm.getSmartWakeup() ? wakeupMargin : 0,
                                alarm.getHour(),
                                alarm.getMinute()
                        );
                        pkt.alarms.add(sas);
                    }
                }

                builder.write(
                        getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                        pkt.toByteArray()
                );
            }

            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send alarms", e);
            GB.toast("Failed to save alarms", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private void sendDisplaySettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));

        String localeString = prefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO);
        if (localeString == null || localeString.equals(DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO)) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (country == null) {
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }
        LOG.info("Setting device to locale: " + localeString);

    Language languageCode = Language.ENGLISH;

        switch (localeString.substring(0, 2)) {
            case "en":
                languageCode = Language.ENGLISH;
                break;
            case "ja":
                languageCode = Language.JAPANESE;
                break;
        }
        LOG.info("Resolved locale: " + languageCode.name());

        DisplaySetting pkt = new DisplaySetting(
                prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SCREEN_LIFT_WRIST, false),
                languageCode,
                prefs.getInt(DeviceSettingsPreferenceConst.PREF_SCREEN_TIMEOUT, 5),
                (prefs.getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION, "left")
                        .equals("left") ? DisplayOrientation.LEFT_HAND : DisplayOrientation.RIGHT_HAND),
                (prefs.getBoolean(SonyWena3SettingKeys.RICH_DESIGN_MODE, false) ? DisplayDesign.RICH : DisplayDesign.NORMAL),
                (prefs.getBoolean(SonyWena3SettingKeys.LARGE_FONT_SIZE, false) ? FontSize.LARGE : FontSize.NORMAL),
                prefs.getBoolean(SonyWena3SettingKeys.WEATHER_IN_STATUSBAR, true)
        );

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                pkt.toByteArray()
        );
    }

    private void sendDnDSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String dndMode = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF);
        boolean isDndOn = (dndMode != null && dndMode.equals(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED));
        String start = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, "22:00");
        String end = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        byte startH = (byte)startCalendar.get(Calendar.HOUR_OF_DAY);
        byte startM = (byte)startCalendar.get(Calendar.MINUTE);
        byte endH = (byte)endCalendar.get(Calendar.HOUR_OF_DAY);
        byte endM = (byte)endCalendar.get(Calendar.MINUTE);

        DoNotDisturbSettings dndPkt = new DoNotDisturbSettings(isDndOn, startH, startM, endH, endM);
        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                dndPkt.toByteArray()
        );
    }

    private void sendAutoPowerSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String autoPowerMode = prefs.getString(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_KIND, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF);
        boolean isAutoPowerOffEnabled = (autoPowerMode != null && autoPowerMode.equals(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED));
        String start = prefs.getString(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_START_HHMM, "22:00");
        String end = prefs.getString(SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_END_HHMM, "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        byte startH = (byte)startCalendar.get(Calendar.HOUR_OF_DAY);
        byte startM = (byte)startCalendar.get(Calendar.MINUTE);
        byte endH = (byte)endCalendar.get(Calendar.HOUR_OF_DAY);
        byte endM = (byte)endCalendar.get(Calendar.MINUTE);

        AutoPowerOffSettings powerOffPkt = new AutoPowerOffSettings(isAutoPowerOffEnabled, startH, startM, endH, endM);
        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                powerOffPkt.toByteArray()
        );
    }

    private void sendVibrationSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        boolean smartVibration = prefs.getBoolean(SonyWena3SettingKeys.SMART_VIBRATION, true);
        VibrationStrength strength = VibrationStrength.valueOf(prefs.getString(SonyWena3SettingKeys.VIBRATION_STRENGTH, VibrationStrength.NORMAL.name()).toUpperCase());
        VibrationSetting pkt = new VibrationSetting(smartVibration, strength);

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                pkt.toByteArray()
        );
    }

    private void sendHomeScreenSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String leftIdName = prefs.getString(SonyWena3SettingKeys.LEFT_HOME_ICON, HomeIconId.MUSIC.name()).toUpperCase();
        String centerIdName = prefs.getString(SonyWena3SettingKeys.CENTER_HOME_ICON, HomeIconId.PEDOMETER.name()).toUpperCase();
        String rightIdName = prefs.getString(SonyWena3SettingKeys.RIGHT_HOME_ICON, HomeIconId.CALORIES.name()).toUpperCase();

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                new HomeIconOrderSetting(
                        HomeIconId.valueOf(leftIdName),
                        HomeIconId.valueOf(centerIdName),
                        HomeIconId.valueOf(rightIdName)
                ).toByteArray()
        );
    }

    private void sendMenuSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String[] csv = prefs.getString(SonyWena3SettingKeys.MENU_ICON_CSV_KEY,
                        TextUtils.join(",", getContext().getResources().getStringArray(R.array.prefs_wena3_menu_icons_default_list)))
                .toUpperCase()
                .split(",");

        MenuIconSetting menu = new MenuIconSetting();

        for(String iconIdName: csv) {
            if(!iconIdName.equals(MenuIconId.NONE.name())) {
                menu.iconList.add(MenuIconId.valueOf(iconIdName));
            }
        }

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                menu.toByteArray()
        );
    }

    private void sendStatusPageSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        StatusPageOrderSetting pageOrderSetting = new StatusPageOrderSetting();
        String[] csv = prefs.getString(SonyWena3SettingKeys.STATUS_PAGE_CSV_KEY,
                        TextUtils.join(",", getContext().getResources().getStringArray(R.array.prefs_wena3_status_page_default_list)))
                .toUpperCase()
                .split(",");
        for(String idName: csv) {
            if(!idName.equals(StatusPageId.NONE.name())) {
                pageOrderSetting.pages.add(StatusPageId.valueOf(idName));
            }
        }

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                pageOrderSetting.toByteArray()
        );
    }

    private void sendActivityGoalSettings(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        ActivityUser user = new ActivityUser();
        if(user.getYearOfBirth() < 1920) {
            LOG.error("Device does not support this year of birth");
            return;
        }
        boolean stepsNotification = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION, false);

        GenderSetting gender = user.getGender() == ActivityUser.GENDER_FEMALE ? GenderSetting.FEMALE : GenderSetting.MALE;

        // Maybe we need to set the full birth date?
        BodyPropertiesSetting bodyPropertiesSetting = new BodyPropertiesSetting(gender, (short)user.getYearOfBirth(), (short)0, (short)1, (short)user.getHeightCm(), (short)user.getWeightKg());
        GoalStepsSetting stepsSetting = new GoalStepsSetting(stepsNotification, user.getStepsGoal());

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                bodyPropertiesSetting.toByteArray()
        );
        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                stepsSetting.toByteArray()
        );
    }

    private void sendDayStartHour(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        int hour = prefs.getInt(SonyWena3SettingKeys.DAY_START_HOUR, 6);
        DayStartHourSetting setting = new DayStartHourSetting(hour);

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                setting.toByteArray()
        );
    }

    private void sendButtonActions(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String doubleIdName = prefs.getString(SonyWena3SettingKeys.BUTTON_DOUBLE_PRESS_ACTION, DeviceButtonActionId.NONE.name()).toUpperCase();
        String longIdName = prefs.getString(SonyWena3SettingKeys.BUTTON_LONG_PRESS_ACTION, DeviceButtonActionId.NONE.name()).toUpperCase();
        DeviceButtonActionSetting setting = new DeviceButtonActionSetting(
                DeviceButtonActionId.valueOf(longIdName),
                DeviceButtonActionId.valueOf(doubleIdName)
        );

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                setting.toByteArray()
        );
    }

    private void sendCalendarNotificationToggles(TransactionBuilder b) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        boolean enableCalendar = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false);
        boolean enableNotifications = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        CalendarNotificationEnableSetting setting = new CalendarNotificationEnableSetting(enableCalendar, enableNotifications);

        b.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                setting.toByteArray()
        );
    }

    private void sendAllSettings(TransactionBuilder builder) {
        sendCurrentTime(builder);
        builder.write(
                getCharacteristic(SonyWena3Constants.COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID),
                CameraAppTypeSetting.findOut(getContext().getPackageManager()).toByteArray()
        );
        sendMenuSettings(builder);
        sendStatusPageSettings(builder);
        sendDisplaySettings(builder);
        sendDnDSettings(builder);
        sendAutoPowerSettings(builder);
        sendVibrationSettings(builder);
        sendHomeScreenSettings(builder);
        sendActivityGoalSettings(builder);
        sendDayStartHour(builder);
        sendButtonActions(builder);
        sendCalendarNotificationToggles(builder);
    }

    private void sendAllCalendarEvents(TransactionBuilder b) {
        try {
            Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
            boolean enableCalendar = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false);

            TransactionBuilder builder = b == null ? performInitialized("updateCalendarEvents") : b;

            if(!enableCalendar || calendarEvents.isEmpty()) {
                builder.write(
                        getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                        CalendarEntry.byteArrayForEmptyEvent((byte) 0, (byte) 0)
                );
            }
            else {
                int i = 1;
                int total = Math.min(calendarEvents.size(), 255);
                for(CalendarEventSpec evt: calendarEvents) {
                    builder.write(
                            getCharacteristic(SonyWena3Constants.NOTIFICATION_SERVICE_CHARACTERISTIC_UUID),
                            new CalendarEntry(
                                    new Date(evt.timestamp * 1000L),
                                    new Date((evt.timestamp * 1000L) + (evt.durationInSeconds * 1000L)),
                                    evt.allDay,
                                    (evt.title == null ? "" : evt.title),
                                    (evt.location == null ? "" : evt.location),
                                    (byte) i,
                                    (byte) total
                            ).toByteArray()
                    );
                    if(i == 255) break;
                    else i++;
                }
            }

            if(b == null) performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to send calendar events", e);
        }
    }

    @Override
    public void onSetTime() {
        sendCurrentTime(null);
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        calendarEvents.add(calendarEventSpec);
        sendAllCalendarEvents(null);
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        for(CalendarEventSpec evt : calendarEvents) {
            if(evt.type == type && evt.id == id) {
                calendarEvents.remove(evt);
            }
        }
        sendAllCalendarEvents(null);
    }

    @Override
    public void onSendConfiguration(String config) {
        try {
            TransactionBuilder builder = performInitialized("sendConfig");
            switch (config) {
                case SonyWena3SettingKeys.STATUS_PAGE_CSV_KEY:
                    sendStatusPageSettings(builder);
                    break;

                case SonyWena3SettingKeys.MENU_ICON_CSV_KEY:
                    sendMenuSettings(builder);
                    break;

                case DeviceSettingsPreferenceConst.PREF_SCREEN_LIFT_WRIST:
                case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                case DeviceSettingsPreferenceConst.PREF_SCREEN_TIMEOUT:
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                case SonyWena3SettingKeys.RICH_DESIGN_MODE:
                case SonyWena3SettingKeys.LARGE_FONT_SIZE:
                case SonyWena3SettingKeys.WEATHER_IN_STATUSBAR:
                    sendDisplaySettings(builder);
                    break;

                case SonyWena3SettingKeys.SMART_WAKEUP_MARGIN_MINUTES:
                    // Resend alarms
                    onSetAlarms(new ArrayList<>(DBHelper.getAlarms(gbDevice)));
                    break;

                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START:
                    sendDnDSettings(builder);
                    break;

                case SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_KIND:
                case SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_START_HHMM:
                case SonyWena3SettingKeys.AUTO_POWER_SCHEDULE_END_HHMM:
                    sendAutoPowerSettings(builder);
                    break;

                case SonyWena3SettingKeys.VIBRATION_STRENGTH:
                case SonyWena3SettingKeys.SMART_VIBRATION:
                    sendVibrationSettings(builder);
                    break;

                case SonyWena3SettingKeys.LEFT_HOME_ICON:
                case SonyWena3SettingKeys.CENTER_HOME_ICON:
                case SonyWena3SettingKeys.RIGHT_HOME_ICON:
                    sendHomeScreenSettings(builder);
                    break;

                case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
                case ActivityUser.PREF_USER_GENDER:
                case ActivityUser.PREF_USER_HEIGHT_CM:
                case ActivityUser.PREF_USER_WEIGHT_KG:
                case ActivityUser.PREF_USER_STEPS_GOAL:
                case DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION:
                    sendActivityGoalSettings(builder);
                    break;

                case SonyWena3SettingKeys.DAY_START_HOUR:
                    sendDayStartHour(builder);
                    break;

                case SonyWena3SettingKeys.BUTTON_DOUBLE_PRESS_ACTION:
                case SonyWena3SettingKeys.BUTTON_LONG_PRESS_ACTION:
                    sendButtonActions(builder);
                    break;

                case DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE:
                case DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR:
                    sendCalendarNotificationToggles(builder);
                    sendAllCalendarEvents(builder);
                    break;

                default:
                    LOG.warn("Unsupported setting %s", config);
                    return;
            }

            performImmediately(builder);
        } catch(Exception e) {
            GB.toast("Failed to send settings update", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }
}
