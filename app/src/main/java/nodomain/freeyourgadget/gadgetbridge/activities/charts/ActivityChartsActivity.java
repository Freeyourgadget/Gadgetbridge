/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractFragmentPagerAdapter;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ActivityChartsActivity extends AbstractChartsActivity {
    LimitedQueue<Integer, ActivityAmounts> mActivityAmountCache = new LimitedQueue<>(60);

    @Override
    protected AbstractFragmentPagerAdapter createFragmentPagerAdapter(final FragmentManager fragmentManager) {
        return new SectionsPagerAdapter(fragmentManager);
    }

    @Override
    protected int getRecordedDataType() {
        return RecordedDataTypes.TYPE_ACTIVITY | RecordedDataTypes.TYPE_STRESS;
    }

    @Override
    protected boolean supportsRefresh() {
        final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
        return coordinator.supportsActivityDataFetching();
    }

    @Override
    protected boolean allowRefresh() {
        final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
        return coordinator.allowFetchActivityData(getDevice()) && supportsRefresh();
    }

    @Override
    protected List<String> fillChartsTabsList() {
        return fillChartsTabsList(getDevice(), this);
    }

    private static List<String> fillChartsTabsList(final GBDevice device, final Context context) {
        final List<String> tabList;
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final String myTabs = prefs.getString(DeviceSettingsPreferenceConst.PREFS_DEVICE_CHARTS_TABS, null);

        if (myTabs == null) {
            //make list mutable to be able to remove items later
            tabList = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_charts_tabs_items_default)));
        } else {
            tabList = new ArrayList<>(Arrays.asList(myTabs.split(",")));
        }
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!coordinator.supportsActivityTabs()) {
            tabList.remove("activity");
            tabList.remove("activitylist");
        }
        if (!coordinator.supportsSleepMeasurement()) {
            tabList.remove("sleep");
        }
        if (!coordinator.supportsStressMeasurement()) {
            tabList.remove("stress");
        }
        if (!coordinator.supportsPai()) {
            tabList.remove("pai");
        }
        if (!coordinator.supportsSpo2(device)) {
            tabList.remove("spo2");
        }
        if (!coordinator.supportsStepCounter()) {
            tabList.remove("stepsweek");
        }
        if (!coordinator.supportsSpeedzones()) {
            tabList.remove("speedzones");
        }
        if (!coordinator.supportsRealtimeData()) {
            tabList.remove("livestats");
        }
        if (!coordinator.supportsTemperatureMeasurement()) {
            tabList.remove("temperature");
        }
        if (!coordinator.supportsCyclingData()) {
            tabList.remove("cycling");
        }
        if (!coordinator.supportsWeightMeasurement()) {
            tabList.remove("weight");
        }
        if (!coordinator.supportsHrvMeasurement()) {
            tabList.remove("hrvstatus");
        }
        if (!coordinator.supportsHeartRateMeasurement(device)) {
            tabList.remove("heartrate");
        }
        if (!coordinator.supportsBodyEnergy()) {
            tabList.remove("bodyenergy");
        }
        if (!coordinator.supportsVO2Max()) {
            tabList.remove("vo2max");
        }
        if (!coordinator.supportsActiveCalories()) {
            tabList.remove("calories");
        }
        if (!coordinator.supportsRespiratoryRate()) {
            tabList.remove("respiratoryrate");
        }
        return tabList;
    }

    public static int getChartsTabIndex(final String tab, final GBDevice device, final Context context) {
        final List<String> enabledTabsList = fillChartsTabsList(device, context);
        return enabledTabsList.indexOf(tab);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends AbstractFragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
            // getItem is called to instantiate the fragment for the given page.
            switch (enabledTabsList.get(position)) {
                case "activity":
                    return new ActivitySleepChartFragment();
                case "activitylist":
                    return new ActivityListingChartFragment();
                case "sleep":
                    return SleepCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "heartrate":
                    return new HeartRateDailyFragment();
                case "hrvstatus":
                    return new HRVStatusFragment();
                case "bodyenergy":
                    return new BodyEnergyFragment();
                case "vo2max":
                    return new VO2MaxFragment();
                case "stress":
                    return new StressChartFragment();
                case "pai":
                    return new PaiChartFragment();
                case "stepsweek":
                    return StepsCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "speedzones":
                    return new SpeedZonesFragment();
                case "livestats":
                    return new LiveActivityFragment();
                case "spo2":
                    return new Spo2ChartFragment();
                case "temperature":
                    return coordinator.supportsContinuousTemperature()? new TemperatureDailyFragment(): new TemperatureChartFragment();
                case "cycling":
                    return new CyclingChartFragment();
                case "weight":
                    return new WeightChartFragment();
                case "calories":
                    Intent intent = getIntent();
                    String mode = intent.getStringExtra(ActivityChartsActivity.EXTRA_MODE);
                    return CaloriesDailyFragment.newInstance(mode);
                case "respiratoryrate":
                    return RespiratoryRateCollectionFragment.newInstance(enabledTabsList.size() == 1);
            }

            return new UnknownFragment();
        }

        @Override
        public int getCount() {
            return enabledTabsList.toArray().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (enabledTabsList.get(position)) {
                case "activity":
                    return getString(R.string.activity_sleepchart_activity_and_sleep);
                case "activitylist":
                    return getString(R.string.charts_activity_list);
                case "sleep":
                    return getString(R.string.sleepchart_your_sleep);
                case "heartrate":
                    return getString(R.string.menuitem_hr);
                case "hrvstatus":
                    return getString(R.string.pref_header_hrv_status);
                case "bodyenergy":
                    return getString(R.string.body_energy);
                case "vo2max":
                    return getString(R.string.menuitem_vo2_max);
                case "stress":
                    return getString(R.string.menuitem_stress);
                case "pai":
                    return getString(getDevice().getDeviceCoordinator().getPaiName());
                case "stepsweek":
                    return getString(R.string.steps);
                case "speedzones":
                    return getString(R.string.stats_title);
                case "livestats":
                    return getString(R.string.liveactivity_live_activity);
                case "spo2":
                    return getString(R.string.pref_header_spo2);
                case "temperature":
                    return getString(R.string.menuitem_temperature);
                case "cycling":
                    return getString(R.string.title_cycling);
                case "weight":
                    return getString(R.string.menuitem_weight);
                case "calories":
                    return getString(R.string.calories);
                case "respiratoryrate":
                    return getString(R.string.respiratoryrate);
            }

            return String.format(Locale.getDefault(), "Unknown %d", position);
        }
    }

    /**
     * A dummy fragment to avoid a crash when we get a unknown tab position (eg. broken
     * preference migration).
     */
    public static class UnknownFragment extends AbstractGBFragment {
        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            return null;
        }

        @Nullable
        @Override
        protected CharSequence getTitle() {
            return "Unknown";
        }
    }
}
