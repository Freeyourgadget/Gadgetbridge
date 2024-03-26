package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.Nullable;

import java.nio.ByteOrder;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionDayOfWeek;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTemperature;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;

public enum GlobalDefinitionsEnum {
    TODAY_WEATHER_CONDITIONS(MesgType.TODAY_WEATHER_CONDITIONS, new RecordDefinition(
            new RecordHeader(true, false, MesgType.TODAY_WEATHER_CONDITIONS, null),
            ByteOrder.BIG_ENDIAN,
            MesgType.TODAY_WEATHER_CONDITIONS,
            Arrays.asList(new FieldDefinition(0, 1, BaseType.ENUM, "weather_report"),
                    new FieldDefinitionTimestamp(253, 4, BaseType.UINT32, "timestamp"),
                    new FieldDefinitionTimestamp(9, 4, BaseType.UINT32, "observed_at_time"),
                    new FieldDefinitionTemperature(1, 1, BaseType.SINT8, "temperature"),
                    new FieldDefinitionTemperature(14, 1, BaseType.SINT8, "low_temperature"),
                    new FieldDefinitionTemperature(13, 1, BaseType.SINT8, "high_temperature"),
                    new FieldDefinitionWeatherCondition(2, 1, BaseType.ENUM, "condition"),
                    new FieldDefinition(3, 2, BaseType.UINT16, "wind_direction"),
                    new FieldDefinition(5, 1, BaseType.UINT8, "precipitation_probability"),
                    new FieldDefinition(4, 2, BaseType.UINT16, "wind_speed", 298, 0),
                    new FieldDefinitionTemperature(6, 1, BaseType.SINT8, "temperature_feels_like"),
                    new FieldDefinition(7, 1, BaseType.UINT8, "relative_humidity"),
                    new FieldDefinition(10, 4, BaseType.SINT32, "observed_location_lat"),
                    new FieldDefinition(11, 4, BaseType.SINT32, "observed_location_long"),
                    new FieldDefinition(8, 15, BaseType.STRING, "location")))),

    HOURLY_WEATHER_FORECAST(MesgType.HOURLY_WEATHER_FORECAST, new RecordDefinition(
            new RecordHeader(true, false, MesgType.HOURLY_WEATHER_FORECAST, null),
            ByteOrder.BIG_ENDIAN,
            MesgType.HOURLY_WEATHER_FORECAST,
            Arrays.asList(new FieldDefinition(0, 1, BaseType.ENUM, "weather_report"),
                    new FieldDefinitionTimestamp(253, 4, BaseType.UINT32, "timestamp"),
                    new FieldDefinitionTemperature(1, 1, BaseType.SINT8, "temperature"),
                    new FieldDefinitionWeatherCondition(2, 1, BaseType.ENUM, "condition"),
                    new FieldDefinition(3, 2, BaseType.UINT16, "wind_direction"),
                    new FieldDefinition(4, 2, BaseType.UINT16, "wind_speed", 298, 0),
                    new FieldDefinition(5, 1, BaseType.UINT8, "precipitation_probability"),
                    new FieldDefinition(7, 1, BaseType.UINT8, "relative_humidity"),
                    new FieldDefinition(15, 1, BaseType.SINT8, "dew_point"),
                    new FieldDefinition(16, 4, BaseType.FLOAT32, "uv_index"),
                    new FieldDefinition(17, 1, BaseType.ENUM, "air_quality")))),

    DAILY_WEATHER_FORECAST(MesgType.DAILY_WEATHER_FORECAST, new RecordDefinition(
            new RecordHeader(true, false, MesgType.DAILY_WEATHER_FORECAST, null),
            ByteOrder.BIG_ENDIAN,
            MesgType.DAILY_WEATHER_FORECAST,
            Arrays.asList(new FieldDefinition(0, 1, BaseType.ENUM, "weather_report"),
                    new FieldDefinitionTimestamp(253, 4, BaseType.UINT32, "timestamp"),
                    new FieldDefinitionTemperature(14, 1, BaseType.SINT8, "low_temperature"),
                    new FieldDefinitionTemperature(13, 1, BaseType.SINT8, "high_temperature"),
                    new FieldDefinitionWeatherCondition(2, 1, BaseType.ENUM, "condition"),
                    new FieldDefinition(5, 1, BaseType.UINT8, "precipitation_probability"),
                    new FieldDefinitionDayOfWeek(12, 1, BaseType.ENUM, "day_of_week")))),
    ;

    private final MesgType mesgType;
    private final RecordDefinition recordDefinition;

    GlobalDefinitionsEnum(MesgType mesgType, RecordDefinition recordDefinition) {
        this.mesgType = mesgType;
        this.recordDefinition = recordDefinition;
    }

    @Nullable
    public static RecordDefinition getRecordDefinitionfromMesgType(final MesgType code) {
        for (final GlobalDefinitionsEnum globalDefinitionsEnum : GlobalDefinitionsEnum.values()) {
            if (globalDefinitionsEnum.getMesgType().getIdentifier() == code.getIdentifier()) {
                return globalDefinitionsEnum.getRecordDefinition();
            }
        }
        return null;
    }

    public MesgType getMesgType() {
        return mesgType;
    }

    public RecordDefinition getRecordDefinition() {
        return recordDefinition;
    }


}
