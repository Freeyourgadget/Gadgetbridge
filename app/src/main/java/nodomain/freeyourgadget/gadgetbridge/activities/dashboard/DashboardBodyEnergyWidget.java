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

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;

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
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;

public class DashboardBodyEnergyWidget extends AbstractGaugeWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardBodyEnergyWidget.class);

    public DashboardBodyEnergyWidget() {
        super(R.string.body_energy, "bodyenergy");
    }

    public static DashboardBodyEnergyWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardBodyEnergyWidget fragment = new DashboardBodyEnergyWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsBodyEnergy();
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);

        final boolean isToday = DateUtils.isToday(dashboardData.timeTo * 1000L);

        final BodyEnergyData data = new BodyEnergyData();
        data.isToday = isToday;

        if (isToday) {
            // Latest stress sample for today
            BodyEnergySample sample = null;

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    final BodyEnergySample latestSample = dev.getDeviceCoordinator().getBodyEnergySampleProvider(dev, dbHandler.getDaoSession())
                            .getLatestSample();

                    if (latestSample != null && (sample == null || latestSample.getTimestamp() > sample.getTimestamp())) {
                        sample = latestSample;
                    }
                }

                if (sample != null) {
                    data.value = sample.getEnergy();
                }

            } catch (final Exception e) {
                LOG.error("Could not get body energy for today", e);
            }
        } else {
            // Gain / loss for the period
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsBodyEnergy()) {
                        final List<? extends BodyEnergySample> samples = dev.getDeviceCoordinator()
                                .getBodyEnergySampleProvider(dev, dbHandler.getDaoSession())
                                .getAllSamples(dashboardData.timeFrom * 1000L, dashboardData.timeTo * 1000L);

                        if (samples.size() > 1) {
                            int gained = 0;
                            int lost = 0;
                            for (int i = 1; i < samples.size(); i++) {
                                final BodyEnergySample s1 = samples.get(i - 1);
                                final BodyEnergySample s2 = samples.get(i);
                                if (s2.getEnergy() > s1.getEnergy()) {
                                    gained += s2.getEnergy() - s1.getEnergy();
                                } else {
                                    lost += s1.getEnergy() - s2.getEnergy();
                                }
                            }

                            data.gained = gained;
                            data.lost = lost;
                        }
                    }
                }
            } catch (final Exception e) {
                LOG.error("Could not calculate average stress", e);
            }
        }

        dashboardData.put("bodyenergy", data);
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final BodyEnergyData bodyEnergyData = (BodyEnergyData) dashboardData.get("bodyenergy");
        if (bodyEnergyData == null) {
            drawSimpleGauge(0, -1);
            return;
        }

        final int colorEnergy = ContextCompat.getColor(GBApplication.getContext(), R.color.body_energy_level_color);

        if (bodyEnergyData.isToday) {
            setText(String.valueOf(bodyEnergyData.value));
            drawSimpleGauge(
                    colorEnergy,
                    bodyEnergyData.value / 100f
            );
        } else {
            final int diff = bodyEnergyData.gained - bodyEnergyData.lost;

            final SpannableString spanGain = new SpannableString("↑" + bodyEnergyData.gained);
            final SpannableString spanLost = new SpannableString("↓" + bodyEnergyData.lost);
            spanGain.setSpan(new RelativeSizeSpan(0.65f), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanLost.setSpan(new RelativeSizeSpan(0.65f), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            setText(TextUtils.concat(spanGain, " ", spanLost));
            drawSimpleGauge(
                    colorEnergy,
                    Math.abs(diff) / 100f
            );

            final int[] colors = {
                    colorEnergy,
                    ContextCompat.getColor(GBApplication.getContext(), R.color.body_energy_lost_color)
            };
            final float[] segments = {
                    bodyEnergyData.gained / (float) (bodyEnergyData.gained + bodyEnergyData.lost),
                    bodyEnergyData.lost / (float) (bodyEnergyData.gained + bodyEnergyData.lost),
            };

            drawSegmentedGauge(
                    colors,
                    segments,
                    -1,
                    false,
                    true
            );
        }
    }

    private static class BodyEnergyData implements Serializable {
        private int value;
        private int gained;
        private int lost;
        private boolean isToday;
    }
}
