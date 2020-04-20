package nodomain.freeyourgadget.gadgetbridge.entities;

public abstract class AbstractHybridHRActivitySample extends AbstractActivitySample {
    abstract public int getCalories();
    abstract public byte getWear_type();

    @Override
    public int getRawKind() {
        return getWear_type();
    }

    @Override
    public int getRawIntensity() {
        return getCalories();
    }

    @Override
    public void setUserId(long userId) {}

    @Override
    public long getUserId() {
        return 0;
    }
}
