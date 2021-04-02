package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class ActivityListingDashboard extends DialogFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivityListingDashboard.class);
    GBDevice gbDevice;
    ActivityListingAdapter stepListAdapter;
    ActivitySession stepSessionsSummary;
    private int timeFrom;
    private int timeTo;
    private View fragmentView;

    public ActivityListingDashboard() {

    }

    public static ActivityListingDashboard newInstance(int timestamp, GBDevice device) {

        ActivityListingDashboard frag = new ActivityListingDashboard();

        Bundle args = new Bundle();
        args.putInt("time", timestamp);
        args.putParcelable(GBDevice.EXTRA_DEVICE, device);
        frag.setArguments(args);
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,

                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_list_total_dashboard, container);

    }

    @Override

    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        int time = getArguments().getInt("time", 1);
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(time * 1000L);
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        timeTo = (int) (day.getTimeInMillis() / 1000);


        gbDevice = getArguments().getParcelable(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        stepListAdapter = new ActivityListingAdapter(getContext());
        fragmentView = view;

        final TextView battery_status_date_from_text = (TextView) view.findViewById(R.id.battery_status_date_from_text);
        final TextView battery_status_date_to_text = (TextView) view.findViewById(R.id.battery_status_date_to_text);
        LinearLayout battery_status_date_to_layout = (LinearLayout) view.findViewById(R.id.battery_status_date_to_layout);
        final SeekBar battery_status_time_span_seekbar = (SeekBar) view.findViewById(R.id.battery_status_time_span_seekbar);

        boolean activity_list_debug_extra_time_range_value = GBApplication.getPrefs().getPreferences().getBoolean("activity_list_debug_extra_time_range", false);

        if (!activity_list_debug_extra_time_range_value) {
            battery_status_time_span_seekbar.setMax(3);
        }
        final TextView battery_status_time_span_text = (TextView) view.findViewById(R.id.battery_status_time_span_text);

        battery_status_time_span_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String text;
                switch (i) {
                    case 0:
                        text = getString(R.string.calendar_day);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -1);
                        break;
                    case 1:
                        text = getString(R.string.calendar_week);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -7);
                        break;
                    case 2:
                        text = getString(R.string.calendar_two_weeks);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -14);
                        break;
                    case 3:
                        text = getString(R.string.calendar_month);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -1);
                        break;
                    case 4:
                        text = getString(R.string.calendar_three_months);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -3);
                        break;
                    case 5:
                        text = getString(R.string.calendar_six_months);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -6);
                        break;
                    case 6:
                        text = getString(R.string.calendar_year);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -12);
                        break;
                    default:
                        text = getString(R.string.calendar_two_weeks);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -14);

                }

                battery_status_time_span_text.setText(text);
                battery_status_date_from_text.setText(DateTimeUtils.formatDate(new Date(timeFrom * 1000L)));
                battery_status_date_to_text.setText(DateTimeUtils.formatDate(new Date(timeTo * 1000L)));
                createRefreshTask("Visualizing data", getActivity()).execute();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        battery_status_date_to_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar currentDate = Calendar.getInstance();
                currentDate.setTimeInMillis(timeTo * 1000L);

                new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        Calendar date = Calendar.getInstance();
                        date.set(year, monthOfYear, dayOfMonth);
                        int time = (int) (date.getTimeInMillis() / 1000);
                        Calendar day = Calendar.getInstance();
                        day.setTimeInMillis(time * 1000L);
                        day.set(Calendar.HOUR_OF_DAY, 23);
                        day.set(Calendar.MINUTE, 59);
                        day.set(Calendar.SECOND, 59);
                        timeTo = (int) (day.getTimeInMillis() / 1000);

                        battery_status_date_to_text.setText(DateTimeUtils.formatDate(new Date(timeTo * 1000L)));
                        battery_status_time_span_seekbar.setProgress(0);
                        battery_status_time_span_seekbar.setProgress(1);
                    }
                }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
            }
        });
        battery_status_time_span_seekbar.setProgress(2);
    }

    protected RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    private ActivitySession get_data(GBDevice gbDevice, DBHandler db, int timeFrom, int timeTo) {

        List<ActivitySession> stepSessions;
        List<? extends ActivitySample> activitySamples = getAllSamples(db, gbDevice, timeFrom, timeTo);
        StepAnalysis stepAnalysis = new StepAnalysis();

        boolean isEmptySummary = false;
        if (activitySamples != null) {
            stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
            if (stepSessions.toArray().length == 0) {
                isEmptySummary = true;
            }
            stepSessionsSummary = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
        }
        return stepSessionsSummary;
    }

    SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }

    protected List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    void indicate_progress(boolean inProgress) {
        LinearLayout activity_list_dashboard_results_layout = fragmentView.findViewById(R.id.activity_list_dashboard_results_layout);
        RelativeLayout activity_list_dashboard_loading_layout = fragmentView.findViewById(R.id.activity_list_dashboard_loading_layout);
        if (inProgress) {
            activity_list_dashboard_results_layout.setVisibility(View.GONE);
            activity_list_dashboard_loading_layout.setVisibility(View.VISIBLE);
        } else {
            activity_list_dashboard_results_layout.setVisibility(View.VISIBLE);
            activity_list_dashboard_loading_layout.setVisibility(View.GONE);
        }
    }

    void populateData(ActivitySession item) {
        TextView stepLabel = fragmentView.findViewById(R.id.line_layout_step_label);
        TextView stepTotalLabel = fragmentView.findViewById(R.id.line_layout_total_step_label);
        TextView distanceLabel = fragmentView.findViewById(R.id.line_layout_distance_label);
        TextView durationLabel = fragmentView.findViewById(R.id.line_layout_duration_label);
        TextView sessionCountLabel = fragmentView.findViewById(R.id.line_layout_count_label);
        LinearLayout durationLayout = fragmentView.findViewById(R.id.line_layout_duration);
        LinearLayout countLayout = fragmentView.findViewById(R.id.line_layout_count);
        LinearLayout stepsLayout = fragmentView.findViewById(R.id.line_layout_step);
        LinearLayout stepsTotalLayout = fragmentView.findViewById(R.id.line_layout_total_step);
        LinearLayout distanceLayout = fragmentView.findViewById(R.id.line_layout_distance);

        stepLabel.setText(stepListAdapter.getStepLabel(item));
        stepTotalLabel.setText(stepListAdapter.getStepTotalLabel(item));
        distanceLabel.setText(stepListAdapter.getDistanceLabel(item));
        durationLabel.setText(stepListAdapter.getDurationLabel(item));
        sessionCountLabel.setText(stepListAdapter.getSessionCountLabel(item));

        if (!stepListAdapter.hasDistance(item)) {
            distanceLayout.setVisibility(View.GONE);
        } else {
            distanceLayout.setVisibility(View.VISIBLE);
        }

        if (!stepListAdapter.hasSteps(item)) {
            stepsLayout.setVisibility(View.GONE);
        } else {
            stepsLayout.setVisibility(View.VISIBLE);
        }

        if (!stepListAdapter.hasTotalSteps(item)) {
            stepsTotalLayout.setVisibility(View.GONE);
            countLayout.setVisibility(View.GONE);
            durationLayout.setVisibility(View.GONE);
        } else {
            stepsTotalLayout.setVisibility(View.VISIBLE);
            countLayout.setVisibility(View.VISIBLE);
            durationLayout.setVisibility(View.VISIBLE);
        }
    }

    public class RefreshTask extends DBAccess {

        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler db) {
            stepSessionsSummary = get_data(gbDevice, db, timeFrom, timeTo);
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
                populateData(stepSessionsSummary);
                indicate_progress(false);
            } else {
                LOG.info("Not filling data because activity is not available anymore");
            }
        }
    }
}