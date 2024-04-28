package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType.Type;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitFileId extends RecordData {
    public FitFileId(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 0) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 0 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Type getType() {
        return (Type) getFieldByNumber(0);
    }

    @Nullable
    public Integer getManufacturer() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getProduct() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getSerialNumber() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Long getTimeCreated() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Integer getNumber() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getManufacturerPartner() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public String getProductName() {
        return (String) getFieldByNumber(8);
    }
}
