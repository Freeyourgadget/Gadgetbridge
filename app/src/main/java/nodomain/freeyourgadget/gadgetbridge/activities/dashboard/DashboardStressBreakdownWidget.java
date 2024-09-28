/*  Copyright (C) 2024 José Rebelo

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

import androidx.core.content.ContextCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardData;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardStressData;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class DashboardStressBreakdownWidget extends AbstractGaugeWidget {
    public DashboardStressBreakdownWidget() {
        super(R.string.menuitem_stress, "stress");
    }

    public static DashboardStressBreakdownWidget newInstance(final DashboardData dashboardData) {
        final DashboardStressBreakdownWidget fragment = new DashboardStressBreakdownWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsStressMeasurement();
    }

    @Override
    protected void populateData(final DashboardData dashboardData) {
        dashboardData.computeIfAbsent("stress", () -> DashboardStressData.compute(dashboardData));
    }

    @Override
    protected void draw(final DashboardData dashboardData) {
        final DashboardStressData stressData = (DashboardStressData) dashboardData.get("stress");
        if (stressData == null) {
            drawSimpleGauge(0, -1);
            return;
        }

        final int[] colors = new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_relaxed),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_mild),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_moderate),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_high),
        };

        final float[] segments = new float[4];

        int sum = 0;
        for (final int stressTime : stressData.totalTime) {
            sum += stressTime;
        }
        if (sum != 0) {
            for (int i = 0; i < 4; i++) {
                segments[i] = stressData.totalTime[i] / (float) sum;
            }
        }

        setText(String.valueOf(stressData.value));

        drawSegmentedGauge(
                colors,
                segments,
                -1,
                false,
                true
        );
    }
}
