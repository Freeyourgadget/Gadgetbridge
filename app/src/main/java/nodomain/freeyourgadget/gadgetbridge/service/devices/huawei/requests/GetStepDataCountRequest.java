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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetStepDataCountRequest extends Request {
    private int start = 0;
    private int end = 0;

    public GetStepDataCountRequest(HuaweiSupportProvider support, int start, int end) {
        super(support);

        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.MessageCount.stepId;

        this.start = start;
        this.end = end;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FitnessData.MessageCount.Request(paramsProvider, this.commandId, this.start, this.end).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof FitnessData.MessageCount.Response))
            throw new ResponseTypeMismatchException(receivedPacket, FitnessData.MessageCount.Response.class);

        short count = ((FitnessData.MessageCount.Response) receivedPacket).count;

        if (count > 0) {
            GetStepDataRequest nextRequest = new GetStepDataRequest(supportProvider, count, (short) 0);
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        }
    }
}
