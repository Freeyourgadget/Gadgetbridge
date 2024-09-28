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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StressChartFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;

public class DashboardStressData implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardStressData.class);

    public int value;
    public int[] ranges;
    public int[] totalTime;

    public static DashboardStressData compute(final DashboardData dashboardData) {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();

        GBDevice stressDevice = null;
        double averageStress = -1;

        final int[] totalTime = new int[StressChartFragment.StressType.values().length];

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsStressMeasurement()) {
                    final List<? extends StressSample> samples = dev.getDeviceCoordinator()
                            .getStressSampleProvider(dev, dbHandler.getDaoSession())
                            .getAllSamples(dashboardData.timeFrom * 1000L, dashboardData.timeTo * 1000L);

                    if (!samples.isEmpty()) {
                        stressDevice = dev;
                        final int[] stressRanges = dev.getDeviceCoordinator().getStressRanges();
                        averageStress = samples.stream()
                                .mapToInt(StressSample::getStress)
                                .peek(stress -> {
                                    final StressChartFragment.StressType stressType = StressChartFragment.StressType.fromStress(stress, stressRanges);
                                    if (stressType != StressChartFragment.StressType.UNKNOWN) {
                                        totalTime[stressType.ordinal() - 1] += 60;
                                    }
                                })
                                .average()
                                .orElse(0);
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Could not compute stress", e);
        }

        if (stressDevice != null) {
            final DashboardStressData stressData = new DashboardStressData();
            stressData.value = (int) Math.round(averageStress);
            stressData.ranges = stressDevice.getDeviceCoordinator().getStressRanges();
            stressData.totalTime = totalTime;

            return stressData;
        }

        return null;
    }
}
