package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitPhysiologicalMetrics extends RecordData {
    public FitPhysiologicalMetrics(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 140) {
            throw new IllegalArgumentException("FitPhysiologicalMetrics expects global messages of " + 140 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getAerobicEffect() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Double getMetMax() {
        return (Double) getFieldByNumber(7);
    }

    @Nullable
    public Integer getRecoveryTime() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getLactateThresholdHeartRate() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Float getAnaerobicEffect() {
        return (Float) getFieldByNumber(20);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
