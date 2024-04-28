package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitWatchfaceSettings extends RecordData {
    public FitWatchfaceSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 159) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 159 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getMode() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getLayout() {
        return (Integer) getFieldByNumber(1);
    }
}
