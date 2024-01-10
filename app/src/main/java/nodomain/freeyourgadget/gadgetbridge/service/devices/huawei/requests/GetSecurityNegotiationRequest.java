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

import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSecurityNegotiationRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetSecurityNegotiationRequest.class);
    public int authType = 0x00;

    public GetSecurityNegotiationRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.SecurityNegotiation.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.SecurityNegotiation.Request(
                    paramsProvider,
                    paramsProvider.getAuthMode(),
                    supportProvider.getAndroidId(),
                    Build.MODEL
                ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Security and Negotiation");

        if (!(receivedPacket instanceof DeviceConfig.SecurityNegotiation.Response)) {
            // TODO: exception
            return;
        }

        this.authType = ((DeviceConfig.SecurityNegotiation.Response) receivedPacket).authType;
    }
}
