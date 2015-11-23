package nodomain.freeyourgadget.gadgetbridge.model;

public interface SummaryOfDay {
    byte getProvider();

    int getSteps();

    int getDayStartWakeupTime();

    int getDayEndFallAsleepTime();

}
