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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.hidePrefIfNoneVisible;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.populateOrHideListPreference;

import android.os.Parcel;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.GpsCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
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
        populateOrHideListPreference(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, handler, prefs);
        populateOrHideListPreference(HuamiConst.PREF_SHORTCUTS_SORTABLE, handler, prefs);
        populateOrHideListPreference(HuamiConst.PREF_CONTROL_CENTER_SORTABLE, handler, prefs);
        populateOrHideListPreference(DeviceSettingsPreferenceConst.SHORTCUT_CARDS_SORTABLE, handler, prefs);
        populateOrHideListPreference(DeviceSettingsPreferenceConst.PREF_WATCHFACE, handler, prefs);
        populateOrHideListPreference(DeviceSettingsPreferenceConst.MORNING_UPDATES_CATEGORIES_SORTABLE, handler, prefs);
        populateOrHideListPreference(DeviceSettingsPreferenceConst.PREF_VOICE_SERVICE_LANGUAGE, handler, prefs);

        for (final ZeppOsConfigService.ConfigArg config : ZeppOsConfigService.ConfigArg.values()) {
            if (config.getPrefKey() == null) {
                continue;
            }
            final ZeppOsConfigService.ConfigType configType = config.getConfigType(null);
            if (configType == null) {
                // Should never happen
                LOG.error("configType is null - this should never happen");
                return;
            }
            switch (configType) {
                case BYTE:
                case BYTE_LIST:
                case STRING_LIST:
                    // For list preferences, remove the unsupported items
                    populateOrHideListPreference(config.getPrefKey(), handler, prefs);
                    break;
                case SHORT:
                case INT:
                    hidePrefIfNoConfigSupported(handler, prefs, config.getPrefKey(), config.name());
                    enforceMinMax(handler, prefs, config);
                    break;
                case BOOL:
                case DATETIME_HH_MM:
                case TIMESTAMP_MILLIS:
                default:
                    // For other preferences, just hide them if they were not reported as supported by the device
                    hidePrefIfNoConfigSupported(handler, prefs, config.getPrefKey(), config.name());
                    break;
            }
        }

        // Hide all config groups that may not be mapped directly to a preference
        final Map<String, List<String>> configScreens = new HashMap<String, List<String>>() {{
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_NIGHT_MODE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_MODE.name(),
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.NIGHT_MODE_SCHEDULED_END.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_SLEEP_MODE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.SLEEP_MODE_SLEEP_SCREEN.name(),
                    ZeppOsConfigService.ConfigArg.SLEEP_MODE_SMART_ENABLE.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_LIFT_WRIST, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_MODE.name(),
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_SCHEDULED_END.name(),
                    ZeppOsConfigService.ConfigArg.LIFT_WRIST_RESPONSE_SENSITIVITY.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.PASSWORD_ENABLED.name(),
                    ZeppOsConfigService.ConfigArg.PASSWORD_TEXT.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_ALWAYS_ON_DISPLAY, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_MODE.name(),
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_END.name(),
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE.name(),
                    ZeppOsConfigService.ConfigArg.ALWAYS_ON_DISPLAY_STYLE.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.SCREEN_AUTO_BRIGHTNESS.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_HEARTRATE_MONITORING, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.HEART_RATE_ALL_DAY_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.HEART_RATE_HIGH_ALERTS.name(),
                    ZeppOsConfigService.ConfigArg.HEART_RATE_LOW_ALERTS.name(),
                    ZeppOsConfigService.ConfigArg.HEART_RATE_ACTIVITY_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.SLEEP_HIGH_ACCURACY_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.SLEEP_BREATHING_QUALITY_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.STRESS_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.STRESS_RELAXATION_REMINDER.name(),
                    ZeppOsConfigService.ConfigArg.SPO2_ALL_DAY_MONITORING.name(),
                    ZeppOsConfigService.ConfigArg.SPO2_LOW_ALERT.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_INACTIVITY_EXTENDED, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_ENABLED.name(),
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_END.name(),
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_ENABLED.name(),
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_END.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_HEADER_GPS, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_PRESET.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_BAND.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_COMBINATION.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_GPS_SATELLITE_SEARCH.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_AGPS_EXPIRY_REMINDER_ENABLED.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_AGPS_EXPIRY_REMINDER_TIME.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_SOUND_AND_VIBRATION, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.VOLUME.name(),
                    ZeppOsConfigService.ConfigArg.CROWN_VIBRATION.name(),
                    ZeppOsConfigService.ConfigArg.ALERT_TONE.name(),
                    ZeppOsConfigService.ConfigArg.COVER_TO_MUTE.name(),
                    ZeppOsConfigService.ConfigArg.VIBRATE_FOR_ALERT.name(),
                    ZeppOsConfigService.ConfigArg.TEXT_TO_SPEECH.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_DO_NOT_DISTURB, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.DND_MODE.name(),
                    ZeppOsConfigService.ConfigArg.DND_SCHEDULED_START.name(),
                    ZeppOsConfigService.ConfigArg.DND_SCHEDULED_END.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_HEADER_WORKOUT_DETECTION, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_CATEGORY.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_ALERT.name(),
                    ZeppOsConfigService.ConfigArg.WORKOUT_DETECTION_SENSITIVITY.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_OFFLINE_VOICE, Arrays.asList(
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPOND_TURN_WRIST.name(),
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPOND_SCREEN_ON.name(),
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING.name(),
                    ZeppOsConfigService.ConfigArg.OFFLINE_VOICE_LANGUAGE.name()
            ));
            put(DeviceSettingsPreferenceConst.PREF_SCREEN_MORNING_UPDATES, Arrays.asList(
                    DeviceSettingsPreferenceConst.MORNING_UPDATES_ENABLED,
                    DeviceSettingsPreferenceConst.MORNING_UPDATES_CATEGORIES_SORTABLE
            ));
        }};

        for (final Map.Entry<String, List<String>> configScreen : configScreens.entrySet()) {
            hidePrefIfNoConfigSupported(
                    handler,
                    prefs,
                    configScreen.getKey(),
                    configScreen.getValue().toArray(new String[0])
            );
        }

        // Hides the headers if none of the preferences under them are available
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_APPS, Arrays.asList(
                LoyaltyCardsSettingsConst.PREF_KEY_LOYALTY_CARDS
        ));
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
                DeviceSettingsPreferenceConst.PREF_HEADER_WORKOUT_DETECTION,
                DeviceSettingsPreferenceConst.PREF_WORKOUT_KEEP_SCREEN_ON
        ));
        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_AGPS, Arrays.asList(
                DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRY_REMINDER_ENABLED,
                DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRY_REMINDER_TIME,
                DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME,
                DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRE_TIME
        ));

        setupGpsPreference(handler, prefs);
        setupButtonClickPreferences(handler);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        final Set<String> preferenceKeysWithSummary = super.getPreferenceKeysWithSummary();

        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_SSID);
        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_PASSWORD);
        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_STATUS);

        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.FTP_SERVER_ROOT_DIR);
        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.FTP_SERVER_ADDRESS);
        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.FTP_SERVER_USERNAME);
        preferenceKeysWithSummary.add(DeviceSettingsPreferenceConst.FTP_SERVER_STATUS);

        return preferenceKeysWithSummary;
    }

    private void setupGpsPreference(final DeviceSpecificSettingsHandler handler, final Prefs prefs) {
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

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        final Preference prefAgpsUpdateTime = handler.findPreference(DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME);
        if (prefAgpsUpdateTime != null) {
            final long ts = prefs.getLong(DeviceSettingsPreferenceConst.PREF_AGPS_UPDATE_TIME, 0L);
            if (ts > 0) {
                prefAgpsUpdateTime.setSummary(sdf.format(new Date(ts)));
            } else {
                prefAgpsUpdateTime.setSummary(handler.getContext().getString(R.string.unknown));
            }
        }

        final Preference prefAgpsExpireTime = handler.findPreference(DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRE_TIME);
        if (prefAgpsExpireTime != null) {
            final long ts = prefs.getLong(DeviceSettingsPreferenceConst.PREF_AGPS_EXPIRE_TIME, 0L);
            if (ts > 0) {
                prefAgpsExpireTime.setSummary(sdf.format(new Date(ts)));
            } else {
                prefAgpsExpireTime.setSummary(handler.getContext().getString(R.string.unknown));
            }
        }
    }

    private void setupButtonClickPreferences(final DeviceSpecificSettingsHandler handler) {
        // Notify preference changed on button click, so we can react to them
        final List<Preference> wifiFtpButtons = Arrays.asList(
                handler.findPreference(DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_PAIR),
                handler.findPreference(DeviceSettingsPreferenceConst.PREF_APP_LOGS_START),
                handler.findPreference(DeviceSettingsPreferenceConst.PREF_APP_LOGS_STOP),
                handler.findPreference(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_START),
                handler.findPreference(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_STOP),
                handler.findPreference(DeviceSettingsPreferenceConst.FTP_SERVER_START),
                handler.findPreference(DeviceSettingsPreferenceConst.FTP_SERVER_STOP),
                // TODO: These are temporary for debugging and will be removed
                handler.findPreference("zepp_os_alexa_btn_trigger"),
                handler.findPreference("zepp_os_alexa_btn_send_simple"),
                handler.findPreference("zepp_os_alexa_btn_send_complex")
        );

        for (final Preference btn : wifiFtpButtons) {
            if (btn != null) {
                btn.setOnPreferenceClickListener(preference -> {
                    handler.notifyPreferenceChanged(btn.getKey());
                    return true;
                });
            }
        }
    }

    /**
     * Hides prefToHide if no configuration from the list has been reported by the band.
     */
    private void hidePrefIfNoConfigSupported(final DeviceSpecificSettingsHandler handler,
                                             final Prefs prefs,
                                             final String prefToHide,
                                             final String... supportedPref) {
        final Preference pref = handler.findPreference(prefToHide);
        if (pref == null) {
            return;
        }

        for (final String prefKey : supportedPref) {
            final boolean deviceHasConfig = prefs.getBoolean(DeviceSettingsUtils.getPrefKnownConfig(prefKey), false);
            if (deviceHasConfig) {
                // This preference is supported, don't hide
                return;
            }
        }

        // None of the configs were supported by the device, hide this preference
        pref.setVisible(false);
    }

    private void enforceMinMax(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final ZeppOsConfigService.ConfigArg config) {
        final String prefKey = config.getPrefKey();
        final Preference pref = handler.findPreference(prefKey);
        if (pref == null) {
            return;
        }

        if (!(pref instanceof EditTextPreference)) {
            return;
        }

        final int minValue = prefs.getInt(ZeppOsConfigService.getPrefMinKey(prefKey), Integer.MAX_VALUE);
        if (minValue == Integer.MAX_VALUE) {
            LOG.warn("Missing min value for {}", prefKey);
            return;
        }

        final int maxValue = prefs.getInt(ZeppOsConfigService.getPrefMaxKey(prefKey), Integer.MIN_VALUE);
        if (maxValue == Integer.MAX_VALUE) {
            LOG.warn("Missing max value for {}", prefKey);
            return;
        }

        DeviceSettingsUtils.enforceMinMax((EditTextPreference) pref, minValue, maxValue);
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
