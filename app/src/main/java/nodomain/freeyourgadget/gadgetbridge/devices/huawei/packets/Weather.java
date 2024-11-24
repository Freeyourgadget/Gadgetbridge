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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Weather {
    public static final byte id = 0x0f;

    public static class Settings {
        // WeatherSupport
        public boolean weatherSupported = false;
        public boolean windSupported = false;
        public boolean pm25Supported = false;
        public boolean temperatureSupported = false;
        public boolean locationNameSupported = false;
        public boolean currentTemperatureSupported = false;
        public boolean unitSupported = false;
        public boolean airQualityIndexSupported = false;

        // WeatherExtendedSupport
        public boolean timeSupported = false;
        public boolean sourceSupported = false;
        public boolean weatherIconSupported = false;

        // WeatherSunMoonSupport
        public boolean sunRiseSetSupported = false;
        public boolean moonPhaseSupported = false;

        // ExpandCapabilityRequest
        public boolean uvIndexSupported = false;
    }

    public enum WeatherIcon {
        // Also used for the text on the watch
        SUNNY,
        CLOUDY,
        OVERCAST,
        SHOWERS,
        THUNDERSTORMS,
        THUNDER_AND_HAIL,
        SLEET,
        LIGHT_RAIN,
        RAIN,
        HEAVY_RAIN,
        RAIN_STORM,
        HEAVY_RAIN_STORMS,
        SEVERE_RAIN_STORMS,
        SNOW_FLURRIES,
        LIGHT_SNOW,
        SNOW,
        HEAVY_SNOW,
        SNOWSTORMS,
        FOG,
        FREEZING_RAIN,
        DUST_STORM,
        LIGHT_TO_MODERATE_RAIN,
        MODERATE_TO_HEAVY_RAIN,
        HEAVY_TO_SEVERE_RAIN,
        HEAVY_TO_TORRENTIAL_RAIN,
        SEVERE_TO_TORRENTIAL_RAIN,
        LIGHT_TO_MODERATE_SNOW,
        MODERATE_TO_HEAVY_SNOW,
        HEAVY_SNOW_TO_BLIZZARD,
        DUST,
        SAND,
        SANDSTORMS,
        FREEZING, // misses small/non-moving icon
        HOT, // misses small/non-moving icon
        COLD, // misses small/non-moving icon
        WINDY,
        HAZY,
        UNKNOWN // Good to have probably
    }

    private static byte iconToByte(WeatherIcon weatherIcon) {
        switch (weatherIcon) {
            case SUNNY:
                return 0x00;
            case CLOUDY:
                return 0x01;
            case OVERCAST:
                return 0x02;
            case SHOWERS:
                return 0x03;
            case THUNDERSTORMS:
                return 0x04;
            case THUNDER_AND_HAIL:
                return 0x05;
            case SLEET:
                return 0x06;
            case LIGHT_RAIN:
                return 0x07;
            case RAIN:
                return 0x08;
            case HEAVY_RAIN:
                return 0x09;
            case RAIN_STORM:
                return 0x0a;
            case HEAVY_RAIN_STORMS:
                return 0x0b;
            case SEVERE_RAIN_STORMS:
                return 0x0c;
            case SNOW_FLURRIES:
                return 0x0d;
            case LIGHT_SNOW:
                return 0x0e;
            case SNOW:
                return 0x0f;
            case HEAVY_SNOW:
                return 0x10;
            case SNOWSTORMS:
                return 0x11;
            case FOG:
                return 0x12;
            case FREEZING_RAIN:
                return 0x13;
            case DUST_STORM:
                return 0x14;
            case LIGHT_TO_MODERATE_RAIN:
                return 0x15;
            case MODERATE_TO_HEAVY_RAIN:
                return 0x16;
            case HEAVY_TO_SEVERE_RAIN:
                return 0x17;
            case HEAVY_TO_TORRENTIAL_RAIN:
                return 0x18;
            case SEVERE_TO_TORRENTIAL_RAIN:
                return 0x19;
            case LIGHT_TO_MODERATE_SNOW:
                return 0x1a;
            case MODERATE_TO_HEAVY_SNOW:
                return 0x1b;
            case HEAVY_SNOW_TO_BLIZZARD:
                return 0x1c;
            case DUST:
                return 0x1d;
            case SAND:
                return 0x1e;
            case SANDSTORMS:
                return 0x1f;
            case FREEZING:
                return 0x20;
            case HOT:
                return 0x21;
            case COLD:
                return 0x22;
            case WINDY:
                return 0x23;
            case HAZY:
                return 0x35;
            default:
                return 0x63; // Any higher and the current weather breaks
        }
    }

    private static WeatherIcon byteToIcon(byte weatherIcon) {
        switch (weatherIcon) {
            case 0x00:
                return WeatherIcon.SUNNY;
            case 0x01:
                return WeatherIcon.CLOUDY;
            case 0x02:
                return WeatherIcon.OVERCAST;
            case 0x03:
                return WeatherIcon.SHOWERS;
            case 0x04:
                return WeatherIcon.THUNDERSTORMS;
            case 0x05:
                return WeatherIcon.THUNDER_AND_HAIL;
            case 0x06:
                return WeatherIcon.SLEET;
            case 0x07:
                return WeatherIcon.LIGHT_RAIN;
            case 0x08:
                return WeatherIcon.RAIN;
            case 0x09:
                return WeatherIcon.HEAVY_RAIN;
            case 0x0a:
                return WeatherIcon.RAIN_STORM;
            case 0x0b:
                return WeatherIcon.HEAVY_RAIN_STORMS;
            case 0x0c:
                return WeatherIcon.SEVERE_RAIN_STORMS;
            case 0x0d:
                return WeatherIcon.SNOW_FLURRIES;
            case 0x0e:
                return WeatherIcon.LIGHT_SNOW;
            case 0x0f:
                return WeatherIcon.SNOW;
            case 0x10:
                return WeatherIcon.HEAVY_SNOW;
            case 0x11:
                return WeatherIcon.SNOWSTORMS;
            case 0x12:
                return WeatherIcon.FOG;
            case 0x13:
                return WeatherIcon.FREEZING_RAIN;
            case 0x14:
                return WeatherIcon.DUST_STORM;
            case 0x15:
                return WeatherIcon.LIGHT_TO_MODERATE_RAIN;
            case 0x16:
                return WeatherIcon.MODERATE_TO_HEAVY_RAIN;
            case 0x17:
                return WeatherIcon.HEAVY_TO_SEVERE_RAIN;
            case 0x18:
                return WeatherIcon.HEAVY_TO_TORRENTIAL_RAIN;
            case 0x19:
                return WeatherIcon.SEVERE_TO_TORRENTIAL_RAIN;
            case 0x1a:
                return WeatherIcon.LIGHT_TO_MODERATE_SNOW;
            case 0x1b:
                return WeatherIcon.MODERATE_TO_HEAVY_SNOW;
            case 0x1c:
                return WeatherIcon.HEAVY_SNOW_TO_BLIZZARD;
            case 0x1d:
                return WeatherIcon.DUST;
            case 0x1e:
                return WeatherIcon.SAND;
            case 0x1f:
                return WeatherIcon.SANDSTORMS;
            case 0x20:
                return WeatherIcon.FREEZING;
            case 0x21:
                return WeatherIcon.HOT;
            case 0x22:
                return WeatherIcon.COLD;
            case 0x23:
                return WeatherIcon.WINDY;
            case 0x35:
                return WeatherIcon.HAZY;
            default:
                return WeatherIcon.UNKNOWN;
        }
    }

    public enum HuaweiTemperatureFormat {
        CELSIUS,
        FAHRENHEIT
    }

    private static byte temperatureFormatToByte(HuaweiTemperatureFormat temperatureFormat) {
        if (temperatureFormat == HuaweiTemperatureFormat.FAHRENHEIT)
            return 1;
        return 0;
    }

    public enum MoonPhase {
        NEW_MOON,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL_MOON,
        WANING_GIBBOUS,
        THIRD_QUARTER,
        WANING_CRESCENT
    }

    public static MoonPhase degreesToMoonPhase(int degrees) {
        final int leeway = 6; // Give some leeway for the new moon, first quarter, full moon, and third quarter
        if (degrees < 0 || degrees > 360)
            return null;
        else if (degrees >= 360 - leeway || degrees <= leeway)
            return MoonPhase.NEW_MOON;
        else if (degrees < 90)
            return MoonPhase.WAXING_CRESCENT;
        else if (degrees <= 90 + leeway)
            return MoonPhase.FIRST_QUARTER;
        else if (degrees < 180 - leeway)
            return MoonPhase.WAXING_GIBBOUS;
        else if (degrees <= 180 + leeway)
            return MoonPhase.FULL_MOON;
        else if (degrees < 270 - leeway)
            return MoonPhase.WANING_GIBBOUS;
        else if (degrees <= 270 + leeway)
            return MoonPhase.THIRD_QUARTER;
        else
            return MoonPhase.WANING_CRESCENT;
    }

    private static byte moonPhaseToByte (MoonPhase moonPhase) {
        switch (moonPhase) {
            case NEW_MOON:
                return 1;
            case WAXING_CRESCENT:
                return 2;
            case FIRST_QUARTER:
                return 3;
            case WAXING_GIBBOUS:
                return 4;
            case FULL_MOON:
                return 5;
            case WANING_GIBBOUS:
                return 6;
            case THIRD_QUARTER:
                return 7;
            case WANING_CRESCENT:
                return 8;
            default:
                return -1;
        }
    }

    private static MoonPhase byteToMoonPhase(byte moonPhase) {
        switch (moonPhase) {
            case 1:
                return MoonPhase.NEW_MOON;
            case 2:
                return MoonPhase.WAXING_CRESCENT;
            case 3:
                return MoonPhase.FIRST_QUARTER;
            case 4:
                return MoonPhase.WAXING_GIBBOUS;
            case 5:
                return MoonPhase.FULL_MOON;
            case 6:
                return MoonPhase.WANING_GIBBOUS;
            case 7:
                return MoonPhase.THIRD_QUARTER;
            case 8:
                return MoonPhase.WANING_CRESCENT;
            default:
                return null;
        }
    }

    public enum ErrorCode {
        NETWORK_ERROR,
        GPS_PERMISSION_ERROR,
        WEATHER_DISABLED
    }

    private static byte errorCodeToByte(ErrorCode errorCode) {
        switch (errorCode) {
            case NETWORK_ERROR:
                return 0;
            case GPS_PERMISSION_ERROR:
                return 1;
            case WEATHER_DISABLED:
                return 2;
        }
        throw new RuntimeException(); // Shouldn't happen
    }

    public static class CurrentWeatherRequest extends HuaweiPacket {
        public static final byte id = 0x01;

        public CurrentWeatherRequest(
                ParamsProvider paramsProvider,
                Settings settings,
                WeatherIcon icon,
                Byte windDirection,
                Byte windSpeed,
                Byte lowestTemperature,
                Byte highestTemperature,
                Short pm25, // TODO: might be float?
                String locationName,
                Byte currentTemperature,
                HuaweiTemperatureFormat temperatureUnit,
                Short airQualityIndex,
                Integer observationTime,
                Float uvIndex,
                String sourceName
        ) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV();

            HuaweiTLV tlv81 = new HuaweiTLV();

            if (icon != null && settings.weatherIconSupported) {
                tlv81.put(0x02, iconToByte(icon));
            }

            if (settings.windSupported) {
                short wind = 0;
                if (windSpeed != null)
                    wind = (short) windSpeed;

                if (windDirection != null) {
                    if (windDirection > 0)
                        wind |= (short) (((short) (windDirection * 8 / 360)) << 8);
                    else
                        wind |= (short) (((short) (360 + windDirection) * 8 / 360) << 8);
                }
                tlv81.put(0x03, wind);
            }

            if (settings.weatherIconSupported || settings.windSupported)
                this.tlv.put(0x81, tlv81);

            if (lowestTemperature != null && highestTemperature != null && settings.temperatureSupported) {
                this.tlv.put(0x85, new HuaweiTLV()
                        .put(0x06, lowestTemperature)
                        .put(0x07, highestTemperature)
                );
            }
            if (windDirection != null && windSpeed != null && settings.windSupported)
                this.tlv.put(0x03, (short) ((((short) windDirection) << 8) | ((short) windSpeed)));
            if (pm25 != null && settings.pm25Supported)
                this.tlv.put(0x04, pm25);
            if (locationName != null && settings.locationNameSupported)
                this.tlv.put(0x08, locationName);
            if (currentTemperature != null && settings.currentTemperatureSupported)
                this.tlv.put(0x09, currentTemperature);
            if (temperatureUnit != null && settings.unitSupported)
                this.tlv.put(0x0a, temperatureFormatToByte(temperatureUnit));
            if (airQualityIndex != null && settings.airQualityIndexSupported)
                this.tlv.put(0x0b, airQualityIndex);
            if (observationTime != null && settings.timeSupported)
                this.tlv.put(0x0c, observationTime);
            if (sourceName != null && settings.sourceSupported)
                this.tlv.put(0x0e, sourceName);
            if (uvIndex != null && settings.uvIndexSupported)
                this.tlv.put(0x0f, uvIndex.byteValue());

            this.isEncrypted = true;
            this.complete = true;
        }
    }

    public static class WeatherSupport {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte supportedBitmap = 0;

            public boolean weatherSupported = false;
            public boolean windSupported = false;
            public boolean pm25Supported = false;
            public boolean temperatureSupported = false;
            public boolean locationNameSupported = false;
            public boolean currentTemperatureSupported = false;
            public boolean unitSupported = false;
            public boolean airQualityIndexSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getByte(0x01);

                this.weatherSupported = (this.supportedBitmap & 0x01) != 0;
                this.windSupported = (this.supportedBitmap & 0x02) != 0;
                this.pm25Supported = (this.supportedBitmap & 0x04) != 0;
                this.temperatureSupported = (this.supportedBitmap & 0x08) != 0;
                this.locationNameSupported = (this.supportedBitmap & 0x10) != 0;
                this.currentTemperatureSupported = (this.supportedBitmap & 0x20) != 0;
                this.unitSupported = (this.supportedBitmap & 0x40) != 0;
                this.airQualityIndexSupported = (this.supportedBitmap & 0x80) != 0;
            }
        }
    }

    public static class WeatherDeviceRequest extends HuaweiPacket {
        public static final byte id = 0x04;

        public WeatherDeviceRequest(ParamsProvider paramsProvider, int responseValue) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV().put(0x01, responseValue);
            this.isEncrypted = false;
            this.complete = true;
        }
    }

    public static class WeatherUnitRequest extends HuaweiPacket {
        public static final byte id = 0x05;

        public WeatherUnitRequest(ParamsProvider paramsProvider, HuaweiTemperatureFormat temperatureFormat) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV().put(0x01, temperatureFormatToByte(temperatureFormat));
            this.isEncrypted = true;
            this.complete = true;
        }
    }

    public static class WeatherExtendedSupport {
        public static final byte id = 0x06;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public short supportedBitmap = 0;

            public boolean timeSupported = false;
            public boolean sourceSupported = false;
            public boolean weatherIconSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getShort(0x01);

                this.timeSupported = (this.supportedBitmap & 0x01) != 0;
                this.sourceSupported = (this.supportedBitmap & 0x02) != 0;
                this.weatherIconSupported = (this.supportedBitmap & 0x04) != 0;
            }
        }
    }

    public static class WeatherErrorSimple {
        public static final byte id = 0x07;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, ErrorCode errorCode) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01, errorCodeToByte(errorCode));
                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class WeatherForecastData {
        public static final byte id = 0x08;

        public static class TimeData {
            public int timestamp;
            public WeatherIcon icon;
            public Byte temperature;

            @NonNull
            @Override
            public String toString() {
                String timestampStr = new Date(timestamp * 1000L).toString();
                return "TimeData{" +
                        "timestamp=" + timestamp +
                        ", timestamp=" + timestampStr +
                        ", icon=" + icon +
                        ", temperature=" + temperature +
                        '}';
            }
        }

        public static class DayData {
            public int timestamp;
            public WeatherIcon icon;
            public Byte highTemperature;
            public Byte lowTemperature;
            public Integer sunriseTime;
            public Integer sunsetTime;
            public Integer moonRiseTime;
            public Integer moonSetTime;
            public MoonPhase moonPhase;

            @NonNull
            @Override
            public String toString() {
                String timestampStr = new Date(timestamp * 1000L).toString();
                return "DayData{" +
                        "timestamp=" + timestamp +
                        ", timestamp=" + timestampStr +
                        ", icon=" + icon +
                        ", highTemperature=" + highTemperature +
                        ", lowTemperature=" + lowTemperature +
                        ", sunriseTime=" + sunriseTime +
                        ", sunsetTime=" + sunsetTime +
                        ", moonRiseTime=" + moonRiseTime +
                        ", moonSetTime=" + moonSetTime +
                        ", moonPhase=" + moonPhase +
                        '}';
            }
        }

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    Settings settings,
                    List<TimeData> timeDataList,
                    List<DayData> dayDataList
            ) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV();

                if (timeDataList != null && !timeDataList.isEmpty()) {
                    HuaweiTLV timeDataTlv = new HuaweiTLV();
                    for (TimeData timeData : timeDataList) {
                        HuaweiTLV timeTlv = new HuaweiTLV();
                        timeTlv.put(0x03, timeData.timestamp);
                        if (timeData.icon != null && settings.weatherIconSupported)
                            timeTlv.put(0x04, iconToByte(timeData.icon));
                        if (timeData.temperature != null && (settings.temperatureSupported || settings.currentTemperatureSupported))
                            timeTlv.put(0x05, timeData.temperature);
                        timeDataTlv.put(0x82, timeTlv);
                    }
                    this.tlv.put(0x81, timeDataTlv);
                }

                if (dayDataList != null && !dayDataList.isEmpty()) {
                    HuaweiTLV dayDataTlv = new HuaweiTLV();
                    for (DayData dayData : dayDataList) {
                        HuaweiTLV dayTlv = new HuaweiTLV();
                        dayTlv.put(0x12, dayData.timestamp);
                        if (dayData.icon != null && settings.weatherIconSupported)
                            dayTlv.put(0x13, iconToByte(dayData.icon));
                        if (settings.temperatureSupported) {
                            if (dayData.highTemperature != null)
                                dayTlv.put(0x14, dayData.highTemperature);
                            if (dayData.lowTemperature != null)
                                dayTlv.put(0x15, dayData.lowTemperature);
                        }
                        if (settings.sunRiseSetSupported) {
                            if (dayData.sunriseTime != null && dayData.sunriseTime != 0)
                                dayTlv.put(0x16, dayData.sunriseTime);
                            if (dayData.sunsetTime != null && dayData.sunsetTime != 0)
                                dayTlv.put(0x17, dayData.sunsetTime);
                            if (dayData.moonRiseTime != null && dayData.moonRiseTime != 0)
                                dayTlv.put(0x1a, dayData.moonRiseTime);
                            if (dayData.moonSetTime != null && dayData.moonSetTime != 0)
                                dayTlv.put(0x1b, dayData.moonSetTime);
                        }
                        if (dayData.moonPhase != null && settings.moonPhaseSupported)
                            dayTlv.put(0x1e, moonPhaseToByte(dayData.moonPhase));
                        dayDataTlv.put(0x91, dayTlv);
                    }
                    this.tlv.put(0x90, dayDataTlv);
                }

                this.isEncrypted = true;
                this.isSliced = true;
                this.complete = true;
            }
        }

        public static class OutgoingRequest extends HuaweiPacket {
            List<TimeData> timeDataList;
            List<DayData> dayDataList;

            public OutgoingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.complete = false;
            }

            @Override
            public void parseTlv() throws ParseException {
                timeDataList = new ArrayList<>(this.tlv.getObject(0x81).getObjects(0x82).size());
                for (HuaweiTLV timeTlv : this.tlv.getObject(0x81).getObjects(0x82)) {
                    TimeData timeData = new TimeData();
                    timeData.timestamp = timeTlv.getInteger(0x03);
                    timeData.icon = byteToIcon(timeTlv.getByte(0x04));
                    timeData.temperature = timeTlv.getByte(0x05);
                    timeDataList.add(timeData);
                }

                dayDataList = new ArrayList<>(this.tlv.getObject(0x90).getObjects(0x91).size());
                for (HuaweiTLV dayTlv : this.tlv.getObject(0x90).getObjects(0x91)) {
                    DayData dayData = new DayData();
                    dayData.timestamp = dayTlv.getInteger(0x12);
                    dayData.icon = byteToIcon(dayTlv.getByte(0x13));
                    dayData.highTemperature = dayTlv.getByte(0x14);
                    dayData.lowTemperature = dayTlv.getByte(0x15);
                    dayData.sunriseTime = dayTlv.getInteger(0x16);
                    dayData.sunsetTime = dayTlv.getInteger(0x17);
                    dayData.moonRiseTime = dayTlv.getInteger(0x1a);
                    dayData.moonSetTime = dayTlv.getInteger(0x1b);
                    dayData.moonPhase = byteToMoonPhase(dayTlv.getByte(0x1e));
                    dayDataList.add(dayData);
                }
            }
        }
    }

    public static class WeatherStart {
        public static final byte id = 0x09;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01, (byte) 0x03); // TODO: find out what this means
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public int successCode = -1;
            public boolean success = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                this.successCode = this.tlv.getInteger(0x7f);
                this.success = this.successCode == 0x000186A0 || this.successCode == 0x000186A3;
            }
        }
    }

    public static class WeatherSunMoonSupport {
        public static final byte id = 0x0a;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public int supportedBitmap = 0;

            public boolean sunRiseSetSupported = false;
            public boolean moonPhaseSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getInteger(0x01);

                this.sunRiseSetSupported = (this.supportedBitmap & 0x01) != 0;
                this.moonPhaseSupported = (this.supportedBitmap & 0x02) != 0;
            }
        }
    }

    public static class WeatherErrorExtended {
        public static final byte id = 0x0c;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, ErrorCode errorCode, boolean serverError) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;

                HuaweiTLV innerTlv = new HuaweiTLV();
                innerTlv.put(0x02, errorCodeToByte(errorCode));
                if (errorCode == ErrorCode.NETWORK_ERROR && serverError)
                    innerTlv.put(0x03, (byte) 0x01);

                this.tlv = new HuaweiTLV().put(0x81, innerTlv);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }
}
