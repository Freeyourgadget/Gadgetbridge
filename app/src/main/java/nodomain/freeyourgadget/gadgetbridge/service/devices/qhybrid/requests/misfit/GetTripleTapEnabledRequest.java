package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

public class GetTripleTapEnabledRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 7, 3};
    }
}
