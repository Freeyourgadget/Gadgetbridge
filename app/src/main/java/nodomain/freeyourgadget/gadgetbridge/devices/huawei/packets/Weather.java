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

import java.time.Instant;
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
    }

    public static class CurrentWeatherRequest extends HuaweiPacket {
        public static final byte id = 0x01;

        public CurrentWeatherRequest(
                ParamsProvider paramsProvider,
                Settings settings,
                Byte windDirection,
                Byte windSpeed,
                Byte lowestTemperature,
                Byte highestTemperature,
                Short pm25, // TODO: might be float?
                String locationName,
                Byte currentTemperature,
                Byte temperatureUnit,
                Short airQualityIndex,
                Integer observationTime,
                String sourceName
        ) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV();

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
                this.tlv.put(0x0a, temperatureUnit);
            if (airQualityIndex != null && settings.airQualityIndexSupported)
                this.tlv.put(0x0b, airQualityIndex);
            if (observationTime != null && settings.timeSupported)
                this.tlv.put(0x0c, observationTime);
            if (sourceName != null && settings.sourceSupported)
                this.tlv.put(0x0e, sourceName);
            this.tlv.put(0x0f, (byte) 0);

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

    public static class WeatherUnitRequest extends HuaweiPacket {
        public static final byte id = 0x05;

        public WeatherUnitRequest(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV().put(0x01, (byte) 0); // TODO: find out what unit is what
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

    public static class WeatherForecastData {
        public static final byte id = 0x08;

        public static class TimeData {
            public int timestamp;
            public byte icon;
            public byte temperature;

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
            public byte icon;
            public byte highTemperature;
            public byte lowTemperature;
            public int sunriseTime;
            public int sunsetTime;
            public int moonRiseTime;
            public int moonSetTime;
            public byte moonPhase; // TODO: probably enum

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
                        // TODO: NULLs?
                        timeDataTlv.put(0x82, new HuaweiTLV()
                                .put(0x03, timeData.timestamp)
                                .put(0x04, timeData.icon)
                                .put(0x05, timeData.temperature)
                        );
                    }
                    this.tlv.put(0x81, timeDataTlv);
                }
    //            this.tlv.put(0x81);

                if (dayDataList != null && !dayDataList.isEmpty()) {
                    HuaweiTLV dayDataTlv = new HuaweiTLV();
                    for (DayData dayData : dayDataList) {
                        // TODO: NULLs?
                        dayDataTlv.put(0x91, new HuaweiTLV()
                                .put(0x12, dayData.timestamp)
                                .put(0x13, dayData.icon)
                                .put(0x14, dayData.highTemperature)
                                .put(0x15, dayData.lowTemperature)
                                .put(0x16, dayData.sunriseTime)
                                .put(0x17, dayData.sunsetTime)
                                .put(0x1a, dayData.moonRiseTime)
                                .put(0x1b, dayData.moonSetTime)
                                .put(0x1e, dayData.moonPhase)
                        );
                    }
                    this.tlv.put(0x90, dayDataTlv);
                }
    //            this.tlv.put(0x90);

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
                    timeData.icon = timeTlv.getByte(0x04);
                    timeData.temperature = timeTlv.getByte(0x05);
                    timeDataList.add(timeData);
                }

                dayDataList = new ArrayList<>(this.tlv.getObject(0x90).getObjects(0x91).size());
                for (HuaweiTLV dayTlv : this.tlv.getObject(0x90).getObjects(0x91)) {
                    DayData dayData = new DayData();
                    dayData.timestamp = dayTlv.getInteger(0x12);
                    dayData.icon = dayTlv.getByte(0x13);
                    dayData.highTemperature = dayTlv.getByte(0x14);
                    dayData.lowTemperature = dayTlv.getByte(0x15);
                    dayData.sunriseTime = dayTlv.getInteger(0x16);
                    dayData.sunsetTime = dayTlv.getInteger(0x17);
                    dayData.moonRiseTime = dayTlv.getInteger(0x1a);
                    dayData.moonSetTime = dayTlv.getInteger(0x1b);
                    dayData.moonPhase = dayTlv.getByte(0x1e);
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
            public byte supportedBitmap = 0;

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
                this.supportedBitmap = this.tlv.getByte(0x01);

                this.sunRiseSetSupported = (this.supportedBitmap & 0x01) != 0;
                this.moonPhaseSupported = (this.supportedBitmap & 0x02) != 0;
            }
        }
    }
}
