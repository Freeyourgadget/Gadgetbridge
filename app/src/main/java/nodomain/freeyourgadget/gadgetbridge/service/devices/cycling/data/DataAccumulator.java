package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.data;

import androidx.collection.CircularArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol.CSCMeasurement;

public class DataAccumulator {
    final int MAX_QUEUE_ELEMENTS = 10;
    final int TIME_UNITS_ROLLOVER = 64 * 1024;

    LimitedQueue<CSCMeasurement> measurements = new LimitedQueue<>(MAX_QUEUE_ELEMENTS);

    public void captureCSCMeasurement(CSCMeasurement measurement){
        measurements.add(measurement);
    }

    public Queue<CSCMeasurement> getMeasurements(){
        return measurements;
    }

    public CSCMeasurement[] getMeasurementsInTimeSpan(long timespanMillis) throws NotEnoughMeasurementsException{
        int listSize = 1;
        int accumulatedTimeDelta = 0;
        for(int i = measurements.size() - 1; i > 0; i--){
            CSCMeasurement previousMeasurement;
            CSCMeasurement currentMeasurement;
            try{
                previousMeasurement = measurements.get(i - 1);
                currentMeasurement = measurements.get(i);
            }catch (IndexOutOfBoundsException e){
                throw new NotEnoughMeasurementsException();
            }
            long localTimeDelta = currentMeasurement.arrivalTimestamp - previousMeasurement.arrivalTimestamp;
            accumulatedTimeDelta += localTimeDelta;

            listSize++;

            if(accumulatedTimeDelta >= timespanMillis){
                break;
            }
        }
        if(accumulatedTimeDelta < timespanMillis){
            throw new NotEnoughMeasurementsException();
        }

        CSCMeasurement[] array = new CSCMeasurement[listSize];
        for(int i = 0; i < listSize; i++){
            array[i] = measurements.get(measurements.size() - 1 - i);
        }
        return array;
    }
}
