package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSleepStats extends RecordData {
    public FitSleepStats(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 346) {
            throw new IllegalArgumentException("FitSleepStats expects global messages of " + 346 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getCombinedAwakeScore() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getAwakeTimeScore() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getAwakeningsCountScore() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getDeepSleepScore() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getSleepDurationScore() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getLightSleepScore() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getOverallSleepScore() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getSleepQualityScore() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getSleepRecoveryScore() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getRemSleepScore() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getSleepRestlessnessScore() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getAwakeningsCount() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getUnk12() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getUnk13() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getInterruptionsScore() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Float getAverageStressDuringSleep() {
        return (Float) getFieldByNumber(15);
    }

    @Nullable
    public Integer getUnk16() {
        return (Integer) getFieldByNumber(16);
    }
}
