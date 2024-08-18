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
    public Float getWeeklyAverage() {
        return (Float) getFieldByNumber(0);
    }

    @Nullable
    public Float getLastNightAverage() {
        return (Float) getFieldByNumber(1);
    }

    @Nullable
    public Float getLastNight5MinHigh() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Float getBaselineLowUpper() {
        return (Float) getFieldByNumber(3);
    }

    @Nullable
    public Float getBaselineBalancedLower() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Float getBaselineBalancedUpper() {
        return (Float) getFieldByNumber(5);
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
