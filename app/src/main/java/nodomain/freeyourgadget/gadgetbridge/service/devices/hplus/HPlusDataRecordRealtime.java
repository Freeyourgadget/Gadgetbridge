/*  Copyright (C) 2017-2019 Daniele Gobbetti, João Paulo Barraca

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


import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


class HPlusDataRecordRealtime extends HPlusDataRecord {

    /**
     * Distance accumulated during the day in meters
     */
    public int distance;

    /**
     * Calories consumed during the day in KCalories
     */
    public int calories;

    /**
     * Instantaneous Heart Rate measured in Beats Per Minute
     */
    public int heartRate;

    /**
     * Battery level from 0 to 100
     */
    public byte battery;

    /**
     * Number of steps today
     */
    public int steps;

    /**
     * Time active (To be determined how it works)
     */
    public int activeTime;

    /**
     * Computing intensity
     * To be calculated appropriately
     */
    public int intensity;

    public HPlusDataRecordRealtime(byte[] data, int age) {
        super(data, TYPE_REALTIME);

        if (data.length < 15) {
            throw new IllegalArgumentException("Invalid data packet");
        }

        timestamp = (int) (GregorianCalendar.getInstance().getTimeInMillis() / 1000);
        distance = 10 * ((data[4] & 0xFF) * 256 + (data[3] & 0xFF)); // meters
        steps = (data[2] & 0xFF) * 256 + (data[1] & 0xFF);
        int x = (data[6] & 0xFF) * 256 + (data[5] & 0xFF);
        int y = (data[8] & 0xFF) * 256 + (data[7] & 0xFF);

        battery = data[9];
        calories = x + y; // KCal
        activeTime = (data[14] & 0xFF * 256) + (data[13] & 0xFF);

        if (battery == 255) {
            battery = ActivitySample.NOT_MEASURED;
            heartRate = ActivitySample.NOT_MEASURED;
            intensity = 0;
            activityKind = ActivityKind.TYPE_NOT_WORN;
        } else {
            heartRate = data[11] & 0xFF; // BPM
            if (heartRate == 255) {
                intensity = 0;
                activityKind = ActivityKind.TYPE_NOT_MEASURED;
                heartRate = ActivitySample.NOT_MEASURED;
            } else {
                intensity = (int) ((100 * heartRate) / (208 - 0.7 * age));
                activityKind = HPlusDataRecord.TYPE_REALTIME;
            }
        }
    }

    public void computeActivity(HPlusDataRecordRealtime prev){
        if(prev == null)
            return;

        int deltaDistance = distance - prev.distance;

        if(deltaDistance <= 0)
            return;

        int deltaTime = timestamp - prev.timestamp;

        if(deltaTime <= 0)
            return;

        double speed = deltaDistance / deltaTime;

        if(speed >= 1.6) // ~6 KM/h
            activityKind = ActivityKind.TYPE_ACTIVITY;
    }

    public boolean same(HPlusDataRecordRealtime other){
        if(other == null)
            return false;

        return steps == other.steps && distance == other.distance && calories == other.calories && heartRate == other.heartRate && battery == other.battery;
    }

    public String toString(){
        return String.format(Locale.US, "Distance: %d Steps: %d Calories: %d HeartRate: %d Battery: %d ActiveTime: %d Intensity: %d", distance, steps, calories, heartRate, battery, activeTime, intensity);
    }

}
