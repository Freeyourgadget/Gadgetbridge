package nodomain.freeyourgadget.gadgetbridge.util;

import android.text.format.DateUtils;

import com.github.pfichtner.durationformatter.DurationFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class DateTimeUtils {
    private static SimpleDateFormat DAY_STORAGE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static String formatDateTime(Date date) {
        return DateUtils.formatDateTime(GBApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
    }

    public static String formatDate(Date date) {
        return DateUtils.formatDateTime(GBApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE);
//        long dateMillis = date.getTime();
//        if (isToday(dateMillis)) {
//            return "Today";
//        }
//        if (isYesterday(dateMillis)) {
//            return "Yesterday";
//        }
//        DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }

    public static String formatDurationHoursMinutes(long duration, TimeUnit unit) {
        DurationFormatter df = DurationFormatter.Builder.SYMBOLS
                .maximum(TimeUnit.DAYS)
                .minimum(TimeUnit.MINUTES)
                .suppressZeros(DurationFormatter.SuppressZeros.LEADING)
                .maximumAmountOfUnitsToShow(2)
                .build();
        return df.format(duration, unit);
    }

    public static String formatDateRange(Date from, Date to) {
        return DateUtils.formatDateRange(GBApplication.getContext(), from.getTime(), to.getTime(), DateUtils.FORMAT_SHOW_DATE);
    }

    public static Date shiftByDays(Date date, int offset) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(GregorianCalendar.DAY_OF_YEAR, offset);
        Date newDate = cal.getTime();
        return newDate;
    }

    public static Date parseTimeStamp(int timestamp) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000L); // make sure it's converted to long
        return cal.getTime();
    }

    public static String dayToString(Date date) {
        return DAY_STORAGE_FORMAT.format(date);
    }

    public static Date dayFromString(String day) throws ParseException {
        return DAY_STORAGE_FORMAT.parse(day);
    }

    public static Date todayUTC() {
        Calendar cal = getCalendarUTC();
        return cal.getTime();
    }

    public static Calendar getCalendarUTC() {
        return GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    }
}
