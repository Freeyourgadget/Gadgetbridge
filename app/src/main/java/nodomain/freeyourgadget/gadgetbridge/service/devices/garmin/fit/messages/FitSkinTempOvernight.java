package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSkinTempOvernight extends RecordData {
    public FitSkinTempOvernight(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 398) {
            throw new IllegalArgumentException("FitSkinTempOvernight expects global messages of " + 398 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getLocalTimestamp() {
        return (Long) getFieldByNumber(0);
    }

    @Nullable
    public Float getAverageDeviation() {
        return (Float) getFieldByNumber(1);
    }

    @Nullable
    public Float getAverage7DayDeviation() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Integer getUnk3() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
