package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class StepStreaksDashboard extends DialogFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(StepStreaksDashboard.class);
    GBDevice gbDevice;
    int goal;
    boolean cancelTasks = false;
    boolean backgroundFinished = false;
    private View fragmentView;
    private StepsStreaks stepsStreaks = new StepsStreaks();

    public StepStreaksDashboard() {

    }

    public static StepStreaksDashboard newInstance(int goal, GBDevice device) {

        StepStreaksDashboard fragment = new StepStreaksDashboard();

        Bundle args = new Bundle();
        args.putInt("goal", goal);
        args.putParcelable(GBDevice.EXTRA_DEVICE, device);
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,

                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.steps_streaks_dashboard, container);

    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTasks = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTasks = true;
    }


    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goal = getArguments().getInt("goal", 0);
        gbDevice = getArguments().getParcelable(GBDevice.EXTRA_DEVICE);
        fragmentView = view;
        if (gbDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        createTaskCalculateLatestStepsStreak("Visualizing data current", getActivity(), "current").execute();
        createTaskCalculateLatestStepsStreak("Visualizing data maximum", getActivity(), "totals").execute();
    }


    void indicate_progress(boolean inProgress) {
        ProgressBar step_streak_dashboard_loading_circle = fragmentView.findViewById(R.id.step_streak_dashboard_loading_circle);
        if (inProgress) {
            step_streak_dashboard_loading_circle.setAlpha(0.5f);
        } else {
            step_streak_dashboard_loading_circle.setAlpha(0);
        }
    }

    void populateData() {

        LinearLayout current = getView().findViewById(R.id.step_streak_current_layout);
        TextView days_current = current.findViewById(R.id.step_streak_days_value);
        TextView average_current = current.findViewById(R.id.step_streak_average_value);
        TextView total_current = current.findViewById(R.id.step_streak_total_value);

        LinearLayout maximum = getView().findViewById(R.id.step_streak_maximum_layout);
        TextView days_maximum = maximum.findViewById(R.id.step_streak_days_value);
        TextView average_maximum = maximum.findViewById(R.id.step_streak_average_value);
        TextView total_maximum = maximum.findViewById(R.id.step_streak_total_value);
        TextView date_maximum_value = maximum.findViewById(R.id.step_streak_maximum_date_value);

        LinearLayout total = getView().findViewById(R.id.step_streak_total_layout);
        TextView days_total = total.findViewById(R.id.step_streak_days_value);
        TextView days_total_label = total.findViewById(R.id.step_streak_days_label);
        TextView total_total = total.findViewById(R.id.step_streak_total_value);

        if (stepsStreaks.current.days > 0) {
            current.setVisibility(View.VISIBLE);
            days_current.setText(Integer.toString(stepsStreaks.current.days));
            average_current.setText(Integer.toString(stepsStreaks.current.steps / stepsStreaks.current.days));
            total_current.setText(Integer.toString(stepsStreaks.current.steps));
        }

        if (stepsStreaks.maximum.days > 0) {
            maximum.setVisibility(View.VISIBLE);
            days_maximum.setText(Integer.toString(stepsStreaks.maximum.days));
            average_maximum.setText(Integer.toString(stepsStreaks.maximum.steps / stepsStreaks.maximum.days));
            total_maximum.setText(Integer.toString(stepsStreaks.maximum.steps));
            date_maximum_value.setText(DateTimeUtils.formatDate(new Date(stepsStreaks.maximum.timestamp * 1000l)));
            LOG.debug("petr " + stepsStreaks.total.timestamp);
        }
        if (stepsStreaks.total.steps > 0 || backgroundFinished) {
            total.setVisibility(View.VISIBLE);
            days_total_label.setText("Achievement\n rate");
            days_total.setText(String.format("%.1f%%", 0.0));
            if (stepsStreaks.total.total_days > 0) {
                days_total.setText(String.format("%.1f%%", (float) stepsStreaks.total.days / stepsStreaks.total.total_days * 100));
            }
            total_total.setText(Integer.toString(stepsStreaks.total.steps));
        }
    }

    protected TaskCalculateLatestStepsStreak createTaskCalculateLatestStepsStreak(String taskName, Context context, String period) {
        return new TaskCalculateLatestStepsStreak(taskName, context, period);
    }

    public class TaskCalculateLatestStepsStreak extends DBAccess {
        String period;

        public TaskCalculateLatestStepsStreak(String taskName, Context context, String period) {
            super(taskName, context);
            this.period = period;
        }

        @Override
        protected void doInBackground(DBHandler db) {
            switch (period) {
                case "current":
                    calculateStreakData(db, "current", gbDevice, goal);

                    break;
                case "totals":
                    calculateStreakData(db, "totals", gbDevice, goal);
                    break;
            }
        }

        @Override
        protected void onPreExecute() {
            indicate_progress(true);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            FragmentActivity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                if (period.equals("totals")) {
                    backgroundFinished = true;
                    indicate_progress(false);
                }
                populateData();
            } else {
                LOG.info("Not filling data because activity is not available anymore");
            }
        }
    }

    private void calculateStreakData(DBHandler db, String period, GBDevice device, int goal) {
        Calendar day = Calendar.getInstance();
        int streak_steps = 0;
        int streak_days = 0;
        int timestamp = (int) (day.getTimeInMillis() / 1000);

        int all_step_days = 0;
        int all_streak_days = 0;
        int all_steps = 0;

        DailyTotals dailyTotals = new DailyTotals();
        ActivitySample firstSample = dailyTotals.getFirstSample(db, device);
        Calendar firstDate = Calendar.getInstance();
        firstDate.setTime(DateTimeUtils.shiftByDays(new Date(firstSample.getTimestamp() * 1000l), -1));
        //go one day back, to ensure we are before the first day, to calculate first day data as well
        //NOTE: getting the first sample as a first day reference is not reliable

        while (true) {
            if (cancelTasks) {
                GB.toast("Bailing out background jobs", Toast.LENGTH_SHORT, GB.INFO);
                break;
            }

            long[] daily_data = dailyTotals.getDailyTotalsForDevice(device, day, db);
            int steps_this_day = (int) daily_data[0];

            if (steps_this_day > 0) {
                all_step_days++;
                all_steps += steps_this_day;
            }

            if (steps_this_day >= goal) {
                streak_steps += steps_this_day;
                streak_days++;
                all_streak_days++;
                Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                day.setTime(newDate);
            } else if (DateUtils.isToday(day.getTimeInMillis())) {
                //if goal is not reached today, we might still get our steps later
                // so do not count this day but do not interrupt
                Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                day.setTime(newDate);
            } else {
                if (period.equals("current")) {
                    stepsStreaks.current.days = streak_days;
                    stepsStreaks.current.steps = streak_steps;
                    return;
                } else if (period.equals("totals")) {
                    //reset max
                    if (streak_days > stepsStreaks.maximum.days) {
                        stepsStreaks.maximum.steps = streak_steps;
                        stepsStreaks.maximum.days = streak_days;
                        stepsStreaks.maximum.timestamp = timestamp;
                    }
                    stepsStreaks.total.steps = all_steps;
                    stepsStreaks.total.days = all_streak_days;
                    stepsStreaks.total.total_days = all_step_days;

                    streak_days = 0;
                    streak_steps = 0;
                    Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                    day.setTime(newDate);
                    timestamp = (int) (day.getTimeInMillis() / 1000);
                    if (day.before(firstDate) || day.get(Calendar.YEAR) < 2015) { //avoid rolling back too far
                        return;
                    }
                }
            }
        }
    }

    private static class StepsStreak {
        private int days = 0;
        private int steps = 0;
        private int timestamp;
        private int total_days = 0;
    }

    private class StepsStreaks {
        private StepsStreak current = new StepsStreak();
        private StepsStreak maximum = new StepsStreak();
        private StepsStreak total = new StepsStreak();
    }
}

