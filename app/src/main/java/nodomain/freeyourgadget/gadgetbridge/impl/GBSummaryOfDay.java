package nodomain.freeyourgadget.gadgetbridge.impl;

import nodomain.freeyourgadget.gadgetbridge.model.SummaryOfDay;

public class GBSummaryOfDay implements SummaryOfDay {
    private byte provider;
    private int steps;
    private int dayStartWakeupTime;
    private int dayEndFallAsleepTime;

    public byte getProvider() {
        return provider;
    }

    public int getSteps() {
        return steps;
    }

    public int getDayStartWakeupTime() {
        return dayStartWakeupTime;
    }

    public int getDayEndFallAsleepTime() {
        return dayEndFallAsleepTime;
    }


}
