package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSleepDataInfo extends RecordData {
    public FitSleepDataInfo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 273) {
            throw new IllegalArgumentException("FitSleepDataInfo expects global messages of " + 273 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getUnk0() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getSampleLength() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getTimestampInTz() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Integer getUnk3() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public String getVersion() {
        return (String) getFieldByNumber(4);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
