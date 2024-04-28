package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionDayOfWeek;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTemperature;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;

public class FieldDefinitionFactory {
    public static FieldDefinition create(int localNumber, int size, FIELD field, BaseType baseType, String name, int scale, int offset) {
        if (null == field) {
            return new FieldDefinition(localNumber, size, baseType, name, scale, offset);
        }
        switch (field) {
            case ALARM:
                return new FieldDefinitionAlarm(localNumber, size, baseType, name);
            case DAY_OF_WEEK:
                return new FieldDefinitionDayOfWeek(localNumber, size, baseType, name);
            case FILE_TYPE:
                return new FieldDefinitionFileType(localNumber, size, baseType, name);
            case GOAL_SOURCE:
                return new FieldDefinitionGoalSource(localNumber, size, baseType, name);
            case GOAL_TYPE:
                return new FieldDefinitionGoalType(localNumber, size, baseType, name);
            case MEASUREMENT_SYSTEM:
                return new FieldDefinitionMeasurementSystem(localNumber, size, baseType, name);
            case TEMPERATURE:
                return new FieldDefinitionTemperature(localNumber, size, baseType, name);
            case TIMESTAMP:
                return new FieldDefinitionTimestamp(localNumber, size, baseType, name);
            case WEATHER_CONDITION:
                return new FieldDefinitionWeatherCondition(localNumber, size, baseType, name);
            case LANGUAGE:
                return new FieldDefinitionLanguage(localNumber, size, baseType, name);
            case SLEEP_STAGE:
                return new FieldDefinitionSleepStage(localNumber, size, baseType, name);
            default:
                return new FieldDefinition(localNumber, size, baseType, name);
        }
    }

    public enum FIELD {
        ALARM,
        DAY_OF_WEEK,
        FILE_TYPE,
        GOAL_SOURCE,
        GOAL_TYPE,
        MEASUREMENT_SYSTEM,
        TEMPERATURE,
        TIMESTAMP,
        WEATHER_CONDITION,
        LANGUAGE,
        SLEEP_STAGE,
    }
}
