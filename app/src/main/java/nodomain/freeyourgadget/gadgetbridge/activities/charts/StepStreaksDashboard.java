/*  Copyright (C) 2022-2024 Arjan Schrijver, Petr Vaněk

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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialDialogFragment;

public class StepStreaksDashboard extends MaterialDialogFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(StepStreaksDashboard.class);
    GBDevice gbDevice;
    int stepsGoal;
    boolean cancelTasks = false;
    boolean backgroundTaskFinished = false;
    private StepsStreaks stepsStreaks = new StepsStreaks();
    private static final String GOAL = "goal";
    private static final String STREAKS = "streaks";
    private static final String PERIOD_CURRENT = "current";
    private static final String PERIOD_TOTALS = "totals";
    private static final int MIN_YEAR = 2015; //we go back in time, this is minimal year boundary

    public StepStreaksDashboard() {

    }

    //Calculates some stats for longest streak (daily steps goal being reached for subsequent days
    //without interruption (day with steps less then goal)
    //Possible improvements/nice to haves:
    //- cache values until new activity fetch is performed
    //- read the goals from the USER_ATTRIBUTES table. But, this would also require to be able
    //to edit/add values there...

    public static StepStreaksDashboard newInstance(int goal, GBDevice device) {

        StepStreaksDashboard fragment = new StepStreaksDashboard();
        Bundle args = new Bundle();
        args.putInt(GOAL, goal);
        args.putParcelable(GBDevice.EXTRA_DEVICE, device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (backgroundTaskFinished) {
            outState.putParcelable(STREAKS, stepsStreaks);
        }
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stepsGoal = getArguments().getInt(GOAL, 0);
        gbDevice = getArguments().getParcelable(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        if (savedInstanceState != null) {
            StepsStreaks streaks = (StepsStreaks) savedInstanceState.getParcelable(STREAKS);
            if (streaks != null) {
                stepsStreaks = streaks;
                backgroundTaskFinished = true;
                cancelTasks = true;
                indicate_progress(false);
                populateData();
            }
        }
        createTaskCalculateLatestStepsStreak("Visualizing data current", getActivity(), PERIOD_CURRENT).execute();
        createTaskCalculateLatestStepsStreak("Visualizing data maximum", getActivity(), PERIOD_TOTALS).execute();
    }

    void indicate_progress(boolean inProgress) {
        ProgressBar step_streak_dashboard_loading_circle = getView().findViewById(R.id.step_streak_dashboard_loading_circle);
        if (inProgress) {
            step_streak_dashboard_loading_circle.setAlpha(0.4f); //make it a bit softer
        } else {
            step_streak_dashboard_loading_circle.setAlpha(0);
        }
    }

    void populateData() {

        LinearLayout current = getView().findViewById(R.id.step_streak_current_layout);
        TextView days_current = current.findViewById(R.id.step_streak_days_value);
        TextView average_current = current.findViewById(R.id.step_streak_average_value);
        TextView total_current = current.findViewById(R.id.step_streak_total_value);
        TextView date_current_value = current.findViewById(R.id.step_streak_current_date_value);

        LinearLayout maximum = getView().findViewById(R.id.step_streak_maximum_layout);
        TextView days_maximum = maximum.findViewById(R.id.step_streak_days_value);
        TextView average_maximum = maximum.findViewById(R.id.step_streak_average_value);
        TextView total_maximum = maximum.findViewById(R.id.step_streak_total_value);
        TextView date_maximum_value = maximum.findViewById(R.id.step_streak_maximum_date_value);

        LinearLayout total = getView().findViewById(R.id.step_streak_total_layout);
        TextView days_total = total.findViewById(R.id.step_streak_days_value);
        TextView days_total_label = total.findViewById(R.id.step_streak_days_label);
        TextView total_total = total.findViewById(R.id.step_streak_total_value);
        TextView date_total_value = total.findViewById(R.id.step_streak_total_date_value);
        TextView date_total_label = total.findViewById(R.id.step_streak_total_label);

        ImageButton step_streak_share_button = getView().findViewById(R.id.step_streak_share_button);
        step_streak_share_button.setVisibility(View.GONE);
        step_streak_share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                take_share_screenshot(getActivity());
            }
        });

        if (stepsStreaks.current.days > 0) {
            current.setVisibility(View.VISIBLE);
            days_current.setText(Integer.toString(stepsStreaks.current.days));
            average_current.setText(Integer.toString(stepsStreaks.current.steps / stepsStreaks.current.days));
            total_current.setText(Integer.toString(stepsStreaks.current.steps));

            Date startDate = new Date(stepsStreaks.current.timestamp * 1000L);
            Date endDate = DateTimeUtils.shiftByDays(startDate, stepsStreaks.current.days - 1); //first day is 1 not 0
            date_current_value.setText(DateTimeUtils.formatDateRange(startDate, endDate));
        }

        if (stepsStreaks.maximum.days > 0) {
            maximum.setVisibility(View.VISIBLE);
            days_maximum.setText(Integer.toString(stepsStreaks.maximum.days));
            average_maximum.setText(Integer.toString(stepsStreaks.maximum.steps / stepsStreaks.maximum.days));
            total_maximum.setText(Integer.toString(stepsStreaks.maximum.steps));

            Date startDate = new Date(stepsStreaks.maximum.timestamp * 1000L);
            Date endDate = DateTimeUtils.shiftByDays(startDate, stepsStreaks.maximum.days - 1); //first day is 1 not 0
            date_maximum_value.setText(DateTimeUtils.formatDateRange(startDate, endDate));
        }
        if (stepsStreaks.total.steps > 0 || backgroundTaskFinished) {
            step_streak_share_button.setVisibility(View.VISIBLE);
            total.setVisibility(View.VISIBLE);
            days_total_label.setText(R.string.steps_streaks_achievement_rate);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //labels here have diferent meaning, so we must also add proper hint
                days_total_label.setTooltipText(getString(R.string.steps_streaks_total_days_hint_totals));
                days_total.setTooltipText(getString(R.string.steps_streaks_total_days_hint_totals));
                date_total_label.setTooltipText(getString(R.string.steps_streaks_total_steps_hint_totals));
            }

            days_total.setText(String.format("%.1f%%", 0.0));
            if (stepsStreaks.total.total_days > 0) {
                days_total.setText(String.format("%.1f%%", (float) stepsStreaks.total.days / stepsStreaks.total.total_days * 100));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    total_total.setTooltipText(String.format(getString(R.string.steps_streaks_total_steps_average_hint), stepsStreaks.total.steps / stepsStreaks.total.total_days));
                }
            }
            if (stepsStreaks.total.timestamp > 0) {
                date_total_value.setVisibility(View.VISIBLE);
                date_total_value.setText(String.format(getString(R.string.steps_streaks_since_date), DateTimeUtils.formatDate(new Date(stepsStreaks.total.timestamp * 1000L))));
            } else {
                date_total_value.setVisibility(View.GONE);
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
                case PERIOD_CURRENT:
                    calculateStreakData(db, PERIOD_CURRENT, gbDevice, stepsGoal);

                    break;
                case PERIOD_TOTALS:
                    calculateStreakData(db, PERIOD_TOTALS, gbDevice, stepsGoal);
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
                if (period.equals(PERIOD_TOTALS)) {
                    backgroundTaskFinished = true;
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
        int timestamp = 0;

        int all_step_days = 0;
        int all_streak_days = 0;
        int all_steps = 0;
        int firstDataTimestamp = 0;

        ActivitySample firstSample = DailyTotals.getFirstSample(db, device);
        if (firstSample == null) { //no data at all
            return;
        }
        Calendar firstDate = Calendar.getInstance();
        firstDate.setTime(DateTimeUtils.shiftByDays(new Date(firstSample.getTimestamp() * 1000L), -1));
        //go one day back, to ensure we are before the first day, to calculate first day data as well

        while (true) {
            if (cancelTasks) {
                GB.toast("Cancelling background jobs", Toast.LENGTH_SHORT, GB.INFO);
                break;
            }

            DailyTotals daily_data = DailyTotals.getDailyTotalsForDevice(device, day, db);
            int steps_this_day = (int) daily_data.getSteps();

            if (steps_this_day > 0) {
                all_step_days++;
                all_steps += steps_this_day;
                firstDataTimestamp = (int) (day.getTimeInMillis() / 1000);
            }

            if (steps_this_day >= goal) {
                streak_steps += steps_this_day;
                streak_days++;
                all_streak_days++;
                timestamp = (int) (day.getTimeInMillis() / 1000);
                Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                day.setTime(newDate);
            } else if (DateUtils.isToday(day.getTimeInMillis())) {
                //if goal is not reached today, we might still get our steps later
                // so do not count this day but do not interrupt
                Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                day.setTime(newDate);
            } else {
                if (period.equals(PERIOD_CURRENT)) {
                    stepsStreaks.current.days = streak_days;
                    stepsStreaks.current.steps = streak_steps;
                    stepsStreaks.current.timestamp = timestamp;
                    return;
                } else if (period.equals(PERIOD_TOTALS)) {
                    //reset max
                    if (streak_days > stepsStreaks.maximum.days) {
                        stepsStreaks.maximum.steps = streak_steps;
                        stepsStreaks.maximum.days = streak_days;
                        stepsStreaks.maximum.timestamp = timestamp;
                    }

                    streak_days = 0;
                    streak_steps = 0;
                    Date newDate = DateTimeUtils.shiftByDays(new Date(day.getTimeInMillis()), -1);
                    day.setTime(newDate);
                    if (day.before(firstDate) || day.get(Calendar.YEAR) < MIN_YEAR) {
                        //avoid rolling back too far, if the data has a timestamp too far into future
                        //we could make this date configurable if needed for people who imported old data
                        stepsStreaks.total.steps = all_steps;
                        stepsStreaks.total.days = all_streak_days;
                        stepsStreaks.total.total_days = all_step_days;
                        stepsStreaks.total.timestamp = firstDataTimestamp;
                        return;
                    }
                }
            }
        }
    }

    private void take_share_screenshot(Context context) {
        final ScrollView layout = getView().findViewById(R.id.streaks_dashboard);
        final LinearLayout sharingLayout = getView().findViewById(R.id.streaks_dashboard_inner);
        int width = layout.getChildAt(0).getHeight();
        int height = layout.getChildAt(0).getWidth();
        Bitmap screenShot = getScreenShot(sharingLayout, width, height, context);
        String fileName = FileUtils.makeValidFileName("Screenshot-" + "StepsStreak-" + DateTimeUtils.formatIso8601(new Date(Calendar.getInstance().getTimeInMillis())) + ".png");

        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            shareScreenshot(targetFile, context);
            GB.toast(getActivity(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        getActivity().grantUriPermission(
                context.getApplicationContext().getPackageName(),
                contentUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.setType("image/*");
        String shareBody = getString(R.string.step_streaks_achievements_sharing_message);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.step_streaks_achievements_sharing_title));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap getScreenShot(View view, int height, int width, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(context));
        view.draw(canvas);
        return bitmap;
    }

    private static class StepsStreak implements Parcelable {
        private int days = 0;
        private int steps = 0;
        private int timestamp;
        private int total_days = 0;

        private StepsStreak() {

        }

        protected StepsStreak(Parcel in) {
            days = in.readInt();
            steps = in.readInt();
            timestamp = in.readInt();
            total_days = in.readInt();
        }

        public static final Creator<StepsStreak> CREATOR = new Creator<StepsStreak>() {
            @Override
            public StepsStreak createFromParcel(Parcel in) {
                return new StepsStreak(in);
            }

            @Override
            public StepsStreak[] newArray(int size) {
                return new StepsStreak[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(days);
            dest.writeInt(steps);
            dest.writeInt(timestamp);
            dest.writeInt(total_days);
        }
    }

    private class StepsStreaks implements Parcelable {
        private StepsStreak current = new StepsStreak();
        private StepsStreak maximum = new StepsStreak();
        private StepsStreak total = new StepsStreak();

        private StepsStreaks() {

        }

        protected StepsStreaks(Parcel in) {
            current = in.readParcelable(StepsStreak.class.getClassLoader());
            maximum = in.readParcelable(StepsStreak.class.getClassLoader());
            total = in.readParcelable(StepsStreak.class.getClassLoader());
        }

        public final Creator<StepsStreaks> CREATOR = new Creator<StepsStreaks>() {
            @Override
            public StepsStreaks createFromParcel(Parcel in) {
                return new StepsStreaks(in);
            }

            @Override
            public StepsStreaks[] newArray(int size) {
                return new StepsStreaks[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(current, flags);
            dest.writeParcelable(maximum, flags);
            dest.writeParcelable(total, flags);
        }
    }
}

