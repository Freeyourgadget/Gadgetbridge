package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.GBToStringBuilder;

public class BangleJSActivityPoint {
    private static final Logger LOG = LoggerFactory.getLogger(BangleJSActivityPoint.class);

    private final GPSCoordinate location;
    private final long time;
    private final int heartRate;
    private final int hrConfidence;
    private final String hrSource;
    private final int steps;
    private final int batteryPercentage;
    private final double batteryVoltage;
    private final boolean charging;
    private final double barometerTemperature;
    private final double barometerPressure;
    private final double barometerAltitude;

    public BangleJSActivityPoint(final long time,
                                 final GPSCoordinate location,
                                 final int heartRate,
                                 final int hrConfidence,
                                 final String hrSource,
                                 final int steps,
                                 final int batteryPercentage,
                                 final double batteryVoltage,
                                 final boolean charging,
                                 final double barometerTemperature,
                                 final double barometerPressure,
                                 final double barometerAltitude) {
        this.time = time;
        this.location = location;
        this.heartRate = heartRate;
        this.hrConfidence = hrConfidence;
        this.hrSource = hrSource;
        this.steps = steps;
        this.batteryPercentage = batteryPercentage;
        this.batteryVoltage = batteryVoltage;
        this.charging = charging;
        this.barometerTemperature = barometerTemperature;
        this.barometerPressure = barometerPressure;
        this.barometerAltitude = barometerAltitude;
    }

    public long getTime() {
        return time;
    }

    @Nullable
    public GPSCoordinate getLocation() {
        return location;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public int getHrConfidence() {
        return hrConfidence;
    }

    public String getHrSource() {
        return hrSource;
    }

    public int getSteps() {
        return steps;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public boolean isCharging() {
        return charging;
    }

    public double getBarometerTemperature() {
        return barometerTemperature;
    }

    public double getBarometerPressure() {
        return barometerPressure;
    }

    public double getBarometerAltitude() {
        return barometerAltitude;
    }

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(new Date(time));
        if (heartRate > 0) {
            activityPoint.setHeartRate(heartRate);
        }
        if (location != null) {
            activityPoint.setLocation(location);
        }
        return activityPoint;
    }

    @NonNull
    @Override
    public String toString() {
        final GBToStringBuilder tsb = new GBToStringBuilder(this);
        tsb.append("location", location);
        tsb.append("time", time);
        tsb.append("heartRate", heartRate);
        tsb.append("hrConfidence", hrConfidence);
        tsb.append("hrSource", hrSource);
        tsb.append("steps", steps);
        tsb.append("batteryPercentage", batteryPercentage);
        tsb.append("batteryVoltage", batteryVoltage);
        tsb.append("charging", charging);
        tsb.append("barometerTemperature", barometerTemperature);
        tsb.append("barometerPressure", barometerPressure);
        tsb.append("barometerAltitude", barometerAltitude);
        return tsb.toString();
    }

    public static List<BangleJSActivityPoint> fromCsv(final File inputFile) {
        final List<BangleJSActivityPoint> points = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            final List<String> header = Arrays.asList(reader.readLine().split(","));
            String line;
            while ((line = reader.readLine()) != null) {
                points.add(BangleJSActivityPoint.fromCsvLine(header, line));
            }
        } catch (final IOException e) {
            LOG.error("Failed to read {}", inputFile);
            return null;
        }

        return points;
    }

