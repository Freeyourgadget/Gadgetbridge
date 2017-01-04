package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;


class HPlusDataRecordRealtime extends HPlusDataRecord {
    public int distance;
    public int calories;
    public int heartRate;
    public byte battery;
    public int activeTime;
    public int intensity;

    public HPlusDataRecordRealtime(byte[] data) {
        super(data);

        if (data.length < 15) {
            throw new IllegalArgumentException("Invalid data packet");
        }

        timestamp = (int) (GregorianCalendar.getInstance().getTimeInMillis() / 1000);
        distance = 10 * ((data[4] & 0xFF) * 256 + (data[3] & 0xFF)); // meters

        int x = (data[6] & 0xFF) * 256 + data[5] & 0xFF;
        int y = (data[8] & 0xFF) * 256 + data[7] & 0xFF;

        battery = data[9];

        calories = x + y; // KCal

        heartRate = data[11] & 0xFF; // BPM
        activeTime = (data[14] & 0xFF * 256) + (data[13] & 0xFF);
        if(heartRate == 255)
            intensity = 0;
        else
            intensity = (int) (100 * Math.max(0, Math.min((heartRate - 60) / 120.0, 1))); // TODO: Calculate a proper value
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

        return distance == other.distance && calories == other.calories && heartRate == other.heartRate && battery == other.battery;
    }

    public String toString(){
        return String.format(Locale.US, "Distance: %d Calories: %d HeartRate: %d Battery: %d ActiveTime: %d Intensity: %d", distance, calories, heartRate, battery, activeTime, intensity);
    }

}
