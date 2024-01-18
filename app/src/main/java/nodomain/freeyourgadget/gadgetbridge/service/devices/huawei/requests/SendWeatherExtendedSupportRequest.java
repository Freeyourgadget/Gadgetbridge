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

public class SendWeatherExtendedSupportRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendWeatherExtendedSupportRequest.class);

    private Weather.Settings settings;

    public SendWeatherExtendedSupportRequest(HuaweiSupportProvider support, Weather.Settings settings) {
        super(support);
        this.serviceId = Weather.id;
        this.commandId = Weather.WeatherExtendedSupport.id;
        this.settings = settings;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Weather.WeatherExtendedSupport.Request(this.paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (receivedPacket instanceof Weather.WeatherExtendedSupport.Response) {
            this.settings.timeSupported = ((Weather.WeatherExtendedSupport.Response) receivedPacket).timeSupported;
            this.settings.sourceSupported = ((Weather.WeatherExtendedSupport.Response) receivedPacket).sourceSupported;
            this.settings.weatherIconSupported = ((Weather.WeatherExtendedSupport.Response) receivedPacket).weatherIconSupported;
        } else {
            LOG.error("WeatherExtendedSupport response is not of type WeatherExtendedSupport response");
        }
    }
}
