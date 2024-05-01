package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitDeviceSettings extends RecordData {
    public FitDeviceSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 2) {
            throw new IllegalArgumentException("FitDeviceSettings expects global messages of " + 2 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getActiveTimeZone() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getUtcOffset() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Long getTimeOffset() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Integer getTimeMode() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getTimeZoneOffset() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getBacklightMode() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getActivityTrackerEnabled() {
        return (Integer) getFieldByNumber(36);
    }

    @Nullable
    public Integer getMoveAlertEnabled() {
        return (Integer) getFieldByNumber(46);
    }

    @Nullable
    public Integer getDateMode() {
        return (Integer) getFieldByNumber(47);
    }

    @Nullable
    public Integer getDisplayOrientation() {
        return (Integer) getFieldByNumber(55);
    }

    @Nullable
    public Integer getMountingSide() {
        return (Integer) getFieldByNumber(56);
    }

    @Nullable
    public Integer getDefaultPage() {
        return (Integer) getFieldByNumber(57);
    }

    @Nullable
    public Integer getAutosyncMinSteps() {
        return (Integer) getFieldByNumber(58);
    }

    @Nullable
    public Integer getAutosyncMinTime() {
        return (Integer) getFieldByNumber(59);
    }

    @Nullable
    public Integer getBleAutoUploadEnabled() {
        return (Integer) getFieldByNumber(86);
    }

    @Nullable
    public Long getAutoActivityDetect() {
        return (Long) getFieldByNumber(90);
    }
}
