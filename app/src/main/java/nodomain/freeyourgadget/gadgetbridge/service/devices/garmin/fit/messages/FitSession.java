package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSession extends RecordData {
    public FitSession(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 18) {
            throw new IllegalArgumentException("FitSession expects global messages of " + 18 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getEvent() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getEventType() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Double getStartLatitude() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartLongitude() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(6);
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
    public Long getTotalSteps() {
        return (Long) getFieldByNumber(10);
    }

    @Nullable
    public Integer getTotalCalories() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getAverageHeartRate() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getAverageCadence() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Integer getMaxCadence() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Integer getFirstLapIndex() {
        return (Integer) getFieldByNumber(25);
    }

    @Nullable
    public Integer getNumLaps() {
        return (Integer) getFieldByNumber(26);
    }

    @Nullable
    public Double getNecLatitude() {
        return (Double) getFieldByNumber(29);
    }

    @Nullable
    public Double getNecLongitude() {
        return (Double) getFieldByNumber(30);
    }

    @Nullable
    public Double getSwcLatitude() {
        return (Double) getFieldByNumber(31);
    }

    @Nullable
    public Double getSwcLongitude() {
        return (Double) getFieldByNumber(32);
    }

    @Nullable
    public Double getEndLatitude() {
        return (Double) getFieldByNumber(38);
    }

    @Nullable
    public Double getEndLongitude() {
        return (Double) getFieldByNumber(39);
    }

    @Nullable
    public String getSportProfileName() {
        return (String) getFieldByNumber(110);
    }

    @Nullable
    public Double getEnhancedAvgSpeed() {
        return (Double) getFieldByNumber(124);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return (Double) getFieldByNumber(125);
    }

    @Nullable
    public Integer getEstimatedSweatLoss() {
        return (Integer) getFieldByNumber(178);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
