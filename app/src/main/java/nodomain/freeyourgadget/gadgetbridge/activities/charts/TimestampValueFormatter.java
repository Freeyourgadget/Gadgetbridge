/*  Copyright (C) 2016-2018 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimestampValueFormatter implements IAxisValueFormatter {
    private final Calendar cal;
    //    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private DateFormat dateFormat;

    public TimestampValueFormatter() {
        this(new SimpleDateFormat("HH:mm"));

    }

    public TimestampValueFormatter(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        cal = GregorianCalendar.getInstance();
        cal.clear();
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        cal.setTimeInMillis((int) value * 1000L);
        Date date = cal.getTime();
        String dateString = dateFormat.format(date);
        return dateString;
    }
}
