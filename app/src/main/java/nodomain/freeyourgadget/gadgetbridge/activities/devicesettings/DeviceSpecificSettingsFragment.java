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

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mobeta.android.dslv.DragSortListPreference;
import com.mobeta.android.dslv.DragSortListPreferenceFragment;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3.MakibesHR3Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreferenceFragment;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALTITUDE_CALIBRATE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AMPM_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ANTILOST_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOREMOVE_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BT_CONNECTED_ADVERTISEMENT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOREMOVE_MESSAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_DOUBLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_LONG;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_DOUBLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_LONG;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_DOUBLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_LONG;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUTTON_BP_CALIBRATE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FAKE_RING_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYBRID_HR_FORCE_WHITE_COLOR;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_KEY_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LEFUN_INTERFACE_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LONGSIT_PERIOD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LONGSIT_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LONGSIT_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LONGSIT_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SLEEP;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_POWER_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SCREEN_ORIENTATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SLEEP_TIME;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONYSWR12_LOW_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONYSWR12_SMART_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONYSWR12_STAMINA;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WEARLOCATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_VIBRATION_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_AUDIOMODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_INEAR;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISCONNECT_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISCONNECT_NOTIFICATION_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISCONNECT_NOTIFICATION_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ON_LIFT_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ON_LIFT_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_EXPOSE_HR_THIRDPARTY;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DO_NOT_DISTURB;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DO_NOT_DISTURB_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DO_NOT_DISTURB_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DO_NOT_DISTURB_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DO_NOT_DISTURB_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_SWIPE_UNLOCK;

