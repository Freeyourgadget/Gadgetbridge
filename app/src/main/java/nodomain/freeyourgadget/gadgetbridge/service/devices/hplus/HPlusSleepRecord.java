package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;



public class HPlusSleepRecord {
    private long bedTimeStart;
    private long bedTimeEnd;
    private int deepSleepSeconds;
    private int spindleSeconds;
    private int remSleepSeconds;
    private int wakeupTime;
    private int wakeupCount;
    private int enterSleepSeconds;
    private byte[] rawData;

    HPlusSleepRecord(byte[] data) {
        rawData = data;
        int year = data[2] * 256 + data[1];
        int month = data[3];
        int day = data[4];

        enterSleepSeconds = data[6] * 256 + data[5];
        spindleSeconds = data[8] * 256 + data[7];
        deepSleepSeconds = data[10] * 256 + data[9];
        remSleepSeconds = data[12] * 256 + data[11];
        wakeupTime = data[14] * 256 + data[13];
        wakeupCount = data[16] * 256 + data[15];
        int hour = data[17];
        int minute = data[18];

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        bedTimeStart = (c.getTimeInMillis() / 1000L);
        bedTimeEnd = bedTimeStart + enterSleepSeconds + spindleSeconds + deepSleepSeconds + remSleepSeconds + wakeupTime;
    }

    byte[] getRawData() {

        return rawData;
    }

    public long getBedTimeStart() {
        return bedTimeStart;
    }

    public long getBedTimeEnd() {
        return bedTimeEnd;
    }

    public int getDeepSleepSeconds() {
        return deepSleepSeconds;
    }

    public int getSpindleSeconds() {
        return spindleSeconds;
    }

    public int getRemSleepSeconds() {
        return remSleepSeconds;
    }

    public int getWakeupTime() {
        return wakeupTime;
    }

    public int getWakeupCount() {
        return wakeupCount;
    }

    public int getEnterSleepSeconds() {
        return enterSleepSeconds;
    }

}
