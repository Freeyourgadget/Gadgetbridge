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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetStepDataRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetStepDataRequest.class);

    short maxCount;
    short count;

    public GetStepDataRequest(HuaweiSupportProvider support, short maxCount, short count) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.MessageData.stepId;
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
        if (!(receivedPacket instanceof FitnessData.MessageData.StepResponse))
            throw new ResponseTypeMismatchException(receivedPacket, FitnessData.MessageData.StepResponse.class);

        FitnessData.MessageData.StepResponse response = (FitnessData.MessageData.StepResponse) receivedPacket;

        if (response.number != this.count) {
            LOG.warn("Counts do not match! Received: " + response.number + ", expected: " + this.count);
            this.count = response.number; // This stops it from going into a loop
        }

        for (FitnessData.MessageData.StepResponse.SubContainer subContainer : response.containers) {
            int dataTimestamp = subContainer.timestamp;

            if (subContainer.parsedData != null) {
                short steps = (short) subContainer.steps;
                short calories = (short) subContainer.calories;
                short distance = (short) subContainer.distance;
                byte heartrate = (byte) subContainer.heartrate;
                byte spo = (byte) subContainer.spo;

                if (steps == -1)
                    steps = 0;
                if (calories == -1)
                    calories = 0;
                if (distance == -1)
                    distance = 0;

                for (FitnessData.MessageData.StepResponse.SubContainer.TV tv : subContainer.unknownTVs) {
                    LOG.warn("Unknown tag in step data: " + tv);
                }

                this.supportProvider.addStepData(dataTimestamp, steps, calories, distance, spo, heartrate);
            } else {
                LOG.error(subContainer.parsedDataError);
            }
        }

        if (count + 1 < maxCount) {
            GetStepDataRequest nextRequest = new GetStepDataRequest(supportProvider, this.maxCount, (short) (this.count + 1));
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        }
    }
}
