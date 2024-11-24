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

public class SendWeatherErrorRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendWeatherErrorRequest.class);

    private final Weather.ErrorCode errorCode;

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsWeatherErrorSimple() || supportProvider.getHuaweiCoordinator().supportsWeatherErrorExtended();
    }

    public SendWeatherErrorRequest(HuaweiSupportProvider support, Weather.ErrorCode errorCode) {
        super(support);
        this.serviceId = Weather.id;
        if (supportProvider.getHuaweiCoordinator().supportsWeatherErrorExtended())
            this.commandId = Weather.WeatherErrorExtended.id;
        else
            this.commandId = Weather.WeatherErrorSimple.id;
        this.errorCode = errorCode;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            if (supportProvider.getHuaweiCoordinator().supportsWeatherErrorExtended()) {
                Weather.WeatherErrorExtended.Request request = new Weather.WeatherErrorExtended.Request(paramsProvider, errorCode, false);
                return request.serialize();
            } else {
                Weather.WeatherErrorSimple.Request request = new Weather.WeatherErrorSimple.Request(paramsProvider, errorCode);
                return request.serialize();
            }
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
