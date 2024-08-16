package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitLap extends RecordData {
    public FitLap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 19) {
            throw new IllegalArgumentException("FitLap expects global messages of " + 19 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getStartLat() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartLong() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Double getEndLat() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Double getEndLong() {
        return (Double) getFieldByNumber(6);
    }

    @Nullable
    public Long getTotalElapsedTime() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Long getTotalTimerTime() {
        return (Long) getFieldByNumber(8);
    }

    @Nullable
    public Long getTotalDistance() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
