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

public class GetBondParamsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetBondParamsRequest.class);

    public GetBondParamsRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.BondParams.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new DeviceConfig.BondParams.Request(
                    paramsProvider,
                    supportProvider.getSerial(),
                    supportProvider.getMacAddress()
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle BondParams");

        if (!(receivedPacket instanceof DeviceConfig.BondParams.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.BondParams.Response.class);

        paramsProvider.setEncryptionCounter(((DeviceConfig.BondParams.Response) receivedPacket).encryptionCounter);
        if (paramsProvider.getAuthMode() != 0x02) {
            if (((DeviceConfig.BondParams.Response) receivedPacket).status == 1) {
                stopChain(this);
            }
        }
    }
}
