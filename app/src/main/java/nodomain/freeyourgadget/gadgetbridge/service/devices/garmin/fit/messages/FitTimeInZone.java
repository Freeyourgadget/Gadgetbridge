package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitTimeInZone extends RecordData {
    public FitTimeInZone(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 216) {
            throw new IllegalArgumentException("FitTimeInZone expects global messages of " + 216 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getReferenceMessage() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getReferenceIndex() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Double[] getTimeInZone() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(2);
        final Double[] ret = new Double[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Double) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer[] getHrZoneHighBoundary() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(6);
        final Integer[] ret = new Integer[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Integer) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getHrCalcType() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getThresholdHeartRate() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
