package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionWeatherAqi extends FieldDefinition {

    public FieldDefinitionWeatherAqi(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int idx = (int) baseType.decode(byteBuffer, scale, offset);
        return AQI_LEVELS.values()[idx];
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof AQI_LEVELS) {
            baseType.encode(byteBuffer, ((AQI_LEVELS) o).ordinal(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, aqiAbsoluteValueToIndex((int) o), scale, offset);
    }

    private int aqiAbsoluteValueToIndex(int rawValue) { //see https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/domain/weather/index/PollutantIndex.kt#L38
        if (rawValue == -1) {
            return rawValue; //invalid
        }
        if (rawValue < 20) {
            return AQI_LEVELS.GOOD.ordinal();
        } else if (rawValue < 50) {
            return AQI_LEVELS.MODERATE.ordinal();
        } else if (rawValue < 100) {
            return AQI_LEVELS.UNHEALTHY_SENSITIVE.ordinal();
        } else if (rawValue < 150) {
            return AQI_LEVELS.UNHEALTHY.ordinal();
        } else if (rawValue < 250) {
            return AQI_LEVELS.VERY_UNHEALTHY.ordinal();
        } else {
            return AQI_LEVELS.HAZARDOUS.ordinal();
        }
    }

    public enum AQI_LEVELS {
        GOOD,
        MODERATE,
        UNHEALTHY_SENSITIVE,
        UNHEALTHY,
        VERY_UNHEALTHY,
        HAZARDOUS,
    }
}
