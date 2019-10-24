package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

public class GoalTrackingSetRequest extends Request {
    public GoalTrackingSetRequest(int id, boolean state) {

    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{02, 20, 01};
    }

    @Override
    public int getPayloadLength() {
        return 5;
    }
}
