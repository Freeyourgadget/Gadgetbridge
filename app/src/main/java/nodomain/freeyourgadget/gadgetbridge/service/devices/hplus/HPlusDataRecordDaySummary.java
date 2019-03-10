/*  Copyright (C) 2017-2019 João Paulo Barraca

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


class HPlusDataRecordDaySummary extends HPlusDataRecord{

    /**
     * Year of the record reported by the device
     * Sometimes the device will report a low number (e.g, 116) which will be "corrected"
     * by adding 1900
     */
    public int year;

    /**
     * Month of the record reported by the device from 1 to 12
     */
    public int month;

    /**
     * Day of the record reported by the device
     */
    public int day;

    /**
     * Number of steps accumulated in the day reported
     */
    public int steps;

    /**
     * Distance in meters accumulated in the day reported
     */
    public int distance;

    /**
     * Amount of time active in the day (Units are To Be Determined)
     */
    public int activeTime;

    /**
     * Max Heart Rate recorded in Beats Per Minute
     */
    public int maxHeartRate;

    /**
     * Min Heart  Rate recorded in Beats Per Minute
     */
    public int minHeartRate;

    /**
     * Amount of estimated calories consumed during the day in KCalories
     */
    public int calories;

    HPlusDataRecordDaySummary(byte[] data) {
        super(data, TYPE_DAY_SUMMARY);

        year =  (data[10] & 0xFF) * 256 + (data[9] & 0xFF);
        month = data[11] & 0xFF;
        day = data[12] & 0xFF;

        //Recover from bug in firmware where year is corrupted
        //data[10] will be set to 0, effectively offsetting values by minus 1900 years
        if(year < 1900)
            year += 1900;

        if (year < 2000 || month > 12 || day > 31) {
            throw new IllegalArgumentException("Invalid record date "+year+"-"+month+"-"+day);
        }
        steps = (data[2] & 0xFF) * 256 + (data[1] & 0xFF);
        distance = ((data[4] & 0xFF) * 256 + (data[3] & 0xFF)) * 10;
        activeTime = (data[14] & 0xFF) * 256 + (data[13] & 0xFF);
        calories = (data[6] & 0xFF) * 256 + (data[5] & 0xFF);
        calories += (data[8] & 0xFF) * 256 + (data[7] & 0xFF);

        maxHeartRate = data[15] & 0xFF;
        minHeartRate = data[16] & 0xFF;

        Calendar date = GregorianCalendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month - 1);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 999);

        timestamp = (int) (date.getTimeInMillis() / 1000);
    }

    public String toString(){
        return String.format(Locale.US, "%s-%s-%s steps:%d distance:%d minHR:%d maxHR:%d calories:%d activeTime:%d", year, month, day, steps, distance,minHeartRate, maxHeartRate, calories, activeTime);
    }
}