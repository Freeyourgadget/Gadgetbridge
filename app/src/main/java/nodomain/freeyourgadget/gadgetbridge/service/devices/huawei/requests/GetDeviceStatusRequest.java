/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetDeviceStatusRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetDeviceStatusRequest.class);

    public byte status;
    private boolean askStatus;

    public GetDeviceStatusRequest(HuaweiSupportProvider support, boolean askStatus) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.DeviceStatus.id;
        this.askStatus = askStatus;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.DeviceStatus.Request(paramsProvider, askStatus).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Device Status");

        if (!(receivedPacket instanceof DeviceConfig.DeviceStatus.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.DeviceStatus.Response.class);

        this.status = ((DeviceConfig.DeviceStatus.Response) receivedPacket).status;
    }
}
