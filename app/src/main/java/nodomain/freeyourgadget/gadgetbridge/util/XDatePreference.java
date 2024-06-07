/*  Copyright (C) 2024 José Rebelo

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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class XDatePreference extends DialogPreference {
    private int year;
    private int month;
    private int day;
    private long minDate; // TODO actually read minDate
    private long maxDate; // TODO actually read maxDate

    public XDatePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(final Object defaultValue) {
        final String persistedString = getPersistedString((String) defaultValue);

        final String dateStr;

        if (StringUtils.isNullOrEmpty(persistedString)) {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            dateStr = getPersistedString(sdf.format(new Date()));
        } else {
            dateStr = persistedString;
        }

        final String[] pieces = dateStr.split("-");

        year = Integer.parseInt(pieces[0]);
        month = Integer.parseInt(pieces[1]);
        day = Integer.parseInt(pieces[2]);

        updateSummary();
    }

    public void setMinDate(final long minDate) {
        this.minDate = minDate;
    }

    public void setMaxDate(final long maxDate) {
        this.maxDate = maxDate;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public long getMinDate() {
        return minDate;
    }

    public long getMaxDate() {
        return maxDate;
    }

    String getPrefValue() {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    public void setValue(final int year, final int month, final int day) {
        this.year = year;
        this.month = month;
        this.day = day;

        persistStringValue(getPrefValue());
    }

    void updateSummary() {
        setSummary(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day));
    }

    void persistStringValue(final String value) {
        persistString(value);
    }
}
