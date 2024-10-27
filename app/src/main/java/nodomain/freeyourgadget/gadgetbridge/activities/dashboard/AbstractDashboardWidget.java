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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractDashboardWidget extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardWidget.class);

    protected static String ARG_DASHBOARD_DATA = "dashboard_widget_argument_data";

    protected DashboardFragment.DashboardData dashboardData;

    protected @ColorInt int color_unknown = Color.argb(25, 128, 128, 128);
    protected @ColorInt int color_not_worn = Color.BLACK;
    protected @ColorInt int color_worn = Color.rgb(128, 128, 128);
    protected @ColorInt int color_activity = Color.GREEN;
    protected @ColorInt int color_exercise = Color.rgb(255, 128, 0);
    protected @ColorInt int color_deep_sleep = Color.rgb(0, 84, 163);
    protected @ColorInt int color_light_sleep = Color.rgb(7, 158, 243);
    protected @ColorInt int color_rem_sleep = Color.rgb(228, 39, 199);
    protected @ColorInt int color_awake_sleep = Color.rgb(0xff, 0x86, 0x6e);
    protected @ColorInt int color_distance = Color.BLUE;
    protected @ColorInt int color_active_time = Color.rgb(170, 0, 255);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dashboardData = (DashboardFragment.DashboardData) getArguments().getSerializable(ARG_DASHBOARD_DATA);
        }
    }

    public void update() {
        fillData();
    }

    protected abstract void fillData();

    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsActivityTracking();
    }

    protected List<GBDevice> getSupportedDevices(final DashboardFragment.DashboardData dashboardData) {
        return GBApplication.app().getDeviceManager().getDevices()
                .stream()
                .filter(dev -> dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress()))
                .filter(this::isSupportedBy)
                .collect(Collectors.toList());
    }

    protected void onClickOpenChart(final View view, final String chart, final int label, final String mode) {
        view.setOnClickListener(v -> {
            chooseDevice(dashboardData, device -> {
                final Intent startIntent;
                startIntent = new Intent(requireContext(), ActivityChartsActivity.class);
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                startIntent.putExtra(ActivityChartsActivity.EXTRA_SINGLE_FRAGMENT_NAME, chart);
                startIntent.putExtra(ActivityChartsActivity.EXTRA_ACTIONBAR_TITLE, label);
                startIntent.putExtra(ActivityChartsActivity.EXTRA_TIMESTAMP, dashboardData.timeTo);
                startIntent.putExtra(ActivityChartsActivity.EXTRA_MODE, mode);
                requireContext().startActivity(startIntent);
            });
        });
    }

    protected void chooseDevice(final DashboardFragment.DashboardData dashboardData,
                                final Consumer<GBDevice> consumer) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);

        if (devices.size() == 1) {
            consumer.accept(devices.get(0));
            return;
        }

        if (devices.isEmpty()) {
            GB.toast(GBApplication.getContext(), R.string.no_supported_devices_found, Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        final String[] deviceNames = devices.stream()
                .map(GBDevice::getAliasOrName)
                .toArray(String[]::new);

        final Context activity = getActivity();
        if (activity == null) {
            return;
        }

        new MaterialAlertDialogBuilder(activity)
                .setCancelable(true)
                .setTitle(R.string.choose_device)
                .setItems(deviceNames, (dialog, which) -> consumer.accept(devices.get(which)))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                })
                .show();
    }
}
