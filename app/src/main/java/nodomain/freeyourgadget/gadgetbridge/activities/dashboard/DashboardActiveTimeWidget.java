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

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardActiveTimeWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardActiveTimeWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardActiveTimeWidget.class);
    private TextView activeTime;
    private ImageView activeTimeGauge;

    public DashboardActiveTimeWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardActiveTimeWidget.
     */
    public static DashboardActiveTimeWidget newInstance(DashboardFragment.DashboardData dashboardData) {
        DashboardActiveTimeWidget fragment = new DashboardActiveTimeWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dashboard_widget_active_time, container, false);
        activeTime = fragmentView.findViewById(R.id.activetime_text);
        activeTimeGauge = fragmentView.findViewById(R.id.activetime_gauge);

        fillData();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activeTime != null && activeTimeGauge != null) fillData();
    }

    @Override
    protected void fillData() {
        if (activeTimeGauge == null) return;
        activeTimeGauge.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            dashboardData.getActiveMinutesTotal();
            dashboardData.getActiveMinutesGoalFactor();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);

            // Update text representation
            long totalActiveMinutes = dashboardData.getActiveMinutesTotal();
            String activeHours = String.format("%d", (int) Math.floor(totalActiveMinutes / 60f));
            String activeMinutes = String.format("%02d", (int) (totalActiveMinutes % 60f));
            activeTime.setText(activeHours + ":" + activeMinutes);

            // Draw gauge
            activeTimeGauge.setImageBitmap(drawGauge(200, 15, color_active_time, dashboardData.getActiveMinutesGoalFactor()));
        }
    }
}