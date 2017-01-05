package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;


public class HPlusDataRecordDaySlot extends HPlusDataRecord {

    /**
     * The device reports data aggregated in slots.
     * There are 144 slots in a given day, summarizing 10 minutes of data
     * Integer with the slot number from 0 to 143
     */
    public int slot;

    /**
     * Number of steps
     */
    public int steps;

    /**
     * Number of seconds without activity (TBC)
     */
    public int secondsInactive;

    /**
     * Average Heart Rate in Beats Per Minute
     */
    public int heartRate;

    public HPlusDataRecordDaySlot(byte[] data) {
        super(data);

        int a = (data[4] & 0xFF) * 256 + (data[5] & 0xFF);
        if (a >= 144) {
            throw new IllegalArgumentException("Invalid Slot Number");
        }

        slot = a;
        heartRate = data[1] & 0xFF;

        if(heartRate == 255)
            heartRate = ActivityKind.TYPE_NOT_MEASURED;

        steps = (data[2] & 0xFF) * 256 + data[3] & 0xFF;

        //?? data[6];
        secondsInactive = data[7] & 0xFF; // ?

        int now = (int) (GregorianCalendar.getInstance().getTimeInMillis() / (3600 * 24 * 1000L));
        timestamp = now * 3600 * 24 + (slot / 6 * 3600 + slot % 6 * 10);
    }

}
