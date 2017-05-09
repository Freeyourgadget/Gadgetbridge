/*  Copyright (C) 2017 João Paulo Barraca

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


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

    private int age = 0;
    /**
     * Raw intensity calculated from calories
     */
    public int intensity;

    public HPlusDataRecordDaySlot(byte[] data, int age) {
        super(data, TYPE_DAY_SLOT);

        int a = (data[4] & 0xFF) * 256 + (data[5] & 0xFF);
        if (a >= 144) {
            throw new IllegalArgumentException("Invalid Slot Number");
        }

        slot = a;
        heartRate = data[1] & 0xFF;

        if(heartRate == 255 || heartRate == 0)
            heartRate = ActivitySample.NOT_MEASURED;

        steps = (data[2] & 0xFF) * 256 + (data[3] & 0xFF);

        //?? data[6]; atemp?? always 0
        secondsInactive = data[7] & 0xFF; // ?

        Calendar slotTime = GregorianCalendar.getInstance();

        slotTime.set(Calendar.MINUTE, (slot % 6) * 10);
        slotTime.set(Calendar.HOUR_OF_DAY, slot / 6);
        slotTime.set(Calendar.SECOND, 0);

        timestamp = (int) (slotTime.getTimeInMillis() / 1000L);

        this.age = age;
    }

    public String toString(){
        Calendar slotTime = GregorianCalendar.getInstance();
        slotTime.setTimeInMillis(timestamp * 1000L);
        return String.format(Locale.US, "Slot: %d, Time: %s, Steps: %d, InactiveSeconds: %d, HeartRate: %d", slot, slotTime.getTime(), steps, secondsInactive, heartRate);
    }

    public void accumulate(HPlusDataRecordDaySlot other){
        if(other == null)
            return;

        if(steps == ActivitySample.NOT_MEASURED)
            steps = other.steps;
        else if(other.steps != ActivitySample.NOT_MEASURED)
            steps += other.steps;

        if(heartRate == ActivitySample.NOT_MEASURED)
            heartRate = other.heartRate;
        else if(other.heartRate != ActivitySample.NOT_MEASURED) {
            heartRate = (heartRate + other.heartRate) / 2;
        }

        secondsInactive += other.secondsInactive;

        intensity = (int) ((100*heartRate)/(208-0.7*age));

    }

    public boolean isValid() {
        return steps != 0 || secondsInactive != 0 || heartRate != -1;
    }
}
