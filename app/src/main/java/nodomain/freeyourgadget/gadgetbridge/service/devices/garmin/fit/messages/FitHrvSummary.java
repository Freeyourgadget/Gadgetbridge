package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus.HrvStatus;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitHrvSummary extends RecordData {
    public FitHrvSummary(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 370) {
            throw new IllegalArgumentException("FitHrvSummary expects global messages of " + 370 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getWeeklyAverage() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getLastNightAverage() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getLastNight5MinHigh() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getBaselineLowUpper() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getBaselineBalancedLower() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getBaselineBalancedUpper() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public HrvStatus getStatus() {
        return (HrvStatus) getFieldByNumber(6);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
