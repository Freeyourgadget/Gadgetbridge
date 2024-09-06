/*  Copyright (C) 2024 Damien Gaignon

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

/* In order to be compatible with all devices, request send all possible commands
to all possible services. This implies long packet which is not handled on the device.
Thus, this request could be sliced in 3 packets. But this command does not support slicing.
Thus, one need to send multiple requests and concat the response.
Packets should be 240 bytes max */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetExpandCapabilityRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetExpandCapabilityRequest.class);

    public GetExpandCapabilityRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.ExpandCapability.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsExpandCapability();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        DeviceConfig.ExpandCapability.Request expandRequest = new DeviceConfig.ExpandCapability.Request(paramsProvider);
        try {
            return expandRequest.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Expand Capability");

        if (!(receivedPacket instanceof DeviceConfig.ExpandCapability.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.ExpandCapability.Response.class);

        supportProvider.getHuaweiCoordinator().saveExpandCapabilities(((DeviceConfig.ExpandCapability.Response) receivedPacket).expandCapabilities);
    }
}
