package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data;

import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCMeasurement;

public class AverageCalculator {
    private final DataAccumulator accumulator;

    public AverageCalculator(DataAccumulator accumulator) {
        this.accumulator = accumulator;
    }

    public double calculateAverageRevolutionsPerSecond(long timeSpan){
        return calculateAverageRevolutionsPerSecond(accumulator.getMeasurementsInTimeSpan(timeSpan));
    }

    public static double calculateAverageRevolutionsPerSecond(CSCMeasurement[] measurements){
        CSCMeasurement lastMeasurement = measurements[0];
        CSCMeasurement firstMeasurement = measurements[measurements.length - 1];

        long timeUnitsDelta = lastMeasurement.lastWheelRevolutionTime - firstMeasurement.lastWheelRevolutionTime;
        if(timeUnitsDelta == 0){
            return 0;
        }
        while(timeUnitsDelta < 0){
            timeUnitsDelta += 1024 * 64;
        }
        int rotationsDelta = lastMeasurement.wheelRevolutions - firstMeasurement.wheelRevolutions;
        double factor = 1024;
        double rotationsPerTimeUnit = (double)rotationsDelta / timeUnitsDelta;
        double rotationsPerSecond = rotationsPerTimeUnit * factor;

        return rotationsPerSecond;
    }
}
