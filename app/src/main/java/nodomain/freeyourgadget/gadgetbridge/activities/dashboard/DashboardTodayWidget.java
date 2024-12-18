/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardTodayWidget.class);

    private View todayView;
    private ImageView todayChart;

    private boolean mode_24h;

    public DashboardTodayWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardTodayWidget.
     */
    public static DashboardTodayWidget newInstance(DashboardFragment.DashboardData dashboardData) {
        DashboardTodayWidget fragment = new DashboardTodayWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);
        todayChart = todayView.findViewById(R.id.dashboard_today_chart);

        // Determine whether to draw a single or a double chart. In case 24h mode is selected,
        // use just the outer chart (chart_12_24) for all data.
        Prefs prefs = GBApplication.getPrefs();
        mode_24h = prefs.getBoolean("dashboard_widget_today_24h", false);

        // Initialize legend
        TextView legend = todayView.findViewById(R.id.dashboard_piechart_legend);
        SpannableString l_not_worn = new SpannableString("■ " + getString(R.string.abstract_chart_fragment_kind_not_worn));
        l_not_worn.setSpan(new ForegroundColorSpan(color_not_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_worn = new SpannableString("■ " + getString(R.string.activity_type_worn));
        l_worn.setSpan(new ForegroundColorSpan(color_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_activity = new SpannableString("■ " + getString(R.string.activity_type_activity));
        l_activity.setSpan(new ForegroundColorSpan(color_activity), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_exercise = new SpannableString("■ " + getString(R.string.activity_type_exercise));
        l_exercise.setSpan(new ForegroundColorSpan(color_exercise), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_deep_sleep = new SpannableString("■ " + getString(R.string.activity_type_deep_sleep));
        l_deep_sleep.setSpan(new ForegroundColorSpan(color_deep_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_light_sleep = new SpannableString("■ " + getString(R.string.activity_type_light_sleep));
        l_light_sleep.setSpan(new ForegroundColorSpan(color_light_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_rem_sleep = new SpannableString("■ " + getString(R.string.activity_type_rem_sleep));
        l_rem_sleep.setSpan(new ForegroundColorSpan(color_rem_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder legendBuilder = new SpannableStringBuilder();
        legend.setText(legendBuilder.append(l_not_worn).append(" ").append(l_worn).append("\n").append(l_activity).append(" ").append(l_exercise).append("\n").append(l_light_sleep).append(" ").append(l_deep_sleep).append(" ").append(l_rem_sleep));

        legend.setVisibility(prefs.getBoolean("dashboard_widget_today_legend", true) ? View.VISIBLE : View.GONE);

        if (!dashboardData.generalizedActivities.isEmpty()) {
            draw();
        }

        return todayView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (todayChart != null) fillData();
    }

    private void draw() {
        Prefs prefs = GBApplication.getPrefs();
        boolean upsideDown24h = prefs.getBoolean("dashboard_widget_today_24h_upside_down", false);
        boolean showYesterday = prefs.getBoolean("dashboard_widget_today_show_yesterday", false);

        // Prepare circular chart
        long currentDayStart = dashboardData.timeTo - 86400;
        long midDaySecond = currentDayStart + (12 * 60 * 60);
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = width;
        int barWidth = Math.round(width * 0.08f);
        int hourTextSp = Math.round(width * 0.024f);
        float hourTextPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                hourTextSp,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );
        float outerCircleMargin = mode_24h ? barWidth / 2f : barWidth / 2f + hourTextPixels * 1.3f;
        float innerCircleMargin = outerCircleMargin + barWidth * 1.3f;
        float degreeFactor = mode_24h ? 240 : 120;
        Bitmap todayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(todayBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        // Draw clock stripes
        float clockMargin = outerCircleMargin + (mode_24h ? barWidth : barWidth * 2.3f);
        int clockStripesInterval = mode_24h ? 15 : 30;
        float clockStripesWidth = barWidth / 3f;
        paint.setStrokeWidth(clockStripesWidth);
        paint.setColor(color_worn);
        for (int i = 0; i < 360; i += clockStripesInterval) {
            canvas.drawArc(clockMargin, clockMargin, width - clockMargin, height - clockMargin, i, 1, false, paint);
        }

        // Draw hours
        boolean normalClock = DateFormat.is24HourFormat(GBApplication.getContext());
        Map<Integer, String> hours = new HashMap<Integer, String>() {
            {
                put(0, normalClock ? (mode_24h ? "0" : "12") : "12pm");
                put(3, "3");
                put(6, normalClock ? "6" : "6am");
                put(9, "9");
                put(12, normalClock ? (mode_24h ? "12" : "0") : "12am");
                put(15, normalClock ? "15" : "3");
                put(18, normalClock ? "18" : "6pm");
                put(21, normalClock ? "21" : "9");
            }
        };
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(color_worn);
        textPaint.setTextSize(hourTextPixels);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Rect textBounds = new Rect();
        if (mode_24h && upsideDown24h) {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
        } else if (mode_24h) {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
        } else {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(3), 0, hours.get(3).length(), textBounds);
            canvas.drawText(hours.get(3), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(9), 0, hours.get(9).length(), textBounds);
            canvas.drawText(hours.get(9), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(15), 0, hours.get(15).length(), textBounds);
            canvas.drawText(hours.get(15), (float) (width - Math.ceil(textBounds.width() / 2f)), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), width / 2f, height - textBounds.height() / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.getTextBounds(hours.get(21), 0, hours.get(21).length(), textBounds);
            canvas.drawText(hours.get(21), 1, height / 2f + textBounds.height() / 2f, textPaint);
        }

        // Draw generalized activities on circular chart
        long secondIndex = dashboardData.timeFrom;
        long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
        boolean dayIsToday = !(dashboardData.timeTo < currentTime);
        int startAngle = mode_24h && upsideDown24h ? 90 : 270;
        synchronized (dashboardData.generalizedActivities) {
            for (DashboardFragment.DashboardData.GeneralizedActivity activity : dashboardData.generalizedActivities) {
                // Determine margin
                float margin = innerCircleMargin;
                if (mode_24h || activity.timeFrom >= midDaySecond) {
                    margin = outerCircleMargin;
                }
                if (!mode_24h && showYesterday && dayIsToday) {
                    if (activity.timeFrom < currentDayStart && activity.timeFrom > midDaySecond - 86400) {
                        margin = outerCircleMargin;
                    }
                }
                // Skip activities from before 24h ago (to prevent double drawing the same position)
                if (showYesterday && dayIsToday && (activity.timeTo < currentTime - 86400)) {
                    continue;
                }
                // Draw inactive slices
                if (!mode_24h && secondIndex < midDaySecond && activity.timeFrom >= midDaySecond) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_unknown);
                    canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (midDaySecond - secondIndex) / degreeFactor, false, paint);
                    secondIndex = midDaySecond;
                }
                if (activity.timeFrom > secondIndex) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_unknown);
                    canvas.drawArc(margin, margin, width - margin, height - margin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (activity.timeFrom - secondIndex) / degreeFactor, false, paint);
                }
                float start_angle = startAngle + (activity.timeFrom - dashboardData.timeFrom) / degreeFactor;
                float sweep_angle = (activity.timeTo - activity.timeFrom) / degreeFactor;
                if (activity.activityKind == ActivityKind.NOT_MEASURED) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_worn);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.NOT_WORN) {
                    paint.setStrokeWidth(barWidth / 3f);
                    paint.setColor(color_not_worn);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.LIGHT_SLEEP || activity.activityKind == ActivityKind.SLEEP_ANY) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_light_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.REM_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_rem_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.DEEP_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_deep_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.AWAKE_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_awake_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.EXERCISE) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_exercise);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_activity);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                }
                secondIndex = activity.timeTo;
            }
        }
        // Draw indicator for current time
        if (prefs.getBoolean("dashboard_widget_today_time_indicator", false) && currentTime < dashboardData.timeTo) {
            float margin = (mode_24h || currentTime >= midDaySecond) ? outerCircleMargin : innerCircleMargin;
            paint.setStrokeWidth(barWidth);
            paint.setColor(GBApplication.getTextColor(requireContext()));
            canvas.drawArc(margin, margin, width - margin, height - margin, startAngle + (currentTime - dashboardData.timeFrom) / degreeFactor, 300 / degreeFactor, false, paint);
        }
        // Fill remaining time until current time in 12h mode before midday
        if (!mode_24h && currentTime < midDaySecond) {
            // Fill inner bar up until current time
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
            // Fill inner bar up until midday
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (currentTime - dashboardData.timeFrom) / degreeFactor, (midDaySecond - currentTime) / degreeFactor, false, paint);
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 0, 360, false, paint);
        }
        // Fill remaining time until current time in 24h mode or in 12h mode after midday
        if ((mode_24h || currentTime >= midDaySecond) && currentTime < dashboardData.timeTo) {
            // Fill inner bar up until midday
            if (!mode_24h && secondIndex < midDaySecond) {
                paint.setStrokeWidth(barWidth / 3f);
                paint.setColor(color_unknown);
                canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (midDaySecond - secondIndex) / degreeFactor, false, paint);
                secondIndex = midDaySecond;
            }
            // Fill outer bar up until current time
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (currentTime - dashboardData.timeFrom) / degreeFactor, (dashboardData.timeTo - currentTime) / degreeFactor, false, paint);
        }
        // Only when displaying a past day
        if (secondIndex < dashboardData.timeTo && currentTime > dashboardData.timeTo) {
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth / 3f);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (secondIndex - dashboardData.timeFrom) / degreeFactor, (dashboardData.timeTo - secondIndex) / degreeFactor, false, paint);
        }

        todayChart.setImageBitmap(todayBitmap);
    }

    protected void fillData() {
        if (todayView == null) return;

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("dashboard_widget_today_show_yesterday", false)) {
            Calendar today = Calendar.getInstance();
            Calendar dashboardDate = Calendar.getInstance();
            dashboardDate.setTimeInMillis((dashboardData.timeFrom + 1) * 1000L);
            if (DateTimeUtils.isSameDay(today, dashboardDate)) {
                dashboardData.timeFrom -= 86400;
            }
        }

        todayView.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private final HashMap<Long, ActivityKind> activityTimestamps = new HashMap<>();

        /**
         * Add per-second activities to `activityTimestamps`
         */
        private void addActivity(long timeFrom, long timeTo, ActivityKind activityKind) {
            for (long i = timeFrom; i <= timeTo; i++) {
                // If the current timestamp isn't saved yet, do so immediately
                if (activityTimestamps.get(i) == null) {
                    activityTimestamps.put(i, activityKind);
                    continue;
                }
                // If the current timestamp is already saved, compare the activity kinds and
                // keep the most 'important' one
                switch (activityTimestamps.get(i)) {
                    case EXERCISE:
                        break;
                    case ACTIVITY:
                        if (activityKind == ActivityKind.EXERCISE)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case DEEP_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case LIGHT_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case REM_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case AWAKE_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP ||
                                activityKind == ActivityKind.REM_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case SLEEP_ANY:
                    case NOT_MEASURED:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP ||
                                activityKind == ActivityKind.REM_SLEEP ||
                                activityKind == ActivityKind.AWAKE_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    default:
                        activityTimestamps.put(i, activityKind);
                        break;
                }
            }
        }

        /**
         * Add NOT_MEASURED (worn) activities for every successful heart rate measurement
         */
        private void calculateWornSessions(List<ActivitySample> samples) {
            int firstTimestamp = 0;
            int lastTimestamp = 0;

            for (ActivitySample sample : samples) {
                if (sample.getHeartRate() < 10 && firstTimestamp == 0) continue;
                if (firstTimestamp == 0) firstTimestamp = sample.getTimestamp();
                if (lastTimestamp == 0) lastTimestamp = sample.getTimestamp();
                if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())
                        && sample.getTimestamp() > lastTimestamp + dashboardData.hrIntervalSecs
                        && firstTimestamp != lastTimestamp) {
                    LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                    addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
                    if (sample.getHeartRate() < 10) {
                        firstTimestamp = 0;
                        lastTimestamp = 0;
                    } else {
                        firstTimestamp = sample.getTimestamp();
                        lastTimestamp = sample.getTimestamp();
                    }
                    continue;
                }
                if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
                    lastTimestamp = sample.getTimestamp();
                }
            }
            if (firstTimestamp != lastTimestamp) {
                LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
            }
        }

        /**
         * Merge per-second activities from `activityTimestamps` into generalized activity ranges
         * with minute-based resolution
         */
        private void createGeneralizedActivities() {
            long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
            long midDaySecond = dashboardData.timeTo - (12 * 60 * 60);
            DashboardFragment.DashboardData.GeneralizedActivity previous = null;
            List<Map.Entry<Long, ActivityKind>> sortedActivityTimestamps = activityTimestamps.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());
            for (Map.Entry<Long, ActivityKind> activity : sortedActivityTimestamps) {
                long timestamp = activity.getKey();
                ActivityKind activityKind = activity.getValue();
                // Start a new merged activity on certain conditions
                if (previous == null ||
                        previous.activityKind != activityKind ||
                        (!mode_24h && timestamp == midDaySecond) ||
                        (!mode_24h && timestamp == midDaySecond - 86400) ||
                        timestamp == dashboardData.timeTo - 86400 ||
                        timestamp == currentTime - 86400 ||
                        previous.timeTo < timestamp - 60) {
                    previous = new DashboardFragment.DashboardData.GeneralizedActivity(activityKind, timestamp, timestamp);
                    dashboardData.generalizedActivities.add(previous);
                } else {
                    previous.timeTo = timestamp;
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            final long nanoStart = System.nanoTime();

            // Retrieve activity data
            dashboardData.generalizedActivities.clear();
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            List<ActivitySample> allActivitySamples = new ArrayList<>();
            List<ActivitySession> stepSessions = new ArrayList<>();
            List<BaseActivitySummary> activitySummaries = null;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = DashboardUtils.getAllSamples(dbHandler, dev, dashboardData);
                        allActivitySamples.addAll(activitySamples);
                        StepAnalysis stepAnalysis = new StepAnalysis();
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                    }
                }
                activitySummaries = DashboardUtils.getWorkoutSamples(dbHandler, dashboardData);
            } catch (Exception e) {
                LOG.warn("Could not retrieve activity amounts: ", e);
            }
            Collections.sort(allActivitySamples, (lhs, rhs) -> Integer.valueOf(lhs.getTimestamp()).compareTo(rhs.getTimestamp()));

            // Determine worn sessions from heart rate samples
            calculateWornSessions(allActivitySamples);

            // Integrate various data from multiple devices
            for (ActivitySample sample : allActivitySamples) {
                // Handle only TYPE_NOT_WORN and TYPE_SLEEP (including variants) here
                if (sample.getKind() != ActivityKind.NOT_WORN && (sample.getKind() == ActivityKind.NOT_MEASURED || !ActivityKind.isSleep(sample.getKind())))
                    continue;
                // Add to day results
                addActivity(sample.getTimestamp(), sample.getTimestamp() + 60, sample.getKind());
            }
            if (activitySummaries != null) {
                for (BaseActivitySummary baseActivitySummary : activitySummaries) {
                    addActivity(baseActivitySummary.getStartTime().getTime() / 1000, baseActivitySummary.getEndTime().getTime() / 1000, ActivityKind.EXERCISE);
                }
            }
            for (ActivitySession session : stepSessions) {
                addActivity(session.getStartTime().getTime() / 1000, session.getEndTime().getTime() / 1000, ActivityKind.ACTIVITY);
            }

            // Merge per-second activities
            createGeneralizedActivities();

            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", DashboardTodayWidget.this.getClass().getSimpleName(), executionTime);

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {
            super.onPostExecute(unused);
            try {
                draw();
            } catch (final Exception e) {
                LOG.error("calling draw() failed", e);
            }
        }
    }
}