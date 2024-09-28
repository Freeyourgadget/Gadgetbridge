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
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardData;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardSleepWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardSleepWidget extends AbstractGaugeWidget {
    public DashboardSleepWidget() {
        super(R.string.menuitem_sleep, "sleep");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardSleepWidget.
     */
    public static DashboardSleepWidget newInstance(final DashboardData dashboardData) {
        final DashboardSleepWidget fragment = new DashboardSleepWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsSleepMeasurement();
    }

    @Override
    protected void populateData(final DashboardData dashboardData) {
        dashboardData.getSleepMinutesTotal();
        dashboardData.getSleepMinutesGoalFactor();
    }

    @Override
    protected void draw(final DashboardData dashboardData) {
        final long totalSleepMinutes = dashboardData.getSleepMinutesTotal();
        final String valueText = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(totalSleepMinutes / 60f),
                (int) (totalSleepMinutes % 60f)
        );

        setText(valueText);
        drawSimpleGauge(
                color_light_sleep,
                dashboardData.getSleepMinutesGoalFactor()
        );
    }
}
