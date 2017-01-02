package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import java.util.Calendar;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;


public class HPlusDataRecordDay extends HPlusDataRecord {
    int slot;
    int steps;
    int secondsInactive;
    int heartRate;

    public HPlusDataRecordDay(byte[] data) {
        super(data);

        int a = (data[4] & 0xFF) * 256 + (data[5] & 0xFF);
        if (a >= 144) {
            throw new IllegalArgumentException("Invalid Slot Number");
        }

        slot = a; // 10 minute slots as an offset from 0:00 AM
        heartRate = data[1] & 0xFF; //Average Heart Rate ?

        if(heartRate == 255)
            heartRate = ActivityKind.TYPE_NOT_MEASURED;

        steps = (data[2] & 0xFF) * 256 + data[3] & 0xFF; // Steps in this period

        //?? data[6];
        secondsInactive = data[7] & 0xFF; // ?

        int now = (int) (Calendar.getInstance().getTimeInMillis() / (3600 * 24 * 1000L));
        timestamp = now * 3600 * 24 + (slot / 6 * 3600 + slot % 6 * 10);
    }

}
