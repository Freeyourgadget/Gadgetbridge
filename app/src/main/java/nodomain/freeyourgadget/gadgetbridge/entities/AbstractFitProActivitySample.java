package nodomain.freeyourgadget.gadgetbridge.entities;

public abstract class AbstractFitProActivitySample extends AbstractActivitySample {

    abstract public int getSteps();

    @Override
    public int getRawIntensity() {
        return getSteps();
    }

    @Override
    public void setTimestamp(int timestamp) {

    }

    @Override
    public void setUserId(long userId) {

    }

    @Override
    public void setDeviceId(long deviceId) {

    }

    @Override
    public long getDeviceId() {
        return 0;
    }

    @Override
    public long getUserId() {
        return 0;
    }

    @Override
    public int getTimestamp() {
        return 0;
    }
}



