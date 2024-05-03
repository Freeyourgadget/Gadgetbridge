/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo, opcode

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiWeatherService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWeatherService.class);

    private static final int TEMPERATURE_SCALE_CELSIUS = 1;
    private static final int TEMPERATURE_SCALE_FAHRENHEIT = 2;

    public static final int COMMAND_TYPE = 10;

    private static final int CMD_SET_CURRENT_WEATHER = 0;
    private static final int CMD_UPDATE_DAILY_FORECAST = 1;
    private static final int CMD_UPDATE_HOURLY_FORECAST = 2;
    private static final int CMD_REQUEST_CONDITIONS_FOR_LOCATION = 3;
    private static final int CMD_GET_LOCATIONS = 5;
    private static final int CMD_SET_LOCATIONS = 6;
    private static final int CMD_ADD_LOCATION = 7;
    private static final int CMD_REMOVE_LOCATIONS = 8;
    private static final int CMD_GET_WEATHER_PREFS = 9;
    private static final int CMD_SET_WEATHER_PREFS = 10;

    private final Set<XiaomiProto.WeatherLocation> cachedWeatherLocations = new HashSet<>();

    public XiaomiWeatherService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize() {
        // since temperature unit is app-wide instead of device-specific, update device setting during init
        setMeasurementSystem();

        // determine whether multiple weather locations are supported by device
        // device should respond with status 1 if unsupported, we will then send cached weather
        getSupport().sendCommand("get weather locations", COMMAND_TYPE, CMD_GET_LOCATIONS);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        if (cmd.hasStatus() && cmd.getStatus() != 0) {
            LOG.warn("Received Weather command {} with status code {}", cmd.getSubtype(), cmd.getStatus());
        }

        switch (cmd.getSubtype()) {
            case CMD_UPDATE_DAILY_FORECAST: {
                if (!cmd.hasStatus()) {

                    LOG.warn("Received unexpected response to daily forecast update");

                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully sent daily forecast update");
                        break;
                    case 1:
                        LOG.warn("Daily forecasts not supported by device");
                        break;
                    default:
                        LOG.error("Unexpected status code {} in response to daily forecast update", cmd.getStatus());
                        break;
                }

                return;
            }
            case CMD_UPDATE_HOURLY_FORECAST: {
                if (!cmd.hasStatus()) {
                    LOG.warn("Received unexpected response to hourly forecast update");
                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully sent hourly forecast update");
                        break;
                    case 1:
                        LOG.warn("Hourly forecasts not supported by device");
                        break;
                    default:
                        LOG.error("Unexpected status code {} in response to hourly forecast update", cmd.getStatus());
                        break;
                }

                return;
            }
            case CMD_REQUEST_CONDITIONS_FOR_LOCATION: {
                onConditionRequestReceived(cmd);
                return;
            }
            case CMD_GET_LOCATIONS: {
                onWeatherLocationsReceived(cmd);
                return;
            }
            case CMD_SET_LOCATIONS: {
                if (!cmd.hasStatus()) {
                    LOG.warn("Received unexpected response after setting locations");
                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully updated location list");
                        break;
                    case 1:
                        LOG.warn("Device reported that setting the location list is not supported!");
                        break;
                    default:
                        LOG.error("Received status code {} for setting location list", cmd.getStatus());
                        break;
                }

                return;
            }
            case CMD_ADD_LOCATION: {
                if (!cmd.hasStatus()) {
                    LOG.warn("Unexpected response to add location command");
                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully added weather location");
                        break;
                    case 1:
                        LOG.debug("Adding single weather location not supported on this device");
                        break;
                    case 3:
                        LOG.debug("Failed to add single weather location; this location may have already been added");
                        break;
                    default:
                        LOG.warn("Unexpected status code {} in response to adding single weather location", cmd.getStatus());
                        break;
                }

                return;
            }
            case CMD_REMOVE_LOCATIONS: {
                if (!cmd.hasStatus()) {
                    LOG.warn("Unexpected response to remove locations command");
                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully removed weather locations");
                        break;
                    case 1:
                        LOG.debug("Removing weather locations not supported on this device");
                        break;
                    default:
                        LOG.warn("Unexpected status code {} in response to removing weather locations", cmd.getStatus());
                        break;
                }

                return;
            }
            case CMD_SET_WEATHER_PREFS: {
                if (!cmd.hasStatus()) {
                    LOG.warn("Received unexpected response after setting weather preferences");
                    return;
                }

                switch (cmd.getStatus()) {
                    case 0:
                        LOG.debug("Successfully updated weather-related preferences");
                        break;
                    case 1:
                        LOG.warn("Weather-related preferences are not supported on this device");
                        break;
                    default:
                        LOG.debug("Unexpected status code {} received in response to weather prefs update request", cmd.getStatus());
                        break;
                }

                return;
            }
        }

        LOG.warn("Unhandled weather service command {}", cmd.getSubtype());
        LOG.debug("Unhandled command content: {}", cmd);
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        // TODO add preference for warning notifications (if that has any effect at all)

        switch (config) {
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setMeasurementSystem();
                return true;
        }

        return false;
    }

    private static XiaomiProto.WeatherMetadata getWeatherMetaFromSpec(final WeatherSpec weatherSpec) {
        final String location = StringUtils.ensureNotNull(weatherSpec.location);
        return XiaomiProto.WeatherMetadata.newBuilder()
                .setPublicationTimestamp(unixTimestampToISOWithColons(weatherSpec.timestamp))
                .setCityName("")
                .setLocationName(location)
                .setLocationKey(getLocationKey(location)) // FIXME: placeholder because key is not present in spec
                .setIsCurrentLocation(weatherSpec.isCurrentLocation == 1)
                .build();
    }

    private static XiaomiProto.WeatherLocation getWeatherLocationFromSpec(final WeatherSpec weatherSpec) {
        return XiaomiProto.WeatherLocation.newBuilder()
                .setCode(getLocationKey(weatherSpec.location))
                .setName(StringUtils.ensureNotNull(weatherSpec.location))
                .build();
    }

    private static String getLocationKey(final String locationName) {
        return String.format(Locale.ROOT, "accu:%d", Math.abs(StringUtils.ensureNotNull(locationName).hashCode()) % 1000000);
    }

    private static XiaomiProto.WeatherUnitValue buildUnitValue(final int value, final String unit) {
        return XiaomiProto.WeatherUnitValue.newBuilder()
                .setUnit(unit)
                .setValue(value)
                .build();
    }

    private void addWeatherLocation(final XiaomiProto.WeatherLocation location) {
        LOG.debug("Adding weather location: code={}, name={}", location.getCode(), location.getName());

        getSupport().sendCommand("add weather location", XiaomiProto.Command.newBuilder()
                .setType(COMMAND_TYPE)
                .setSubtype(CMD_ADD_LOCATION)
                .setWeather(XiaomiProto.Weather.newBuilder()
                        .setLocation(location))
                .build());
    }

    private void addWeatherLocationFromSpec(final WeatherSpec spec) {
        addWeatherLocation(getWeatherLocationFromSpec(spec));
    }

    public void sendCurrentConditions(final WeatherSpec weatherSpec) {
        LOG.debug("Sending current weather conditions for {}", weatherSpec.location);

        XiaomiProto.Command command = XiaomiProto.Command.newBuilder()
                .setType(COMMAND_TYPE)
                .setSubtype(CMD_SET_CURRENT_WEATHER)
                .setWeather(XiaomiProto.Weather.newBuilder().setCurrent(
                        XiaomiProto.WeatherCurrent.newBuilder()
                                .setMetadata(getWeatherMetaFromSpec(weatherSpec))
                                .setWeatherCondition(XiaomiWeatherConditions.convertOwmConditionToXiaomi(weatherSpec.currentConditionCode))
                                .setTemperature(buildUnitValue(weatherSpec.currentTemp - 273, "℃"))
                                .setHumidity(buildUnitValue(weatherSpec.currentHumidity, "%"))
                                .setWind(buildUnitValue(weatherSpec.windSpeedAsBeaufort(), Integer.toString(weatherSpec.windDirection)))
                                .setUv(buildUnitValue(Math.round(weatherSpec.uvIndex), "")) // This is sent as an sint but seems to be displayed with a decimal point
                                .setAqi(buildUnitValue(
                                        weatherSpec.airQuality != null && weatherSpec.airQuality.aqi >= 0 ? weatherSpec.airQuality.aqi : 0,
                                        "Unknown" // some string like "Moderate"
                                ))
                                .setWarning(XiaomiProto.WeatherWarnings.newBuilder()) // TODO add warnings when they become available through spec
                                .setPressure(weatherSpec.pressure * 100f)
                ))
                .build();

        getSupport().sendCommand("set current weather", command);
    }

    public void sendDailyForecast(final WeatherSpec weatherSpec) {
        final XiaomiProto.ForecastEntries.Builder entryListBuilder = XiaomiProto.ForecastEntries.newBuilder();
        final int daysToSend = Math.min(6, weatherSpec.forecasts.size());

        // reconstruct first forecast element from current conditions, as the first forecast
        // is expected to apply to today
        {
            entryListBuilder.addEntry(XiaomiProto.ForecastEntry.newBuilder()
                    .setAqi(buildUnitValue(
                            weatherSpec.airQuality != null && weatherSpec.airQuality.aqi >= 0 ? weatherSpec.airQuality.aqi : 0,
                            "Unknown" // TODO describe AQI level
                    ))
                    .setTemperatureRange(XiaomiProto.WeatherRange.newBuilder()
                            .setFrom(weatherSpec.todayMinTemp - 273)
                            .setTo(weatherSpec.todayMaxTemp - 273))
                    // FIXME: should preferable be replaced with a best and worst case condition whenever that becomes available
                    .setConditionRange(XiaomiProto.WeatherRange.newBuilder()
                            .setFrom(XiaomiWeatherConditions.convertOwmConditionToXiaomi(weatherSpec.currentConditionCode))
                            .setTo(XiaomiWeatherConditions.convertOwmConditionToXiaomi(weatherSpec.currentConditionCode)))
                    .setTemperatureSymbol("℃")
                    .setSunriseSunset(XiaomiProto.WeatherSunriseSunset.newBuilder()
                            .setSunrise(weatherSpec.sunRise != 0 ? unixTimestampToISOWithColons(weatherSpec.sunRise) : "")
                            .setSunset(weatherSpec.sunSet != 0 ? unixTimestampToISOWithColons(weatherSpec.sunSet) : "")));
        }

        // loop over available forecast entries in weatherSpec
        for (WeatherSpec.Daily currentEntry : weatherSpec.forecasts.subList(0, daysToSend)) {
            entryListBuilder.addEntry(XiaomiProto.ForecastEntry.newBuilder()
                    .setAqi(buildUnitValue(
                            currentEntry.airQuality != null && currentEntry.airQuality.aqi >= 0 ? currentEntry.airQuality.aqi : 0,
                            "Unknown" // TODO describe AQI level
                    ))
                    // FIXME should preferable be replaced with a best and worst case condition whenever that becomes available
                    .setConditionRange(XiaomiProto.WeatherRange.newBuilder()
                            .setFrom(XiaomiWeatherConditions.convertOwmConditionToXiaomi(currentEntry.conditionCode))
                            .setTo(XiaomiWeatherConditions.convertOwmConditionToXiaomi(currentEntry.conditionCode)))
                    .setTemperatureRange(XiaomiProto.WeatherRange.newBuilder()
                            .setTo(currentEntry.maxTemp - 273)
                            .setFrom(currentEntry.minTemp - 273))
                    .setTemperatureSymbol("℃")
                    .setSunriseSunset(XiaomiProto.WeatherSunriseSunset.newBuilder()
                            .setSunrise(currentEntry.sunRise != 0 ? unixTimestampToISOWithColons(currentEntry.sunRise) : "")
                            .setSunset(currentEntry.sunSet != 0 ? unixTimestampToISOWithColons(currentEntry.sunSet) : "")));
        }

        LOG.debug("Sending daily forecast with {} days of info", entryListBuilder.getEntryCount());

        XiaomiProto.Command command = XiaomiProto.Command.newBuilder()
                .setType(COMMAND_TYPE)
                .setSubtype(CMD_UPDATE_DAILY_FORECAST)
                .setWeather(XiaomiProto.Weather.newBuilder().setForecast(
                        XiaomiProto.WeatherForecast.newBuilder()
                                .setMetadata(getWeatherMetaFromSpec(weatherSpec))
                                .setEntries(entryListBuilder)))
                .build();

        getSupport().sendCommand("set daily forecast", command);
    }

    public void sendHourlyForecast(final WeatherSpec weatherSpec) {
        final XiaomiProto.ForecastEntries.Builder entriesBuilder = XiaomiProto.ForecastEntries.newBuilder();
        final int hoursToSend = Math.min(23, weatherSpec.hourly.size());

        for (WeatherSpec.Hourly hourly : weatherSpec.hourly.subList(0, hoursToSend)) {
            entriesBuilder.addEntry(XiaomiProto.ForecastEntry.newBuilder()
                    .setAqi(buildUnitValue(0, "Unknown")) // FIXME when available through spec
                    .setTemperatureRange(XiaomiProto.WeatherRange.newBuilder()
                            .setFrom(0) // not set, but required
                            .setTo(hourly.temp - 273))
                    .setConditionRange(XiaomiProto.WeatherRange.newBuilder()
                            .setFrom(0) // not set, but required
                            .setTo(XiaomiWeatherConditions.convertOwmConditionToXiaomi(hourly.conditionCode)))
                    .setTemperatureSymbol("℃")
                    .setWind(buildUnitValue(hourly.windSpeedAsBeaufort(), Integer.toString(hourly.windDirection))));
        }

        LOG.debug("Sending hourly forecast with {} hours of info", entriesBuilder.getEntryCount());

        final XiaomiProto.Command command = XiaomiProto.Command.newBuilder()
                .setType(COMMAND_TYPE)
                .setSubtype(CMD_UPDATE_HOURLY_FORECAST)
                .setWeather(XiaomiProto.Weather.newBuilder()
                        .setForecast(XiaomiProto.WeatherForecast.newBuilder()
                                .setMetadata(getWeatherMetaFromSpec(weatherSpec))
                                .setEntries(entriesBuilder)))
                .build();

        getSupport().sendCommand("update hourly forecast", command);
    }

    private boolean supportsMultipleWeatherLocations() {
        return getDevicePrefs().getBoolean(XiaomiPreferences.FEAT_MULTIPLE_WEATHER_LOCATIONS, false);
    }

    public void onSendWeather(@NonNull final List<WeatherSpec> weatherSpecList) {
        if (supportsMultipleWeatherLocations()) {
            sendWeatherSpecList(weatherSpecList);
        } else {
            if (!weatherSpecList.isEmpty() && weatherSpecList.get(0) != null) {
                final WeatherSpec specToSend = weatherSpecList.get(0);
                addWeatherLocationFromSpec(specToSend);
                sendWeatherSpec(specToSend);
            }
        }
    }

    private void sendWeatherSpecList(@NonNull final List<WeatherSpec> weatherSpecs) {
        // FIXME: the MB8P seems to support more locations than the original app allows, which is
        //        undoubtedly also applicable to other devices
        List<WeatherSpec> specsToSend = weatherSpecs.subList(0, Math.min(5, weatherSpecs.size()));
        List<XiaomiProto.WeatherLocation> weatherLocations = new ArrayList<>(specsToSend.size());

        LOG.debug("Updating weather for {} location(s): {}", specsToSend.size(), extractWeatherSpecLocations(specsToSend));

        // find locations not present on device
        {
            for (final WeatherSpec spec : specsToSend) {
                final XiaomiProto.WeatherLocation location = getWeatherLocationFromSpec(spec);

                if (!cachedWeatherLocations.contains(location)) {
                    addWeatherLocation(location);

                    // assume adding location goes according to plan
                    cachedWeatherLocations.add(location);
                }

                weatherLocations.add(location);
            }
        }

        // update order of locations list on device
        {
            getSupport().sendCommand("set weather locations order", XiaomiProto.Command.newBuilder()
                    .setType(COMMAND_TYPE)
                    .setSubtype(CMD_SET_LOCATIONS)
                    .setWeather(XiaomiProto.Weather.newBuilder()
                            .setLocations(XiaomiProto.WeatherLocations.newBuilder()
                                    .addAllLocation(weatherLocations)))
                    .build());
        }

        for (WeatherSpec spec : specsToSend) {
            sendWeatherSpec(spec);
        }

        // request current location list from device to remove dangling locations
        getSupport().sendCommand("request weather locations", COMMAND_TYPE, CMD_GET_LOCATIONS);
    }

    private void sendWeatherSpec(@NonNull final WeatherSpec weatherSpec) {
        LOG.debug("Send weather for location {}", weatherSpec.location);

        sendCurrentConditions(weatherSpec);
        sendDailyForecast(weatherSpec);
        sendHourlyForecast(weatherSpec);
    }

    private void setMeasurementSystem() {
        final String metricScale = getSupport().getContext().getString(R.string.p_unit_metric);
        final String imperialScale = getSupport().getContext().getString(R.string.p_unit_imperial);
        final String measurementSystem = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, metricScale);
        LOG.info("Setting measurement system to {}", measurementSystem);

        int unitValue = TEMPERATURE_SCALE_CELSIUS;

        if (measurementSystem.equals(imperialScale)) {
            unitValue = TEMPERATURE_SCALE_FAHRENHEIT;
        } else if (!measurementSystem.equals(metricScale)) {
            LOG.warn("Unknown measurement system, defaulting to celsius");
        }

        getSupport().sendCommand(
                "set temperature scale",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SET_WEATHER_PREFS)
                        .setWeather(XiaomiProto.Weather.newBuilder().setPrefs(
                                XiaomiProto.WeatherPrefs.newBuilder().setTemperatureScale(unitValue)
                        ))
                        .build()
        );
    }

    private void onConditionRequestReceived(final XiaomiProto.Command command) {
        if (command.hasStatus() && command.getStatus() != 0) {
            LOG.warn("Received request for conditions with unexpected status code {}", command.getStatus());
            return;
        }

        if (command.hasWeather() && command.getWeather().hasLocation()) {
            final String locationKey = command.getWeather().getLocation().getCode();
            final String locationName = command.getWeather().getLocation().getName();

            if (!TextUtils.isEmpty(locationKey) && !TextUtils.isEmpty(locationName)) {
                LOG.debug("Received request for conditions (location key = {}, name = {})", locationKey, locationName);

                final List<WeatherSpec> knownWeathers = Weather.getInstance().getWeatherSpecs();
                for (WeatherSpec spec : knownWeathers) {
                    if (TextUtils.equals(spec.location, locationName)) {
                        sendWeatherSpec(spec);
                        return;
                    }
                }
            } else {
                LOG.debug("Received request for conditions at current location");
            }
        }

        final WeatherSpec spec = Weather.getInstance().getWeatherSpec();

        if (spec == null) {
            LOG.warn("Not sending weather conditions: active weather spec is null!");
            return;
        }

        sendWeatherSpec(Weather.getInstance().getWeatherSpec());
    }

    private static String[] weatherLocationsToStringArray(final Collection<XiaomiProto.WeatherLocation> locations) {
        final List<String> result = new ArrayList<>();

        for (XiaomiProto.WeatherLocation l : locations) {
            result.add(String.format("{code=%s, name=%s}", l.getCode(), l.getName()));
        }

        return result.toArray(new String[0]);
    }

    private static String[] extractWeatherSpecLocations(final Collection<WeatherSpec> specs) {
        final List<String> result = new ArrayList<>();

        for (final WeatherSpec spec : specs) {
            result.add(StringUtils.ensureNotNull(spec.location));
        }

        return result.toArray(new String[0]);
    }

    private void onWeatherLocationsReceived(final XiaomiProto.Command cmd) {
        // status code 1 = explicitly not supported
        if (cmd.hasStatus() && cmd.getStatus() == 1) {
            LOG.warn("Multiple weather locations not supported by this device");
            getSupport().setFeatureSupported(XiaomiPreferences.FEAT_MULTIPLE_WEATHER_LOCATIONS, false);

            // now that the feature flag has been updated, send cached weather
            onSendWeather(Weather.getInstance().getWeatherSpecs());

            return;
        }

        if (cmd.hasStatus() && cmd.getStatus() != 0) {
            LOG.warn("Received unexpected status code for configured weather locations request: {}", cmd.getStatus());
            return;
        }

        getSupport().setFeatureSupported(XiaomiPreferences.FEAT_MULTIPLE_WEATHER_LOCATIONS, true);

        if (!cmd.hasWeather() || !cmd.getWeather().hasLocations()) {
            LOG.warn("Received unexpected payload in response to configured weather locations request");
            LOG.debug("Unexpected weather locations command: {}", cmd);
        }

        final List<XiaomiProto.WeatherLocation> retrievedLocations = cmd.getWeather().getLocations().getLocationList();

        LOG.debug("Received {} weather locations: {}", retrievedLocations.size(), weatherLocationsToStringArray(retrievedLocations));

        // remove any duplicate locations from device before caching locations
        {
            final Set<XiaomiProto.WeatherLocation> duplicateLocations = new HashSet<>();

            for (XiaomiProto.WeatherLocation l : retrievedLocations) {
                if (Collections.frequency(retrievedLocations, l) > 1) {
                    duplicateLocations.add(l);
                }
            }

            if (!duplicateLocations.isEmpty()) {
                LOG.debug("Removing {} locations which were found as duplicates: {}", duplicateLocations.size(), weatherLocationsToStringArray(duplicateLocations));

                getSupport().sendCommand("remove duplicate weather locations", XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_REMOVE_LOCATIONS)
                        .setWeather(XiaomiProto.Weather.newBuilder()
                                .setLocations(XiaomiProto.WeatherLocations.newBuilder()
                                        .addAllLocation(duplicateLocations)))
                        .build());
            }
        }

        final Set<XiaomiProto.WeatherLocation> specLocations = new HashSet<>();

        for (final WeatherSpec s : Weather.getInstance().getWeatherSpecs()) {
            specLocations.add(getWeatherLocationFromSpec(s));
        }

        // remove locations for which a cached weather spec cannot be found
        {
            final Set<XiaomiProto.WeatherLocation> locationsMissingSpec = new HashSet<>();

            for (XiaomiProto.WeatherLocation l : retrievedLocations) {
                if (!specLocations.contains(l)) {
                    locationsMissingSpec.add(l);
                }
            }

            if (!locationsMissingSpec.isEmpty()) {
                LOG.debug("Removing {} weather locations for which a weather spec is not found cached: {}",
                        locationsMissingSpec.size(),
                        weatherLocationsToStringArray(locationsMissingSpec));

                getSupport().sendCommand("remove non-cached weather locations", XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_REMOVE_LOCATIONS)
                        .setWeather(XiaomiProto.Weather.newBuilder()
                                .setLocations(XiaomiProto.WeatherLocations.newBuilder()
                                        .addAllLocation(locationsMissingSpec))).build());
            }
        }

        // cache location that were unique
        final Set<XiaomiProto.WeatherLocation> presentLocations = new HashSet<>();
        for (XiaomiProto.WeatherLocation l : retrievedLocations) {
            if (specLocations.contains(l) && Collections.frequency(retrievedLocations, l) == 1) {
                presentLocations.add(l);
            }
        }

        LOG.debug("Caching {} weather locations: {}", presentLocations.size(), weatherLocationsToStringArray(presentLocations));
        cachedWeatherLocations.clear();
        cachedWeatherLocations.addAll(presentLocations);
    }

    public static String unixTimestampToISOWithColons(int timestamp) {
        return new StringBuilder(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                        .format(new Date(timestamp * 1000L)))
                .insert(22, ':') // FIXME: I bet this fails for some, but all this java date craps sucks
                .toString();
    }
}
