package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HPlusDataRecordSleep extends HPlusDataRecord {

    /**
     * Time which the device determined to be the bed time in seconds
     */
    public int bedTimeStart;

    /**
     * Time which the device determined to be the end of this sleep period in seconds
     */
    public int bedTimeEnd;

    /**
     * Number of minutes in Deep Sleep
     */
    public int deepSleepMinutes;

    /**
     * Number of minutes in Light Sleep
     * This is considered as Light Sleep

     */
    public int lightSleepMinutes;

    /**
     * Number of minutes to start sleeping (??)
     * This is considered as Light Sleep
     */
    public int enterSleepMinutes;

    /**
     * Number of minutes with Sleep Spindles (??)
     * This is considered as Light Sleep
     */
    public int spindleMinutes;

    /**
     * Number of minutes in REM sleep
     * This is considered as Light Sleep

     */
    public int remSleepMinutes;

    /**
     * Number of wake up minutes during the sleep period
     * This is not considered as a sleep activity
     */
    public int wakeupMinutes;

    /**
     * Number of times the user woke up
     */
    public int wakeupCount;

    public HPlusDataRecordSleep(byte[] data) {
        super(data, TYPE_SLEEP);

        int year = (data[2] & 0xFF) * 256 + (data[1] & 0xFF);
        int month = data[3] & 0xFF;
        int day = data[4] & 0xFF;

        if (year < 2000) //Attempt to recover from bug from device.
            year += 1900;

        if (year < 2000 || month > 12 || day <= 0 || day > 31) {
            throw new IllegalArgumentException("Invalid record date: " + year + "-" + month + "-" + day);
        }

        enterSleepMinutes = ((data[6] & 0xFF) * 256 + (data[5] & 0xFF));
        spindleMinutes = ((data[8] & 0xFF) * 256 + (data[7] & 0xFF));
        deepSleepMinutes = ((data[10] & 0xFF) * 256 + (data[9] & 0xFF));
        remSleepMinutes = ((data[12] & 0xFF) * 256 + (data[11] & 0xFF));
        wakeupMinutes = ((data[14] & 0xFF) * 256 + (data[13] & 0xFF));
        wakeupCount = ((data[16] & 0xFF) * 256 + (data[15] & 0xFF));

        int hour = data[17] & 0xFF;
        int minute = data[18] & 0xFF;

        Calendar sleepStart = GregorianCalendar.getInstance();
        sleepStart.clear();
        sleepStart.set(Calendar.YEAR, year);
        sleepStart.set(Calendar.MONTH, month - 1);
        sleepStart.set(Calendar.DAY_OF_MONTH, day);
        sleepStart.set(Calendar.HOUR_OF_DAY, hour);
        sleepStart.set(Calendar.MINUTE, minute);
        sleepStart.set(Calendar.SECOND, 0);
        sleepStart.set(Calendar.MILLISECOND, 0);

        bedTimeStart = (int) (sleepStart.getTimeInMillis() / 1000);
        bedTimeEnd = (enterSleepMinutes + spindleMinutes + deepSleepMinutes + remSleepMinutes + wakeupMinutes) * 60 + bedTimeStart;
        lightSleepMinutes = enterSleepMinutes + spindleMinutes + remSleepMinutes;

        timestamp = bedTimeStart;
        }

    public List<RecordInterval> getIntervals() {
        List<RecordInterval> intervals = new ArrayList<>();

        int ts = bedTimeStart + lightSleepMinutes * 60;
        intervals.add(new RecordInterval(bedTimeStart, ts, ActivityKind.TYPE_LIGHT_SLEEP));
        intervals.add(new RecordInterval(ts, bedTimeEnd, ActivityKind.TYPE_DEEP_SLEEP));
        return intervals;
    }

    public String toString(){
        Calendar s = GregorianCalendar.getInstance();
        s.setTimeInMillis(bedTimeStart * 1000L);

        Calendar end = GregorianCalendar.getInstance();
        end.setTimeInMillis(bedTimeEnd * 1000L);

        return String.format(Locale.US, "Sleep start: %s end: %s enter: %d spindles: %d rem: %d deep: %d wake: %d-%d", s.getTime(), end.getTime(), enterSleepMinutes, spindleMinutes, remSleepMinutes, deepSleepMinutes, wakeupMinutes, wakeupCount);
    }
}
