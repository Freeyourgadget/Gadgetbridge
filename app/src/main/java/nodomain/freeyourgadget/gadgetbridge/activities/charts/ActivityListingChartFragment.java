/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, Pavel Elagin

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class ActivityListingChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivityListingChartFragment.class);
    int tsDateFrom;

    private View rootView;
    private ActivityListingAdapter stepListAdapter;
    private TextView stepsDateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_steps_list, container, false);
        getChartsHost().enableSwipeRefresh(false);
        ListView stepsList = rootView.findViewById(R.id.itemListView);
        stepListAdapter = new ActivityListingAdapter(getContext());
        stepsList.setAdapter(stepListAdapter);
        stepsDateView = rootView.findViewById(R.id.stepsDateView);
        refresh();
        return rootView;
    }


    @Override
    public String getTitle() {
        return "Steps list";
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ChartsHost.REFRESH)) {
            // TODO: use LimitLines to visualize smart alarms?
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> activitySamples;
        activitySamples = getSamples(db, device);
        List<ActivitySession> stepSessions = null;
        StepAnalysis stepAnalysis = new StepAnalysis();
        boolean isEmptySummary = false;

        if (activitySamples != null) {
            stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
            if (stepSessions.toArray().length == 0) {
                isEmptySummary = true;
            }
            stepSessions = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
        }
        return new MyChartsData(stepSessions);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;
        if (mcd.getStepSessions().toArray().length == 0) {
            getChartsHost().enableSwipeRefresh(true); //enable pull to refresh, might be needed
        } else {
            getChartsHost().enableSwipeRefresh(false); //disable pull to refresh as it collides with swipable view
        }

        stepsDateView.setText(DateTimeUtils.formatDate(new Date(tsDateFrom * 1000L)));
        stepListAdapter.setItems(mcd.getStepSessions(), true);
    }

    @Override
    protected void renderCharts() {
            }

    @Override
    protected void setupLegend(Chart chart) {
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(tsTo * 1000L); //we need today initially, which is the end of the time range
        day.set(Calendar.HOUR_OF_DAY, 0); //and we set time for the start and end of the same day
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        tsFrom = (int) (day.getTimeInMillis() / 1000);
        tsTo = tsFrom + 24 * 60 * 60 - 1;
        tsDateFrom = tsFrom;
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    private static class MyChartsData extends ChartsData {
        private final List<ActivitySession> stepSessions;

        MyChartsData(List<ActivitySession> stepSessions) {
            this.stepSessions = stepSessions;
        }

        public List<ActivitySession> getStepSessions() {
            return stepSessions;
        }
    }


}
