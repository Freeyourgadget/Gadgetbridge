package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitConnectivity extends RecordData {
    public FitConnectivity(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 127) {
            throw new IllegalArgumentException("FitConnectivity expects global messages of " + 127 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getBluetoothEnabled() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(3);
    }

    @Nullable
    public Integer getLiveTrackingEnabled() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getWeatherConditionsEnabled() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getWeatherAlertsEnabled() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getAutoActivityUploadEnabled() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getCourseDownloadEnabled() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getWorkoutDownloadEnabled() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getGpsEphemerisDownloadEnabled() {
        return (Integer) getFieldByNumber(10);
    }
}
