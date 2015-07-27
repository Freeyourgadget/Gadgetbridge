package nodomain.freeyourgadget.gadgetbridge.model;

public interface SummaryOfDay {
    public byte getProvider();

    public int getSteps();

    public int getDayStartWakeupTime();

    public int getDayEndFallAsleepTime();

}
