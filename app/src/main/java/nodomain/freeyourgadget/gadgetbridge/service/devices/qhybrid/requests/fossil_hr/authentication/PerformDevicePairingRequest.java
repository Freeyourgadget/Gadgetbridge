package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

public class PerformDevicePairingRequest extends CheckDevicePairingRequest {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x02, 0x16};
    }
}
