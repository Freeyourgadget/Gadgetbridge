package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage.Language;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitUserProfile extends RecordData {
    public FitUserProfile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 3) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 3 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getFriendlyName() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public Integer getGender() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getAge() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHeight() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getWeight() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Language getLanguage() {
        return (Language) getFieldByNumber(5);
    }

    @Nullable
    public Type getElevSetting() {
        return (Type) getFieldByNumber(6);
    }

    @Nullable
    public Type getWeightSetting() {
        return (Type) getFieldByNumber(7);
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getDefaultMaxBikingHeartRate() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getDefaultMaxHeartRate() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getHrSetting() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Type getSpeedSetting() {
        return (Type) getFieldByNumber(13);
    }

    @Nullable
    public Type getDistSetting() {
        return (Type) getFieldByNumber(14);
    }

    @Nullable
    public Integer getPowerSetting() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getActivityClass() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getPositionSetting() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Type getTemperatureSetting() {
        return (Type) getFieldByNumber(21);
    }

    @Nullable
    public Long getWakeTime() {
        return (Long) getFieldByNumber(28);
    }

    @Nullable
    public Long getSleepTime() {
        return (Long) getFieldByNumber(29);
    }

    @Nullable
    public Type getHeightSetting() {
        return (Type) getFieldByNumber(30);
    }

    @Nullable
    public Integer getUserRunningStepLength() {
        return (Integer) getFieldByNumber(31);
    }

    @Nullable
    public Integer getUserWalkingStepLength() {
        return (Integer) getFieldByNumber(32);
    }
}
