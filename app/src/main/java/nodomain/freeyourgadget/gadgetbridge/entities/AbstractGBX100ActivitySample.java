package nodomain.freeyourgadget.gadgetbridge.entities;

public abstract class AbstractGBX100ActivitySample extends AbstractActivitySample {
    abstract public int getCalories();
    abstract public int getSteps();

    @Override
    public int getRawIntensity() {
        return getCalories();
    }
}