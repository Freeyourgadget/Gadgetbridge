package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalDefinitionsEnum;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ConfigurationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MusicControlEntityUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetDeviceSettingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class GarminSupport extends AbstractBTLEDeviceSupport implements ICommunicator.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSupport.class);
    private final ProtocolBufferHandler protocolBufferHandler;
    private ICommunicator communicator;
    private MusicStateSpec musicStateSpec;
    private Timer musicStateTimer;

    public GarminSupport() {
        super(LOG);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI);
        addSupportedService(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI);
        protocolBufferHandler = new ProtocolBufferHandler(this);
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

        communicator.initializeDevice(builder);

        return builder;
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

        if (parsedMessage instanceof ProtobufMessage) {
            ProtobufMessage protobufMessage = protocolBufferHandler.processIncoming((ProtobufMessage) parsedMessage);
            if (protobufMessage != null) {
                communicator.sendMessage(protobufMessage.getOutgoingMessage());
                communicator.sendMessage(protobufMessage.getAckBytestream());
            }
        }

        communicator.sendMessage(parsedMessage.getAckBytestream());

        byte[] response = parsedMessage.getOutgoingMessage();
        if (null != response) {
//            LOG.debug("sending response {}", GB.hexdump(response));
            communicator.sendMessage(response);
        }

        if (parsedMessage instanceof ConfigurationMessage) { //the last forced message exchange
            completeInitialization();
        }

        if (parsedMessage instanceof ProtobufStatusMessage) {
            ProtobufMessage protobufMessage = protocolBufferHandler.processIncoming((ProtobufStatusMessage) parsedMessage);
            if (protobufMessage != null) {
                communicator.sendMessage(protobufMessage.getOutgoingMessage());
                communicator.sendMessage(protobufMessage.getAckBytestream());
            }
        }
    }

    @Override
    public void onSendWeather(final ArrayList<WeatherSpec> weatherSpecs) {
        sendWeatherConditions(weatherSpecs.get(0));
    }

    private void sendWeatherConditions(WeatherSpec weather) {
        List<RecordData> weatherData = new ArrayList<>();

        try {

            RecordData today = new RecordData(GlobalDefinitionsEnum.TODAY_WEATHER_CONDITIONS.getRecordDefinition());
            today.setFieldByName("weather_report", 0); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
            today.setFieldByName("timestamp", weather.timestamp);
            today.setFieldByName("observed_at_time", weather.timestamp);
            today.setFieldByName("temperature", weather.currentTemp - 273.15);
            today.setFieldByName("low_temperature", weather.todayMinTemp - 273.15);
            today.setFieldByName("high_temperature", weather.todayMaxTemp - 273.15);
            today.setFieldByName("condition", FitWeatherConditions.openWeatherCodeToFitWeatherStatus(weather.currentConditionCode));
            today.setFieldByName("wind_direction", weather.windDirection);
            today.setFieldByName("precipitation_probability", weather.precipProbability);
            today.setFieldByName("wind_speed", Math.round(weather.windSpeed));
            today.setFieldByName("temperature_feels_like", weather.feelsLikeTemp - 273.15);
            today.setFieldByName("relative_humidity", weather.currentHumidity);
            today.setFieldByName("observed_location_lat", weather.latitude);
            today.setFieldByName("observed_location_long", weather.longitude);
            today.setFieldByName("location", weather.location);
            weatherData.add(today);

            for (int hour = 0; hour <= 11; hour++) {
                if (hour < weather.hourly.size()) {
                    WeatherSpec.Hourly hourly = weather.hourly.get(hour);
                    RecordData weatherHourlyForecast = new RecordData(GlobalDefinitionsEnum.HOURLY_WEATHER_FORECAST.getRecordDefinition());
                    weatherHourlyForecast.setFieldByName("weather_report", 1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherHourlyForecast.setFieldByName("timestamp", hourly.timestamp);
                    weatherHourlyForecast.setFieldByName("temperature", hourly.temp - 273.15);
                    weatherHourlyForecast.setFieldByName("condition", FitWeatherConditions.openWeatherCodeToFitWeatherStatus(hourly.conditionCode));
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
            RecordData todayDailyForecast = new RecordData(GlobalDefinitionsEnum.DAILY_WEATHER_FORECAST.getRecordDefinition());
            todayDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
            todayDailyForecast.setFieldByName("timestamp", weather.timestamp);
            todayDailyForecast.setFieldByName("low_temperature", weather.todayMinTemp - 273.15);
            todayDailyForecast.setFieldByName("high_temperature", weather.todayMaxTemp - 273.15);
            todayDailyForecast.setFieldByName("condition", FitWeatherConditions.openWeatherCodeToFitWeatherStatus(weather.currentConditionCode));
            todayDailyForecast.setFieldByName("precipitation_probability", weather.precipProbability);
            todayDailyForecast.setFieldByName("day_of_week", GarminTimeUtils.unixTimeToGarminDayOfWeek(weather.timestamp));
            weatherData.add(todayDailyForecast);


            for (int day = 0; day < 4; day++) {
                if (day < weather.forecasts.size()) {
                    WeatherSpec.Daily daily = weather.forecasts.get(day);
                    int ts = weather.timestamp + (day + 1) * 24 * 60 * 60; //TODO: is this needed?
                    RecordData weatherDailyForecast = new RecordData(GlobalDefinitionsEnum.DAILY_WEATHER_FORECAST.getRecordDefinition());
                    weatherDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                    weatherDailyForecast.setFieldByName("timestamp", weather.timestamp);
                    weatherDailyForecast.setFieldByName("low_temperature", daily.minTemp - 273.15);
                    weatherDailyForecast.setFieldByName("high_temperature", daily.maxTemp - 273.15);
                    weatherDailyForecast.setFieldByName("condition", FitWeatherConditions.openWeatherCodeToFitWeatherStatus(daily.conditionCode));
                    weatherDailyForecast.setFieldByName("precipitation_probability", daily.precipProbability);
                    weatherDailyForecast.setFieldByName("day_of_week", GarminTimeUtils.unixTimeToGarminDayOfWeek(ts));
                    weatherData.add(weatherDailyForecast);
                }
            }

            byte[] message = new nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage(weatherData).getOutgoingMessage();
            communicator.sendMessage(message);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

    }

    private void completeInitialization() {


        onSetTime();
        enableWeather();

        //following is needed for vivomove style
        communicator.sendMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_READY, 0).getOutgoingMessage());

        enableBatteryLevelUpdate();

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

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
        communicator.sendMessage(batteryLevelProtobufRequest.getOutgoingMessage());
    }

    private void enableWeather() {
        final Map<SetDeviceSettingsMessage.GarminDeviceSetting, Object> settings = new LinkedHashMap<>(1);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        communicator.sendMessage(new SetDeviceSettingsMessage(settings).getOutgoingMessage());
    }

    @Override
    public void onSetTime() {
        communicator.sendMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.TIME_UPDATED, 0).getOutgoingMessage());
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            final ProtobufMessage findMyWatch = protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setFindRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchRequest.newBuilder()
                                                            .setTimeout(60)
                                            )
                            )
                            .build());
            communicator.sendMessage(findMyWatch.getOutgoingMessage());
        } else {
            final ProtobufMessage cancelFindMyWatch = protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setCancelRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchCancelRequest.newBuilder()
                                            )
                            )
                            .build());
            communicator.sendMessage(cancelFindMyWatch.getOutgoingMessage());
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

        Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.TRACK.ARTIST, musicSpec.artist);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.ALBUM, musicSpec.album);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.TITLE, musicSpec.track);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.DURATION, String.valueOf(musicSpec.duration));

        communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        musicStateSpec = stateSpec;

        if (musicStateTimer != null) {
            musicStateTimer.cancel();
            musicStateTimer.purge();
            musicStateTimer = null;
        }

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
                    communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());

                }
            }, 0, updatePeriod);
        } else {
            String playing = "0";
            String playRate = "0.0";
            String position = new DecimalFormat("#.###").format(stateSpec.position);

            Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
            attributes.put(MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO, StringUtils.join(",", playing, playRate, position).toString());
            communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());
        }
    }

}
