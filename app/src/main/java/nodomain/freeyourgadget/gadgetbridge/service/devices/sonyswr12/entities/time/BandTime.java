package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.time;

import java.util.Calendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.IntFormat;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public class BandTime {
    private final int year;
    private final int month;
    private final int dayOfMonth;
    private final int hour;
    private final int min;
    private final int sec;
    private final int dayOfWeek;
    private final BandTimeZone timeZone;
    private final BandDaylightSavingTime dst;

    public BandTime(Calendar calendar) {
        int dayOfWeek = 7;
        if (calendar == null) {
            throw new IllegalArgumentException("Calendar cant be null");
        }
        this.year = calendar.get(1);
        if (this.year > 2099 || this.year < 2013) {
            throw new RuntimeException("out of 2013-2099");
        }
        this.month = calendar.get(2) + 1;
        this.dayOfMonth = calendar.get(5);
        int value = calendar.get(7);
        if (value != 1) {
            dayOfWeek = value - 1;
        }
        this.dayOfWeek = dayOfWeek;
        this.hour = calendar.get(11);
        this.min = calendar.get(12);
        this.sec = calendar.get(13);
        TimeZone timeZone = calendar.getTimeZone();
        this.timeZone = BandTimeZone.fromOffset(timeZone.getRawOffset());
        if (timeZone.inDaylightTime(calendar.getTime())) {
            this.dst = BandDaylightSavingTime.fromOffset(timeZone.getDSTSavings());
            return;
        }
        this.dst = BandDaylightSavingTime.STANDARD_TIME;
    }

    public byte[] toByteArray() {
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        byteArrayWriter.appendUint16(this.year);
        byteArrayWriter.appendUint8(this.month);
        byteArrayWriter.appendUint8(this.dayOfMonth);
        byteArrayWriter.appendUint8(this.hour);
        byteArrayWriter.appendUint8(this.min);
        byteArrayWriter.appendUint8(this.sec);
        byteArrayWriter.appendUint8(this.dayOfWeek);
        byteArrayWriter.appendValue(this.timeZone.key, IntFormat.SINT8);
        byteArrayWriter.appendUint8(this.dst.key);
        return byteArrayWriter.getByteArray();
    }
}

