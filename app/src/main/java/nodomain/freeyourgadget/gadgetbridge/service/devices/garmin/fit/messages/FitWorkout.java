package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitWorkout extends RecordData {
    public FitWorkout(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 26) {
            throw new IllegalArgumentException("FitWorkout expects global messages of " + 26 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getCapabilities() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getNumValidSteps() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public String getNotes() {
        return (String) getFieldByNumber(17);
    }
}
