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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetWorkoutCountRequest extends Request {
    private final int start;
    private final int end;

    public GetWorkoutCountRequest(
            HuaweiSupportProvider support,
            nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder builder,
            int start,
            int end
    ) {
        super(support, builder);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutCount.id;

        this.start = start;
        this.end = end;
    }

    public GetWorkoutCountRequest(
            HuaweiSupportProvider support,
            nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder builder,
            int start,
            int end
    ) {
        super(support, builder);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutCount.id;

        this.start = start;
        this.end = end;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutCount.Request(paramsProvider, this.start, this.end).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutCount.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutCount.Response.class);

        Workout.WorkoutCount.Response packet = (Workout.WorkoutCount.Response) receivedPacket;

        if (packet.count != packet.workoutNumbers.size())
            throw new WorkoutParseException("Packet count and workout numbers size do not match.");

        if (packet.count > 0) {
            GetWorkoutTotalsRequest nextRequest = new GetWorkoutTotalsRequest(
                    this.supportProvider,
                    packet.workoutNumbers.remove(0),
                    packet.workoutNumbers
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else {
            this.supportProvider.endOfWorkoutSync();
        }
    }
}
