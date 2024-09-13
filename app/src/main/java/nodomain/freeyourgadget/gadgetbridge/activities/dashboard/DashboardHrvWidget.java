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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;

public class DashboardHrvWidget extends AbstractGaugeWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardHrvWidget.class);

    public DashboardHrvWidget() {
        super(R.string.hrv, "hrvstatus");
    }

    public static DashboardHrvWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardHrvWidget fragment = new DashboardHrvWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsHrvMeasurement();
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);

        HrvSummarySample latestSummary = null;

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                final List<? extends HrvSummarySample> deviceLatestSummaries = dev.getDeviceCoordinator().getHrvSummarySampleProvider(dev, dbHandler.getDaoSession())
                        .getAllSamples(dashboardData.timeFrom * 1000L, dashboardData.timeTo * 1000L);

                if (!deviceLatestSummaries.isEmpty() && (latestSummary == null || latestSummary.getTimestamp() < deviceLatestSummaries.get(deviceLatestSummaries.size() - 1).getTimestamp())) {
                    latestSummary = deviceLatestSummaries.get(deviceLatestSummaries.size() - 1);
                }
            }

            final HrvData hrvData = new HrvData();

            if (latestSummary != null) {
                hrvData.weeklyAverage = latestSummary.getWeeklyAverage() != null ? latestSummary.getWeeklyAverage() : 0;
                hrvData.lastNightAverage = latestSummary.getLastNightAverage() != null ? latestSummary.getLastNightAverage() : 0;
                hrvData.lastNight5MinHigh = latestSummary.getLastNight5MinHigh() != null ? latestSummary.getLastNight5MinHigh() : 0;
                hrvData.baselineLowUpper = latestSummary.getBaselineLowUpper() != null ? latestSummary.getBaselineLowUpper() : 0;
                hrvData.baselineBalancedLower = latestSummary.getBaselineBalancedLower() != null ? latestSummary.getBaselineBalancedLower() : 0;
                hrvData.baselineBalancedUpper = latestSummary.getBaselineBalancedUpper() != null ? latestSummary.getBaselineBalancedUpper() : 0;

                dashboardData.put("hrv", hrvData);
            }

        } catch (final Exception e) {
            LOG.error("Could not get hrv sample", e);
        }
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final int[] colors = new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.hrv_status_low),
                ContextCompat.getColor(GBApplication.getContext(), R.color.hrv_status_unbalanced),
                ContextCompat.getColor(GBApplication.getContext(), R.color.hrv_status_balanced),
                ContextCompat.getColor(GBApplication.getContext(), R.color.hrv_status_unbalanced),
        };

        final float[] segments = new float[]{
                0.125f, // low
                0.125f, // unbalanced
                0.5f, // normal
                0.25f, // unbalanced
        };

        final HrvData hrvData = (HrvData) dashboardData.get("hrv");

        final float value;
        final String valueText;
        if (hrvData != null && hrvData.weeklyAverage != 0 && hrvData.hasBaselines()) {
            valueText = getString(R.string.hrv_status_unit, hrvData.weeklyAverage);

            if (hrvData.weeklyAverage < hrvData.baselineLowUpper) {
                value = 0.125f * (float) GaugeDrawer.normalize(hrvData.weeklyAverage, 0f, hrvData.baselineLowUpper);
            } else if (hrvData.weeklyAverage < hrvData.baselineBalancedLower) {
                value = 0.125f + 0.125f * (float) GaugeDrawer.normalize((float) hrvData.weeklyAverage, hrvData.baselineLowUpper, hrvData.baselineBalancedLower);
            } else if (hrvData.weeklyAverage < hrvData.baselineBalancedUpper) {
                value = 0.125f + 0.125f + 0.5f * (float) GaugeDrawer.normalize((float) hrvData.weeklyAverage, hrvData.baselineBalancedLower, hrvData.baselineBalancedUpper);
            } else {
                value = 0.125f + 0.125f + 0.5f + 0.125f * (float) GaugeDrawer.normalize((float) hrvData.weeklyAverage, hrvData.baselineBalancedUpper, 2 * hrvData.baselineBalancedUpper);
            }
        } else {
            value = -1;
            valueText = getString(R.string.stats_empty_value);
        }

        setText(valueText);
        drawSegmentedGauge(
                colors,
                segments,
                value,
                false,
                true
        );
    }

    private static class HrvData implements Serializable {
        private int weeklyAverage;
        private int lastNightAverage;
        private int lastNight5MinHigh;
        private int baselineLowUpper;
        private int baselineBalancedLower;
        private int baselineBalancedUpper;
        private int statusNum;

        public boolean hasBaselines() {
            return baselineLowUpper != 0 && baselineBalancedLower != 0 && baselineBalancedUpper != 0;
        }
    }
}
