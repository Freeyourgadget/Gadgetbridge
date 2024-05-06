package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminAgpsInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSettingsService;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps.GarminAgpsStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1.CommunicatorV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.NotificationSubscriptionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.SupportedFileTypesDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.WeatherRequestDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitAsyncProcessor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.PredefinedLocalMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ConfigurationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MusicControlEntityUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetDeviceSettingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetFileFlagsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SupportedFileTypesMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationSubscriptionStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Optional;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GARMIN_DEFAULT_REPLY_SUFFIX;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS;


public class GarminSupport extends AbstractBTLEDeviceSupport implements ICommunicator.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSupport.class);
    private final ProtocolBufferHandler protocolBufferHandler;
    private final NotificationsHandler notificationsHandler;
    private final FileTransferHandler fileTransferHandler;
    private final Queue<FileTransferHandler.DirectoryEntry> filesToDownload;
    private final List<MessageHandler> messageHandlers;
    private ICommunicator communicator;
    private MusicStateSpec musicStateSpec;
    private Timer musicStateTimer;

    private final List<FileType> supportedFileTypeList = new ArrayList<>();
    private final List<File> filesToProcess = new ArrayList<>();

    public GarminSupport() {
        super(LOG);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI);
        addSupportedService(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI);
        protocolBufferHandler = new ProtocolBufferHandler(this);
        fileTransferHandler = new FileTransferHandler(this);
        filesToDownload = new LinkedList<>();
        messageHandlers = new ArrayList<>();
        notificationsHandler = new NotificationsHandler();
        messageHandlers.add(fileTransferHandler);
        messageHandlers.add(protocolBufferHandler);
        messageHandlers.add(notificationsHandler);
    }

    @Override
    public void dispose() {
        LOG.info("Garmin dispose()");
        GBLocationService.stop(getContext(), getDevice());
        stopMusicTimer();
        super.dispose();
    }

    private void stopMusicTimer() {
        if (musicStateTimer != null) {
            musicStateTimer.cancel();
            musicStateTimer.purge();
            musicStateTimer = null;
        }
    }

    public void addFileToDownloadList(FileTransferHandler.DirectoryEntry directoryEntry) {
        filesToDownload.add(directoryEntry);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        if (getSupportedServices().contains(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI)) {
            communicator = new CommunicatorV2(this);
        } else if (getSupportedServices().contains(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI)) {
            communicator = new CommunicatorV1(this);
        } else {
            LOG.warn("Failed to find a known Garmin service");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.NOT_CONNECTED, getContext()));
            return builder;
        }

        if (getDevicePrefs().getBoolean(PREF_ALLOW_HIGH_MTU, true)) {
            builder.requestMtu(515);
        }

        communicator.initializeDevice(builder);

        return builder;
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        if (mtu < 23) {
            LOG.warn("Ignoring mtu of {}, too low", mtu);
            return;
        }
        if (!getDevicePrefs().getBoolean(PREF_ALLOW_HIGH_MTU, true)) {
            LOG.warn("Ignoring mtu change to {} - high mtu is disabled", mtu);
            return;
        }

        communicator.onMtuChanged(mtu);
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            LOG.debug("Change of characteristic {} handled by parent", characteristicUUID);
            return true;
        }

        return communicator.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onMessage(final byte[] message) {
        if (null == message) {
            return; //message is not complete yet TODO check before calling
        }
//        LOG.debug("COBS decoded MESSAGE: {}", GB.hexdump(message));

        GFDIMessage parsedMessage = GFDIMessage.parseIncoming(message);

        if (null == parsedMessage) {
            return; //message cannot be handled
        }


        /*
        the handler elaborates the followup message but might change the status message since it does
        check the integrity of the incoming message payload. Hence we let the handlers elaborate the
        incoming message, then we send the status message of the incoming message, then the response
        and finally we send the followup.
         */

        GFDIMessage followup = null;
        for (MessageHandler han : messageHandlers) {
            followup = han.handle(parsedMessage);
            if (followup != null) {
                break;
            }
        }

        final List<GBDeviceEvent> events = parsedMessage.getGBDeviceEvent();
        for (final GBDeviceEvent event : events) {
            evaluateGBDeviceEvent(event);
        }

        communicator.sendMessage(parsedMessage.getAckBytestream()); //send status message

        sendOutgoingMessage(parsedMessage); //send reply if any

        sendOutgoingMessage(followup); //send followup message if any

        if (parsedMessage instanceof ConfigurationMessage) { //the last forced message exchange
            completeInitialization();
        }

        processDownloadQueue();

    }


    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.info("INCOMING CALLSPEC: {}", callSpec.command);
        sendOutgoingMessage(notificationsHandler.onSetCallState(callSpec));
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof WeatherRequestDeviceEvent) {
            WeatherSpec weather = Weather.getInstance().getWeatherSpec();
            if (weather != null) {
                sendWeatherConditions(weather);
            }
        } else if (deviceEvent instanceof NotificationSubscriptionDeviceEvent) {
            final boolean enable = ((NotificationSubscriptionDeviceEvent) deviceEvent).enable;
            notificationsHandler.setEnabled(enable);

            final NotificationSubscriptionStatusMessage.NotificationStatus finalStatus;
            if (getDevicePrefs().getBoolean(PREF_SEND_APP_NOTIFICATIONS, true)) {
                finalStatus = NotificationSubscriptionStatusMessage.NotificationStatus.ENABLED;
            } else {
                finalStatus = NotificationSubscriptionStatusMessage.NotificationStatus.DISABLED;
            }

            LOG.info("NOTIFICATIONS ARE NOW enabled={}, status={}", enable, finalStatus);

            sendOutgoingMessage(new NotificationSubscriptionStatusMessage(
                    GFDIMessage.Status.ACK,
                    finalStatus,
                    enable,
                    0
            ));
        } else if (deviceEvent instanceof SupportedFileTypesDeviceEvent) {
            this.supportedFileTypeList.clear();
            this.supportedFileTypeList.addAll(((SupportedFileTypesDeviceEvent) deviceEvent).getSupportedFileTypes());
        } else if (deviceEvent instanceof FileDownloadedDeviceEvent) {
            final FileTransferHandler.DirectoryEntry entry = ((FileDownloadedDeviceEvent) deviceEvent).directoryEntry;
            final String filename = entry.getFileName();
            LOG.debug("FILE DOWNLOAD COMPLETE {}", filename);

            if (entry.getFiletype().isFitFile()) {
                filesToProcess.add(new File(((FileDownloadedDeviceEvent) deviceEvent).localPath));
            }

            if (!getKeepActivityDataOnDevice()) { // delete file from watch upon successful download
                sendOutgoingMessage(new SetFileFlagsMessage(entry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
            }
        }

        super.evaluateGBDeviceEvent(deviceEvent);
    }

    private boolean getKeepActivityDataOnDevice() {
        return getDevicePrefs().getBoolean("keep_activity_data_on_device", false);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        if (this.supportedFileTypeList.isEmpty()) {
            LOG.warn("No known supported file types");
            return;
        }

        // FIXME respect dataTypes?

        sendOutgoingMessage(fileTransferHandler.initiateDownload());
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        sendOutgoingMessage(notificationsHandler.onNotification(notificationSpec));
    }

    @Override
    public void onDeleteNotification(int id) {
        sendOutgoingMessage(notificationsHandler.onDeleteNotification(id));
    }


    @Override
    public void onSendWeather(final ArrayList<WeatherSpec> weatherSpecs) { //todo: find the closest one relative to the requested lat/long
        sendWeatherConditions(weatherSpecs.get(0));
    }

    private void sendOutgoingMessage(GFDIMessage message) {
        if (message == null)
            return;
        communicator.sendMessage(message.getOutgoingMessage());
    }

    private boolean supports(final GarminCapability capability) {
        return getDevicePrefs().getStringSet(GarminPreferences.PREF_GARMIN_CAPABILITIES, Collections.emptySet())
                .contains(capability.name());
    }

    private void sendWeatherConditions(WeatherSpec weather) {
        if (!supports(GarminCapability.WEATHER_CONDITIONS)) {
            // Device does not support sending weather as fit
            return;
        }

        List<RecordData> weatherData = new ArrayList<>();

        final RecordDefinition recordDefinitionToday = PredefinedLocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition();
        final RecordDefinition recordDefinitionHourly = PredefinedLocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition();
        final RecordDefinition recordDefinitionDaily = PredefinedLocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition();

        List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        weatherDefinitions.add(recordDefinitionToday);
        weatherDefinitions.add(recordDefinitionHourly);
        weatherDefinitions.add(recordDefinitionDaily);

        sendOutgoingMessage(new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage(weatherDefinitions));

        try {
            RecordData today = new RecordData(recordDefinitionToday, recordDefinitionToday.getRecordHeader());
            today.setFieldByName("weather_report", 0); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
            today.setFieldByName("timestamp", weather.timestamp);
            today.setFieldByName("observed_at_time", weather.timestamp);
            today.setFieldByName("temperature", weather.currentTemp);
            today.setFieldByName("low_temperature", weather.todayMinTemp);
            today.setFieldByName("high_temperature", weather.todayMaxTemp);
            today.setFieldByName("condition", weather.currentConditionCode);
            today.setFieldByName("wind_direction", weather.windDirection);
            today.setFieldByName("precipitation_probability", weather.precipProbability);
            today.setFieldByName("wind_speed", Math.round(weather.windSpeed));
            today.setFieldByName("temperature_feels_like", weather.feelsLikeTemp);
            today.setFieldByName("relative_humidity", weather.currentHumidity);
            today.setFieldByName("observed_location_lat", weather.latitude);
            today.setFieldByName("observed_location_long", weather.longitude);
            today.setFieldByName("dew_point", weather.dewPoint);
            if (null != weather.airQuality) {
                today.setFieldByName("air_quality", weather.airQuality.aqi);
            }
            today.setFieldByName("location", weather.location);
            weatherData.add(today);

            for (int hour = 0; hour <= 11; hour++) {
                if (hour < weather.hourly.size()) {
                    WeatherSpec.Hourly hourly = weather.hourly.get(hour);
                    RecordData weatherHourlyForecast = new RecordData(recordDefinitionHourly, recordDefinitionHourly.getRecordHeader());
                    weatherHourlyForecast.setFieldByName("weather_report", 1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherHourlyForecast.setFieldByName("timestamp", hourly.timestamp);
                    weatherHourlyForecast.setFieldByName("temperature", hourly.temp);
                    weatherHourlyForecast.setFieldByName("condition", hourly.conditionCode);
                    weatherHourlyForecast.setFieldByName("temperature_feels_like", hourly.temp); //TODO: switch to actual feels like field once Hourly contains this information
                    weatherHourlyForecast.setFieldByName("wind_direction", hourly.windDirection);
                    weatherHourlyForecast.setFieldByName("wind_speed", Math.round(hourly.windSpeed));
                    weatherHourlyForecast.setFieldByName("precipitation_probability", hourly.precipProbability);
                    weatherHourlyForecast.setFieldByName("relative_humidity", hourly.humidity);
//                    weatherHourlyForecast.setFieldByName("dew_point", 0); // TODO: add once Hourly contains this information
                    weatherHourlyForecast.setFieldByName("uv_index", hourly.uvIndex);
//                    weatherHourlyForecast.setFieldByName("air_quality", 0); // TODO: add once Hourly contains this information
                    weatherData.add(weatherHourlyForecast);
                }
            }
//
            RecordData todayDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
            todayDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
            todayDailyForecast.setFieldByName("timestamp", weather.timestamp);
            todayDailyForecast.setFieldByName("low_temperature", weather.todayMinTemp);
            todayDailyForecast.setFieldByName("high_temperature", weather.todayMaxTemp);
            todayDailyForecast.setFieldByName("condition", weather.currentConditionCode);
            todayDailyForecast.setFieldByName("precipitation_probability", weather.precipProbability);
            todayDailyForecast.setFieldByName("day_of_week", weather.timestamp);
            if (null != weather.airQuality) {
                todayDailyForecast.setFieldByName("air_quality", weather.airQuality.aqi);
            }
            weatherData.add(todayDailyForecast);


            for (int day = 0; day < 4; day++) {
                if (day < weather.forecasts.size()) {
                    WeatherSpec.Daily daily = weather.forecasts.get(day);
                    int ts = weather.timestamp + (day + 1) * 24 * 60 * 60; //TODO: is this needed?
                    RecordData weatherDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
                    weatherDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherDailyForecast.setFieldByName("timestamp", weather.timestamp);
                    weatherDailyForecast.setFieldByName("low_temperature", daily.minTemp);
                    weatherDailyForecast.setFieldByName("high_temperature", daily.maxTemp);
                    weatherDailyForecast.setFieldByName("condition", daily.conditionCode);
                    weatherDailyForecast.setFieldByName("precipitation_probability", daily.precipProbability);
                    if (null != daily.airQuality) {
                        weatherDailyForecast.setFieldByName("air_quality", daily.airQuality.aqi);
                    }
                    weatherDailyForecast.setFieldByName("day_of_week", ts);
                    weatherData.add(weatherDailyForecast);
                }
            }

            sendOutgoingMessage(new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage(weatherData));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

    }

    private void completeInitialization() {
        onSetTime();
        enableWeather();

        //following is needed for vivomove style
        sendOutgoingMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_READY, 0));

        enableBatteryLevelUpdate();

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        sendOutgoingMessage(new SupportedFileTypesMessage());

        sendOutgoingMessage(toggleDefaultReplySuffix(getDevicePrefs().getBoolean(PREF_GARMIN_DEFAULT_REPLY_SUFFIX, true)));
    }

    private ProtobufMessage toggleDefaultReplySuffix(boolean value) {
        final GdiSettingsService.SettingsService.Builder enableSignature = GdiSettingsService.SettingsService.newBuilder()
                .setChangeRequest(
                        GdiSettingsService.ChangeRequest.newBuilder()
                                .setPointer1(65566) //TODO: this might be device specific, tested on Instinct 2s
                                .setPointer2(3) //TODO: this might be device specific, tested on Instinct 2s
                                .setEnable(GdiSettingsService.ChangeRequest.Switch.newBuilder().setValue(value)));

        return protocolBufferHandler.prepareProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setSettingsService(enableSignature).build());
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case PREF_GARMIN_DEFAULT_REPLY_SUFFIX:
                sendOutgoingMessage(toggleDefaultReplySuffix(getDevicePrefs().getBoolean(PREF_GARMIN_DEFAULT_REPLY_SUFFIX, true)));
                break;
            case PREF_SEND_APP_NOTIFICATIONS:
                NotificationSubscriptionDeviceEvent notificationSubscriptionDeviceEvent = new NotificationSubscriptionDeviceEvent();
                notificationSubscriptionDeviceEvent.enable = true; // actual status is fetched from preferences
                evaluateGBDeviceEvent(notificationSubscriptionDeviceEvent);
                break;
        }
    }

    private boolean isBusyFetching;

    private void processDownloadQueue() {
        if (!filesToDownload.isEmpty() && !fileTransferHandler.isDownloading()) {
            if (!gbDevice.isBusy()) {
                isBusyFetching = true;
                GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), "", true, 0, getContext());
                getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
                getDevice().sendDeviceUpdateIntent(getContext());
            }

            while (!filesToDownload.isEmpty()) {
                final FileTransferHandler.DirectoryEntry directoryEntry = filesToDownload.remove();
                if (alreadyDownloaded(directoryEntry)) {
                    LOG.debug("File: {} already downloaded, not downloading again.", directoryEntry.getFileName());
                    if (!getKeepActivityDataOnDevice()) { // delete file from watch if already downloaded
                        sendOutgoingMessage(new SetFileFlagsMessage(directoryEntry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
                    }
                    continue;
                }

                final DownloadRequestMessage downloadRequestMessage = fileTransferHandler.downloadDirectoryEntry(directoryEntry);
                LOG.debug("Will download file: {}", directoryEntry.getFileName());
                sendOutgoingMessage(downloadRequestMessage);
                return;
            }
        }

        if (filesToDownload.isEmpty() && !fileTransferHandler.isDownloading() && isBusyFetching) {
            if (filesToProcess.isEmpty()) {
                // No downloaded fit files to process
                if (gbDevice.isBusy() && isBusyFetching) {
                    GB.signalActivityDataFinish();
                    getDevice().unsetBusyTask();
                    GB.updateTransferNotification(null, "", false, 100, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
                isBusyFetching = false;
                return;
            }

            // Keep the device marked as busy while we process the files asynchronously

            final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), getDevice());
            final List <File> filesToProcessClone = new ArrayList<>(filesToProcess);
            filesToProcess.clear();
            final long[] lastNotificationUpdateTs = new long[]{System.currentTimeMillis()};
            fitAsyncProcessor.process(filesToProcessClone, new FitAsyncProcessor.Callback() {
                @Override
                public void onProgress(final int i) {
                    final long now = System.currentTimeMillis();
                    if (now - lastNotificationUpdateTs[0] > 1500L) {
                        lastNotificationUpdateTs[0] = now;
                        GB.updateTransferNotification(
                                "Parsing fit files", "File " + i + " of " + filesToProcessClone.size(),
                                true,
                                (i * 100) / filesToProcessClone.size(), getContext()
                        );
                    }
                }

                @Override
                public void onFinish() {
                    GB.signalActivityDataFinish();
                    getDevice().unsetBusyTask();
                    GB.updateTransferNotification(null, "", false, 100, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
                    isBusyFetching = false;
                }
            });
        }
    }

    private void enableBatteryLevelUpdate() {
        final ProtobufMessage batteryLevelProtobufRequest = protocolBufferHandler.prepareProtobufRequest(GdiSmartProto.Smart.newBuilder()
                .setDeviceStatusService(
                        GdiDeviceStatus.DeviceStatusService.newBuilder()
                                .setRemoteDeviceBatteryStatusRequest(
                                        GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusRequest.newBuilder()
                                )
                )
                .build());
        sendOutgoingMessage(batteryLevelProtobufRequest);
    }

    private void enableWeather() {
        final Map<SetDeviceSettingsMessage.GarminDeviceSetting, Object> settings = new LinkedHashMap<>(3);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.AUTO_UPLOAD_ENABLED, false);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_ALERTS_ENABLED, false);
        sendOutgoingMessage(new SetDeviceSettingsMessage(settings));
    }

    @Override
    public void onSetTime() {
        sendOutgoingMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.TIME_UPDATED, 0));
    }

    @Override
    public void onFindDevice(boolean start) {
        final GdiFindMyWatch.FindMyWatchService.Builder a = GdiFindMyWatch.FindMyWatchService.newBuilder();
        if (start) {
            a.setFindRequest(
                    GdiFindMyWatch.FindMyWatchService.FindMyWatchRequest.newBuilder()
                            .setTimeout(60)
            );
        } else {
            a.setCancelRequest(
                    GdiFindMyWatch.FindMyWatchService.FindMyWatchCancelRequest.newBuilder()
            );
        }
        final ProtobufMessage findMyWatch = protocolBufferHandler.prepareProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setFindMyWatchService(a).build());

        sendOutgoingMessage(findMyWatch);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        sendOutgoingMessage(protocolBufferHandler.setCannedMessages(cannedMessagesSpec));
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

        Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.TRACK.ARTIST, musicSpec.artist);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.ALBUM, musicSpec.album);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.TITLE, musicSpec.track);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.DURATION, String.valueOf(musicSpec.duration));

        sendOutgoingMessage(new MusicControlEntityUpdateMessage(attributes));
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        musicStateSpec = stateSpec;

        stopMusicTimer();

        musicStateTimer = new Timer();
        int updatePeriod = 4000; //milliseconds
        LOG.debug("onSetMusicState: {}", stateSpec.toString());

        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            musicStateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String playing = "1";
                    String playRate = "1.0";
                    String position = new DecimalFormat("#.000").format(musicStateSpec.position);
                    musicStateSpec.position += updatePeriod / 1000;

                    Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
                    attributes.put(MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO, StringUtils.join(",", playing, playRate, position).toString());
                    sendOutgoingMessage(new MusicControlEntityUpdateMessage(attributes));

                }
            }, 0, updatePeriod);
        } else {
            String playing = "0";
            String playRate = "0.0";
            String position = new DecimalFormat("#.###").format(stateSpec.position);

            Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
            attributes.put(MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO, StringUtils.join(",", playing, playRate, position).toString());
            sendOutgoingMessage(new MusicControlEntityUpdateMessage(attributes));
        }
    }

    @Override
    public void onInstallApp(final Uri uri) {
        final GarminAgpsInstallHandler agpsHandler = new GarminAgpsInstallHandler(uri, getContext());
        if (agpsHandler.isValid()) {
            try {
                // Write the AGPS update to a temporary file in cache, so we can load it when requested
                final File agpsFile = getAgpsFile();
                try (FileOutputStream outputStream = new FileOutputStream(agpsFile)) {
                    outputStream.write(agpsHandler.getFile().getBytes());
                    evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                            DeviceSettingsPreferenceConst.PREF_AGPS_STATUS, GarminAgpsStatus.PENDING.name()
                    ));
                    LOG.info("AGPS file successfully written to the cache directory.");
                } catch (final IOException e) {
                    LOG.error("Failed to write AGPS bytes to temporary directory", e);
                }
            } catch (final Exception e) {
                GB.toast(getContext(), "AGPS install error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }
        }
    }

    private boolean alreadyDownloaded(final FileTransferHandler.DirectoryEntry entry) {
        final Optional<File> file = getFile(entry.getFileName());
        if (file.isPresent()) {
            if (file.get().length() == 0) {
                LOG.warn("File {} is empty", entry.getFileName());
                return false;
            }
            return true;
        }

        final Optional<File> legacyFile = getFile(entry.getLegacyFileName());
        if (legacyFile.isPresent()) {
            if (legacyFile.get().length() == 0) {
                LOG.warn("Legacy file {} is empty", entry.getFileName());
                return false;
            }
            return true;
        }

        return false;
    }

    private Optional<File> getFile(final String fileName) {
        File dir;
        try {
            dir = getWritableExportDirectory();
            File outputFile = new File(dir, fileName);
            if (outputFile.exists())
                return Optional.of(outputFile);
        } catch (IOException e) {
            LOG.error("Failed to get file", e);
        }
        return Optional.empty();
    }

    public File getWritableExportDirectory() throws IOException {
        return getDevice().getDeviceCoordinator().getWritableExportDirectory(getDevice());
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        final GdiCore.CoreService.LocationUpdatedNotification.Builder locationUpdatedNotification = GdiCore.CoreService.LocationUpdatedNotification.newBuilder()
                .addLocationData(
                        GarminUtils.toLocationData(location, GdiCore.CoreService.DataType.REALTIME_TRACKING)
                );

        final ProtobufMessage locationUpdatedNotificationRequest = protocolBufferHandler.prepareProtobufRequest(
                GdiSmartProto.Smart.newBuilder().setCoreService(
                        GdiCore.CoreService.newBuilder().setLocationUpdatedNotification(locationUpdatedNotification)
                ).build()
        );
        sendOutgoingMessage(locationUpdatedNotificationRequest);
    }

    public File getAgpsFile() throws IOException {
        return new File(getAgpsCacheDirectory(), "CPE.BIN");
    }

    private File getAgpsCacheDirectory() throws IOException {
        final File cacheDir = getContext().getCacheDir();
        final File agpsCacheDir = new File(cacheDir, "garmin-agps");
        if (agpsCacheDir.mkdir()) {
            LOG.info("AGPS cache directory for Garmin devices successfully created.");
        } else if (!agpsCacheDir.exists() || !agpsCacheDir.isDirectory()) {
            throw new IOException("Cannot create/locate AGPS directory for Garmin devices.");
        }
        return agpsCacheDir;
    }

    public GarminCoordinator getCoordinator() {
        return (GarminCoordinator) getDevice().getDeviceCoordinator();
    }

    @Override
    public void onTestNewFunction() {
        parseAllFitFilesFromStorage();
    }

    private void parseAllFitFilesFromStorage() {
        // This function as-is should only be used for debug purposes
        if (!BuildConfig.DEBUG) {
            LOG.error("This should never be used in release builds");
            return;
        }

        LOG.info("Parsing all fit files from storage");

        final File[] fitFiles;
        try {
            final File exportDir = getWritableExportDirectory();

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                return;
            }

            fitFiles = exportDir.listFiles((dir, name) -> name.endsWith(".fit"));
            if (fitFiles == null) {
                LOG.error("fitFiles is null for {}", exportDir);
                return;
            }
            if (fitFiles.length == 0) {
                LOG.error("No fit files found in {}", exportDir);
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            return;
        }

        GB.updateTransferNotification("Parsing fit files", "...", true, 0, getContext());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            getCoordinator().deleteAllActivityData(device, session);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error deleting activity data", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        final long[] lastNotificationUpdateTs = new long[]{System.currentTimeMillis()};
        final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), getDevice());
        fitAsyncProcessor.process(Arrays.asList(fitFiles), new FitAsyncProcessor.Callback() {
            @Override
            public void onProgress(final int i) {
                final long now = System.currentTimeMillis();
                if (now - lastNotificationUpdateTs[0] > 1500L) {
                    lastNotificationUpdateTs[0] = now;
                    GB.updateTransferNotification(
                            "Parsing fit files", "File " + i + " of " + fitFiles.length,
                            true,
                            (i * 100) / fitFiles.length, getContext()
                    );
                }
            }

            @Override
            public void onFinish() {
                GB.updateTransferNotification("", "", false, 100, getContext());
                GB.signalActivityDataFinish();
            }
        });
    }
}
