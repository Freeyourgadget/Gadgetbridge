package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor;

import junit.framework.TestCase;

import org.checkerframework.checker.units.qual.C;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Queue;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;

import android.provider.ContactsContract;

import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.DataAccumulator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data.NotEnoughMeasurementsException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCMeasurement;

public class DataAccumulatorTest {
    @BeforeClass
    public static void setupSuite(){

    }

    @Test
    public void testAccumulateData(){
        DataAccumulator accumulator = new DataAccumulator();
        CSCMeasurement[] testData = getTestData();
        enrichAccumulator(accumulator, testData);

        Queue<CSCMeasurement> capturedMeasurements = accumulator.getMeasurements();
        Assert.assertEquals(capturedMeasurements.size(), 10);
    }

    @Test
    public void testGetTimeRange(){
        DataAccumulator accumulator = new DataAccumulator();
        CSCMeasurement[] testData = getTestData();
        enrichAccumulator(accumulator, testData);

        assertEquals(accumulator.getMeasurementsInTimeSpan(1000).length, 2);
        assertEquals(accumulator.getMeasurementsInTimeSpan(1024).length, 3);
        assertEquals(accumulator.getMeasurementsInTimeSpan(2000).length, 3);
        assertEquals(accumulator.getMeasurementsInTimeSpan(2001).length, 4);
        assertEquals(accumulator.getMeasurementsInTimeSpan(7000).length, 8);
    }

    @Test
    public void testThrowsNotEnoughDataExceptioin(){
        DataAccumulator accumulator = new DataAccumulator();
        try{
            accumulator.getMeasurementsInTimeSpan(1);
            fail();
        }catch (NotEnoughMeasurementsException e){
        }
        accumulator.captureCSCMeasurement(new CSCMeasurement(0, 0, 0));
        try{
            accumulator.getMeasurementsInTimeSpan(1);
            fail();
        }catch (NotEnoughMeasurementsException e){
        }
        accumulator.captureCSCMeasurement(new CSCMeasurement(1000, 1, 1000));
        try{
            accumulator.getMeasurementsInTimeSpan(1001);
            fail();
        }catch (NotEnoughMeasurementsException e){
        }
        accumulator.captureCSCMeasurement(new CSCMeasurement(2000, 3, 2000));
        try{
            accumulator.getMeasurementsInTimeSpan(2001);
            fail();
        }catch (NotEnoughMeasurementsException e){
        }
        accumulator.captureCSCMeasurement(new CSCMeasurement(3000, 6, 3000));
        accumulator.getMeasurementsInTimeSpan(3000);
    }

    private void enrichAccumulator(DataAccumulator accumulator, CSCMeasurement[] measurements){
        for(int i = 0; i < measurements.length; i++){
            accumulator.captureCSCMeasurement(measurements[i]);
        }
    }

    private CSCMeasurement[] getTestData(){
        int count = 20;
        CSCMeasurement[] testData = new CSCMeasurement[count];
        for(int i = 0; i < count; i++){
            testData[i] = new CSCMeasurement(i * 1000, i * 5 + i, (50 * 1024 + i * 1024) % 65536);
        }

        return testData;
    }
}
