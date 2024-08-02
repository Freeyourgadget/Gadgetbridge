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

public class SetTimeRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetTimeRequest.class);
    private boolean checkTime;

    public SetTimeRequest(HuaweiSupportProvider support, boolean syncTime) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.TimeRequest.id;
        this.checkTime = syncTime;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.TimeRequest(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle Set Time");
        if (!(receivedPacket instanceof DeviceConfig.TimeRequest.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.TimeRequest.Response.class);

        int deviceTime = ((DeviceConfig.TimeRequest.Response) receivedPacket).deviceTime;
        int systemTime = (int) (System.currentTimeMillis() / 1000);
        int diff = Math.abs(systemTime - deviceTime);
        LOG.debug("systemTime: {} deviceTime: {} diff: {}",systemTime, deviceTime, diff);
        if (this.checkTime && diff > 0) {
            SetTimeRequest setTimeReq = new SetTimeRequest(supportProvider, false);
            setTimeReq.nextRequest(this.nextRequest);
            nextRequest(setTimeReq);
        }
    }
}
