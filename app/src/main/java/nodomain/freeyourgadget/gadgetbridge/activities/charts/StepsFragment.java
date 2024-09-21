package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;

abstract class StepsFragment<T extends ChartsData> extends AbstractChartFragment<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(StepsDailyFragment.class);

    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int TOTAL_DAYS = 1;

    @Override
    public String getTitle() {
        return getString(R.string.steps);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
    }

    protected List<StepsFragment.StepsDay> getMyStepsDaysData(DBHandler db, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);

        List<StepsDay> daysData = new ArrayList<>();;
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            long totalSteps = 0;
            long totalDistance = 0;
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            for (ActivityAmount amount : amounts.getAmounts()) {
                if (amount.getTotalSteps() > 0) {
                    totalSteps += amount.getTotalSteps();
                }
                if (amount.getTotalDistance() > 0) {
                    totalDistance += amount.getTotalDistance();
                }
            }
            double distance = totalDistance;
            if (totalDistance == 0 && totalSteps > 0) {
                // For gadgets that do not report distance, compute it from the steps
                ActivityUser activityUser = new ActivityUser();
                int stepLength = activityUser.getStepLengthCm();
                distance = stepLength * totalSteps;
            }
            Calendar d = (Calendar) day.clone();
            daysData.add(new StepsDay(d, totalSteps, distance / 100_000));
            day.add(Calendar.DATE, 1);
        }
        return daysData;
    }

    protected ActivityAmounts getActivityAmountsForDay(DBHandler db, Calendar day, GBDevice device) {
        LimitedQueue<Integer, ActivityAmounts> activityAmountCache = null;
        ActivityAmounts amounts = null;

        Activity activity = getActivity();
        int key = (int) (day.getTimeInMillis() / 1000);
        if (activity != null) {
            activityAmountCache = ((ActivityChartsActivity) activity).mActivityAmountCache;
            amounts = activityAmountCache.lookup(key);
        }

        if (amounts == null) {
            ActivityAnalysis analysis = new ActivityAnalysis();
            amounts = analysis.calculateActivityAmounts(getSamplesOfDay(db, day, 0, device));
            if (activityAmountCache != null) {
                activityAmountCache.add(key, amounts);
            }
        }

        return amounts;
    }

    protected List<? extends ActivitySample> getSamplesOfDay(DBHandler db, Calendar day, int offsetHours, GBDevice device) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, offsetHours);

        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        return getSamples(db, device, startTs, endTs);
    }

    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    protected static class StepsDay {
        public long steps;
        public double distance;
        public Calendar day;

        protected StepsDay(Calendar day, long steps, double distance) {
            this.steps = steps;
            this.distance = distance;
            this.day = day;
        }
    }


}
