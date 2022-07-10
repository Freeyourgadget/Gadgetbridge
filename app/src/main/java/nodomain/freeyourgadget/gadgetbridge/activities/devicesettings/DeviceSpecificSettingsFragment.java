/*  Copyright (C) 2019-2020 Andreas Shimokawa, Cre3per

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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.mobeta.android.dslv.DragSortListPreference;
import com.mobeta.android.dslv.DragSortListPreferenceFragment;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.CalBlacklistActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureWorldClocks;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreferenceFragment;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_EXPOSE_HR_THIRDPARTY;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_SWIPE_UNLOCK;

public class DeviceSpecificSettingsFragment extends PreferenceFragmentCompat implements DeviceSpecificSettingsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceSpecificSettingsFragment.class);

    static final String FRAGMENT_TAG = "DEVICE_SPECIFIC_SETTINGS_FRAGMENT";

    private final SharedPreferencesChangeHandler sharedPreferencesChangeHandler = new SharedPreferencesChangeHandler();

    private DeviceSpecificSettingsCustomizer deviceSpecificSettingsCustomizer;

    private GBDevice device;

    private void setSettingsFileSuffix(String settingsFileSuffix, @NonNull int[] supportedSettings, String[] supportedLanguages) {
        Bundle args = new Bundle();
        args.putString("settingsFileSuffix", settingsFileSuffix);
        args.putIntArray("supportedSettings", supportedSettings);
        args.putStringArray("supportedLanguages", supportedLanguages);
        setArguments(args);
    }

    private void setDeviceSpecificSettingsCustomizer(final DeviceSpecificSettingsCustomizer customizer) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("deviceSpecificSettingsCustomizer", customizer);
        setArguments(args);
    }

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        String settingsFileSuffix = arguments.getString("settingsFileSuffix", null);
        int[] supportedSettings = arguments.getIntArray("supportedSettings");
        String[] supportedLanguages = arguments.getStringArray("supportedLanguages");
        this.deviceSpecificSettingsCustomizer = arguments.getParcelable("deviceSpecificSettingsCustomizer");
        this.device = arguments.getParcelable("device");

        if (settingsFileSuffix == null || supportedSettings == null) {
            return;
        }

        getPreferenceManager().setSharedPreferencesName("devicesettings_" + settingsFileSuffix);

        if (rootKey == null) {
            // we are the main preference screen
            boolean first = true;
            for (int setting : supportedSettings) {
                if (first) {
                    setPreferencesFromResource(setting, null);
                    first = false;
                } else {
                    addPreferencesFromResource(setting);
                }
                if (setting == R.xml.devicesettings_language_generic) {
                    ListPreference languageListPreference = findPreference("language");
                    CharSequence[] entries = languageListPreference.getEntries();
                    CharSequence[] values = languageListPreference.getEntryValues();
                    for (int i=entries.length-1;i>=0;i--) {
                        if (!ArrayUtils.contains(supportedLanguages,values[i])) {
                            entries = ArrayUtils.remove(entries,i);
                            values = ArrayUtils.remove(values,i);
                        }
                    }
                    languageListPreference.setEntries(entries);
                    languageListPreference.setEntryValues(values);
                }
            }
        } else {
            // Now, this is ugly: search all the xml files for the rootKey
            for (int setting : supportedSettings) {
                try {
                    setPreferencesFromResource(setting, rootKey);
                } catch (Exception ignore) {
                    continue;
                }
                break;
            }
        }
        setChangeListener();
    }

    @Override
    public void onStart() {
        super.onStart();

        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        reloadPreferences(sharedPreferences, getPreferenceScreen());

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);
    }

    @Override
    public void onStop() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);

        super.onStop();
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    @Override
    public void notifyPreferenceChanged(final String preferenceKey) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                GBApplication.deviceService().onSendConfiguration(preferenceKey);
            }
        });
    }

    private void setChangeListener() {
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);

        final Prefs prefs = new Prefs(getPreferenceManager().getSharedPreferences());
        String disconnectNotificationState = prefs.getString(PREF_DISCONNECT_NOTIFICATION, PREF_DO_NOT_DISTURB_OFF);
        boolean disconnectNotificationScheduled = disconnectNotificationState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference disconnectNotificationStart = findPreference(PREF_DISCONNECT_NOTIFICATION_START);
        if (disconnectNotificationStart != null) {
            disconnectNotificationStart.setEnabled(disconnectNotificationScheduled);
            disconnectNotificationStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION_START);
                    return true;
                }
            });
        }

        final Preference disconnectNotificationEnd = findPreference(PREF_DISCONNECT_NOTIFICATION_END);
        if (disconnectNotificationEnd != null) {
            disconnectNotificationEnd.setEnabled(disconnectNotificationScheduled);
            disconnectNotificationEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION_END);
                    return true;
                }
            });
        }

        final Preference disconnectNotification = findPreference(PREF_DISCONNECT_NOTIFICATION);
        if (disconnectNotification != null) {
            disconnectNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(disconnectNotificationStart).setEnabled(scheduled);
                    Objects.requireNonNull(disconnectNotificationEnd).setEnabled(scheduled);
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION);
                    return true;
                }
            });

        }

        String nightModeState = prefs.getString(MiBandConst.PREF_NIGHT_MODE, PREF_NIGHT_MODE_OFF);
        boolean nightModeScheduled = nightModeState.equals(PREF_NIGHT_MODE_SCHEDULED);

        final Preference nightModeStart = findPreference(PREF_NIGHT_MODE_START);
        if (nightModeStart != null) {
            nightModeStart.setEnabled(nightModeScheduled);
            nightModeStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_NIGHT_MODE_START);
                    return true;
                }
            });
        }

        final Preference nightModeEnd = findPreference(PREF_NIGHT_MODE_END);
        if (nightModeEnd != null) {
            nightModeEnd.setEnabled(nightModeScheduled);
            nightModeEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_NIGHT_MODE_END);
                    return true;
                }
            });
        }

        final Preference nightMode = findPreference(PREF_NIGHT_MODE);
        if (nightMode != null) {

            nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_NIGHT_MODE_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(nightModeStart).setEnabled(scheduled);
                    Objects.requireNonNull(nightModeEnd).setEnabled(scheduled);

                    notifyPreferenceChanged(PREF_NIGHT_MODE);
                    return true;
                }
            });
        }


        String doNotDisturbState = prefs.getString(PREF_DO_NOT_DISTURB, PREF_DO_NOT_DISTURB_OFF);
        boolean doNotDisturbScheduled = doNotDisturbState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference doNotDisturbStart = findPreference(PREF_DO_NOT_DISTURB_START);
        if (doNotDisturbStart != null) {
            doNotDisturbStart.setEnabled(doNotDisturbScheduled);
            doNotDisturbStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB_START);
                    return true;
                }
            });
        }

        final Preference doNotDisturbEnd = findPreference(PREF_DO_NOT_DISTURB_END);
        if (doNotDisturbEnd != null) {
            doNotDisturbEnd.setEnabled(doNotDisturbScheduled);
            doNotDisturbEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB_END);
                    return true;
                }
            });
        }

        final Preference doNotDisturb = findPreference(PREF_DO_NOT_DISTURB);
        if (doNotDisturb != null) {
            doNotDisturb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(doNotDisturbStart).setEnabled(scheduled);
                    Objects.requireNonNull(doNotDisturbEnd).setEnabled(scheduled);

                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB);
                    return true;
                }
            });
        }

        final Preference enableHeartrateSleepSupport = findPreference(PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION);
        if (enableHeartrateSleepSupport != null) {
            enableHeartrateSleepSupport.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    GBApplication.deviceService().onEnableHeartRateSleepSupport(Boolean.TRUE.equals(newVal));
                    return true;
                }
            });
        }

        final ListPreference heartrateMeasurementInterval = findPreference(PREF_HEARTRATE_MEASUREMENT_INTERVAL);
        if (heartrateMeasurementInterval != null) {
            final SwitchPreference activityMonitoring = findPreference(PREF_HEARTRATE_ACTIVITY_MONITORING);
            final SwitchPreference heartrateAlertEnabled = findPreference(PREF_HEARTRATE_ALERT_ENABLED);
            final SwitchPreference stressMonitoring = findPreference(PREF_HEARTRATE_STRESS_MONITORING);

            heartrateMeasurementInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(final Preference preference, final Object newVal) {
                    GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));

                    final boolean isMeasurementIntervalEnabled = !newVal.equals("0");

                    if (activityMonitoring != null) {
                        activityMonitoring.setEnabled(isMeasurementIntervalEnabled);
                    }
                    if (heartrateAlertEnabled != null) {
                        heartrateAlertEnabled.setEnabled(isMeasurementIntervalEnabled);
                    }
                    if (stressMonitoring != null) {
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
            if (stressMonitoring != null) {
                stressMonitoring.setEnabled(isMeasurementIntervalEnabled);
            }
        }

        addPreferenceHandlerFor(PREF_SWIPE_UNLOCK);
        addPreferenceHandlerFor(PREF_MI2_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS_SORTABLE);
        addPreferenceHandlerFor(PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE);
        addPreferenceHandlerFor(PREF_SHORTCUTS);
        addPreferenceHandlerFor(PREF_SHORTCUTS_SORTABLE);
        addPreferenceHandlerFor(PREF_LANGUAGE);
        addPreferenceHandlerFor(PREF_EXPOSE_HR_THIRDPARTY);
        addPreferenceHandlerFor(PREF_BT_CONNECTED_ADVERTISEMENT);
        addPreferenceHandlerFor(PREF_WEARLOCATION);
        addPreferenceHandlerFor(PREF_VIBRATION_ENABLE);
        addPreferenceHandlerFor(PREF_NOTIFICATION_ENABLE);
        addPreferenceHandlerFor(PREF_SCREEN_ORIENTATION);
        addPreferenceHandlerFor(PREF_TIMEFORMAT);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_VIBRATION_STRENGH_PERCENTAGE);
        addPreferenceHandlerFor(PREF_POWER_MODE);
        addPreferenceHandlerFor(PREF_LIFTWRIST_NOSHED);
        addPreferenceHandlerFor(PREF_DISCONNECTNOTIF_NOSHED);
        addPreferenceHandlerFor(PREF_BUTTON_BP_CALIBRATE);
        addPreferenceHandlerFor(PREF_ALTITUDE_CALIBRATE);
        addPreferenceHandlerFor(PREF_INACTIVITY_ENABLE);
        addPreferenceHandlerFor(PREF_INACTIVITY_START);
        addPreferenceHandlerFor(PREF_INACTIVITY_END);
        addPreferenceHandlerFor(PREF_INACTIVITY_THRESHOLD);
        addPreferenceHandlerFor(PREF_INACTIVITY_THRESHOLD_EXTENDED);
        addPreferenceHandlerFor(PREF_INACTIVITY_MO);
        addPreferenceHandlerFor(PREF_INACTIVITY_TU);
        addPreferenceHandlerFor(PREF_INACTIVITY_WE);
        addPreferenceHandlerFor(PREF_INACTIVITY_TH);
        addPreferenceHandlerFor(PREF_INACTIVITY_FR);
        addPreferenceHandlerFor(PREF_INACTIVITY_SA);
        addPreferenceHandlerFor(PREF_INACTIVITY_SU);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND_START);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND_END);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SWITCH);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SLEEP);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_INTERVAL);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_START);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_END);
        addPreferenceHandlerFor(PREF_HEARTRATE_ACTIVITY_MONITORING);
        addPreferenceHandlerFor(PREF_HEARTRATE_ALERT_THRESHOLD);
        addPreferenceHandlerFor(PREF_HEARTRATE_ALERT_ENABLED);
        addPreferenceHandlerFor(PREF_HEARTRATE_STRESS_MONITORING);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_START);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_END);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_START);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_END);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_LIFT_WRIST);
        addPreferenceHandlerFor(PREF_FIND_PHONE);
        addPreferenceHandlerFor(PREF_FIND_PHONE_DURATION);
        addPreferenceHandlerFor(PREF_AUTOLIGHT);
        addPreferenceHandlerFor(PREF_AUTOREMOVE_MESSAGE);
        addPreferenceHandlerFor(PREF_AUTOREMOVE_NOTIFICATIONS);
        addPreferenceHandlerFor(PREF_KEY_VIBRATION);
        addPreferenceHandlerFor(PREF_OPERATING_SOUNDS);
        addPreferenceHandlerFor(PREF_FAKE_RING_DURATION);
        addPreferenceHandlerFor(PREF_ANTILOST_ENABLED);
        addPreferenceHandlerFor(PREF_HYDRATION_SWITCH);
        addPreferenceHandlerFor(PREF_HYDRATION_PERIOD);
        addPreferenceHandlerFor(PREF_AMPM_ENABLED);
        addPreferenceHandlerFor(PREF_SOUNDS);

        addPreferenceHandlerFor(PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_FORCE_WHITE_COLOR);
        addPreferenceHandlerFor(PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING);

        addPreferenceHandlerFor(PREF_SONYSWR12_STAMINA);
        addPreferenceHandlerFor(PREF_SONYSWR12_LOW_VIBRATION);
        addPreferenceHandlerFor(PREF_SONYSWR12_SMART_INTERVAL);

        addPreferenceHandlerFor(PREF_NOTHING_EAR1_INEAR);
        addPreferenceHandlerFor(PREF_NOTHING_EAR1_AUDIOMODE);

        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_VOLUME);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_LOCK_TOUCH);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_GAME_MODE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER_DOLBY);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER_MODE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_LEFT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_RIGHT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_LIVE_ANC);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRESSURE_RELIEF);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_ANC_LEVEL);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_SOUND);
        addPreferenceHandlerFor(PREF_GALAXY_PRO_DOUBLE_TAP_EDGE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_IN_EAR_DETECTION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_VOICE_DETECT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_VOICE_DETECT_DURATION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_BALANCE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_READ_NOTIFICATIONS_OUTLOUD);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_MODE_DURING_CALL);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_RIGHT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_LEFT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_SOUND_TONE);
        addPreferenceHandlerFor(PREFS_NOISE_CONTROL_WITH_ONE_EARBUD);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_MODE);
        addPreferenceHandlerFor(PREFS_GALAXY_BUDS_SEAMLESS_CONNECTION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH);

        addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL);
        addPreferenceHandlerFor(PREF_SONY_FOCUS_VOICE);
        addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_LEVEL);
        addPreferenceHandlerFor(PREF_SONY_SOUND_POSITION);
        addPreferenceHandlerFor(PREF_SONY_SURROUND_MODE);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_MODE);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_400);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_1000);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_2500);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_6300);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_16000);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BASS);
        addPreferenceHandlerFor(PREF_SONY_AUDIO_UPSAMPLING);
        addPreferenceHandlerFor(PREF_SONY_TOUCH_SENSOR);
        addPreferenceHandlerFor(PREF_SONY_PAUSE_WHEN_TAKEN_OFF);
        addPreferenceHandlerFor(PREF_SONY_BUTTON_MODE_LEFT);
        addPreferenceHandlerFor(PREF_SONY_BUTTON_MODE_RIGHT);
        addPreferenceHandlerFor(PREF_SONY_AUTOMATIC_POWER_OFF);
        addPreferenceHandlerFor(PREF_SONY_NOTIFICATION_VOICE_GUIDE);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_TIMEOUT);
        addPreferenceHandlerFor(PREF_SONY_CONNECT_TWO_DEVICES);

        addPreferenceHandlerFor(PREF_QC35_NOISE_CANCELLING_LEVEL);
        addPreferenceHandlerFor(PREF_USER_FITNESS_GOAL);
        addPreferenceHandlerFor(PREF_USER_FITNESS_GOAL_NOTIFICATION);

        addPreferenceHandlerFor(PREF_UM25_SHOW_THRESHOLD_NOTIFICATION);
        addPreferenceHandlerFor(PREF_UM25_SHOW_THRESHOLD);

        String sleepTimeState = prefs.getString(PREF_SLEEP_TIME, PREF_DO_NOT_DISTURB_OFF);
        boolean sleepTimeScheduled = sleepTimeState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference sleepTimeInfo = findPreference(PREF_SLEEP_TIME);
        if (sleepTimeInfo != null) {
            //sleepTimeInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(sleepTimeInfo));
            sleepTimeInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME);
                    return true;
                }
            });
        }

        final Preference sleepTimeStart = findPreference(PREF_SLEEP_TIME_START);
        if (sleepTimeStart != null) {
            sleepTimeStart.setEnabled(sleepTimeScheduled);
            sleepTimeStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME_START);
                    return true;
                }
            });
        }

        final Preference sleepTimeEnd = findPreference(PREF_SLEEP_TIME_END);
        if (sleepTimeEnd != null) {
            sleepTimeEnd.setEnabled(sleepTimeScheduled);
            sleepTimeEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME_END);
                    return true;
                }
            });
        }

        final Preference sleepTime = findPreference(PREF_SLEEP_TIME);
        if (sleepTime != null) {
            sleepTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
                    Objects.requireNonNull(sleepTimeStart).setEnabled(scheduled);
                    Objects.requireNonNull(sleepTimeEnd).setEnabled(scheduled);
                    if (sleepTimeInfo != null) {
                        //sleepTimeInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(newVal.toString()));
                    }
                    notifyPreferenceChanged(PREF_SLEEP_TIME);
                    return true;
                }
            });
        }
        String displayOnLiftState = prefs.getString(PREF_ACTIVATE_DISPLAY_ON_LIFT, PREF_DO_NOT_DISTURB_OFF);
        boolean displayOnLiftScheduled = displayOnLiftState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);
        boolean displayOnLiftOff = displayOnLiftState.equals(PREF_DO_NOT_DISTURB_OFF);

        final Preference rotateWristCycleInfo = findPreference(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
        if (rotateWristCycleInfo != null) {
            rotateWristCycleInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(displayOnLiftState));
            rotateWristCycleInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
                    return true;
                }
            });
        }

        final Preference displayOnLiftStart = findPreference(PREF_DISPLAY_ON_LIFT_START);
        if (displayOnLiftStart != null) {
            displayOnLiftStart.setEnabled(displayOnLiftScheduled);
            displayOnLiftStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_START);
                    return true;
                }
            });
        }

        final Preference displayOnLiftEnd = findPreference(PREF_DISPLAY_ON_LIFT_END);
        if (displayOnLiftEnd != null) {
            displayOnLiftEnd.setEnabled(displayOnLiftScheduled);
            displayOnLiftEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_END);
                    return true;
                }
            });
        }

        final Preference displayOnLiftSensitivity = findPreference(PREF_DISPLAY_ON_LIFT_SENSITIVITY);
        if (displayOnLiftSensitivity != null) {
            displayOnLiftSensitivity.setEnabled(!displayOnLiftOff);
            displayOnLiftSensitivity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_SENSITIVITY);
                    return true;
                }
            });
        }

        final Preference displayOnLift = findPreference(PREF_ACTIVATE_DISPLAY_ON_LIFT);
        if (displayOnLift != null) {
            displayOnLift.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
                    final boolean off = PREF_DO_NOT_DISTURB_OFF.equals(newVal.toString());
                    Objects.requireNonNull(displayOnLiftStart).setEnabled(scheduled);
                    Objects.requireNonNull(displayOnLiftEnd).setEnabled(scheduled);
                    if (rotateWristCycleInfo != null) {
                        rotateWristCycleInfo.setEnabled(!off);
                    }
                    if (displayOnLiftSensitivity != null) {
                        displayOnLiftSensitivity.setEnabled(!off);
                    }
                    notifyPreferenceChanged(PREF_ACTIVATE_DISPLAY_ON_LIFT);
                    return true;
                }
            });
        }

        final Preference worldClocks = findPreference(PREF_WORLD_CLOCKS);
        if (worldClocks != null) {
            worldClocks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getContext(), ConfigureWorldClocks.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference calendarBlacklist = findPreference("blacklist_calendars");
        if (calendarBlacklist != null) {
            calendarBlacklist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), CalBlacklistActivity.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference cannedMessagesDismissCall = findPreference("canned_messages_dismisscall_send");
        if (cannedMessagesDismissCall != null) {
            cannedMessagesDismissCall.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    ArrayList<String> messages = new ArrayList<>();
                    for (int i = 1; i <= 16; i++) {
                        String message = prefs.getString("canned_message_dismisscall_" + i, null);
                        if (message != null && !message.equals("")) {
                            messages.add(message);
                        }
                    }
                    CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                    cannedMessagesSpec.type = CannedMessagesSpec.TYPE_REJECTEDCALLS;
                    cannedMessagesSpec.cannedMessages = messages.toArray(new String[0]);
                    GBApplication.deviceService().onSetCannedMessages(cannedMessagesSpec);
                    return true;
                }
            });
        }

        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_BROADCAST_DELAY, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_PRESS_MAX_INTERVAL, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_PRESS_COUNT, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(MiBandConst.PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_RESERVER_REMINDERS_CALENDAR, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, InputType.TYPE_CLASS_NUMBER);

        new PasswordCapabilityImpl().registerPreferences(getContext(), coordinator.getPasswordCapability(), this);

        String deviceActionsFellSleepSelection = prefs.getString(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION, PREF_DEVICE_ACTION_SELECTION_OFF);
        final Preference deviceActionsFellSleep = findPreference(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION);
        final Preference deviceActionsFellSleepBroadcast = findPreference(PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST);
        boolean deviceActionsFellSleepSelectionBroadcast = deviceActionsFellSleepSelection.equals(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsFellSleep != null) {
            deviceActionsFellSleep.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean broadcast = PREF_DEVICE_ACTION_SELECTION_BROADCAST.equals(newVal.toString());
                    Objects.requireNonNull(deviceActionsFellSleepBroadcast).setEnabled(broadcast);
                    return true;
                }
            });
        }
        if (deviceActionsFellSleepBroadcast != null) {
            deviceActionsFellSleepBroadcast.setEnabled(deviceActionsFellSleepSelectionBroadcast);
        }

        String deviceActionsWokeUpSelection = prefs.getString(PREF_DEVICE_ACTION_WOKE_UP_SELECTION, PREF_DEVICE_ACTION_SELECTION_OFF);
        final Preference deviceActionsWokeUp = findPreference(PREF_DEVICE_ACTION_WOKE_UP_SELECTION);
        final Preference deviceActionsWokeUpBroadcast = findPreference(PREF_DEVICE_ACTION_WOKE_UP_BROADCAST);
        boolean deviceActionsWokeUpSelectionBroadcast = deviceActionsWokeUpSelection.equals(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsWokeUp != null) {
            deviceActionsWokeUp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean broadcast = PREF_DEVICE_ACTION_SELECTION_BROADCAST.equals(newVal.toString());
                    Objects.requireNonNull(deviceActionsWokeUpBroadcast).setEnabled(broadcast);
                    return true;
                }
            });
        }
        if (deviceActionsWokeUpBroadcast != null) {
            deviceActionsWokeUpBroadcast.setEnabled(deviceActionsWokeUpSelectionBroadcast);
        }

        String deviceActionsStartNonWearSelection = prefs.getString(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION, PREF_DEVICE_ACTION_SELECTION_OFF);
        final Preference deviceActionsStartNonWear = findPreference(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION);
        final Preference deviceActionsStartNonWearBroadcast = findPreference(PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST);
        boolean deviceActionsStartNonWearSelectionBroadcast = deviceActionsStartNonWearSelection.equals(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsStartNonWear != null) {
            deviceActionsStartNonWear.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean broadcast = PREF_DEVICE_ACTION_SELECTION_BROADCAST.equals(newVal.toString());
                    Objects.requireNonNull(deviceActionsStartNonWearBroadcast).setEnabled(broadcast);
                    return true;
                }
            });
        }
        if (deviceActionsStartNonWearBroadcast != null) {
            deviceActionsStartNonWearBroadcast.setEnabled(deviceActionsStartNonWearSelectionBroadcast);
        }

        // this is to ensure that Control Center device cards are refreshed on preference changes
        final Preference activityInDeviceCard = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD);
        final Preference activityInDeviceSteps = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS);
        final Preference activityInDeviceSleep = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP);
        final Preference activityInDeviceDistance = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE);
        final Preference chartsTabsOrderSelection = findPreference(PREFS_DEVICE_CHARTS_TABS);

        Preference.OnPreferenceClickListener sendIntentRefreshDeviceListListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(refreshIntent);
                return true;
            }
        };

        Preference[] preferencesInControlCenter = {
                activityInDeviceCard,
                activityInDeviceSteps,
                activityInDeviceSleep,
                activityInDeviceDistance,
                chartsTabsOrderSelection,
        };

        for (Preference preferenceInControlCenter : preferencesInControlCenter) {
            if (preferenceInControlCenter != null) {
                preferenceInControlCenter.setOnPreferenceClickListener(sendIntentRefreshDeviceListListener);
            }
        }

        if (deviceSpecificSettingsCustomizer != null) {
            deviceSpecificSettingsCustomizer.customizeSettings(this, prefs);
        }
    }

    static DeviceSpecificSettingsFragment newInstance(GBDevice device, DeviceSettingsActivity.MENU_ENTRY_POINTS applicationSpecificSettings) {
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        int[] supportedSettings = new int[0];
        String[] supportedLanguages = null;

        if (applicationSpecificSettings.equals(DeviceSettingsActivity.MENU_ENTRY_POINTS.APPLICATION_SETTINGS)) { //assemble device settings specific to the application
            supportedSettings = ArrayUtils.insert(0, supportedSettings, coordinator.getSupportedDeviceSpecificConnectionSettings());
            supportedSettings = ArrayUtils.addAll(supportedSettings, coordinator.getSupportedDeviceSpecificApplicationSettings());
            if (coordinator.supportsActivityTracking()) {
                supportedSettings = ArrayUtils.addAll(supportedSettings, R.xml.devicesettings_chartstabs);
                supportedSettings = ArrayUtils.addAll(supportedSettings, R.xml.devicesettings_device_card_activity_card_preferences);
            }
        } else if (applicationSpecificSettings.equals(DeviceSettingsActivity.MENU_ENTRY_POINTS.AUTH_SETTINGS)) { //auth settings screen
            supportedSettings = ArrayUtils.insert(0, supportedSettings, coordinator.getSupportedDeviceSpecificAuthenticationSettings());
            supportedSettings = ArrayUtils.addAll(supportedSettings, R.xml.devicesettings_pairingkey_explanation);
            if (coordinator.getDeviceType() == DeviceType.MIBAND6) { // miband6 might require new protocol and people do not know what to do, hint them:
                supportedSettings = ArrayUtils.addAll(supportedSettings, R.xml.devicesettings_miband6_new_protocol);
                supportedSettings = ArrayUtils.addAll(supportedSettings, R.xml.devicesettings_miband6_new_auth_protocol_explanation);
            }
        } else { //device settings
            supportedSettings = ArrayUtils.insert(0, supportedSettings, coordinator.getSupportedDeviceSpecificSettings(device));
            supportedLanguages = coordinator.getSupportedLanguageSettings(device);
            if (supportedLanguages != null) {
                supportedSettings = ArrayUtils.insert(0, supportedSettings, R.xml.devicesettings_language_generic);
            }
            supportedSettings = ArrayUtils.addAll(supportedSettings, coordinator.getSupportedDeviceSpecificAuthenticationSettings());
        }

        final DeviceSpecificSettingsCustomizer deviceSpecificSettingsCustomizer = coordinator.getDeviceSpecificSettingsCustomizer(device);
        final String settingsFileSuffix = device.getAddress();
        final DeviceSpecificSettingsFragment fragment = new DeviceSpecificSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix, supportedSettings, supportedLanguages);
        fragment.setDeviceSpecificSettingsCustomizer(deviceSpecificSettingsCustomizer);
        fragment.setDevice(device);

        return fragment;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment;
        if (preference instanceof XTimePreference) {
            dialogFragment = new XTimePreferenceFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        } else if (preference instanceof DragSortListPreference) {
            dialogFragment = new DragSortListPreferenceFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void addPreferenceHandlerFor(final String preferenceKey) {
        addPreferenceHandlerFor(preferenceKey, null);
    }

    @Override
    public void addPreferenceHandlerFor(final String preferenceKey, final Preference.OnPreferenceChangeListener extraListener) {
        Preference pref = findPreference(preferenceKey);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(preferenceKey);

                    if (extraListener != null) {
                        return extraListener.onPreferenceChange(preference, newVal);
                    }

                    return true;
                }
            });
        }
    }

    @Override
    public void setInputTypeFor(final String preferenceKey, final int editTypeFlags) {
        EditTextPreference textPreference = findPreference(preferenceKey);
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(editTypeFlags);
                }
            });
        }
    }

    /**
     * Keys of preferences which should print its values as a summary below the preference name.
     */
    protected Set<String> getPreferenceKeysWithSummary() {
        final Set<String> keysWithSummary = new HashSet<>();

        keysWithSummary.add(PREF_INACTIVITY_THRESHOLD);

        if (deviceSpecificSettingsCustomizer != null) {
            keysWithSummary.addAll(deviceSpecificSettingsCustomizer.getPreferenceKeysWithSummary());
        }

        return keysWithSummary;
    }

    /**
     * Reload the preferences in the current screen. This is needed when the user enters or exists a PreferenceScreen,
     * otherwise the settings won't be reloaded by the {@link SharedPreferencesChangeHandler}, as the preferences return
     * null, since they're not visible.
     *
     * @param sharedPreferences the {@link SharedPreferences} instance
     * @param preferenceGroup the {@link PreferenceGroup} for which preferences will be reloaded
     */
    private void reloadPreferences(final SharedPreferences sharedPreferences, final PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            if (preferenceGroup == null) {
                return;
            }

            final Preference preference = preferenceGroup.getPreference(i);

            LOG.debug("Reloading {}", preference.getKey());

            if (preference instanceof PreferenceCategory) {
                reloadPreferences(sharedPreferences, (PreferenceCategory) preference);
                continue;
            }

            sharedPreferencesChangeHandler.onSharedPreferenceChanged(sharedPreferences, preference.getKey());
        }
    }

    /**
     * Handler for preference changes, update UI accordingly (if device updates the preferences).
     */
    private class SharedPreferencesChangeHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            LOG.debug("Preference changed: {}", key);

            if (key == null){
                LOG.warn("Preference null, ignoring");
                return;
            }

            final Preference preference = findPreference(key);
            if (preference == null) {
                LOG.warn("Preference {} not found", key);

                return;
            }

            if (preference instanceof SeekBarPreference) {
                final SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                seekBarPreference.setValue(prefs.getInt(key, seekBarPreference.getValue()));
            } else if (preference instanceof SwitchPreference) {
                final SwitchPreference switchPreference = (SwitchPreference) preference;
                switchPreference.setChecked(prefs.getBoolean(key, switchPreference.isChecked()));
            } else if (preference instanceof ListPreference) {
                final ListPreference listPreference = (ListPreference) preference;
                listPreference.setValue(prefs.getString(key, listPreference.getValue()));
            } else if (preference instanceof EditTextPreference) {
                final EditTextPreference editTextPreference = (EditTextPreference) preference;
                editTextPreference.setText(prefs.getString(key, editTextPreference.getText()));
            } else if (preference instanceof PreferenceScreen) {
                // Ignoring
            } else {
                LOG.warn("Unknown preference class {}, ignoring", preference.getClass());
            }

            if (getPreferenceKeysWithSummary().contains(key)) {
                final String summary = prefs.getString(key, preference.getSummary() != null ? preference.getSummary().toString() : "");
                preference.setSummary(summary);
            }

            if (deviceSpecificSettingsCustomizer != null) {
                deviceSpecificSettingsCustomizer.onPreferenceChange(preference, DeviceSpecificSettingsFragment.this);
            }
        }
    }
}
