/*  Copyright (C) 2023-2024 Arjan Schrijver

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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.gridlayout.widget.GridLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DashboardCalendarActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardCalendarActivity.class);
    public static String EXTRA_TIMESTAMP = "dashboard_calendar_chosen_day";
    private final ConcurrentHashMap<Calendar, TextView> dayCells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> dayColors = new ConcurrentHashMap<>();

    private boolean showAllDevices;
    private Set<String> showDeviceList;

    TextView monthTextView;
    TextView arrowLeft;
    TextView arrowRight;
    GridLayout calendarGrid;
    Calendar currentDay;
    Calendar cal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_calendar);
        monthTextView = findViewById(R.id.calendar_month);
        calendarGrid = findViewById(R.id.dashboard_calendar_grid);
        currentDay = Calendar.getInstance();
        cal = Calendar.getInstance();
        long receivedTimestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP, 0);
        if (receivedTimestamp != 0) {
            currentDay.setTimeInMillis(receivedTimestamp);
            cal.setTimeInMillis(receivedTimestamp);
        }

        Prefs prefs = GBApplication.getPrefs();
        showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());

        arrowLeft = findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            cal.add(Calendar.MONTH, -1);
            draw();
        });
        arrowRight = findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameMonth(today, cal)) {
                cal.add(Calendar.MONTH, 1);
                draw();
            }
        });

        draw();
    }

    private void displayColorsAsync() {
        calendarGrid.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private void draw() {
        // Remove previous calendar days
        dayCells.clear();
        dayColors.clear();
        calendarGrid.removeAllViews();
        // Update month display
        SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
        monthTextView.setText(monthFormat.format(cal.getTime()));
        Calendar today = GregorianCalendar.getInstance();
        today.set(Calendar.HOUR, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        if (DateTimeUtils.isSameMonth(today, cal)) {
            arrowRight.setAlpha(0.5f);
        } else {
            arrowRight.setAlpha(1);
        }
        // Calculate grid cell size for dates
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int cellSize = screenWidth / 7;
        // Determine first day that should be displayed
        Calendar drawCal = (Calendar) cal.clone();
        drawCal.set(Calendar.DAY_OF_MONTH, 1);
        int displayMonth = drawCal.get(Calendar.MONTH);
        int firstDayOfWeek = cal.getFirstDayOfWeek();
        int daysToFirstDay = (drawCal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7;
        drawCal.add(Calendar.DAY_OF_MONTH, -daysToFirstDay);
        // Determine last day that should be displayed
        Calendar lastDay = (Calendar) cal.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        lastDay.add(Calendar.DAY_OF_MONTH, (firstDayOfWeek + 7 - lastDay.get(Calendar.DAY_OF_WEEK)) % 7);
        // Add day names header
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        Calendar weekdays = Calendar.getInstance();
        for (int i=0; i<7; i++) {
            int currentDayOfWeek = (firstDayOfWeek + i - 1) % 7 + 1;
            weekdays.set(Calendar.DAY_OF_WEEK, currentDayOfWeek);
            createWeekdayCell(dayFormat.format(weekdays.getTime()), cellSize);
        }
        // Loop through month days and create grid cells for them
        while (!DateTimeUtils.isSameDay(drawCal, lastDay)) {
            boolean clickable = drawCal.get(Calendar.MONTH) == displayMonth;
            if (drawCal.after(today)) clickable = false;
            createDateCell(drawCal, cellSize, clickable);
            drawCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Asynchronously determine and display goal colors
        displayColorsAsync();
    }

    private TextView prepareGridElement(int cellSize) {
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL,1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL,1f)
        );
        int margin = cellSize / 10;
        layoutParams.width = 0;
        layoutParams.height = cellSize - 2 * margin;
        layoutParams.setMargins(margin, margin, margin, margin);
        TextView text = new TextView(this);
        text.setLayoutParams(layoutParams);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private void createWeekdayCell(String day, int cellSize) {
        TextView text = prepareGridElement(cellSize);
        text.setText(day);
        calendarGrid.addView(text);
    }

    private void createDateCell(Calendar day, int cellSize, boolean clickable) {
        TextView text = prepareGridElement(cellSize);
        text.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        if (clickable) {
            // Save textview for later coloring
            dayCells.put((Calendar) day.clone(), text);
        }
        calendarGrid.addView(text);
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            for (Calendar day : dayCells.keySet()) {
                // Determine day color by the amount of the steps goal reached
                DashboardFragment.DashboardData dashboardData = new DashboardFragment.DashboardData();
                dashboardData.showAllDevices = showAllDevices;
                dashboardData.showDeviceList = showDeviceList;
                dashboardData.timeTo = (int) (day.getTimeInMillis() / 1000);
                dashboardData.timeFrom = DateTimeUtils.shiftDays(dashboardData.timeTo, -1);
                float goalFactor = DashboardUtils.getStepsGoalFactor(dashboardData);
                @ColorInt int dayColor;
                if (goalFactor >= 1) {
                    dayColor = Color.argb(128, 0, 255, 0); // Green
                } else if (goalFactor >= 0.75) {
                    dayColor = Color.argb(128, 0, 128, 0); // Dark green
                } else if (goalFactor >= 0.5) {
                    dayColor = Color.argb(128, 255, 255, 0); // Yellow
                } else if (goalFactor > 0.25) {
                    dayColor = Color.argb(128, 255, 128, 0); // Orange
                } else if (goalFactor > 0) {
                    dayColor = Color.argb(128, 255, 0, 0); // Red
                } else {
                    dayColor = Color.argb(50, 128, 128, 128);
                }
                dayColors.put(day.get(Calendar.DAY_OF_MONTH), dayColor);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            for (Map.Entry<Calendar, TextView> entry : dayCells.entrySet()) {
                Calendar day = entry.getKey();
                TextView text = entry.getValue();
                @ColorInt int dayColor;
                try {
                    dayColor = dayColors.get(day.get(Calendar.DAY_OF_MONTH));
                } catch (NullPointerException e) {
                    continue;
                }
                final long timestamp = day.getTimeInMillis();
                // Draw colored circle
                GradientDrawable backgroundDrawable = new GradientDrawable();
                backgroundDrawable.setShape(GradientDrawable.OVAL);
                backgroundDrawable.setColor(dayColor);
                if (DateTimeUtils.isSameDay(day, currentDay)) {
                    GradientDrawable borderDrawable = new GradientDrawable();
                    borderDrawable.setShape(GradientDrawable.OVAL);
                    borderDrawable.setColor(Color.TRANSPARENT);
                    borderDrawable.setStroke(5, GBApplication.getTextColor(getApplicationContext()));
                    LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, borderDrawable});
                    text.setBackground(layerDrawable);
                } else {
                    text.setBackground(backgroundDrawable);
                }
                text.setOnClickListener(v -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_TIMESTAMP, timestamp);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
        }
    }
}
