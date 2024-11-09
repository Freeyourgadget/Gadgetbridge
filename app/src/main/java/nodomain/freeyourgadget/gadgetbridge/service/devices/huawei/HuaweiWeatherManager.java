/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendGpsAndTimeToDeviceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherCurrentRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherDeviceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherExtendedSupportRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherForecastRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherStartRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherSunMoonSupportRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherSupportRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWeatherUnitRequest;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiWeatherManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiWeatherManager.class);

    private final HuaweiSupportProvider supportProvider;
    private boolean syncInProgress;

    public HuaweiWeatherManager(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
        this.syncInProgress = false;
    }

    public Weather.WeatherIcon openWeatherMapConditionCodeToHuaweiIcon(int conditionCode) {
        // More exact first, groups after
        switch (conditionCode) {
            case 500:
                return Weather.WeatherIcon.LIGHT_RAIN;
            case 501:
                return Weather.WeatherIcon.RAIN;
            case 502:
                return Weather.WeatherIcon.HEAVY_RAIN;
            case 503:
                return Weather.WeatherIcon.RAIN_STORM;
            case 504:
                return Weather.WeatherIcon.SEVERE_RAIN_STORMS;
            case 511:
                return Weather.WeatherIcon.FREEZING_RAIN;
            case 600:
                return Weather.WeatherIcon.LIGHT_SNOW;
            case 601:
                return Weather.WeatherIcon.SNOW;
            case 602:
                return Weather.WeatherIcon.HEAVY_SNOW;
            case 611:
                return Weather.WeatherIcon.SLEET;
            case 701:
            case 741:
                return Weather.WeatherIcon.FOG;
            case 721:
                return Weather.WeatherIcon.HAZY;
            case 751:
                return Weather.WeatherIcon.SAND;
            case 761:
                return Weather.WeatherIcon.DUST;
            case 800:
                return Weather.WeatherIcon.SUNNY;
            case 801:
            case 802:
                return Weather.WeatherIcon.CLOUDY;
            case 803:
            case 804:
                return Weather.WeatherIcon.OVERCAST;
        }
        if (conditionCode >= 200 && conditionCode < 300)
            return Weather.WeatherIcon.THUNDERSTORMS;
        if (conditionCode >= 300 && conditionCode < 400)
            return Weather.WeatherIcon.LIGHT_RAIN;
        if (conditionCode >= 500 && conditionCode < 600)
            return Weather.WeatherIcon.RAIN;
        if (conditionCode >= 600 && conditionCode < 700)
            return Weather.WeatherIcon.SNOW;
        return Weather.WeatherIcon.UNKNOWN;
    }

    private void handleException(Request.ResponseParseException e) {
        LOG.error("Error in weather sending", e);

        synchronized (this) {
            // Allow a new sync to start again
            this.syncInProgress = false;
        }
    }

    // This is to reset the syncInProgress flag when an error occurs
    private final Request.RequestCallback errorHandler = new Request.RequestCallback() {
        @Override
        public void handleException(Request.ResponseParseException e) {
            HuaweiWeatherManager.this.handleException(e);
        }
    };

    // This is to reset the syncInProgress flag when everything goes well, or an error occurs
    // Only for the last request in the chain
    private final Request.RequestCallback lastHandler = new Request.RequestCallback() {
        @Override
        public void call() {
            synchronized (this) {
                HuaweiWeatherManager.this.syncInProgress = false;
            }
        }

        @Override
        public void handleException(Request.ResponseParseException e) {
            HuaweiWeatherManager.this.handleException(e);
        }
    };

    private boolean timeOutOfDateInterval(int check, Date start, Date end) {
        return check * 1000L <= start.getTime() || check * 1000L > end.getTime();
    }

    /**
     * Some erroneous data will stop the watch from showing any weather data at all, this attempts
     * to remove such data.
     */
    private void fixupWeather(WeatherSpec weatherSpec) {
        // Ignore sun/moon data if they are not in the correct day
        // If this is wrong the watch does not display any weather data at all
        Date currentDay = DateTimeUtils.dayStart(DateTimeUtils.todayUTC());
        Date nextDay = DateTimeUtils.shiftByDays(currentDay, 1);

        if (timeOutOfDateInterval(weatherSpec.sunRise, currentDay, nextDay)) {
            LOG.warn("Sun rise for today out of bounds: {}", DateTimeUtils.parseTimeStamp(weatherSpec.sunRise));
            weatherSpec.sunRise = 0;
        }
        if (timeOutOfDateInterval(weatherSpec.sunSet, currentDay, nextDay)) {
            LOG.warn("Sun set for today out of bounds: {}", DateTimeUtils.parseTimeStamp(weatherSpec.sunSet));
            weatherSpec.sunSet = 0;
        }
        if (timeOutOfDateInterval(weatherSpec.moonRise, currentDay, nextDay)) {
            LOG.warn("Moon rise for today out of bounds: {}", DateTimeUtils.parseTimeStamp(weatherSpec.moonRise));
            weatherSpec.moonRise = 0;
        }
        if (timeOutOfDateInterval(weatherSpec.moonSet, currentDay, nextDay)) {
            LOG.warn("Moon set for today out of bounds: {}", DateTimeUtils.parseTimeStamp(weatherSpec.moonSet));
            weatherSpec.moonSet = 0;
        }

        for (WeatherSpec.Daily point : weatherSpec.forecasts) {
            currentDay = nextDay;
            nextDay = DateTimeUtils.shiftByDays(currentDay, 1);

            if (timeOutOfDateInterval(point.sunRise, currentDay, nextDay)) {
                LOG.warn("Sun rise for {} out of bounds: {}", currentDay, DateTimeUtils.parseTimeStamp(point.sunRise));
                point.sunRise = 0;
            }
            if (timeOutOfDateInterval(point.sunSet, currentDay, nextDay)) {
                LOG.warn("Sun set for {} out of bounds: {}", currentDay, DateTimeUtils.parseTimeStamp(point.sunSet));
                point.sunSet = 0;
            }
            if (timeOutOfDateInterval(point.moonRise, currentDay, nextDay)) {
                LOG.warn("Moon rise for {} out of bounds: {}", currentDay, DateTimeUtils.parseTimeStamp(point.moonRise));
                point.moonRise = 0;
            }
            if (timeOutOfDateInterval(point.moonSet, currentDay, nextDay)) {
                LOG.warn("Moon set for {} out of bounds: {}", currentDay, DateTimeUtils.parseTimeStamp(point.moonSet));
                point.moonSet = 0;
            }
        }
    }

    public void sendWeather(WeatherSpec weatherSpec) {
        // Initialize weather settings and send weather
        if (!supportProvider.getHuaweiCoordinator().supportsWeather()) {
            LOG.error("onSendWeather called while weather is not supported.");
            return;
        }

        // Do not allow sending weather if it's still going
        synchronized (this) {
            if (syncInProgress) {
                LOG.warn("Weather sync already in progress, ignoring next one");
                return;
            }
            syncInProgress = true;
        }

        fixupWeather(weatherSpec);

        Weather.Settings weatherSettings = new Weather.Settings();
        weatherSettings.uvIndexSupported = supportProvider.getHuaweiCoordinator().supportsWeatherUvIndex();

        SendWeatherStartRequest weatherStartRequest = new SendWeatherStartRequest(supportProvider, weatherSettings);
        weatherStartRequest.setFinalizeReq(errorHandler);
        weatherStartRequest.setupTimeoutUntilNext(1000);
        Request lastRequest = weatherStartRequest;

        if (supportProvider.getHuaweiCoordinator().supportsWeatherUnit()) {
            SendWeatherUnitRequest weatherUnitRequest = new SendWeatherUnitRequest(supportProvider);
            weatherUnitRequest.setFinalizeReq(errorHandler);
            lastRequest.nextRequest(weatherUnitRequest);
            lastRequest = weatherUnitRequest;
        }

        SendWeatherSupportRequest weatherSupportRequest = new SendWeatherSupportRequest(supportProvider, weatherSettings);
        weatherSupportRequest.setFinalizeReq(errorHandler);
        lastRequest.nextRequest(weatherSupportRequest);
        lastRequest = weatherSupportRequest;

        if (supportProvider.getHuaweiCoordinator().supportsWeatherExtended()) {
            SendWeatherExtendedSupportRequest weatherExtendedSupportRequest = new SendWeatherExtendedSupportRequest(supportProvider, weatherSettings);
            weatherExtendedSupportRequest.setFinalizeReq(errorHandler);
            lastRequest.nextRequest(weatherExtendedSupportRequest);
            lastRequest = weatherExtendedSupportRequest;
        }

        if (supportProvider.getHuaweiCoordinator().supportsWeatherMoonRiseSet()) {
            SendWeatherSunMoonSupportRequest weatherSunMoonSupportRequest = new SendWeatherSunMoonSupportRequest(supportProvider, weatherSettings);
            weatherSunMoonSupportRequest.setFinalizeReq(errorHandler);
            lastRequest.nextRequest(weatherSunMoonSupportRequest);
            lastRequest = weatherSunMoonSupportRequest;
        }

        // End of initialization and start of actually sending weather

        SendWeatherCurrentRequest sendWeatherCurrentRequest = new SendWeatherCurrentRequest(supportProvider, weatherSettings, weatherSpec);
        sendWeatherCurrentRequest.setFinalizeReq(errorHandler);
        lastRequest.nextRequest(sendWeatherCurrentRequest);
        lastRequest = sendWeatherCurrentRequest;

        SendGpsAndTimeToDeviceRequest sendGpsAndTimeToDeviceRequest = new SendGpsAndTimeToDeviceRequest(supportProvider);
        sendGpsAndTimeToDeviceRequest.setFinalizeReq(errorHandler);
        lastRequest.nextRequest(sendGpsAndTimeToDeviceRequest);
        lastRequest = sendGpsAndTimeToDeviceRequest;

        if (supportProvider.getHuaweiCoordinator().supportsWeatherForecasts()) {
            SendWeatherForecastRequest sendWeatherForecastRequest = new SendWeatherForecastRequest(supportProvider, weatherSettings, weatherSpec);
            sendWeatherForecastRequest.setFinalizeReq(errorHandler);
            lastRequest.nextRequest(sendWeatherForecastRequest);
            lastRequest = sendWeatherForecastRequest;
        }

        lastRequest.setFinalizeReq(lastHandler);

        try {
            weatherStartRequest.doPerform();
        } catch (IOException e) {
            // TODO: Use translatable string
            GB.toast(supportProvider.getContext(), "Failed to send weather", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send weather", e);
        }
    }

    public void handleAsyncMessage(HuaweiPacket response) {
        if (response.getTlv().getInteger(0x7f, -1) == 0x000186AA) {
            // Send weather
            final ArrayList<WeatherSpec> specs = new ArrayList<>(nodomain.freeyourgadget.gadgetbridge.model.Weather.getInstance().getWeatherSpecs());
            // TODO: could be empty, not really an issue but we need to check what to send back in that case
            this.sendWeather(specs.get(0));
            return;
        }

        // Send back ok
        try {
            SendWeatherDeviceRequest sendWeatherDeviceRequest = new SendWeatherDeviceRequest(supportProvider);
            sendWeatherDeviceRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Could not send weather device request", e);
        }
    }
}
