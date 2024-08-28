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

import android.os.Bundle;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardActiveTimeWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardActiveTimeWidget extends AbstractGaugeWidget {
    public DashboardActiveTimeWidget() {
        super(R.string.activity_list_summary_active_time, "activity");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardActiveTimeWidget.
     */
    public static DashboardActiveTimeWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardActiveTimeWidget fragment = new DashboardActiveTimeWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        dashboardData.getActiveMinutesTotal();
        dashboardData.getActiveMinutesGoalFactor();
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final long totalActiveMinutes = dashboardData.getActiveMinutesTotal();
        final String valueText = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(totalActiveMinutes / 60f),
                (int) (totalActiveMinutes % 60f)
        );

        setText(valueText);

        drawSimpleGauge(
                color_active_time,
                dashboardData.getActiveMinutesGoalFactor()
        );
    }
}
