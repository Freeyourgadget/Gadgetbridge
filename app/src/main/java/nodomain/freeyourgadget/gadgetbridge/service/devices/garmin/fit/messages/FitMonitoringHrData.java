package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitMonitoringHrData extends RecordData {
    public FitMonitoringHrData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 211) {
            throw new IllegalArgumentException("FitMonitoringHrData expects global messages of " + 211 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getCurrentDayRestingHeartRate() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
