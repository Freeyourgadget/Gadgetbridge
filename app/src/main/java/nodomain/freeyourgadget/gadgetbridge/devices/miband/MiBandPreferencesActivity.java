/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Christian
    Fischer, Daniele Gobbetti, José Rebelo, Szymon Tomasz Stefanek

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_ALARM_CLOCK;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_INCOMING_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ENABLE_TEXT_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_GOAL_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ADDRESS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_USER_ALIAS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefKey;

public class MiBandPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.miband_preferences);

        addTryListeners();

        Prefs prefs = GBApplication.getPrefs();

        final Preference enableHeartrateSleepSupport = findPreference(PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION);
        enableHeartrateSleepSupport.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onEnableHeartRateSleepSupport(Boolean.TRUE.equals(newVal));
                return true;
            }
        });

        final Preference heartrateMeasurementInterval = findPreference("heartrate_measurement_interval");
        heartrateMeasurementInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));
                return true;
            }
        });

        final Preference goalNotification = findPreference(PREF_MI2_GOAL_NOTIFICATION);
        goalNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_GOAL_NOTIFICATION);
                    }
                });
                return true;
            }
        });

        final Preference setDateFormat = findPreference(PREF_MI2_DATEFORMAT);
        setDateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_DATEFORMAT);
                    }
                });
                return true;
            }
        });

        final Preference displayPages = findPreference(PREF_MI2_DISPLAY_ITEMS);
        displayPages.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_DISPLAY_ITEMS);
                    }
                });
                return true;
            }
        });

        final Preference activateDisplayOnLift = findPreference(PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT);
        activateDisplayOnLift.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT);
                    }
                });
                return true;
            }
        });

        final Preference rotateWristCycleInfo = findPreference(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
        rotateWristCycleInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarnings = findPreference(PREF_MI2_INACTIVITY_WARNINGS);
        inactivityWarnings.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsThreshold = findPreference(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD);
        inactivityWarningsThreshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsStart = findPreference(PREF_MI2_INACTIVITY_WARNINGS_START);
        inactivityWarningsStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_START);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsEnd = findPreference(PREF_MI2_INACTIVITY_WARNINGS_END);
        inactivityWarningsEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_END);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsDnd = findPreference(PREF_MI2_INACTIVITY_WARNINGS_DND);
        inactivityWarningsDnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsDndStart = findPreference(PREF_MI2_INACTIVITY_WARNINGS_DND_START);
        inactivityWarningsDndStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND_START);
                    }
                });
                return true;
            }
        });

        final Preference inactivityWarningsDndEnd = findPreference(PREF_MI2_INACTIVITY_WARNINGS_DND_END);
        inactivityWarningsDndEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND_END);
                    }
                });
                return true;
            }
        });

        String doNotDisturbState = prefs.getString(MiBandConst.PREF_MI2_DO_NOT_DISTURB, PREF_MI2_DO_NOT_DISTURB_OFF);
        boolean doNotDisturbScheduled = doNotDisturbState.equals(PREF_MI2_DO_NOT_DISTURB_SCHEDULED);

        final Preference doNotDisturbStart = findPreference(PREF_MI2_DO_NOT_DISTURB_START);
        doNotDisturbStart.setEnabled(doNotDisturbScheduled);
        doNotDisturbStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB_START);
                    }
                });
                return true;
            }
        });

        final Preference doNotDisturbEnd = findPreference(PREF_MI2_DO_NOT_DISTURB_END);
        doNotDisturbEnd.setEnabled(doNotDisturbScheduled);
        doNotDisturbEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB_END);
                    }
                });
                return true;
            }
        });

        final Preference doNotDisturb = findPreference(PREF_MI2_DO_NOT_DISTURB);
        doNotDisturb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                final boolean scheduled = PREF_MI2_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());

                doNotDisturbStart.setEnabled(scheduled);
                doNotDisturbEnd.setEnabled(scheduled);

                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB);
                    }
                });
                return true;
            }
        });

        final Preference fitnessGoal = findPreference(ActivityUser.PREF_USER_STEPS_GOAL);
        fitnessGoal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ActivityUser.PREF_USER_STEPS_GOAL);
                    }
                });
                return true;
            }
        });
    }

    /**
     * delayed execution so that the preferences are applied first
      */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final Preference developmentMiaddr = findPreference(PREF_MIBAND_ADDRESS);
        developmentMiaddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });
    }

    private void addTryListeners() {
        for (final NotificationType type : NotificationType.values()) {
            String prefKey = "mi_try_" + type.getGenericType();
            final Preference tryPref = findPreference(prefKey);
            if (tryPref != null) {
                tryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        tryVibration(type);
                        return true;
                    }
                });
            } else {
                GB.toast(getBaseContext(), "Unable to find preference key: " + prefKey + ", trying the vibration won't work", Toast.LENGTH_LONG, GB.WARN);
            }
        }
    }

    private void tryVibration(NotificationType type) {
        NotificationSpec spec = new NotificationSpec();
        spec.type = type;
        GBApplication.deviceService().onNotification(spec);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        Set<String> prefKeys = new HashSet<>();
        prefKeys.add(PREF_USER_ALIAS);
        prefKeys.add(PREF_MIBAND_ADDRESS);
        prefKeys.add(ActivityUser.PREF_USER_STEPS_GOAL);
        prefKeys.add(PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR);
        prefKeys.add(PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS);
        prefKeys.add(PREF_MI2_ENABLE_TEXT_NOTIFICATIONS);
        prefKeys.add(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD);
        prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_ALARM_CLOCK));
        prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_INCOMING_CALL));

        for (NotificationType type : NotificationType.values()) {
            String key = type.getGenericType();
            prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, key));
        }

        return prefKeys.toArray(new String[0]);
    }
}
