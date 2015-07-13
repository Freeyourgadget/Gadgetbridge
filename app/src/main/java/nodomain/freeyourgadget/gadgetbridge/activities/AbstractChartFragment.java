package nodomain.freeyourgadget.gadgetbridge.activities;

import android.graphics.Color;
import android.support.v4.app.Fragment;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.charts.SleepUtils;

public abstract class AbstractChartFragment extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

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

    protected List<GBActivitySample> getAllSamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getAllActivitySamples(tsFrom, tsTo, provider);
    }

    private int getTSLast24Hours() {
        long now = System.currentTimeMillis();
        return (int) ((now / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
    }

    protected List<GBActivitySample> getActivitySamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getActivitySamples(tsFrom, tsTo, provider);
    }


    protected List<GBActivitySample> getSleepSamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getSleepSamples(tsFrom, tsTo, provider);
    }

    protected List<GBActivitySample> getTestSamples(GBDevice device, int tsFrom, int tsTo) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JUNE, 10, 6, 40);
        // ignore provided date ranges
        tsTo = (int) ((cal.getTimeInMillis() / 1000) & 0xffffffff);
        tsFrom = tsTo - (24 * 60 * 60);

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getAllActivitySamples(tsFrom, tsTo, provider);
    }

    protected void configureChartDefaults(Chart<?> chart) {
        // if enabled, the chart will always start at zero on the y-axis
        chart.setNoDataText(getString(R.string.chart_no_data_synchronize));

        // disable value highlighting
        chart.setHighlightEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);
    }

    protected void configureBarLineChartDefaults(BarLineChartBase<?> chart) {
        configureChartDefaults(chart);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
//        chart.setPinchZoom(true);

        chart.setDrawGridBackground(false);
    }

    protected void refresh(GBDevice mGBDevice, BarLineChartBase chart) {
        if (mGBDevice == null) {
            return;
        }

//        ArrayList<GBActivitySample> samples = getTestSamples(mGBDevice, -1, -1);
        List<GBActivitySample> samples = getSamples(mGBDevice, -1, -1);

        Calendar cal = GregorianCalendar.getInstance();
        cal.clear();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        LOG.info("number of samples:" + samples.size());
        if (samples.size() > 1) {
            float movement_divisor;
            boolean annotate = true;
            boolean use_steps_as_movement;
            switch (getProvider(mGBDevice)) {
                case GBActivitySample.PROVIDER_MIBAND:
                    movement_divisor = 256.0f;
                    use_steps_as_movement = true;
                    break;
                default: // Morpheuz
                    movement_divisor = 5000.0f;
                    use_steps_as_movement = false;
                    break;
            }

            byte last_type = GBActivitySample.TYPE_UNKNOWN;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            SimpleDateFormat annotationDateFormat = new SimpleDateFormat("HH:mm");

            int numEntries = samples.size();
            List<String> xLabels = new ArrayList<>(numEntries);
//            List<BarEntry> deepSleepEntries = new ArrayList<>(numEntries / 4);
//            List<BarEntry> lightSleepEntries = new ArrayList<>(numEntries / 4);
            List<BarEntry> activityEntries = new ArrayList<>(numEntries);
            List<Integer> colors = new ArrayList<>(numEntries); // this is kinda inefficient...

            for (int i = 0; i < numEntries; i++) {
                GBActivitySample sample = samples.get(i);
                byte type = sample.getType();

                // determine start and end dates
                if (i == 0) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // make sure it's converted to long
                    date = cal.getTime();
                    dateStringFrom = dateFormat.format(date);
                } else if (i == samples.size() - 1) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // same here
                    date = cal.getTime();
                    dateStringTo = dateFormat.format(date);
                }

                short movement = sample.getIntensity();

                float value;
                if (type == GBActivitySample.TYPE_DEEP_SLEEP) {
//                    value = Y_VALUE_DEEP_SLEEP;
                    value = ((float) movement) / movement_divisor;
                    value += SleepUtils.Y_VALUE_DEEP_SLEEP;
                    activityEntries.add(createBarEntry(value, i));
                    colors.add(akDeepSleep.color);
                } else {
                    if (type == GBActivitySample.TYPE_LIGHT_SLEEP) {
                        value = ((float) movement) / movement_divisor;
//                        value += SleepUtils.Y_VALUE_LIGHT_SLEEP;
//                        value = Math.min(1.0f, Y_VALUE_LIGHT_SLEEP);
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akLightSleep.color);
                    } else {
                        byte steps = sample.getSteps();
                        if (use_steps_as_movement && steps != 0) {
                            // I'm not sure using steps for this is actually a good idea
                            movement = steps;
                        }
                        value = ((float) movement) / movement_divisor;
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akActivity.color);
                    }
                }

                String xLabel = "";
                if (annotate) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    String dateString = annotationDateFormat.format(date);
                    xLabel = dateString;
//                    if (last_type != type) {
//                        if (isSleep(last_type) && !isSleep(type)) {
//                            // woken up
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextColor(Color.WHITE);
//                            line.setTextSize(15);
//                            chart.getXAxis().addLimitLine(line);
//                        } else if (!isSleep(last_type) && isSleep(type)) {
//                            // fallen asleep
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextSize(15);
//                            line.setTextColor(Color.WHITE);
//                            chart.getXAxis().addLimitLine(line);
//                        }
//                    }
                    last_type = type;
                }
                xLabels.add(xLabel);
            }

            chart.getXAxis().setValues(xLabels);

//            BarDataSet deepSleepSet = createDeepSleepSet(deepSleepEntries, "Deep Sleep");
//            BarDataSet lightSleepSet = createLightSleepSet(lightSleepEntries, "Light Sleep");
            BarDataSet activitySet = createActivitySet(activityEntries, colors, "Activity");

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
//            dataSets.add(deepSleepSet);
//            dataSets.add(lightSleepSet);
            dataSets.add(activitySet);

            // create a data object with the datasets
            BarData data = new BarData(xLabels, dataSets);
            data.setGroupSpace(0);

            chart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            chart.setDescriptionPosition(?, ?);
            // set data

            setupLegend(chart);

            chart.setData(data);

            chart.animateX(500, Easing.EasingOption.EaseInOutQuart);

//            textView.setText(dateStringFrom + " to " + dateStringTo);
        }
    }

    protected abstract List<GBActivitySample> getSamples(GBDevice device, int tsFrom, int tsTo);

    protected abstract void setupLegend(BarLineChartBase chart);

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
