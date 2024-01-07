/*  Copyright (C) 2022 Gaignon Damien
    Copyright (C) 2022-2023 MartinJM

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSupportedServicesRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetSupportedServicesRequest.class);

    private final byte[] knownSupportedServices;

    public GetSupportedServicesRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.SupportedServices.id;
        this.knownSupportedServices = DeviceConfig.SupportedServices.knownSupportedServices;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.SupportedServices.Request(paramsProvider, this.knownSupportedServices).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Supported Services");

        if (!(receivedPacket instanceof DeviceConfig.SupportedServices.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.SupportedServices.Response.class);

        byte[] supportedServices = ((DeviceConfig.SupportedServices.Response) receivedPacket).supportedServices;
        List<Byte> activatedServices = new ArrayList<>();
        for (int i = 0; i < supportedServices.length; i++) {
            if (supportedServices[i] == 1) {
                activatedServices.add(knownSupportedServices[i]);
            }
        }

        GetSupportedCommandsRequest supportedCommandsReq = new GetSupportedCommandsRequest(supportProvider, activatedServices);
        nextRequest(supportedCommandsReq);
    }
}
