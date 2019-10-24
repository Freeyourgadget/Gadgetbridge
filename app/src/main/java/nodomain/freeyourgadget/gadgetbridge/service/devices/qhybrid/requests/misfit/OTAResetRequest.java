package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

public class OTAResetRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, -15, 10};
    }
}
