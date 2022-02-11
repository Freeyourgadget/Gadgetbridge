package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol;

import androidx.annotation.NonNull;

public class CSCMeasurement {
    public final boolean wheelRevolutionsDataAvailable, crankRevolutionsDataAvailable;
    public final int wheelRevolutions, crankRevolutions;
    public final int lastWheelRevolutionTime, lastCrankRevolutionTime;

    public CSCMeasurement(int wheelRevolutions, int lastWheelRevolutionTime, boolean wheelRevolutionsDataAvailable, int crankRevolutions, int lastCrankRevolutionTime, boolean crankRevolutionsDataAvailable) {
        this.wheelRevolutionsDataAvailable = wheelRevolutionsDataAvailable;
        this.crankRevolutionsDataAvailable = crankRevolutionsDataAvailable;
        this.wheelRevolutions = wheelRevolutions;
        this.crankRevolutions = crankRevolutions;
        this.lastWheelRevolutionTime = lastWheelRevolutionTime;
        this.lastCrankRevolutionTime = lastCrankRevolutionTime;
    }

    public CSCMeasurement(int wheelRevolutions, int lastWheelRevolutionTime) {
        this(wheelRevolutions, lastWheelRevolutionTime, true, 0, 0, false);
    }

    public CSCMeasurement(int wheelRevolutions, int lastWheelRevolutionTime, int crankRevolutions, int lastCrankRevolutionTime) {
        this(wheelRevolutions, lastWheelRevolutionTime, true, crankRevolutions, lastCrankRevolutionTime, true);
    }

    @NonNull
    @Override
    public String toString() {
        String result = "CSC Measurement ";
        if(wheelRevolutionsDataAvailable){
            result += String.format("%d wheel revolutions, last wheel event %d", wheelRevolutions, lastWheelRevolutionTime);
        }
        if(crankRevolutionsDataAvailable){
            result += String.format("%d crank revolutions, last wheel event %d", crankRevolutions, lastCrankRevolutionTime);
        }

        return result;
    }
}
