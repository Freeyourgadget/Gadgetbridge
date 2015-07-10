package nodomain.freeyourgadget.gadgetbridge.activities;

import android.graphics.Color;
import android.support.v4.app.Fragment;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;

public class AbstractChartFragment extends Fragment {
    public static final String ACTION_REFRESH
            = "nodomain.freeyourgadget.gadgetbride.chart.action.refresh";

    protected static final class ActivityKind {
        public final byte type;
        public final String label;
        public final Integer color;

        public ActivityKind(byte type, String label, Integer color) {
            this.type = type;
            this.label = label;
            this.color = color;
        }
    }

    protected ActivityKind akActivity = new ActivityKind(GBActivitySample.TYPE_UNKNOWN, "Activity", Color.rgb(89, 178, 44));
    protected ActivityKind akLightSleep = new ActivityKind(GBActivitySample.TYPE_LIGHT_SLEEP, "Light Sleep", Color.rgb(182, 191, 255));
    protected ActivityKind akDeepSleep = new ActivityKind(GBActivitySample.TYPE_DEEP_SLEEP, "Deep Sleep", Color.rgb(76, 90, 255));

    protected static final int BACKGROUND_COLOR = Color.rgb(24, 22, 24);
    protected static final int DESCRIPTION_COLOR = Color.WHITE;
    protected static final int CHART_TEXT_COLOR = Color.WHITE;
    protected static final int LEGEND_TEXT_COLOR = Color.WHITE;

    protected byte getProvider(GBDevice device) {
        byte provider = -1;
        switch (device.getType()) {
            case MIBAND:
                provider = GBActivitySample.PROVIDER_MIBAND;
                break;
            case PEBBLE:
                provider = GBActivitySample.PROVIDER_PEBBLE_MORPHEUZ; // FIXME
                break;
        }
        return provider;
    }

    protected ArrayList<GBActivitySample> getSamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            long ts = System.currentTimeMillis();
            tsFrom = (int) ((ts / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
        }

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getGBActivitySamples(tsFrom, tsTo, provider);
    }

    protected ArrayList<GBActivitySample> getSleepSamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            long ts = System.currentTimeMillis();
            tsFrom = (int) ((ts / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
        }

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getGBActivitySamples(tsFrom, tsTo, provider);
    }

    protected  ArrayList<GBActivitySample> getTestSamples(GBDevice device, int tsFrom, int tsTo) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JUNE, 10, 6, 40);
        // ignore provided date ranges
        tsTo = (int) ((cal.getTimeInMillis() / 1000) & 0xffffffff);
        tsFrom = tsTo - (24 * 60 * 60);

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getGBActivitySamples(tsFrom, tsTo, provider);
    }

    protected void configureChartDefaults(Chart<?> mChart) {
        // if enabled, the chart will always start at zero on the y-axis

        // disable value highlighting
        mChart.setHighlightEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);
    }

    protected void configureBarLineChartDefaults(BarLineChartBase<?> mChart) {
        configureChartDefaults(mChart);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
//        mChart.setPinchZoom(true);

        mChart.setDrawGridBackground(false);
    }

    protected BarEntry createBarEntry(float value, int index) {
        return new BarEntry(value, index);
    }

    protected BarDataSet createActivitySet(List<BarEntry> values, List<Integer> colors, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
        set1.setColors(colors);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        return set1;
    }

    protected BarDataSet createDeepSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(76, 90, 255));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        return set1;
    }

    protected BarDataSet createLightSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);

//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(182, 191, 255));
        set1.setValueTextColor(CHART_TEXT_COLOR);
//        set1.setColor(Color.CYAN);
        return set1;
    }
}
