/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ACTIVITY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;

public class HeartRateCapability {
    public enum MeasurementInterval {
        OFF(0, R.string.off),
        SMART(-1, R.string.smart),
        MINUTES_1(60, R.string.interval_one_minute),
        MINUTES_5(300, R.string.interval_five_minutes),
        MINUTES_10(600, R.string.interval_ten_minutes),
        MINUTES_15(900, R.string.interval_fifteen_minutes),
        MINUTES_30(1800, R.string.interval_thirty_minutes),
        MINUTES_45(2700, R.string.interval_forty_five_minutes),
        HOUR_1(3600, R.string.interval_one_hour),
        ;

        private final int intervalSeconds;
        private final int label;

        MeasurementInterval(final int intervalSeconds, final int label) {
            this.intervalSeconds = intervalSeconds;
            this.label = label;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public int getLabel() {
            return label;
        }

        public String getLabel(final Context context) {
            return context.getString(label);
        }
    }

    public void registerPreferences(final Context context, final List<MeasurementInterval> intervals, final DeviceSpecificSettingsHandler handler) {
        final Preference enableHeartrateSleepSupport = handler.findPreference(PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION);
        if (enableHeartrateSleepSupport != null) {
            enableHeartrateSleepSupport.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    GBApplication.deviceService(handler.getDevice()).onEnableHeartRateSleepSupport(Boolean.TRUE.equals(newVal));
                    return true;
                }
            });
        }

        handler.addPreferenceHandlerFor(PREF_HEARTRATE_ALERT_HIGH_THRESHOLD);
        handler.addPreferenceHandlerFor(PREF_HEARTRATE_ALERT_LOW_THRESHOLD);

        final ListPreference heartrateMeasurementInterval = handler.findPreference(PREF_HEARTRATE_MEASUREMENT_INTERVAL);
        final ListPreference heartrateAlertHigh = handler.findPreference(PREF_HEARTRATE_ALERT_HIGH_THRESHOLD);
        final ListPreference heartrateAlertLow = handler.findPreference(PREF_HEARTRATE_ALERT_LOW_THRESHOLD);
        // Newer devices that have low alert threshold can only use it if measurement interval is smart (-1) or 1 minute
        final boolean hrAlertsNeedSmartOrOne = heartrateAlertHigh != null && heartrateAlertLow != null && heartrateMeasurementInterval != null;
        if (hrAlertsNeedSmartOrOne) {
            final boolean hrMonitoringIsSmartOrOne = heartrateMeasurementInterval.getValue().equals("60") ||
                    heartrateMeasurementInterval.getValue().equals("-1");

            heartrateAlertHigh.setEnabled(hrMonitoringIsSmartOrOne);
            heartrateAlertLow.setEnabled(hrMonitoringIsSmartOrOne);
        }

        if (heartrateMeasurementInterval != null) {
            // Set the measurement intervals dynamically, as per the device's capability
            final CharSequence[] entries = new CharSequence[intervals.size()];
            final CharSequence[] values = new CharSequence[intervals.size()];
            for (int i = 0; i < intervals.size(); i++) {
                entries[i] = intervals.get(i).getLabel(context);
                values[i] = String.valueOf(intervals.get(i).getIntervalSeconds());
            }
            heartrateMeasurementInterval.setEntries(entries);
            heartrateMeasurementInterval.setEntryValues(values);

            final SwitchPreference activityMonitoring = handler.findPreference(PREF_HEARTRATE_ACTIVITY_MONITORING);
            final SwitchPreference heartrateAlertEnabled = handler.findPreference(PREF_HEARTRATE_ALERT_ENABLED);
            final SwitchPreference stressMonitoring = handler.findPreference(PREF_HEARTRATE_STRESS_MONITORING);

            heartrateMeasurementInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(final Preference preference, final Object newVal) {
                    GBApplication.deviceService(handler.getDevice()).onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));

                    final boolean isMeasurementIntervalEnabled = !newVal.equals("0");

                    if (activityMonitoring != null) {
                        activityMonitoring.setEnabled(isMeasurementIntervalEnabled);
                    }
                    if (heartrateAlertEnabled != null) {
                        heartrateAlertEnabled.setEnabled(isMeasurementIntervalEnabled);
                    }
                    if (hrAlertsNeedSmartOrOne) {
                        // Same as above, check if smart or 1 minute
                        final boolean hrMonitoringIsSmartOrOne = newVal.equals("60") || newVal.equals("-1");

                        heartrateAlertHigh.setEnabled(hrMonitoringIsSmartOrOne);
                        heartrateAlertLow.setEnabled(hrMonitoringIsSmartOrOne);
                    }
                    if (stressMonitoring != null && !hrAlertsNeedSmartOrOne) {
                        // Newer devices (that have hrAlertsNeedSmartOrOne) also don't need HR monitoring for stress monitoring
                        stressMonitoring.setEnabled(isMeasurementIntervalEnabled);
                    }

                    return true;
                }
            });

            final boolean isMeasurementIntervalEnabled = !heartrateMeasurementInterval.getValue().equals("0");

            if (activityMonitoring != null) {
                activityMonitoring.setEnabled(isMeasurementIntervalEnabled);
            }
            if (heartrateAlertEnabled != null) {
                heartrateAlertEnabled.setEnabled(isMeasurementIntervalEnabled);
            }
            if (stressMonitoring != null && !hrAlertsNeedSmartOrOne) {
                // Newer devices (that have hrAlertsNeedSmartOrOne) also don't need HR monitoring for stress monitoring
                stressMonitoring.setEnabled(isMeasurementIntervalEnabled);
            }
        }
    }
}
