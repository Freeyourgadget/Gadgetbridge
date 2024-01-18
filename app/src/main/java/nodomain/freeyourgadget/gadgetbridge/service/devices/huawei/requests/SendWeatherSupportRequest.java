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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendWeatherSupportRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendWeatherSupportRequest.class);

    private Weather.Settings settings;

    public SendWeatherSupportRequest(HuaweiSupportProvider support, Weather.Settings settings) {
        super(support);
        this.serviceId = Weather.id;
        this.commandId = Weather.WeatherSupport.id;
        this.settings = settings;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Weather.WeatherSupport.Request(this.paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (receivedPacket instanceof Weather.WeatherSupport.Response) {
            this.settings.weatherSupported = ((Weather.WeatherSupport.Response) receivedPacket).weatherSupported;
            this.settings.windSupported = ((Weather.WeatherSupport.Response) receivedPacket).windSupported;
            this.settings.pm25Supported = ((Weather.WeatherSupport.Response) receivedPacket).pm25Supported;
            this.settings.temperatureSupported = ((Weather.WeatherSupport.Response) receivedPacket).temperatureSupported;
            this.settings.locationNameSupported = ((Weather.WeatherSupport.Response) receivedPacket).locationNameSupported;
            this.settings.currentTemperatureSupported = ((Weather.WeatherSupport.Response) receivedPacket).currentTemperatureSupported;
            this.settings.unitSupported = ((Weather.WeatherSupport.Response) receivedPacket).unitSupported;
            this.settings.airQualityIndexSupported = ((Weather.WeatherSupport.Response) receivedPacket).airQualityIndexSupported;
        } else {
            LOG.error("WeatherSupport response is not of type WeatherSupport response");
        }
    }
}
