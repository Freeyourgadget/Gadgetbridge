package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitDeviceInfo extends RecordData {
    public FitDeviceInfo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 23) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 23 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getManufacturer() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getSerialNumber() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Integer getProduct() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSoftwareVersion() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
