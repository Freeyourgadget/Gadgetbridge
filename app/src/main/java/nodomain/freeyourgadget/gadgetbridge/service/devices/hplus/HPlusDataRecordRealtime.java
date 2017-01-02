package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;


public class HPlusDataRecordRealtime extends HPlusDataRecord {
    int distance;
    int calories;
    int heartRate;
    byte battery;
    int activeTime;

    public HPlusDataRecordRealtime(byte[] data) {
        super(data);

        if (data.length < 15) {
            throw new IllegalArgumentException("Invalid data packet");
        }

        timestamp = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        distance = 10 * ((data[4] & 0xFF) * 256 + (data[3] & 0xFF)); // meters

        int x = (data[6] & 0xFF) * 256 + data[5] & 0xFF;
        int y = (data[8] & 0xFF) * 256 + data[7] & 0xFF;

        battery = data[9];

        calories = x + y; // KCal

        heartRate = data[11] & 0xFF; // BPM
        activeTime = (data[14] & 0xFF * 256) + (data[13] & 0xFF);

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

}
