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
