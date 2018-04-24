/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.text.format.DateUtils;

import com.github.pfichtner.durationformatter.DurationFormatter;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
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
            return rfcFormat.insert(rfcFormat.length() - 2, ":");
        }

    }; //no public access, we have to workaround Android bugs

    public static String formatDateTime(Date date) {
        return DateUtils.formatDateTime(GBApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_YEAR);
    }

    public static String formatIso8601(Date date) {
        if(GBApplication.isRunningNougatOrLater()){
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(date);
        }
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
                .minimum(TimeUnit.MINUTES)
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

    public static String minutesToHHMM(int minutes) {
        return String.format(Locale.US, "%d:%02d", minutes / 60, minutes % 60); // no I do not want to use durationformatter :P
    }
}
