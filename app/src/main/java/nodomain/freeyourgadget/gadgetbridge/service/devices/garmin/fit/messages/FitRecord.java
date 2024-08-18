package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitRecord extends RecordData {
    public FitRecord(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 20) {
            throw new IllegalArgumentException("FitRecord expects global messages of " + 20 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getLatitude() {
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Double getLongitude() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Float getAltitude() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHeartRate() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Double getDistance() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Float getSpeed() {
        return (Float) getFieldByNumber(6);
    }

    @Nullable
    public Long getEnhancedSpeed() {
        return (Long) getFieldByNumber(73);
    }

    @Nullable
    public Double getEnhancedAltitude() {
        return (Double) getFieldByNumber(78);
    }

    @Nullable
    public Integer getWristHeartRate() {
        return (Integer) getFieldByNumber(136);
    }

    @Nullable
    public Integer getBodyBattery() {
        return (Integer) getFieldByNumber(143);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    // manual changes below

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getLatitude() != null && getLongitude() != null) {
            activityPoint.setLocation(new GPSCoordinate(
                    getLongitude(),
                    getLatitude(),
                    getEnhancedAltitude() != null ? getEnhancedAltitude() : GPSCoordinate.UNKNOWN_ALTITUDE
            ));
        }
        if (getHeartRate() != null) {
            activityPoint.setHeartRate(getHeartRate());
        }
        if (getEnhancedSpeed() != null) {
            activityPoint.setSpeed(getEnhancedSpeed());
        }
        return activityPoint;
    }
}
