package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;

import org.apache.commons.lang3.EnumUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DefaultRestingMetabolicRateProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;

public class CaloriesDailyFragment extends AbstractChartFragment<CaloriesDailyFragment.CaloriesData> {

    private ImageView caloriesGauge;
    private TextView dateView;
    private TextView caloriesResting;
    private TextView caloriesActive;
    private LinearLayout caloriesActiveWrapper;
    private TextView caloriesActiveGoal;
    private LinearLayout caloriesActiveGoalWrapper;
    protected int CALORIES_GOAL;
    protected int ACTIVE_CALORIES_GOAL;
    public enum GaugeViewMode {
        ACTIVE_CALORIES_GOAL,
        TOTAL_CALORIES_SEGMENT
    }
    private GaugeViewMode gaugeViewMode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String mode = getArguments().getString(ActivityChartsActivity.EXTRA_MODE, "");
            if (EnumUtils.isValidEnum(GaugeViewMode.class, mode)) {
                gaugeViewMode = GaugeViewMode.valueOf(mode);
            }
        }
    }

    public static CaloriesDailyFragment newInstance(final String mode) {
        final CaloriesDailyFragment fragment = new CaloriesDailyFragment();
        final Bundle args = new Bundle();
        args.putString(ActivityChartsActivity.EXTRA_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calories, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        caloriesGauge = rootView.findViewById(R.id.calories_gauge);
        dateView = rootView.findViewById(R.id.date_view);
        caloriesResting = rootView.findViewById(R.id.calories_resting);
        caloriesActive = rootView.findViewById(R.id.calories_active);
        caloriesActiveWrapper = rootView.findViewById(R.id.calories_active_wrapper);
        caloriesActiveGoal = rootView.findViewById(R.id.calories_active_goal);
        caloriesActiveGoalWrapper = rootView.findViewById(R.id.calories_active_goal_wrapper);
        ActivityUser activityUser = new ActivityUser();
        ACTIVE_CALORIES_GOAL = activityUser.getCaloriesBurntGoal();

        refresh();
        if (!supportsActiveCalories()) {
            caloriesActiveWrapper.setVisibility(View.GONE);
            caloriesActiveGoalWrapper.setVisibility(View.GONE);
        }

        if (gaugeViewMode == null) {
            gaugeViewMode = GaugeViewMode.TOTAL_CALORIES_SEGMENT;
        }

        if (gaugeViewMode.equals(GaugeViewMode.ACTIVE_CALORIES_GOAL)) {
            CALORIES_GOAL = ACTIVE_CALORIES_GOAL;
        }

        return rootView;
    }

    public boolean supportsActiveCalories() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsActiveCalories();
    }

    protected RestingMetabolicRateSample getRestingMetabolicRate(DBHandler db, GBDevice device) {
        TimeSampleProvider<? extends RestingMetabolicRateSample> provider = device.getDeviceCoordinator().getRestingMetabolicRateProvider(device, db.getDaoSession());
        RestingMetabolicRateSample latestSample = provider.getLatestSample();
        if (latestSample != null) {
            return latestSample;
        }
        DefaultRestingMetabolicRateProvider defaultProvider = new DefaultRestingMetabolicRateProvider(device, db.getDaoSession());
        return defaultProvider.getLatestSample();
    }

    protected List<? extends AbstractActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    @Override
    public String getTitle() {
        return getString(R.string.calories);
    }

    @Override
    protected void init() {}

    @Override
    protected CaloriesDailyFragment.CaloriesData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar calendar = Calendar.getInstance();
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        day.add(Calendar.DATE, 0);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000);
        int endTs = startTs + 24 * 60 * 60 - 1;
        Date date = new Date((long) endTs * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        dateView.setText(formattedDate);
        List<? extends ActivitySample> samples = getActivitySamples(db, device, startTs, endTs);
        RestingMetabolicRateSample metabolicRate = getRestingMetabolicRate(db, device);
        if (metabolicRate == null) {
            return new CaloriesData(0, 0, 0);
        }
        int totalBurnt;
        int activeBurnt = 0;
        boolean sameDay = calendar.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == day.get(Calendar.YEAR);
        double passedDayProportion = 1;
        if (sameDay) {
            passedDayProportion = (double) (calendar.getTimeInMillis() - day.getTimeInMillis()) / (24L * 60 * 60 * 1000);
        }
        int restingBurnt = (int) (metabolicRate.getRestingMetabolicRate() * passedDayProportion);

        for (int i = 0; i <= samples.size() - 1; i++) {
            ActivitySample sample = samples.get(i);
            if (sample.getActiveCalories() > 0) {
                activeBurnt += sample.getActiveCalories();
            }
        }
        // Convert calories to kcal
        activeBurnt = activeBurnt / 1000;
        totalBurnt = restingBurnt + activeBurnt;

        return new CaloriesData(totalBurnt, activeBurnt, restingBurnt);
    }

    @Override
    protected void updateChartsnUIThread(CaloriesDailyFragment.CaloriesData data) {
        int restingCalories = data.restingBurnt;
        int activeCalories = data.activeBurnt;
        int totalCalories = activeCalories + restingCalories;
        caloriesActive.setText(String.valueOf(activeCalories));
        caloriesResting.setText(String.valueOf(restingCalories));
        caloriesActiveGoal.setText(String.valueOf(ACTIVE_CALORIES_GOAL));

        if (gaugeViewMode.equals(GaugeViewMode.TOTAL_CALORIES_SEGMENT)) {
            int[] colors = new int[] {
                    ContextCompat.getColor(GBApplication.getContext(), R.color.calories_resting_color),
                    ContextCompat.getColor(GBApplication.getContext(), R.color.calories_color)
            };
            float[] segments = new float[] {
                    restingCalories > 0 ? (float) restingCalories / totalCalories : 0,
                    activeCalories > 0 ? (float) activeCalories / totalCalories : 0
            };
            final int width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    300,
                    GBApplication.getContext().getResources().getDisplayMetrics()
            );
            caloriesGauge.setImageBitmap(GaugeDrawer.drawCircleGaugeSegmented(
                    width,
                    width / 15,
                    colors,
                    segments,
                    true,
                    String.valueOf(totalCalories),
                    getContext().getString(R.string.total_calories_burnt),
                    getContext()
            ));
        } else {
            int value = 0;
            if (gaugeViewMode.equals(GaugeViewMode.ACTIVE_CALORIES_GOAL)) {
                value = activeCalories;
            }
            final int width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    300,
                    GBApplication.getContext().getResources().getDisplayMetrics()
            );
            caloriesGauge.setImageBitmap(GaugeDrawer.drawCircleGauge(
                    width,
                    width / 15,
                    getResources().getColor(R.color.calories_color),
                    value,
                    CALORIES_GOAL,
                    getContext()
            ));
        }
    }

    @Override
    protected void renderCharts() {}

    @Override
    protected void setupLegend(Chart<?> chart) {}

    protected static class CaloriesData extends ChartsData {
        public int activeBurnt;
        public int restingBurnt;
        public int totalBurnt;

        protected CaloriesData(int totalBurnt, int activeBurnt, int restingBurnt) {
            this.totalBurnt = totalBurnt;
            this.activeBurnt = activeBurnt;
            this.restingBurnt = restingBurnt;
        }
    }
}
