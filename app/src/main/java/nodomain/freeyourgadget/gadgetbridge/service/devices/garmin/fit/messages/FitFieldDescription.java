package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitFieldDescription extends RecordData {
    public FitFieldDescription(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 206) {
            throw new IllegalArgumentException("FitFieldDescription expects global messages of " + 206 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getDeveloperDataIndex() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getFieldDefinitionNumber() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getFitBaseTypeId() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public String getFieldName() {
        return (String) getFieldByNumber(3);
    }

    @Nullable
    public String getUnits() {
        return (String) getFieldByNumber(8);
    }
}
