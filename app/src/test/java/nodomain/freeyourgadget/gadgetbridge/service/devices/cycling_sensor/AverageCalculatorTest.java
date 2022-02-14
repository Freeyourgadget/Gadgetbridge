package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor;

import junit.framework.TestCase;

import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.AverageCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.DataAccumulator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCMeasurement;

public class AverageCalculatorTest extends TestCase {
    public void testCalculateAverageMoving(){
        DataAccumulator accumulator = new DataAccumulator();
        accumulator.captureCSCMeasurement(new CSCMeasurement(0 * 1000, 0 * 1024, 0 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(1 * 1000, 1 * 1024, 1 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(2 * 1000, 2 * 1024, 2 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(3 * 1000, 3 * 1024, 3 * 1024));
        AverageCalculator calculator = new AverageCalculator(accumulator);
        assertEquals(1024.0, calculator.calculateAverageRevolutionsPerSecond(3000));
    }

    public void testCalculateAverageStanding(){
        DataAccumulator accumulator = new DataAccumulator();
        accumulator.captureCSCMeasurement(new CSCMeasurement(0 * 1000, 0 * 1024, 0 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(1 * 1000, 1 * 1024, 1 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(2 * 1000, 2 * 1024, 2 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(3 * 1000, 3 * 1024, 3 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(4 * 1000, 3 * 1024, 3 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(5 * 1000, 3 * 1024, 3 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(6 * 1000, 3 * 1024, 3 * 1024));
        AverageCalculator calculator = new AverageCalculator(accumulator);
        assertEquals(0.0, calculator.calculateAverageRevolutionsPerSecond(1000));
        assertEquals(0.0, calculator.calculateAverageRevolutionsPerSecond(2000));
        assertEquals(0.0, calculator.calculateAverageRevolutionsPerSecond(3000));
    }

    public void testCalculateAverageAccelerating(){
        DataAccumulator accumulator = new DataAccumulator();
        accumulator.captureCSCMeasurement(new CSCMeasurement(0 * 1000, 0 * 1024, 0 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(1 * 1000, 1 * 1024, 1 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(2 * 1000, 3 * 1024, 2 * 1024));
        AverageCalculator calculator = new AverageCalculator(accumulator);
        assertEquals(1536.0, calculator.calculateAverageRevolutionsPerSecond(2000));
    }

    public void testCalculateAverageBreaking(){
        DataAccumulator accumulator = new DataAccumulator();
        accumulator.captureCSCMeasurement(new CSCMeasurement(0 * 1000, 0 * 1024, 0 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(1 * 1000, 1 * 1024, 1 * 1024));
        accumulator.captureCSCMeasurement(new CSCMeasurement(2 * 1000, 2 * 1024, 2 * 1024));
        AverageCalculator calculator = new AverageCalculator(accumulator);
        assertEquals(1024.0, calculator.calculateAverageRevolutionsPerSecond(2000));
        accumulator.captureCSCMeasurement(new CSCMeasurement(3 * 1000, 2 * 1024, 2 * 1024));
        assertEquals(1024.0, calculator.calculateAverageRevolutionsPerSecond(2000));
        accumulator.captureCSCMeasurement(new CSCMeasurement(4 * 1000, 2 * 1024, 2 * 1024));
        assertEquals(0.0, calculator.calculateAverageRevolutionsPerSecond(2000));
    }
}
