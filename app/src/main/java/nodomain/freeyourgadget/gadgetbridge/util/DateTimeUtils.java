/*  Copyright (C) 2015-2024 Andreas Shimokawa, AndrewH, Carsten Pfeiffer,
    Daniele Gobbetti, José Rebelo, Pavel Elagin, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.text.format.DateUtils;

import com.github.pfichtner.durationformatter.DurationFormatter;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class DateTimeUtils {
    private static SimpleDateFormat DAY_STORAGE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static SimpleDateFormat HOURS_MINUTES_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    public static SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US){
        //see https://github.com/Freeyourgadget/Gadgetbridge/issues/1076#issuecomment-383834116 and https://stackoverflow.com/a/30221245

        @Override
        public Date parse(String text, ParsePosition pos) {
            if (text.length() > 3) {
                text = text.substring(0, text.length() - 3) + text.substring(text.length() - 2);
            }
            return super.parse(text, pos);

        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
            StringBuffer rfcFormat = super.format(date, toAppendTo, pos);
            if (this.getTimeZone().equals(TimeZone.getTimeZone("UTC"))) {
                rfcFormat.setLength(rfcFormat.length()-5);
                return rfcFormat.append("Z");
            } else {
                return rfcFormat.insert(rfcFormat.length() - 2, ":");
            }
        }

    }; //no public access, we have to workaround Android bugs

    public static String formatDateTime(Date date) {
        return DateUtils.formatDateTime(GBApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_YEAR);
    }

    public static String formatIso8601(Date date) {
        if(GBApplication.isRunningNougatOrLater()){
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(date);
        }
        ISO_8601_FORMAT.setTimeZone(TimeZone.getDefault());
        return ISO_8601_FORMAT.format(date);
    }

    public static String formatIso8601UTC(Date date) {
        if(GBApplication.isRunningNougatOrLater()){
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(date);
        }
        ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return ISO_8601_FORMAT.format(date);
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
                .minimum(TimeUnit.SECONDS)
                .suppressZeros(DurationFormatter.SuppressZeros.LEADING, DurationFormatter.SuppressZeros.TRAILING)
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

    public static Date dayStart(final Date date) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date dayStart(final LocalDate date) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date dayEnd(final Date date) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Date parseTimeStamp(int timestamp) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000L); // make sure it's converted to long
        return cal.getTime();
    }

    public static Date parseTimestampMillis(long timestamp) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.getTime();
    }

    public static String dayToString(Date date) {
        return DAY_STORAGE_FORMAT.format(date);
    }

    public static Date dayFromString(String day) throws ParseException {
        return DAY_STORAGE_FORMAT.parse(day);
    }

    public static String timeToString(Date date) {
        return HOURS_MINUTES_FORMAT.format(date);
    }

    public static String formatTime(int hours, int minutes) {
        return String.format(Locale.US, "%02d", hours) + ":" + String.format(Locale.US, "%02d", minutes);
    }


    public static Date todayUTC() {
        Calendar cal = getCalendarUTC();
        return cal.getTime();
    }

    public static Calendar getCalendarUTC() {
        return GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public static String minutesToHHMM(int minutes) {
        return String.format(Locale.US, "%d:%02d", minutes / 60, minutes % 60); // no I do not want to use durationformatter :P
    }
    public static boolean isYesterday(Date d) {
        return DateUtils.isToday(d.getTime() + DateUtils.DAY_IN_MILLIS);
    }

    /**
     * Calculates new timestamp with a month offset (positive to add or negative to remove)
     * from a given time
     *
     * @param time
     * @param month
     */
    public static int shiftMonths(int time, int month) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(time * 1000L);
        day.add(Calendar.MONTH, month);
        return (int) (day.getTimeInMillis() / 1000);
    }

    /**
     * Calculates new timestamp with a day offset (positive to add or negative to remove)
     * from a given time
     *
     * @param time
     * @param days
     */
    public static int shiftDays(int time, int days) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(time * 1000L);
        day.add(Calendar.DAY_OF_YEAR, days);
        return (int) (day.getTimeInMillis() / 1000);
    }

    /**
     * Calculates difference in days between two timestamps
     *
     * @param time1
     * @param time2
     */
    public static int  getDaysBetweenTimes(int time1, int time2) {
        return (int) TimeUnit.MILLISECONDS.toDays((time2 - time1) * 1000L);
    }

    /**
     * Determine whether two Calendar instances are on the same day
     *
     * @param calendar1 The first calendar to compare
     * @param calendar2 The second calendar to compare
     * @return true if the Calendar instances are on the same day
     */
    public static boolean isSameDay(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Determine whether two Calendar instances are in the same month
     *
     * @param calendar1 The first calendar to compare
     * @param calendar2 The second calendar to compare
     * @return true if the Calendar instances are in the same month
     */
    public static boolean isSameMonth(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
    }
}
