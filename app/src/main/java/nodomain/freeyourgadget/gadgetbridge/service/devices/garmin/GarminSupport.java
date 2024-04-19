package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1.CommunicatorV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.NotificationSubscriptionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.SupportedFileTypesDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.WeatherRequestDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.LocalMessage;
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
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;


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
        super.dispose();
        stopMusicTimer();
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

        evaluateGBDeviceEvent(parsedMessage.getGBDeviceEvent());

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
            LOG.info("NOTIFICATIONS ARE NOW {}", enable ? "ON" : "OFF");
        } else if (deviceEvent instanceof SupportedFileTypesDeviceEvent) {
            this.supportedFileTypeList.clear();
            this.supportedFileTypeList.addAll(((SupportedFileTypesDeviceEvent) deviceEvent).getSupportedFileTypes());
        } else if (deviceEvent instanceof FileDownloadedDeviceEvent) {
            LOG.debug("FILE DOWNLOAD COMPLETE {}", ((FileDownloadedDeviceEvent) deviceEvent).directoryEntry.getFileName());

            if (false) // delete file from watch upon successful download TODO: add device setting
                sendOutgoingMessage(new SetFileFlagsMessage(((FileDownloadedDeviceEvent) deviceEvent).directoryEntry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
        }

        super.evaluateGBDeviceEvent(deviceEvent);
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
    public void onNotification(NotificationSpec notificationSpec) {
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

    private void sendWeatherConditions(WeatherSpec weather) {
        List<RecordData> weatherData = new ArrayList<>();

        List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        weatherDefinitions.add(LocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition());
        weatherDefinitions.add(LocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition());
        weatherDefinitions.add(LocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition());

        sendOutgoingMessage(new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage(weatherDefinitions));

        try {
            RecordData today = new RecordData(LocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition());
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
            today.setFieldByName("location", weather.location);
            weatherData.add(today);

            for (int hour = 0; hour <= 11; hour++) {
                if (hour < weather.hourly.size()) {
                    WeatherSpec.Hourly hourly = weather.hourly.get(hour);
                    RecordData weatherHourlyForecast = new RecordData(LocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition());
                    weatherHourlyForecast.setFieldByName("weather_report", 1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherHourlyForecast.setFieldByName("timestamp", hourly.timestamp);
                    weatherHourlyForecast.setFieldByName("temperature", hourly.temp);
                    weatherHourlyForecast.setFieldByName("condition", hourly.conditionCode);
                    weatherHourlyForecast.setFieldByName("wind_direction", hourly.windDirection);
                    weatherHourlyForecast.setFieldByName("wind_speed", Math.round(hourly.windSpeed));
                    weatherHourlyForecast.setFieldByName("precipitation_probability", hourly.precipProbability);
                    weatherHourlyForecast.setFieldByName("relative_humidity", hourly.humidity);
//                weatherHourlyForecast.setFieldByName("dew_point", 0); // dew_point sint8
                    weatherHourlyForecast.setFieldByName("uv_index", hourly.uvIndex);
//                weatherHourlyForecast.setFieldByName("air_quality", 0); // air_quality enum
                    weatherData.add(weatherHourlyForecast);
                }
            }
//
            RecordData todayDailyForecast = new RecordData(LocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition());
            todayDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
            todayDailyForecast.setFieldByName("timestamp", weather.timestamp);
            todayDailyForecast.setFieldByName("low_temperature", weather.todayMinTemp);
            todayDailyForecast.setFieldByName("high_temperature", weather.todayMaxTemp);
            todayDailyForecast.setFieldByName("condition", weather.currentConditionCode);
            todayDailyForecast.setFieldByName("precipitation_probability", weather.precipProbability);
            todayDailyForecast.setFieldByName("day_of_week", weather.timestamp);
            weatherData.add(todayDailyForecast);


            for (int day = 0; day < 4; day++) {
                if (day < weather.forecasts.size()) {
                    WeatherSpec.Daily daily = weather.forecasts.get(day);
                    int ts = weather.timestamp + (day + 1) * 24 * 60 * 60; //TODO: is this needed?
                    RecordData weatherDailyForecast = new RecordData(LocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition());
                    weatherDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherDailyForecast.setFieldByName("timestamp", weather.timestamp);
                    weatherDailyForecast.setFieldByName("low_temperature", daily.minTemp);
                    weatherDailyForecast.setFieldByName("high_temperature", daily.maxTemp);
                    weatherDailyForecast.setFieldByName("condition", daily.conditionCode);
                    weatherDailyForecast.setFieldByName("precipitation_probability", daily.precipProbability);
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
    }

    private void processDownloadQueue() {
        if (!filesToDownload.isEmpty() && !fileTransferHandler.isDownloading()) {
            if (!gbDevice.isBusy()) {
                GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), "", true, 0, getContext());
                getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
                getDevice().sendDeviceUpdateIntent(getContext());
            }

            try {
                FileTransferHandler.DirectoryEntry directoryEntry = filesToDownload.remove();
                while (checkFileExists(directoryEntry.getFileName())) {
                    LOG.debug("File: {} already downloaded, not downloading again.", directoryEntry.getFileName());
                    if (false) // delete file from watch if already downloaded TODO: add device setting
                        sendOutgoingMessage(new SetFileFlagsMessage(directoryEntry.getFileIndex(), SetFileFlagsMessage.FileFlags.ARCHIVE));
                    directoryEntry = filesToDownload.remove();
                }
                DownloadRequestMessage downloadRequestMessage = fileTransferHandler.downloadDirectoryEntry(directoryEntry);
                if (downloadRequestMessage != null) {
                    sendOutgoingMessage(downloadRequestMessage);
                } else {
                    LOG.debug("File: {} already downloaded, not downloading again, from inside.", directoryEntry.getFileName());
                }
            } catch (NoSuchElementException e) {
                // we ran out of files to download
                // FIXME this is ugly
                if (gbDevice.isBusy() && gbDevice.getBusyTask().equals(getContext().getString(R.string.busy_task_fetch_activity_data))) {
                    getDevice().unsetBusyTask();
                    GB.updateTransferNotification(null, "", false, 100, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
            }
        } else if (filesToDownload.isEmpty() && !fileTransferHandler.isDownloading()) {
            if (gbDevice.isBusy() && gbDevice.getBusyTask().equals(getContext().getString(R.string.busy_task_fetch_activity_data))) {
                getDevice().unsetBusyTask();
                GB.updateTransferNotification(null, "", false, 100, getContext());
                getDevice().sendDeviceUpdateIntent(getContext());
            }
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

    private boolean checkFileExists(String fileName) {
        File dir;
        try {
            dir = getWritableExportDirectory();
            File outputFile = new File(dir, fileName);
            if (outputFile.exists()) //do not download again already downloaded file
                return true;
        } catch (IOException e) {
            LOG.error("IOException: " + e);
        }
        return false;
    }

    public File getWritableExportDirectory() throws IOException {
        File dir;
        dir = new File(FileUtils.getExternalFilesDir() + "/" + FileUtils.makeValidFileName(getDevice().getName() + "_" + getDevice().getAddress()));
        if (!dir.isDirectory()) {
            if (!dir.mkdir()) {
                throw new IOException("Cannot create device specific directory for " + getDevice().getName());
            }
        }
        return dir;
    }


}
