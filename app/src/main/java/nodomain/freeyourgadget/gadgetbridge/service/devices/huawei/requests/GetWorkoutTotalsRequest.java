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

import java.io.IOException;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;

public class GetWorkoutTotalsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutTotalsRequest.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;

    /**
     * Request to get workout totals
     * @param support The support
     * @param workoutNumbers The numbers of the current workout
     * @param remainder The numbers of the remainder of the workouts to get
     */
    public GetWorkoutTotalsRequest(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutTotals.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutTotals.Request(paramsProvider, workoutNumbers.workoutNumber).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutTotals.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutTotals.Response.class);

        Workout.WorkoutTotals.Response packet = (Workout.WorkoutTotals.Response) receivedPacket;

        if (packet.number != this.workoutNumbers.workoutNumber)
            throw new WorkoutParseException("Incorrect workout number!");

        LOG.info("Workout {} totals:", this.workoutNumbers.workoutNumber);
        LOG.info("Number  : " + packet.number);
        LOG.info("Status  : " + packet.status);
        LOG.info("Start   : " + packet.startTime);
        LOG.info("End     : " + packet.endTime);
        LOG.info("Calories: " + packet.calories);
        LOG.info("Distance: " + packet.distance);
        LOG.info("Steps   : " + packet.stepCount);
        LOG.info("Time    : " + packet.totalTime);
        LOG.info("Duration: " + packet.duration);
        LOG.info("Type    : " + packet.type);

        Long databaseId = this.supportProvider.addWorkoutTotalsData(packet);

        // Create the next request
        if (this.workoutNumbers.dataCount > 0) {
            GetWorkoutDataRequest nextRequest = new GetWorkoutDataRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.paceCount > 0) {
            GetWorkoutPaceRequest nextRequest = new GetWorkoutPaceRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.segmentsCount > 0) {
            GetWorkoutSwimSegmentsRequest nextRequest = new GetWorkoutSwimSegmentsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        }  else {
            new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(databaseId);
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, databaseId, new Runnable() {
                @Override
                public void run() {
                    if (!remainder.isEmpty()) {
                        GetWorkoutTotalsRequest nextRequest = new GetWorkoutTotalsRequest(
                                GetWorkoutTotalsRequest.this.supportProvider,
                                remainder.remove(0),
                                remainder
                        );
                        nextRequest.setFinalizeReq(GetWorkoutTotalsRequest.this.finalizeReq);
                        // Cannot do this with nextRequest because it's in a callback
                        try {
                            nextRequest.doPerform();
                        } catch (IOException e) {
                            finalizeReq.handleException(new ResponseParseException("Cannot send next request", e));
                        }
                    } else {
                        supportProvider.endOfWorkoutSync();
                    }
                }
            });
        }
    }
}
