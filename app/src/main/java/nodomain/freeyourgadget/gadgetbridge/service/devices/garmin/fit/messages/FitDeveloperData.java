package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitDeveloperData extends RecordData {
    public FitDeveloperData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 207) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 207 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getApplicationId() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getDeveloperDataIndex() {
        return (Integer) getFieldByNumber(3);
    }
}
