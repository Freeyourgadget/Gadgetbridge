package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;

public class GetWorkoutSwimSegmentsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutSwimSegmentsRequest.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;
    short number;
    Long databaseId;

    public GetWorkoutSwimSegmentsRequest(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder, short number, Long databaseId) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutSwimSegments.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
        this.number = number;

        this.databaseId = databaseId;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutSwimSegments.Request(paramsProvider,this.workoutNumbers.workoutNumber, this.number).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutSwimSegments.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutSwimSegments.Response.class);

        Workout.WorkoutSwimSegments.Response packet = (Workout.WorkoutSwimSegments.Response) receivedPacket;

        if (packet.workoutNumber != this.workoutNumbers.workoutNumber)
            throw new WorkoutParseException("Incorrect workout number!");

        if (packet.segmentNumber != this.number)
            throw new WorkoutParseException("Incorrect pace number!");

        LOG.info("Workout {} segment {}:", this.workoutNumbers.workoutNumber, this.number);
        LOG.info("Workout  : " + packet.workoutNumber);
        LOG.info("Segments  : " + packet.segmentNumber);
        LOG.info("Block num: " + packet.blocks.size());
        LOG.info("Blocks   : " + Arrays.toString(packet.blocks.toArray()));

        supportProvider.addWorkoutSwimSegmentsData(this.databaseId, packet.blocks, packet.segmentNumber);

        if (this.workoutNumbers.segmentsCount > this.number + 1) {
            GetWorkoutSwimSegmentsRequest nextRequest = new GetWorkoutSwimSegmentsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) (this.number + 1),
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else {
            new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(this.databaseId);
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, new Runnable() {
                @Override
                public void run() {
                    if (!remainder.isEmpty()) {
                        GetWorkoutTotalsRequest nextRequest = new GetWorkoutTotalsRequest(
                                GetWorkoutSwimSegmentsRequest.this.supportProvider,
                                remainder.remove(0),
                                remainder
                        );
                        nextRequest.setFinalizeReq(GetWorkoutSwimSegmentsRequest.this.finalizeReq);
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
