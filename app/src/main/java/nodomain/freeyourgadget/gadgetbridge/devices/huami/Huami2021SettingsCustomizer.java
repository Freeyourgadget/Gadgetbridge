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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.os.Parcel;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.capabilities.GpsCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class Huami2021SettingsCustomizer extends HuamiSettingsCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021SettingsCustomizer.class);

    public Huami2021SettingsCustomizer(final GBDevice device, final List<HuamiVibrationPatternNotificationType> vibrationPatternNotificationTypes) {
        super(device, vibrationPatternNotificationTypes);
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs) {
        super.customizeSettings(handler, prefs);

        // These are not reported by the normal configs
        removeUnsupportedElementsFromListPreference(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, handler, prefs);
        removeUnsupportedElementsFromListPreference(HuamiConst.PREF_SHORTCUTS_SORTABLE, handler, prefs);
        removeUnsupportedElementsFromListPreference(HuamiConst.PREF_CONTROL_CENTER_SORTABLE, handler, prefs);

        for (final ZeppOsConfigService.ConfigArg config : ZeppOsConfigService.ConfigArg.values()) {
            if (config.getPrefKey() == null) {
                continue;
            }
            switch (config.getConfigType(null)) {
                case BYTE:
                case BYTE_LIST:
                case STRING_LIST:
                    // For list preferences, remove the unsupported items
                    removeUnsupportedElementsFromListPreference(config.getPrefKey(), handler, prefs);
                    break;
                case BOOL:
                case SHORT:
                case INT:
                case DATETIME_HH_MM:
                    // For other preferences, just hide them if they were not reported as supported by the device
                    hidePrefIfNoConfigSupported(handler, prefs, config.getPrefKey(), config);
                    break;
            }
        }

        // Hide all config groups that may not be mapped directly to a preference
        final Map<String, List<ZeppOsConfigService.ConfigArg>> configScreens = new HashMap<String, List<ZeppOsConfigService.ConfigArg>>() {{
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_NIGHT_MODE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_MODE,
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_SCHEDULED_END
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_SLEEP_MODE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.SLEEP_MODE_SLEEP_SCREEN,
                    ZeppOsConfigService.ConfigArg.SLEEP_MODE_SMART_ENABLE
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_LIFT_WRIST, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_MODE,
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_SCHEDULED_END,
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_RESPONSE_SENSITIVITY
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.PASSWORD_ENABLED,
                    ZeppOsConfigService.ConfigArg.PASSWORD_TEXT
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_ALWAYS_ON_DISPLAY, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_MODE,
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_END,
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE,
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_STYLE
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.SCREEN_AUTO_BRIGHTNESS
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_HEARTRATE_MONITORING, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.HEART_RATE_ALL_DAY_MONITORING,
                    ZeppOsConfigService.ConfigArg.HEART_RATE_HIGH_ALERTS,
                    ZeppOsConfigService.ConfigArg.HEART_RATE_LOW_ALERTS,
                    ZeppOsConfigService.ConfigArg.HEART_RATE_ACTIVITY_MONITORING,
                    ZeppOsConfigService.ConfigArg.SLEEP_HIGH_ACCURACY_MONITORING,
                    ZeppOsConfigService.ConfigArg.SLEEP_BREATHING_QUALITY_MONITORING,
                    ZeppOsConfigService.ConfigArg.STRESS_MONITORING,
                    ZeppOsConfigService.ConfigArg.STRESS_RELAXATION_REMINDER,
                    ZeppOsConfigService.ConfigArg.SPO2_ALL_DAY_MONITORING,
                    ZeppOsConfigService.ConfigArg.SPO2_LOW_ALERT
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_INACTIVITY_EXTENDED, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_ENABLED,
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_END,
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_ENABLED,
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_END
            ));
            put(DeviceSettingsPreferenceConst.PREF_HEADER_GPS, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_PRESET,
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_BAND,
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_COMBINATION,
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_SATELLITE_SEARCH,
                    ZeppOsConfigService.ConfigArg.WORKOUT_AGPS_EXPIRY_REMINDER_ENABLED,
                    ZeppOsConfigService.ConfigArg.WORKOUT_AGPS_EXPIRY_REMINDER_TIME
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_SOUND_AND_VIBRATION, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.VOLUME,
                    ZeppOsConfigService.ConfigArg.CROWN_VIBRATION,
                    ZeppOsConfigService.ConfigArg.ALERT_TONE,
                    ZeppOsConfigService.ConfigArg.COVER_TO_MUTE,
                    ZeppOsConfigService.ConfigArg.VIBRATE_FOR_ALERT,
                    ZeppOsConfigService.ConfigArg.TEXT_TO_SPEECH
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_DO_NOT_DISTURB, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.DND_MODE,
                    ZeppOsConfigService.ConfigArg.DND_SCHEDULED_START,
                    ZeppOsConfigService.ConfigArg.DND_SCHEDULED_END
            ));
            put(DeviceSettingsPreferenceConst.PREF_HEADER_WORKOUT_DETECTION, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_CATEGORY,
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_ALERT,
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_SENSITIVITY
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_OFFLINE_VOICE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPOND_TURN_WRIST,
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPOND_SCREEN_ON,
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING,
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_LANGUAGE
            ));
        }};

        for (final Map.Entry<String, List<ZeppOsConfigService.ConfigArg>> configScreen : configScreens.entrySet()) {
            hidePrefIfNoConfigSupported(
                    handler,
                    prefs,
                    configScreen.getKey(),
                    configScreen.getValue().toArray(new ZeppOsConfigService.ConfigArg[0])
            );
        }

        // Hides the headers if none of the preferences under them are available
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_TIME, Arrays.asList(
                DeviceSettingsPreferenceConst.PREF_TIMEFORMAT,
                DeviceSettingsPreferenceConst.PREF_DATEFORMAT,
                DeviceSettingsPreferenceConst.PREF_WORLD_CLOCKS
        ));
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_DISPLAY, Arrays.asList(
                HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE,
                HuamiConst.PREF_SHORTCUTS_SORTABLE,
                HuamiConst.PREF_CONTROL_CENTER_SORTABLE,
                DeviceSettingsPreferenceConst.PREF_SCREEN_NIGHT_MODE,
                DeviceSettingsPreferenceConst.PREF_SCREEN_SLEEP_MODE,
                DeviceSettingsPreferenceConst.PREF_SCREEN_LIFT_WRIST,
                DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD,
                DeviceSettingsPreferenceConst.PREF_SCREEN_ALWAYS_ON_DISPLAY,
                DeviceSettingsPreferenceConst.PREF_SCREEN_TIMEOUT,
                DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS,
                DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS
        ));
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_HEALTH, Arrays.asList(
                DeviceSettingsPreferenceConst.PREF_SCREEN_HEARTRATE_MONITORING,
                DeviceSettingsPreferenceConst.PREF_SCREEN_INACTIVITY_EXTENDED,
                DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION
        ));
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_WORKOUT, Arrays.asList(
                DeviceSettingsPreferenceConst.PREF_HEADER_GPS,
                DeviceSettingsPreferenceConst.PREF_WORKOUT_START_ON_PHONE,
                DeviceSettingsPreferenceConst.PREF_WORKOUT_SEND_GPS_TO_BAND,
                DeviceSettingsPreferenceConst.PREF_HEADER_WORKOUT_DETECTION
        ));
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_AGPS, Arrays.asList(
                DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRY_REMINDER_ENABLED,
                DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRY_REMINDER_TIME
        ));

        setupGpsPreference(handler);
    }

    private void setupGpsPreference(final DeviceSpecificSettingsHandler handler) {
        final ListPreference prefGpsPreset = handler.findPreference(DeviceSettingsPreferenceConst.PREF_GPS_MODE_PRESET);
        final ListPreference prefGpsBand = handler.findPreference(DeviceSettingsPreferenceConst.PREF_GPS_BAND);
        final ListPreference prefGpsCombination = handler.findPreference(DeviceSettingsPreferenceConst.PREF_GPS_COMBINATION);
        final ListPreference prefGpsSatelliteSearch = handler.findPreference(DeviceSettingsPreferenceConst.PREF_GPS_SATELLITE_SEARCH);

        if (prefGpsPreset != null) {
            // When the preset preference is changed, update the band, combination and satellite search to the corresponding values
            final Preference.OnPreferenceChangeListener onGpsPresetUpdated = (preference, newVal) -> {
                final boolean isCustomPreset = GpsCapability.Preset.CUSTOM.name().toLowerCase(Locale.ROOT).equals(newVal);
                final GpsCapability.Preset preset = GpsCapability.Preset.valueOf(newVal.toString().toUpperCase(Locale.ROOT));
                final GpsCapability.Band presetBand;
                final GpsCapability.Combination presetCombination;
                final GpsCapability.SatelliteSearch presetSatelliteSearch;
                switch (preset) {
                    case ACCURACY:
                        presetBand = GpsCapability.Band.DUAL_BAND;
                        presetCombination = GpsCapability.Combination.ALL_SATELLITES;
                        presetSatelliteSearch = GpsCapability.SatelliteSearch.ACCURACY_FIRST;
                        break;
                    case BALANCED:
                        presetBand = GpsCapability.Band.SINGLE_BAND;
                        presetCombination = GpsCapability.Combination.GPS_BDS;
                        presetSatelliteSearch = GpsCapability.SatelliteSearch.ACCURACY_FIRST;
                        break;
                    case POWER_SAVING:
                        presetBand = GpsCapability.Band.SINGLE_BAND;
                        presetCombination = GpsCapability.Combination.LOW_POWER_GPS;
                        presetSatelliteSearch = GpsCapability.SatelliteSearch.SPEED_FIRST;
                        break;
                    default:
                        presetBand = null;
                        presetCombination = null;
                        presetSatelliteSearch = null;
                        break;
                }

                if (prefGpsBand != null) {
                    prefGpsBand.setEnabled(isCustomPreset);
                    if (!isCustomPreset && presetBand != null) {
                        prefGpsBand.setValue(presetBand.name().toLowerCase(Locale.ROOT));
                    }
                }
                if (prefGpsCombination != null) {
                    prefGpsCombination.setEnabled(isCustomPreset);
                    if (!isCustomPreset && presetBand != null) {
                        prefGpsCombination.setValue(presetCombination.name().toLowerCase(Locale.ROOT));
                    }
                }
                if (prefGpsSatelliteSearch != null) {
                    prefGpsSatelliteSearch.setEnabled(isCustomPreset);
                    if (!isCustomPreset && presetBand != null) {
                        prefGpsSatelliteSearch.setValue(presetSatelliteSearch.name().toLowerCase(Locale.ROOT));
                    }
                }

                return true;
            };

            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_GPS_MODE_PRESET, onGpsPresetUpdated);
            onGpsPresetUpdated.onPreferenceChange(prefGpsPreset, prefGpsPreset.getValue());
        }

        // The gps combination can only be chosen if the gps band is single band
        if (prefGpsBand != null && prefGpsCombination != null) {
            final Preference.OnPreferenceChangeListener onGpsBandUpdate = (preference, newVal) -> {
                final boolean isSingleBand = GpsCapability.Band.SINGLE_BAND.name().toLowerCase(Locale.ROOT).equals(newVal);
                prefGpsCombination.setEnabled(isSingleBand);
                return true;
            };

            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_GPS_BAND, onGpsBandUpdate);
            final boolean isCustomPreset = prefGpsPreset != null &&
                    GpsCapability.Preset.CUSTOM.name().toLowerCase(Locale.ROOT).equals(prefGpsPreset.getValue());
            if (isCustomPreset) {
                onGpsBandUpdate.onPreferenceChange(prefGpsPreset, prefGpsBand.getValue());
            }
        }
    }

    /**
     * Removes all unsupported elements from a list preference. If they are not known, the preference
     * is hidden.
     */
    private void removeUnsupportedElementsFromListPreference(final String prefKey,
                                                             final DeviceSpecificSettingsHandler handler,
                                                             final Prefs prefs) {
        final Preference pref = handler.findPreference(prefKey);
        if (pref == null) {
            return;
        }

        // Get the list of possible values for this preference, as reported by the band
        final List<String> possibleValues = prefs.getList(ZeppOsConfigService.getPrefPossibleValuesKey(prefKey), null);
        if (possibleValues == null || possibleValues.isEmpty()) {
            // The band hasn't reported this setting, so we don't know the possible values.
            // Hide it
            pref.setVisible(false);

            return;
        }

        final CharSequence[] originalEntries;
        final CharSequence[] originalValues;

        if (pref instanceof ListPreference) {
            originalEntries = ((ListPreference) pref).getEntries();
            originalValues = ((ListPreference) pref).getEntryValues();
        } else if (pref instanceof MultiSelectListPreference) {
            originalEntries = ((MultiSelectListPreference) pref).getEntries();
            originalValues = ((MultiSelectListPreference) pref).getEntryValues();
        } else {
            LOG.error("Unknown list pref class {}", pref.getClass().getName());
            return;
        }

        final List<String> prefValues = new ArrayList<>(originalValues.length);
        for (final CharSequence entryValue : originalValues) {
            prefValues.add(entryValue.toString());
        }

        final CharSequence[] entries = new CharSequence[possibleValues.size()];
        final CharSequence[] values = new CharSequence[possibleValues.size()];
        for (int i = 0; i < possibleValues.size(); i++) {
            final String possibleValue = possibleValues.get(i);
            final int idxPrefValue = prefValues.indexOf(possibleValue);

            if (idxPrefValue >= 0) {
                entries[i] = originalEntries[idxPrefValue];
            } else {
                entries[i] = handler.getContext().getString(R.string.menuitem_unknown_app, possibleValue);
            }
            values[i] = possibleValue;
        }

        if (pref instanceof ListPreference) {
            ((ListPreference) pref).setEntries(entries);
            ((ListPreference) pref).setEntryValues(values);
        } else if (pref instanceof MultiSelectListPreference) {
            ((MultiSelectListPreference) pref).setEntries(entries);
            ((MultiSelectListPreference) pref).setEntryValues(values);
        }
    }

    /**
     * Hides prefToHide if no configuration from the list has been reported by the band.
     */
    private void hidePrefIfNoConfigSupported(final DeviceSpecificSettingsHandler handler,
                                             final Prefs prefs,
                                             final String prefToHide,
                                             final ZeppOsConfigService.ConfigArg... configs) {
        final Preference pref = handler.findPreference(prefToHide);
        if (pref == null) {
            return;
        }

        for (final ZeppOsConfigService.ConfigArg config : configs) {
            if (ZeppOsConfigService.deviceHasConfig(prefs, config)) {
                // This preference is supported, don't hide
                return;
            }
        }

        // None of the configs were supported by the device, hide this preference
        pref.setVisible(false);
    }

    /**
     * Hides the the prefToHide preference if none of the preferences in the preferences list are
     * visible.
     */
    private void hidePrefIfNoneVisible(final DeviceSpecificSettingsHandler handler,
                                       final String prefToHide,
                                       final List<String> subPrefs) {
        final Preference pref = handler.findPreference(prefToHide);
        if (pref == null) {
            return;
        }

        for (final String subPrefKey : subPrefs) {
            final Preference subPref = handler.findPreference(subPrefKey);
            if (subPref == null) {
                continue;
            }
            if (subPref.isVisible()) {
                // At least one preference is visible
                return;
            }
        }

        // No preference was visible, hide
        pref.setVisible(false);
    }

    public static final Creator<Huami2021SettingsCustomizer> CREATOR = new Creator<Huami2021SettingsCustomizer>() {
        @Override
        public Huami2021SettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(Huami2021SettingsCustomizer.class.getClassLoader());
            final List<HuamiVibrationPatternNotificationType> vibrationPatternNotificationTypes = new ArrayList<>();
            in.readList(vibrationPatternNotificationTypes, HuamiVibrationPatternNotificationType.class.getClassLoader());
            return new Huami2021SettingsCustomizer(device, vibrationPatternNotificationTypes);
        }

        @Override
        public Huami2021SettingsCustomizer[] newArray(final int size) {
            return new Huami2021SettingsCustomizer[size];
        }
    };
}
