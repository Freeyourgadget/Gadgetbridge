package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import java.time.DayOfWeek;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi.AQI_LEVELS;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition.Condition;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitWeather extends RecordData {
    public FitWeather(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 128) {
            throw new IllegalArgumentException("FitWeather expects global messages of " + 128 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getWeatherReport() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getTemperature() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Condition getCondition() {
        return (Condition) getFieldByNumber(2);
    }

    @Nullable
    public Integer getWindDirection() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getWindSpeed() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getPrecipitationProbability() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getTemperatureFeelsLike() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getRelativeHumidity() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public String getLocation() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public Long getObservedAtTime() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public Long getObservedLocationLat() {
        return (Long) getFieldByNumber(10);
    }

    @Nullable
    public Long getObservedLocationLong() {
        return (Long) getFieldByNumber(11);
    }

    @Nullable
    public DayOfWeek getDayOfWeek() {
        return (DayOfWeek) getFieldByNumber(12);
    }

    @Nullable
    public Integer getHighTemperature() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getLowTemperature() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Integer getDewPoint() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public Float getUvIndex() {
        return (Float) getFieldByNumber(16);
    }

    @Nullable
    public AQI_LEVELS getAirQuality() {
        return (AQI_LEVELS) getFieldByNumber(17);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
