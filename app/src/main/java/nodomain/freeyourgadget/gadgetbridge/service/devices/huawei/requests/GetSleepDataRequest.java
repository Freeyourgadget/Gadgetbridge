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
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSleepDataRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetSleepDataRequest.class);

    private final short maxCount;
    private final short count;

    public GetSleepDataRequest(HuaweiSupportProvider support, short maxCount, short count) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.MessageData.sleepId;

        this.maxCount = maxCount;
        this.count = count;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FitnessData.MessageData.Request(paramsProvider, this.commandId, this.count).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        // FitnessData.MessageData.SleepResponse response = FitnessData.MessageData.SleepResponse.fromTlv(receivedPacket.tlv);
        if (!(receivedPacket instanceof FitnessData.MessageData.SleepResponse))
            throw new ResponseTypeMismatchException(receivedPacket, FitnessData.MessageData.SleepResponse.class);

        FitnessData.MessageData.SleepResponse response = (FitnessData.MessageData.SleepResponse) receivedPacket;

        short receivedCount = response.number;

        if (receivedCount != this.count) {
            LOG.warn("Counts do not match");
        }

        for (FitnessData.MessageData.SleepResponse.SubContainer subContainer : response.containers) {
            // TODO: it might make more sense to convert the timestamp in the FitnessData class
            int[] timestampInts = new int[6];

            for (int i = 0; i < 6; i++) {
                if (subContainer.timestamp[i] >= 0)
                    timestampInts[i] = subContainer.timestamp[i];
                else
                    timestampInts[i] = subContainer.timestamp[i] & 0xFF;
            }

            int timestamp =
                    (timestampInts[0] << 24) +
                            (timestampInts[1] << 16) +
                            (timestampInts[2] << 8) +
                            (timestampInts[3]);

            int durationInt =
                    (timestampInts[4] << 8L) +
                            (timestampInts[5]);
            short duration = (short) (durationInt * 60);

            this.supportProvider.addSleepActivity(timestamp, duration, subContainer.type);
        }

        if (count + 1 < maxCount) {
            GetSleepDataRequest nextRequest = new GetSleepDataRequest(supportProvider, this.maxCount, (short) (this.count + 1));
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        }
    }
}
