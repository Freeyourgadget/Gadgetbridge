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
            public boolean success = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                this.success = this.tlv.getInteger(0x7f) == 0x000186A0;
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
