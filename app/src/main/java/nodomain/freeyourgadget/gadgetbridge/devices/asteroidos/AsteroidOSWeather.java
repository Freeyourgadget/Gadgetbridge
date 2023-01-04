package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class AsteroidOSWeather {
    public class Day {
        public int minTemp;
        public int maxTemp;
        public int condition;
        public Day(WeatherSpec.Forecast forecast) {
            minTemp = forecast.minTemp;
            maxTemp = forecast.maxTemp;
            condition = forecast.conditionCode;
        }
        public Day(WeatherSpec spec) {
            minTemp = spec.todayMinTemp;
            maxTemp = spec.todayMaxTemp;
            condition = spec.currentConditionCode;
        }
    }
    public Day[] days = new Day[5];
    public String cityName = "";


    public AsteroidOSWeather(WeatherSpec spec) {
        cityName = spec.location;
        days[0] = new Day(spec);
        for (int i = 1; i < 5 && i < spec.forecasts.size(); i++) {
            days[i] = new Day(spec.forecasts.get(i));
        }
    }

    public byte[] getCityName() {
        return cityName.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getWeatherConditions() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.condition >> 8));
            stream.write((byte) (day.condition));
        }
        return stream.toByteArray();
    }

    public byte[] getMinTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.minTemp >> 8));
            stream.write((byte) (day.minTemp));
        }
        return stream.toByteArray();
    }

    public byte[] getMaxTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.maxTemp >> 8));
            stream.write((byte) (day.maxTemp));
        }
        return stream.toByteArray();
    }
}
