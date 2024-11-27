package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;

abstract class RespiratoryRateFragment<T extends ChartsData> extends AbstractChartFragment<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(StepsDailyFragment.class);

    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int TOTAL_DAYS = 1;

    @Override
    public String getTitle() {
        return getString(R.string.respiratoryrate);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
    }

    protected List<RespiratoryRateFragment.RespiratoryRateDay> getMyRespiratoryRateDaysData(DBHandler db, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);

        List<RespiratoryRateDay> daysData = new ArrayList<>();;
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            int startTs;
            int endTs;
            day = (Calendar) day.clone(); // do not modify the caller's argument
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.add(Calendar.HOUR, 0);
            startTs = (int) (day.getTimeInMillis() / 1000);
            endTs = startTs + 24 * 60 * 60 - 1;
            List<? extends ActivitySample> activitySamples = getAllActivitySamples(db, device, startTs, endTs);
            SleepAnalysis sleepAnalysis = new SleepAnalysis();
            List<SleepAnalysis.SleepSession> sleepSessions = sleepAnalysis.calculateSleepSessions(activitySamples);
            List<? extends AbstractRespiratoryRateSample> samples = getRespiratoryRateSamples(db, device, startTs, endTs);
            Calendar d = (Calendar) day.clone();
            daysData.add(new RespiratoryRateDay(d, samples, sleepSessions));
            day.add(Calendar.DATE, 1);
        }
        return daysData;
    }

    protected List<? extends AbstractRespiratoryRateSample> getSamplesOfDay(DBHandler db, GBDevice device, int startTs, int endTs) {
        return getRespiratoryRateSamples(db, device, startTs, endTs);
    }

    protected List<AbstractRespiratoryRateSample> getRespiratoryRateSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        TimeSampleProvider<? extends RespiratoryRateSample> provider = device.getDeviceCoordinator().getRespiratoryRateSampleProvider(device, db.getDaoSession());
        return (List<AbstractRespiratoryRateSample>) provider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    protected List<? extends ActivitySample> getAllActivitySamples(DBHandler db, GBDevice device, int startTs, int endTs) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamples(startTs, endTs);
    }

    protected static class RespiratoryRateDay extends ChartsData {
        public int awakeRateAvg;
        public int sleepRateAvg;
        public int rateLowest;
        public int rateHighest;
        public Calendar day;
        List<? extends AbstractRespiratoryRateSample> respiratoryRateSamples;
        List<SleepAnalysis.SleepSession> sleepSessions;

        protected RespiratoryRateDay(Calendar day, List<? extends AbstractRespiratoryRateSample> respiratoryRateSamples, List<SleepAnalysis.SleepSession> sleepSessions) {
            this.day = day;
            this.respiratoryRateSamples = respiratoryRateSamples;
            this.sleepSessions = sleepSessions;
            float awakeRateTotal = 0;
            int awakeCounter = 0;
            float sleepRateTotal = 0;
            int sleepCounter = 0;
            float lowest = 0;
            float highest = 0;
            if (!this.respiratoryRateSamples.isEmpty()) {
                for (AbstractRespiratoryRateSample sample : this.respiratoryRateSamples) {
                    if (isSleepSample(sample)) {
                        sleepRateTotal += sample.getRespiratoryRate();
                        sleepCounter++;
                    } else {
                        awakeRateTotal += sample.getRespiratoryRate();
                        awakeCounter++;
                    }
                    if (sample.getRespiratoryRate() > highest) {
                        highest = sample.getRespiratoryRate();
                    }
                    if (sample.getRespiratoryRate() < lowest || lowest == 0) {
                        lowest = sample.getRespiratoryRate();
                    }
                }
            }
            if (awakeRateTotal > 0) {
                this.awakeRateAvg = Math.round(awakeRateTotal / awakeCounter);
            }
            if (sleepRateTotal > 0) {
                this.sleepRateAvg = Math.round(sleepRateTotal / sleepCounter);
            }
            this.rateLowest = (int) lowest;
            this.rateHighest = (int) highest;
        }

        private boolean isSleepSample(AbstractRespiratoryRateSample sample) {
            if (this.sleepSessions.isEmpty()) {
                return true;
            }

            for (SleepAnalysis.SleepSession session : this.sleepSessions) {
                if (sample.getTimestamp() >= session.getSleepStart().getTime() && sample.getTimestamp() <= session.getSleepEnd().getTime()) {
                    return true;
                }
            }
            return false;
        }
    }


}