public class DeviceSpecificSettingsFragment extends PreferenceFragmentCompat {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceSpecificSettingsFragment.class);

    static final String FRAGMENT_TAG = "DEVICE_SPECIFIC_SETTINGS_FRAGMENT";

    private void setSettingsFileSuffix(String settingsFileSuffix, @NonNull int[] supportedSettings, String[] supportedLanguages) {
        Bundle args = new Bundle();
        args.putString("settingsFileSuffix", settingsFileSuffix);
        args.putIntArray("supportedSettings", supportedSettings);
        args.putStringArray("supportedLanguages", supportedLanguages);
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

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    private void setChangeListener() {
        final Prefs prefs = new Prefs(getPreferenceManager().getSharedPreferences());
        String disconnectNotificationState = prefs.getString(PREF_DISCONNECT_NOTIFICATION, PREF_DO_NOT_DISTURB_OFF);
        boolean disconnectNotificationScheduled = disconnectNotificationState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference disconnectNotificationStart = findPreference(PREF_DISCONNECT_NOTIFICATION_START);
        if (disconnectNotificationStart != null) {
            disconnectNotificationStart.setEnabled(disconnectNotificationScheduled);
            disconnectNotificationStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DISCONNECT_NOTIFICATION_START);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DISCONNECT_NOTIFICATION_END);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DISCONNECT_NOTIFICATION);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_NIGHT_MODE_START);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_NIGHT_MODE_END);
                        }
                    });
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

                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_NIGHT_MODE);
                        }
                    });
                    return true;
                }
            });
        }


        String doNotDisturbState = prefs.getString(MiBandConst.PREF_DO_NOT_DISTURB, PREF_DO_NOT_DISTURB_OFF);
        boolean doNotDisturbScheduled = doNotDisturbState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference doNotDisturbStart = findPreference(PREF_DO_NOT_DISTURB_START);
        if (doNotDisturbStart != null) {
            doNotDisturbStart.setEnabled(doNotDisturbScheduled);
            doNotDisturbStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DO_NOT_DISTURB_START);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DO_NOT_DISTURB_END);
                        }
                    });
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

                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DO_NOT_DISTURB);
                        }
                    });
                    return true;
                }
            });
        }

        addPreferenceHandlerFor(PREF_SWIPE_UNLOCK);
        addPreferenceHandlerFor(PREF_MI2_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS_SORTABLE);
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
        addPreferenceHandlerFor(PREF_LONGSIT_PERIOD);
        addPreferenceHandlerFor(PREF_LONGSIT_SWITCH);
        addPreferenceHandlerFor(PREF_LONGSIT_START);
        addPreferenceHandlerFor(PREF_LONGSIT_END);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SWITCH);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SLEEP);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_INTERVAL);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_START);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_END);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_START);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_END);
        addPreferenceHandlerFor(PREF_FIND_PHONE_ENABLED);
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
        addPreferenceHandlerFor(PREF_LEFUN_INTERFACE_LANGUAGE);
        addPreferenceHandlerFor(PREF_SOUNDS);

        addPreferenceHandlerFor(PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_FORCE_WHITE_COLOR);
        addPreferenceHandlerFor(PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS);

        addPreferenceHandlerFor(PREF_SONYSWR12_STAMINA);
        addPreferenceHandlerFor(PREF_SONYSWR12_LOW_VIBRATION);
        addPreferenceHandlerFor(PREF_SONYSWR12_SMART_INTERVAL);

        addPreferenceHandlerFor(PREF_NOTHING_EAR1_INEAR);
        addPreferenceHandlerFor(PREF_NOTHING_EAR1_AUDIOMODE);

        String sleepTimeState = prefs.getString(PREF_SLEEP_TIME, PREF_DO_NOT_DISTURB_OFF);
        boolean sleepTimeScheduled = sleepTimeState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference sleepTimeInfo = findPreference(PREF_SLEEP_TIME);
        if (sleepTimeInfo != null) {
            //sleepTimeInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(sleepTimeInfo));
            sleepTimeInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_SLEEP_TIME);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_SLEEP_TIME_START);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_SLEEP_TIME_END);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_SLEEP_TIME);
                        }
                    });
                    return true;
                }
            });
        }
        String displayOnLiftState = prefs.getString(PREF_ACTIVATE_DISPLAY_ON_LIFT, PREF_DO_NOT_DISTURB_OFF);
        boolean displayOnLiftScheduled = displayOnLiftState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference rotateWristCycleInfo = findPreference(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
        if (rotateWristCycleInfo != null) {
            rotateWristCycleInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(displayOnLiftState));
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
        }

        final Preference displayOnLiftStart = findPreference(PREF_DISPLAY_ON_LIFT_START);
        if (displayOnLiftStart != null) {
            displayOnLiftStart.setEnabled(displayOnLiftScheduled);
            displayOnLiftStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DISPLAY_ON_LIFT_START);
                        }
                    });
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
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_DISPLAY_ON_LIFT_END);
                        }
                    });
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
                    Objects.requireNonNull(displayOnLiftStart).setEnabled(scheduled);
                    Objects.requireNonNull(displayOnLiftEnd).setEnabled(scheduled);
                    if (rotateWristCycleInfo != null) {
                        rotateWristCycleInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(newVal.toString()));
                    }
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(PREF_ACTIVATE_DISPLAY_ON_LIFT);
                        }
                    });
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
        setInputTypeFor(MakibesHR3Constants.PREF_FIND_PHONE_DURATION, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR, InputType.TYPE_CLASS_NUMBER);

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
    }

    static DeviceSpecificSettingsFragment newInstance(String settingsFileSuffix, @NonNull int[] supportedSettings, String[] supportedLanguages) {
        DeviceSpecificSettingsFragment fragment = new DeviceSpecificSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix, supportedSettings, supportedLanguages);

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

    private void addPreferenceHandlerFor(final String preferenceKey) {
        Preference pref = findPreference(preferenceKey);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            GBApplication.deviceService().onSendConfiguration(preferenceKey);
                        }
                    });
                    return true;
                }
            });
        }
    }

    private void setInputTypeFor(final String preferenceKey, final int editTypeFlags) {
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
}