    /**
     * This parses all the standard fields from the <a href="https://github.com/espruino/BangleApps/blob/master/apps/recorder/widget.js">recorder</a>.
     * Some apps such as bthrm add extra fields or modify others. We attempt to gracefully handle other formats (eg. source being "int" or "bthrm").
     */
    @Nullable
    @VisibleForTesting
    public static BangleJSActivityPoint fromCsvLine(final List<String> header, final String csvLine) {
        final String[] split = csvLine.trim().replace(",", ", ").split(",");
        if (split.length != header.size()) {
            LOG.error("csv line {} length {} differs from header {} length {}", csvLine, split.length, header, header.size());
            return null;
        }
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].strip();
        }

        final int idxTime = header.indexOf("Time");
        final int idxLatitude = header.indexOf("Latitude");
        final int idxLongitude = header.indexOf("Longitude");
        final int idxAltitude = header.indexOf("Altitude");
        final int idxHeartrate = header.indexOf("Heartrate");
        final int idxConfidence = header.indexOf("Confidence");
        final int idxSource = header.indexOf("Source");
        final int idxSteps = header.indexOf("Steps");
        final int idxBatteryPercentage = header.indexOf("Battery Percentage");
        final int idxBatteryVoltage = header.indexOf("Battery Voltage");
        final int idxCharging = header.indexOf("Charging");
        final int idxBarometerTemperature = header.indexOf("Barometer Temperature");
        final int idxBarometerPressure = header.indexOf("Barometer Pressure");
        final int idxBarometerAltitude = header.indexOf("Barometer Altitude");

        final long time = idxTime >= 0 && StringUtils.isNotBlank(split[idxTime]) ? ((long) (Double.parseDouble(split[idxTime]) * 1000L)) : 0L;

        try {
            final GPSCoordinate location;
            if (idxLatitude >= 0 && StringUtils.isNotBlank(split[idxLatitude]) && idxLongitude >= 0 && StringUtils.isNotBlank(split[idxLongitude])) {
                final double latitude = Double.parseDouble(split[idxLatitude]);
                final double longitude = Double.parseDouble(split[idxLongitude]);
                final double altitude;
                if (idxAltitude >= 0 && StringUtils.isNotBlank(split[idxAltitude])) {
                    altitude = Double.parseDouble(split[idxAltitude]);
                } else {
                    altitude = GPSCoordinate.UNKNOWN_ALTITUDE;
                }
                location = new GPSCoordinate(longitude, latitude, altitude);
            } else {
                location = null;
            }
            final int heartRate = idxHeartrate >= 0 && StringUtils.isNotBlank(split[idxHeartrate]) ? (int) Math.round(Double.parseDouble(split[idxHeartrate])) : 0;
            final int confidence = idxConfidence >= 0 && StringUtils.isNotBlank(split[idxConfidence]) ? Integer.parseInt(split[idxConfidence]) : 0;
            final String source = idxSource >= 0 && StringUtils.isNotBlank(split[idxSource]) ? split[idxSource] : "";
            final int steps = idxSteps >= 0 && StringUtils.isNotBlank(split[idxSteps]) ? Integer.parseInt(split[idxSteps]) : 0;
            final int batteryPercentage = idxBatteryPercentage >= 0 && StringUtils.isNotBlank(split[idxBatteryPercentage]) ? Integer.parseInt(split[idxBatteryPercentage]) : -1;
            final double batteryVoltage = idxBatteryVoltage >= 0 && StringUtils.isNotBlank(split[idxBatteryVoltage]) ? Double.parseDouble(split[idxBatteryVoltage]) : -1;
            final boolean charging = idxCharging >= 0 && StringUtils.isNotBlank(split[idxCharging]) && Boolean.parseBoolean(split[idxCharging]);
            final double barometerTemperature = idxBarometerTemperature >= 0 && StringUtils.isNotBlank(split[idxBarometerTemperature]) ? Double.parseDouble(split[idxBarometerTemperature]) : 0;
            final double barometerPressure = idxBarometerPressure >= 0 && StringUtils.isNotBlank(split[idxBarometerPressure]) ? Double.parseDouble(split[idxBarometerPressure]) : 0;
            final double barometerAltitude = idxBarometerAltitude >= 0 && StringUtils.isNotBlank(split[idxBarometerAltitude]) ? Double.parseDouble(split[idxBarometerAltitude]) : GPSCoordinate.UNKNOWN_ALTITUDE;

            return new BangleJSActivityPoint(
                    time,
                    location,
                    heartRate,
                    confidence,
                    source,
                    steps,
                    batteryPercentage,
                    batteryVoltage,
                    charging,
                    barometerTemperature,
                    barometerPressure,
                    barometerAltitude
            );
        } catch (final Exception e) {
            LOG.error("failed to parse '{}'", csvLine, e);
            // Salvage the time at least
            return new BangleJSActivityPoint(time, null, 0, 0, "", 0, -1, -1, false, 0, 0, 0);
        }
    }
}
