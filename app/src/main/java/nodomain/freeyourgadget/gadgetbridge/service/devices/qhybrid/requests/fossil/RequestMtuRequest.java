package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

public class RequestMtuRequest extends FossilRequest {
    private int mtu;
    private boolean finished = false;

    public RequestMtuRequest(int mtu) {
        this.mtu = mtu;
    }

    public int getMtu() {
        return mtu;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[0];
    }
}
