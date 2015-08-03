package nodomain.freeyourgadget.gadgetbridge.service.miband;

public class AbstractInfo {
    protected byte[] mData;

    public AbstractInfo(byte[] data) {
        mData = new byte[data.length];
        System.arraycopy(data, 0, mData, 0, data.length);
    }
}
