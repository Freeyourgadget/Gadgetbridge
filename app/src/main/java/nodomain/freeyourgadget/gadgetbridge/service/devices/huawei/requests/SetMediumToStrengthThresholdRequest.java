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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData.MediumToStrengthThreshold;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetMediumToStrengthThresholdRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetMediumToStrengthThresholdRequest.class);

    public SetMediumToStrengthThresholdRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = MediumToStrengthThreshold.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsFitnessThresholdValue();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            //Hardcoded value till interface enable threshold values
            if(supportProvider.getHuaweiCoordinator().supportsFitnessThresholdValueV2()) {
                return new MediumToStrengthThreshold.Request(paramsProvider,
                        (byte) 0x6E,
                        (byte) 0x3C,
                        (byte) 0x19,
                        (byte) 0x58,
                        (byte) 0x01,
                        (byte) 0x01,
                        0x28,
                        0x28
                ).serialize();
            } else {
                return new MediumToStrengthThreshold.Request(paramsProvider,
                        (byte) 0x6E,
                        (byte) 0x3C,
                        (byte) 0x05,
                        (byte) 0x40,
                        (byte) 0x50,
                        (byte) 0x03,
                        -1,
                        -1
                ).serialize();
            }
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Set Activate On Rotate");
    }
}
