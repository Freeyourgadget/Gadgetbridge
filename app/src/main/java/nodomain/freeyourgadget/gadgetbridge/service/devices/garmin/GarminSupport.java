package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.PendingFileProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminFitFileInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminGpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSettingsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1.CommunicatorV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.CapabilitiesDeviceEvent;
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
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS;


public class GarminSupport extends AbstractBTLEDeviceSupport implements ICommunicator.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSupport.class);
    private final ProtocolBufferHandler protocolBufferHandler;
    private final NotificationsHandler notificationsHandler;
    private final FileTransferHandler fileTransferHandler;
    private final Queue<FileTransferHandler.DirectoryEntry> filesToDownload;
    private final List<MessageHandler> messageHandlers;
    private final List<FileType> supportedFileTypeList = new ArrayList<>();
    private ICommunicator communicator;
    private MediaManager mediaManager;
    private boolean mFirstConnect = false;
    private boolean isBusyFetching;

    public GarminSupport() {
        super(LOG);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V0);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V1);
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
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        this.mediaManager = new MediaManager(context);
    }

    @Override
    public void dispose() {
        LOG.info("Garmin dispose()");
        GBLocationService.stop(getContext(), getDevice());
        super.dispose();
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

        if (getDevicePrefs().getBoolean(PREF_ALLOW_HIGH_MTU, true)) {
            builder.requestMtu(515);
        }

        final CommunicatorV2 communicatorV2 = new CommunicatorV2(this);
        if (communicatorV2.initializeDevice(builder)) {
            communicator = communicatorV2;
        } else {
            // V2 did not manage to initialize, attempt V1
            final CommunicatorV1 communicatorV1 = new CommunicatorV1(this);
            if (!communicatorV1.initializeDevice(builder)) {
                // Neither V1 nor V2 worked, not a Garmin device?
                LOG.warn("Failed to find a known Garmin service");
                builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.NOT_CONNECTED, getContext()));
                return builder;
            }

            communicator = communicatorV1;
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

        communicator.sendMessage("send status", parsedMessage.getAckBytestream()); //send status message

        sendOutgoingMessage("send reply", parsedMessage); //send reply if any

        sendOutgoingMessage("send followup", followup); //send followup message if any

        if (parsedMessage instanceof ConfigurationMessage) { //the last forced message exchange
            completeInitialization();
        }

        processDownloadQueue();

    }

    protected String getNotificationAttachmentPath(int notificationId) {
        return notificationsHandler.getNotificationAttachmentPath(notificationId);
    }

    protected Bitmap getNotificationAttachmentBitmap(int notificationId) {
        final String picturePath = getNotificationAttachmentPath(notificationId);
        final Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        if (bitmap == null) {
            LOG.warn("Failed to load bitmap for {} from {}", notificationId, picturePath);
        }
        return bitmap;
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.info("INCOMING CALLSPEC: {}", callSpec.command);
        sendOutgoingMessage("send call", notificationsHandler.onSetCallState(callSpec));
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof WeatherRequestDeviceEvent) {
            WeatherSpec weather = Weather.getInstance().getWeatherSpec();
            if (weather != null) {
                sendWeatherConditions(weather);
            }
        } else if (deviceEvent instanceof CapabilitiesDeviceEvent) {
            final Set<GarminCapability> capabilities = ((CapabilitiesDeviceEvent) deviceEvent).capabilities;
            if (capabilities.contains(GarminCapability.REALTIME_SETTINGS)) {
                final String language = Locale.getDefault().getLanguage();
                final String country = Locale.getDefault().getCountry();
                final String localeString = language + "_" + country.toUpperCase();
                final ProtobufMessage realtimeSettingsInit = protocolBufferHandler.prepareProtobufRequest(GdiSmartProto.Smart.newBuilder()
                        .setSettingsService(
                                GdiSettingsService.SettingsService.newBuilder()
                                        .setInitRequest(
                                                GdiSettingsService.InitRequest.newBuilder()
                                                        .setLanguage(localeString.length() == 5 ? localeString : "en_US")
                                                        .setRegion("us") // FIXME choose region
                                        )
                        )
                        .build());
                sendOutgoingMessage("init realtime settings", realtimeSettingsInit);
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

            sendOutgoingMessage("toggle notification subscription", new NotificationSubscriptionStatusMessage(
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
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();

                    final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);

                    pendingFileProvider.addPendingFile(((FileDownloadedDeviceEvent) deviceEvent).localPath);
                } catch (final Exception e) {
                    GB.toast(getContext(), "Error saving pending file", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }

            if (!getKeepActivityDataOnDevice()) { // delete file from watch upon successful download
                sendOutgoingMessage("archive file " + entry.getFileIndex(), new SetFileFlagsMessage(entry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
            }
        }

        super.evaluateGBDeviceEvent(deviceEvent);
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
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

        sendOutgoingMessage("fetch recorded data", fileTransferHandler.initiateDownload());
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        sendOutgoingMessage("send notification " + notificationSpec.getId(), notificationsHandler.onNotification(notificationSpec));
    }

    @Override
    public void onDeleteNotification(int id) {
        sendOutgoingMessage("delete notification " + id, notificationsHandler.onDeleteNotification(id));
    }

    @Override
    public void onSendWeather(final ArrayList<WeatherSpec> weatherSpecs) { //todo: find the closest one relative to the requested lat/long
        sendWeatherConditions(weatherSpecs.get(0));
    }

    private void sendOutgoingMessage(final String taskName, final GFDIMessage message) {
        if (message == null)
            return;
        communicator.sendMessage(taskName, message.getOutgoingMessage());
    }

    private void sendWeatherConditions(WeatherSpec weather) {
        if (!getCoordinator().supports(getDevice(), GarminCapability.WEATHER_CONDITIONS)) {
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

        sendOutgoingMessage("send weather definitions", new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage(weatherDefinitions));

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
                //noinspection ExtractMethodRecommender
                WeatherSpec.Daily daily = weather.forecasts.get(day);
                int ts = weather.timestamp + (day + 1) * 24 * 60 * 60;
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

        sendOutgoingMessage("send weather data", new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage(weatherData));

    }

    private void completeInitialization() {
        onSetTime();
        enableWeather();

        //following is needed for vivomove style
        sendOutgoingMessage("set sync ready", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_READY, 0));

        enableBatteryLevelUpdate();

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        sendOutgoingMessage("request supported file types", new SupportedFileTypesMessage());

        if (mFirstConnect) {
            sendOutgoingMessage("set sync complete", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_COMPLETE, 0));
            this.mFirstConnect = false;
        }
    }

    @Override
    public void onSendConfiguration(final String config) {
        if (config.startsWith("protobuf:")) {
            try {
                final GdiSmartProto.Smart smart = GdiSmartProto.Smart.parseFrom(GB.hexStringToByteArray(config.replaceFirst("protobuf:", "")));
                sendOutgoingMessage("send config", protocolBufferHandler.prepareProtobufRequest(smart));
            } catch (final Exception e) {
                LOG.error("Failed to send {} as protobuf", config, e);
            }

            return;
        }

        //noinspection SwitchStatementWithTooFewBranches
        switch (config) {
            case PREF_SEND_APP_NOTIFICATIONS:
                NotificationSubscriptionDeviceEvent notificationSubscriptionDeviceEvent = new NotificationSubscriptionDeviceEvent();
                notificationSubscriptionDeviceEvent.enable = true; // actual status is fetched from preferences
                evaluateGBDeviceEvent(notificationSubscriptionDeviceEvent);
                return;
        }
    }

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
                        sendOutgoingMessage("archive file " + directoryEntry.getFileIndex(), new SetFileFlagsMessage(directoryEntry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
                    }
                    continue;
                }

                final DownloadRequestMessage downloadRequestMessage = fileTransferHandler.downloadDirectoryEntry(directoryEntry);
                LOG.debug("Will download file: {}", directoryEntry.getFileName());
                sendOutgoingMessage("download file " + directoryEntry.getFileIndex(), downloadRequestMessage);
                return;
            }
        }

        if (filesToDownload.isEmpty() && !fileTransferHandler.isDownloading() && isBusyFetching) {
            final List<File> filesToProcess;
            try (DBHandler handler = GBApplication.acquireDB()) {
                final DaoSession session = handler.getDaoSession();

                final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);

                filesToProcess = pendingFileProvider.getAllPendingFiles()
                        .stream()
                        .map(pf -> new File(pf.getPath()))
                        .collect(Collectors.toList());
            } catch (final Exception e) {
                LOG.error("Failed to get pending files", e);
                return;
            }

            if (filesToProcess.isEmpty()) {
                LOG.debug("No pending files to process");
                // No downloaded fit files to process
                if (gbDevice.isBusy() && isBusyFetching) {
                    getDevice().unsetBusyTask();
                    GB.signalActivityDataFinish(getDevice());
                    GB.updateTransferNotification(null, "", false, 100, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
                isBusyFetching = false;
                return;
            }

            // Keep the device marked as busy while we process the files asynchronously, but unset
            // isBusyFetching so we do not start multiple processors
            isBusyFetching = false;

            final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), getDevice());
            final long[] lastNotificationUpdateTs = new long[]{System.currentTimeMillis()};
            fitAsyncProcessor.process(filesToProcess, new FitAsyncProcessor.Callback() {
                @Override
                public void onProgress(final int i) {
                    final long now = System.currentTimeMillis();
                    if (now - lastNotificationUpdateTs[0] > 1500L) {
                        lastNotificationUpdateTs[0] = now;
                        GB.updateTransferNotification(
                                "Parsing fit files", "File " + i + " of " + filesToProcess.size(),
                                true,
                                (i * 100) / filesToProcess.size(), getContext()
                        );
                    }
                }

                @Override
                public void onFinish() {
                    getDevice().unsetBusyTask();
                    GB.signalActivityDataFinish(getDevice());
                    GB.updateTransferNotification(null, "", false, 100, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
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
        sendOutgoingMessage("enable battery updates", batteryLevelProtobufRequest);
    }

    private void enableWeather() {
        final Map<SetDeviceSettingsMessage.GarminDeviceSetting, Object> settings = new LinkedHashMap<>(3);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.AUTO_UPLOAD_ENABLED, false);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_ALERTS_ENABLED, false);
        sendOutgoingMessage("enable weather", new SetDeviceSettingsMessage(settings));
    }

    @Override
    public void onSetTime() {
        sendOutgoingMessage("set time", new SystemEventMessage(SystemEventMessage.GarminSystemEventType.TIME_UPDATED, 0));
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

        sendOutgoingMessage("find device", findMyWatch);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        sendOutgoingMessage("set canned messages", protocolBufferHandler.setCannedMessages(cannedMessagesSpec));
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (!mediaManager.onSetMusicInfo(musicSpec)) {
            return;
        }

        LOG.debug("onSetMusicInfo: {}", musicSpec.toString());

        Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.TRACK.ARTIST, musicSpec.artist);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.ALBUM, musicSpec.album);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.TITLE, musicSpec.track);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.DURATION, String.valueOf(musicSpec.duration));

        sendOutgoingMessage("set music info", new MusicControlEntityUpdateMessage(attributes));

        // Update the music state spec as well
        final MusicStateSpec bufferMusicStateSpec = mediaManager.getBufferMusicStateSpec();
        if (bufferMusicStateSpec != null) {
            sendMusicState(bufferMusicStateSpec, bufferMusicStateSpec.position);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if (!mediaManager.onSetMusicState(stateSpec)) {
            return;
        }

        LOG.debug("onSetMusicState: {}", stateSpec.toString());

        sendMusicState(stateSpec, stateSpec.position);
    }

    private void sendMusicState(final MusicStateSpec stateSpec, final int progress) {
        final int playing;
        final float playRate;
        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            playing = 1;
            playRate = stateSpec.playRate > 0 ? stateSpec.playRate / 100f : 1.0f;
        } else {
            playing = 0;
            playRate = 0;
        }
        final Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
        attributes.put(
                MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO,
                String.format(Locale.ROOT, "%d,%.1f,%.3f", playing, playRate, (float) progress)
        );
        sendOutgoingMessage("set music state", new MusicControlEntityUpdateMessage(attributes));
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        final Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.PLAYER.VOLUME, String.format(Locale.ROOT, "%.2f", volume / 100f));

        sendOutgoingMessage("set phone volume", new MusicControlEntityUpdateMessage(attributes));
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
        sendOutgoingMessage("set gps location", locationUpdatedNotificationRequest);
    }

    @Nullable
    public DocumentFile getAgpsFile(final String url) {
        final Prefs prefs = getDevicePrefs();
        final String filename = prefs.getString(GarminPreferences.agpsFilename(url), "");
        if (filename.isEmpty()) {
            LOG.debug("agps file not configured for {}", url);
            return null;
        }

        final String folderUri = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            LOG.debug("agps folder not set");
            return null;
        }
        final DocumentFile folder = DocumentFile.fromTreeUri(getContext(), Uri.parse(folderUri));
        if (folder == null) {
            LOG.warn("Failed to find agps folder on {}", folderUri);
            return null;
        }

        final DocumentFile localFile = folder.findFile(filename);
        if (localFile == null) {
            LOG.warn("Failed to find agps file '{}' for '{}' on '{}'", filename, url, folderUri);
            return null;
        }
        if (!localFile.isFile()) {
            LOG.warn("Local agps file {} for {} can't be read: isFile={} canRead={}", folderUri, url, localFile.isFile(), localFile.canRead());
            return null;
        }
        return localFile;
    }

    public GarminCoordinator getCoordinator() {
        return (GarminCoordinator) getDevice().getDeviceCoordinator();
    }

    @Override
    public boolean connectFirstTime() {
        mFirstConnect = true;
        return super.connect();
    }

    @Override
    public void onReadConfiguration(final String config) {
        if (config.startsWith("screenId:")) {
            final int screenId = Integer.parseInt(config.replaceFirst("screenId:", ""));

            LOG.debug("Requesting screen {}", screenId);

            final String language = Locale.getDefault().getLanguage();
            final String country = Locale.getDefault().getCountry();
            final String localeString = language + "_" + country.toUpperCase();

            sendOutgoingMessage("get settings screen " + screenId, protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setSettingsService(GdiSettingsService.SettingsService.newBuilder()
                                    .setDefinitionRequest(
                                            GdiSettingsService.ScreenDefinitionRequest.newBuilder()
                                                    .setScreenId(screenId)
                                                    .setUnk2(0)
                                                    .setLanguage(localeString.length() == 5 ? localeString : "en_US")
                                    )
                            ).build()
            ));

            sendOutgoingMessage("get settings state " + screenId, protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setSettingsService(GdiSettingsService.SettingsService.newBuilder()
                                    .setStateRequest(
                                            GdiSettingsService.ScreenStateRequest.newBuilder()
                                                    .setScreenId(screenId)
                                    )
                            ).build()
            ));
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        final GarminFitFileInstallHandler fitFileInstallHandler = new GarminFitFileInstallHandler(uri, getContext());
        if (fitFileInstallHandler.isValid()) {
            communicator.sendMessage(
                    "upload fit file",
                    fileTransferHandler.initiateUpload(
                            fitFileInstallHandler.getRawBytes(),
                            fitFileInstallHandler.getFileType()
                    ).getOutgoingMessage()
            );
        }

        final GarminGpxRouteInstallHandler garminGpxRouteInstallHandler = new GarminGpxRouteInstallHandler(uri, getContext());
        if (garminGpxRouteInstallHandler.isValid()) {
            communicator.sendMessage("upload course file", fileTransferHandler.initiateUpload(garminGpxRouteInstallHandler.getGpxRouteFileConverter().getConvertedFile().getOutgoingMessage(), FileType.FILETYPE.DOWNLOAD_COURSE).getOutgoingMessage());
        }
    }

    @Override
    public void onHeartRateTest() {
        communicator.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        communicator.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        communicator.onEnableRealtimeSteps(enable);
    }

    @Override
    public void onTestNewFunction() {
        parseAllFitFilesFromStorage();
    }

    boolean parsingFitFilesFromStorage = false;

    private void parseAllFitFilesFromStorage() {
        if (parsingFitFilesFromStorage) {
            GB.toast(getContext(), "Already parsing!", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        parsingFitFilesFromStorage = true;

        LOG.info("Parsing all fit files from storage");

        final File[] fitFiles;
        try {
            final File exportDir = getWritableExportDirectory();

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                GB.toast(getContext(), "export directory " + exportDir + " not found", Toast.LENGTH_LONG, GB.ERROR);
                return;
            }

            fitFiles = exportDir.listFiles((dir, name) -> name.endsWith(".fit"));
            if (fitFiles == null) {
                LOG.error("fitFiles is null for {}", exportDir);
                GB.toast(getContext(), "fitFiles is null for " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
            if (fitFiles.length == 0) {
                LOG.error("No fit files found in {}", exportDir);
                GB.toast(getContext(), "No fit files found in " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            GB.toast(getContext(), "Failed to parse from storage", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        GB.toast(getContext(), "Check notification for progress", Toast.LENGTH_LONG, GB.INFO);

        GB.updateTransferNotification("Parsing fit files", "...", true, 0, getContext());

        //try (DBHandler handler = GBApplication.acquireDB()) {
        //    final DaoSession session = handler.getDaoSession();
        //    final Device device = DBHelper.getDevice(gbDevice, session);
        //    //getCoordinator().deleteAllActivityData(device, session);
        //} catch (final Exception e) {
        //    GB.toast(getContext(), "Error deleting activity data", Toast.LENGTH_LONG, GB.ERROR, e);
        //}

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
                parsingFitFilesFromStorage = false;
                GB.updateTransferNotification("", "", false, 100, getContext());
                GB.signalActivityDataFinish(getDevice());
            }
        });
    }
}
