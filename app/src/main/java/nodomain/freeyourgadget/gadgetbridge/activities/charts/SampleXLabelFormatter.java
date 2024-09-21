/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.annotation.SuppressLint;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class SampleXLabelFormatter extends ValueFormatter {
    private final TimestampTranslation tsTranslation;
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat annotationDateFormat;
    //        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final Calendar cal = GregorianCalendar.getInstance();

    public SampleXLabelFormatter(final TimestampTranslation tsTranslation, String simpleDateFormatPattern) {
        this.tsTranslation = tsTranslation;
        this.annotationDateFormat = new SimpleDateFormat(simpleDateFormatPattern);
    }

    // TODO: this does not work. Cannot use precomputed labels
    @Override
    public String getFormattedValue(final float value) {
        cal.clear();
        final int ts = (int) value;
        cal.setTimeInMillis(tsTranslation.toOriginalValue(ts) * 1000L);
        final Date date = cal.getTime();
        return annotationDateFormat.format(date);
    }

    public TimestampTranslation getTsTranslation() {
        return tsTranslation;
    }
}
