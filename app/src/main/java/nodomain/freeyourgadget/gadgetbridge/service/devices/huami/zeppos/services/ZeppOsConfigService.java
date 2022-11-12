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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import static org.apache.commons.lang3.ArrayUtils.subarray;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_PASSWORD;
import static nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_PASSWORD_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_EXPOSE_HR_THIRDPARTY;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_START;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.GpsCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.WorkoutDetectionCapability;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLift;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLiftSensitivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.AlwaysOnDisplay;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DoNotDisturb;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021MenuType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiLanguageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsConfigService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsConfigService.class);

    private static final short ENDPOINT = 0x000a;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_REQUEST = 0x03;
    private static final byte CMD_RESPONSE = 0x04;
    private static final byte CMD_SET = 0x05;
    private static final byte CMD_ACK = 0x06;

    private final Map<ConfigGroup, Byte> mGroupVersions = new HashMap<>();

    public ZeppOsConfigService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_ACK:
                LOG.info("Configuration ACK, status = {}", payload[1]);
                return;

            case CMD_RESPONSE:
                if (payload[1] != 1) {
                    LOG.warn("Configuration response not success: {}", payload[1]);
                    return;
                }

                handle2021ConfigResponse(payload);
                return;
            default:
                LOG.warn("Unexpected configuration payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private boolean sentFitnessGoal = false;

    private void handle2021ConfigResponse(final byte[] payload) {
        final ConfigGroup configGroup = ConfigGroup.fromValue(payload[2]);
        if (configGroup == null) {
            LOG.warn("Unknown config type {}", String.format("0x%02x", payload[2]));
            return;
        }

        final byte version = payload[3];
        if (configGroup.getVersion() != version) {
            // Special case for HEALTH, where we actually support version 1 as well
            // TODO: Support multiple versions in a cleaner way...
            if (!(configGroup == ConfigGroup.HEALTH && configGroup.getVersion() == 1)) {
                LOG.warn("Unexpected version {} for {}", String.format("0x%02x", version), configGroup);
                return;
            }
        }
        mGroupVersions.put(configGroup, version);

        final boolean includesConstraints = payload[4] == 0x01;

        int numConfigs = payload[5] & 0xff;

        LOG.info("Got {} configs for {} version {}", numConfigs, configGroup, version);

        final Map<String, Object> prefs = new ZeppOsConfigService.ConfigParser(configGroup, includesConstraints)
                .parse(numConfigs, subarray(payload, 6, payload.length));

        if (prefs == null) {
            return;
        }

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(prefs);
        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);

        if (getSupport().getDevice().isInitialized()) {
            if (prefs.containsKey(PREF_LANGUAGE) && prefs.get(PREF_LANGUAGE).equals(PREF_LANGUAGE_AUTO)) {
                // Band is reporting automatic language, we need to send the actual language
                getSupport().onSendConfiguration(PREF_LANGUAGE);
            }
            if (prefs.containsKey(PREF_TIMEFORMAT) && prefs.get(PREF_TIMEFORMAT).equals(PREF_TIMEFORMAT_AUTO)) {
                // Band is reporting automatic time format, we need to send the actual time format
                getSupport().onSendConfiguration(PREF_TIMEFORMAT);
            }
        }

        if (configGroup == ConfigGroup.HEALTH && !sentFitnessGoal) {
            // We need to send the fitness goal after we got the protocol version
            getSupport().onSendConfiguration(PREF_USER_FITNESS_GOAL);
            sentFitnessGoal = true;
        }
    }

    public void requestAllConfigs(final TransactionBuilder builder) {
        for (final ConfigGroup configGroup : ConfigGroup.values()) {
            requestConfig(builder, configGroup);
        }
    }

    public void requestConfig(final TransactionBuilder builder, final ConfigGroup config) {
        requestConfig(builder, config, true, ZeppOsConfigService.ConfigArg.getAllArgsForConfigGroup(config));
    }

    public void requestConfig(final TransactionBuilder builder,
                              final ConfigGroup config,
                              final boolean includeConstraints,
                              final List<ZeppOsConfigService.ConfigArg> args) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(CMD_REQUEST);
        baos.write((byte) (includeConstraints ? 1 : 0));
        baos.write(config.getValue());
        baos.write(args.size());
        for (final ZeppOsConfigService.ConfigArg arg : args) {
            baos.write(arg.getCode());
        }

        write(builder, baos.toByteArray());
    }

    public enum ConfigGroup {
        DISPLAY(0x01, 0x02),
        // TODO 0x02
        SOUND_AND_VIBRATION(0x03, 0x02),
        LOCKSCREEN(0x04, 0x01),
        WEARING_DIRECTION(0x05, 0x02),
        OFFLINE_VOICE(0x06, 0x02),
        LANGUAGE(0x07, 0x02),
        HEALTH(0x08, 0x02),
        WORKOUT(0x09, 0x01),
        SYSTEM(0x0a, 0x01),
        BLUETOOTH(0x0b, 0x01),
        ;

        private final byte value;
        private final byte version;

        ConfigGroup(int value, int version) {
            this.value = (byte) value;
            this.version = (byte) version;
        }

        public byte getValue() {
            return value;
        }

        public byte getVersion() {
            return version;
        }

        public static ConfigGroup fromValue(final byte value) {
            for (final ConfigGroup configGroup : values()) {
                if (configGroup.getValue() == value) {
                    return configGroup;
                }
            }

            return null;
        }
    }

    public enum ConfigType {
        BOOL(0x0b),
        STRING(0x20),
        STRING_LIST(0x21),
        SHORT(0x01),
        INT(0x03),
        BYTE(0x10),
        BYTE_LIST(0x11),
        DATETIME_HH_MM(0x30),
        ;

        private final byte value;

        ConfigType(int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return value;
        }

        public static ConfigType fromValue(final byte value) {
            for (final ConfigType configType : values()) {
                if (configType.getValue() == value) {
                    return configType;
                }
            }

            return null;
        }
    }

    public enum ConfigArg {
        // Display
        SCREEN_AUTO_BRIGHTNESS(ConfigGroup.DISPLAY, ConfigType.BOOL, 0x01, PREF_SCREEN_AUTO_BRIGHTNESS),
        SCREEN_BRIGHTNESS(ConfigGroup.DISPLAY, ConfigType.SHORT, 0x02, PREF_SCREEN_BRIGHTNESS),
        SCREEN_TIMEOUT(ConfigGroup.DISPLAY, ConfigType.BYTE, 0x03, PREF_SCREEN_TIMEOUT),
        ALWAYS_ON_DISPLAY_MODE(ConfigGroup.DISPLAY, ConfigType.BYTE, 0x04, PREF_ALWAYS_ON_DISPLAY_MODE),
        ALWAYS_ON_DISPLAY_SCHEDULED_START(ConfigGroup.DISPLAY, ConfigType.DATETIME_HH_MM, 0x05, PREF_ALWAYS_ON_DISPLAY_START),
        ALWAYS_ON_DISPLAY_SCHEDULED_END(ConfigGroup.DISPLAY, ConfigType.DATETIME_HH_MM, 0x06, PREF_ALWAYS_ON_DISPLAY_END),
        LIFT_WRIST_MODE(ConfigGroup.DISPLAY, ConfigType.BYTE, 0x08, PREF_ACTIVATE_DISPLAY_ON_LIFT),
        LIFT_WRIST_SCHEDULED_START(ConfigGroup.DISPLAY, ConfigType.DATETIME_HH_MM, 0x09, PREF_DISPLAY_ON_LIFT_START),
        LIFT_WRIST_SCHEDULED_END(ConfigGroup.DISPLAY, ConfigType.DATETIME_HH_MM, 0x0a, PREF_DISPLAY_ON_LIFT_END),
        LIFT_WRIST_RESPONSE_SENSITIVITY(ConfigGroup.DISPLAY, ConfigType.BYTE, 0x0b, PREF_DISPLAY_ON_LIFT_SENSITIVITY),
        SCREEN_ON_ON_NOTIFICATIONS(ConfigGroup.DISPLAY, ConfigType.BOOL, 0x0c, PREF_SCREEN_ON_ON_NOTIFICATIONS),
        ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE(ConfigGroup.DISPLAY, ConfigType.BOOL, 0x0e, PREF_ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE),
        ALWAYS_ON_DISPLAY_STYLE(ConfigGroup.DISPLAY, ConfigType.STRING_LIST, 0x0f, PREF_ALWAYS_ON_DISPLAY_STYLE),

        // Sound and Vibration
        VOLUME(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.SHORT, 0x02, PREF_VOLUME),
        CROWN_VIBRATION(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.BOOL, 0x06, PREF_CROWN_VIBRATION),
        ALERT_TONE(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.BOOL, 0x07, PREF_ALERT_TONE),
        COVER_TO_MUTE(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.BOOL, 0x08, PREF_COVER_TO_MUTE),
        VIBRATE_FOR_ALERT(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.BOOL, 0x09, PREF_VIBRATE_FOR_ALERT),
        TEXT_TO_SPEECH(ConfigGroup.SOUND_AND_VIBRATION, ConfigType.BOOL, 0x0a, PREF_TEXT_TO_SPEECH),

        // Wearing Direction
        WEARING_DIRECTION_BUTTONS(ConfigGroup.WEARING_DIRECTION, ConfigType.BYTE, 0x02, PREF_WEARDIRECTION),

        // Offline Voice
        OFFLINE_VOICE_RESPOND_TURN_WRIST(ConfigGroup.OFFLINE_VOICE, ConfigType.BOOL, 0x01, PREF_OFFLINE_VOICE_RESPOND_TURN_WRIST),
        OFFLINE_VOICE_RESPOND_SCREEN_ON(ConfigGroup.OFFLINE_VOICE, ConfigType.BOOL, 0x02, PREF_OFFLINE_VOICE_RESPOND_SCREEN_ON),
        OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING(ConfigGroup.OFFLINE_VOICE, ConfigType.BOOL, 0x03, PREF_OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING),
        OFFLINE_VOICE_LANGUAGE(ConfigGroup.OFFLINE_VOICE, ConfigType.BYTE, 0x04, PREF_OFFLINE_VOICE_LANGUAGE),

        // Lock Screen
        PASSWORD_ENABLED(ConfigGroup.LOCKSCREEN, ConfigType.BOOL, 0x01, PREF_PASSWORD_ENABLED),
        PASSWORD_TEXT(ConfigGroup.LOCKSCREEN, ConfigType.STRING, 0x02, PREF_PASSWORD),

        // Language
        LANGUAGE(ConfigGroup.LANGUAGE, ConfigType.BYTE, 0x01, PREF_LANGUAGE),
        LANGUAGE_FOLLOW_PHONE(ConfigGroup.LANGUAGE, ConfigType.BOOL, 0x02, null /* special case, handled below */),

        // Health
        HEART_RATE_ALL_DAY_MONITORING(ConfigGroup.HEALTH, ConfigType.BYTE, 0x01, PREF_HEARTRATE_MEASUREMENT_INTERVAL),
        HEART_RATE_HIGH_ALERTS(ConfigGroup.HEALTH, ConfigType.BYTE, 0x02, PREF_HEARTRATE_ALERT_HIGH_THRESHOLD),
        HEART_RATE_LOW_ALERTS(ConfigGroup.HEALTH, ConfigType.BYTE, 0x03, PREF_HEARTRATE_ALERT_LOW_THRESHOLD),
        HEART_RATE_ACTIVITY_MONITORING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x04, PREF_HEARTRATE_ACTIVITY_MONITORING),
        THIRD_PARTY_HR_SHARING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x05, PREF_EXPOSE_HR_THIRDPARTY),
        SLEEP_HIGH_ACCURACY_MONITORING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x11, PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION),
        SLEEP_BREATHING_QUALITY_MONITORING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x12, PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING),
        STRESS_MONITORING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x13, PREF_HEARTRATE_STRESS_MONITORING),
        STRESS_RELAXATION_REMINDER(ConfigGroup.HEALTH, ConfigType.BOOL, 0x14, PREF_HEARTRATE_STRESS_RELAXATION_REMINDER),
        SPO2_ALL_DAY_MONITORING(ConfigGroup.HEALTH, ConfigType.BOOL, 0x31, PREF_SPO2_ALL_DAY_MONITORING),
        SPO2_LOW_ALERT(ConfigGroup.HEALTH, ConfigType.BYTE, 0x32, PREF_SPO2_LOW_ALERT_THRESHOLD),
        FITNESS_GOAL_NOTIFICATION(ConfigGroup.HEALTH, ConfigType.BOOL, 0x51, PREF_USER_FITNESS_GOAL_NOTIFICATION),
        FITNESS_GOAL_STEPS(ConfigGroup.HEALTH, null /* Special case, handled below */, 0x52, null), // TODO needs to be handled globally
        FITNESS_GOAL_CALORIES(ConfigGroup.HEALTH, ConfigType.SHORT, 0x53, null), // TODO needs to be handled globally
        FITNESS_GOAL_WEIGHT(ConfigGroup.HEALTH, ConfigType.SHORT, 0x54, null), // TODO needs to be handled globally
        FITNESS_GOAL_SLEEP(ConfigGroup.HEALTH, ConfigType.SHORT, 0x55, null), // TODO needs to be handled globally
        FITNESS_GOAL_STANDING_TIME(ConfigGroup.HEALTH, ConfigType.SHORT, 0x56, null), // TODO needs to be handled globally
        FITNESS_GOAL_FAT_BURN_TIME(ConfigGroup.HEALTH, ConfigType.SHORT, 0x57, null), // TODO needs to be handled globally
        INACTIVITY_WARNINGS_ENABLED(ConfigGroup.HEALTH, ConfigType.BOOL, 0x41, PREF_INACTIVITY_ENABLE),
        INACTIVITY_WARNINGS_SCHEDULED_START(ConfigGroup.HEALTH, ConfigType.DATETIME_HH_MM, 0x42, PREF_INACTIVITY_START),
        INACTIVITY_WARNINGS_SCHEDULED_END(ConfigGroup.HEALTH, ConfigType.DATETIME_HH_MM, 0x43, PREF_INACTIVITY_END),
        INACTIVITY_WARNINGS_DND_ENABLED(ConfigGroup.HEALTH, ConfigType.BOOL, 0x44, PREF_INACTIVITY_DND),
        INACTIVITY_WARNINGS_DND_SCHEDULED_START(ConfigGroup.HEALTH, ConfigType.DATETIME_HH_MM, 0x45, PREF_INACTIVITY_DND_START),
        INACTIVITY_WARNINGS_DND_SCHEDULED_END(ConfigGroup.HEALTH, ConfigType.DATETIME_HH_MM, 0x46, PREF_INACTIVITY_DND_END),

        // Workout
        WORKOUT_GPS_PRESET(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x20, PREF_GPS_MODE_PRESET),
        WORKOUT_GPS_BAND(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x21, PREF_GPS_BAND),
        WORKOUT_GPS_COMBINATION(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x22, PREF_GPS_COMBINATION),
        WORKOUT_GPS_SATELLITE_SEARCH(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x23, PREF_GPS_SATELLITE_SEARCH),
        WORKOUT_AGPS_EXPIRY_REMINDER_ENABLED(ConfigGroup.WORKOUT, ConfigType.BOOL, 0x30, PREF_AGPS_EXPIRY_REMINDER_ENABLED),
        WORKOUT_AGPS_EXPIRY_REMINDER_TIME(ConfigGroup.WORKOUT, ConfigType.DATETIME_HH_MM, 0x31, PREF_AGPS_EXPIRY_REMINDER_TIME),
        WORKOUT_DETECTION_CATEGORY(ConfigGroup.WORKOUT, ConfigType.BYTE_LIST, 0x40, PREF_WORKOUT_DETECTION_CATEGORIES),
        WORKOUT_DETECTION_ALERT(ConfigGroup.WORKOUT, ConfigType.BOOL, 0x41, PREF_WORKOUT_DETECTION_ALERT),
        WORKOUT_DETECTION_SENSITIVITY(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x42, PREF_WORKOUT_DETECTION_SENSITIVITY),
        WORKOUT_POOL_SWIMMING_SIZE(ConfigGroup.WORKOUT, ConfigType.BYTE, 0x51, null), // TODO ?

        // System
        TIME_FORMAT(ConfigGroup.SYSTEM, ConfigType.BYTE, 0x01, PREF_TIMEFORMAT),
        DATE_FORMAT(ConfigGroup.SYSTEM, ConfigType.STRING, 0x02, PREF_DATEFORMAT),
        DND_MODE(ConfigGroup.SYSTEM, ConfigType.BYTE, 0x0a, PREF_DO_NOT_DISTURB),
        DND_SCHEDULED_START(ConfigGroup.SYSTEM, ConfigType.DATETIME_HH_MM, 0x0b, PREF_DO_NOT_DISTURB_START),
        DND_SCHEDULED_END(ConfigGroup.SYSTEM, ConfigType.DATETIME_HH_MM, 0x0c, PREF_DO_NOT_DISTURB_END),
        TEMPERATURE_UNIT(ConfigGroup.SYSTEM, ConfigType.BYTE, 0x12, SettingsActivity.PREF_MEASUREMENT_SYSTEM),
        TIME_FORMAT_FOLLOWS_PHONE(ConfigGroup.SYSTEM, ConfigType.BOOL, 0x13, null /* special case, handled below */),
        UPPER_BUTTON_LONG_PRESS(ConfigGroup.SYSTEM, ConfigType.STRING_LIST, 0x15, PREF_UPPER_BUTTON_LONG_PRESS),
        LOWER_BUTTON_PRESS(ConfigGroup.SYSTEM, ConfigType.STRING_LIST, 0x16, PREF_LOWER_BUTTON_SHORT_PRESS),
        DISPLAY_CALLER(ConfigGroup.SYSTEM, ConfigType.BOOL, 0x18, null), // TODO Handle
        NIGHT_MODE_MODE(ConfigGroup.SYSTEM, ConfigType.BYTE, 0x1b, PREF_NIGHT_MODE),
        NIGHT_MODE_SCHEDULED_START(ConfigGroup.SYSTEM, ConfigType.DATETIME_HH_MM, 0x1c, PREF_NIGHT_MODE_START),
        NIGHT_MODE_SCHEDULED_END(ConfigGroup.SYSTEM, ConfigType.DATETIME_HH_MM, 0x1d, PREF_NIGHT_MODE_END),
        SLEEP_MODE_SLEEP_SCREEN(ConfigGroup.SYSTEM, ConfigType.BOOL, 0x21, PREF_SLEEP_MODE_SLEEP_SCREEN),
        SLEEP_MODE_SMART_ENABLE(ConfigGroup.SYSTEM, ConfigType.BOOL, 0x22, PREF_SLEEP_MODE_SMART_ENABLE),

        // Bluetooth
        BLUETOOTH_CONNECTED_ADVERTISING(ConfigGroup.BLUETOOTH, ConfigType.BOOL, 0x02, PREF_BT_CONNECTED_ADVERTISEMENT),
        ;

        private final ConfigGroup configGroup;
        private final ConfigType configType;
        private final byte code;
        private final String prefKey;

        ConfigArg(final ConfigGroup configGroup, final ConfigType configType, final int code, final String prefKey) {
            this.configGroup = configGroup;
            this.configType = configType;
            this.code = (byte) code;
            this.prefKey = prefKey;
        }

        public ConfigGroup getConfigGroup() {
            return configGroup;
        }

        public ConfigType getConfigType(@Nullable final Map<ConfigGroup, Byte> groupVersions) {
            if (this == FITNESS_GOAL_STEPS) {
                if (groupVersions == null) {
                    return ConfigType.INT;
                }

                final Byte groupVersion = groupVersions.get(getConfigGroup());
                if (groupVersion == null) {
                    LOG.error("Version for {} is not known", getConfigGroup());
                    return null;
                }

                switch (groupVersion) {
                    case 0x01:
                        return ConfigType.SHORT;
                    case 0x02:
                    default:
                        return ConfigType.INT;
                }
            }

            return configType;
        }

        public byte getCode() {
            return code;
        }

        public String getPrefKey() {
            return prefKey;
        }

        public static ConfigArg fromCode(final ConfigGroup configGroup, final byte code) {
            for (final ZeppOsConfigService.ConfigArg arg : values()) {
                if (arg.getConfigGroup().equals(configGroup) && arg.getCode() == code) {
                    return arg;
                }
            }
            return null;
        }

        public static List<ConfigArg> getAllArgsForConfigGroup(final ConfigGroup configGroup) {
            final List<ZeppOsConfigService.ConfigArg> configArgs = new ArrayList<>();
            for (final ZeppOsConfigService.ConfigArg arg : values()) {
                if (arg.getConfigGroup().equals(configGroup)) {
                    configArgs.add(arg);
                }
            }
            return configArgs;
        }
    }

    /**
     * Map of pref key to config.
     */
    private static final Map<String, ConfigArg> PREF_TO_CONFIG = new HashMap<String, ConfigArg>() {{
        for (final ConfigArg arg : ConfigArg.values()) {
            if (arg.getPrefKey() != null) {
                if (containsKey(arg.getPrefKey())) {
                    LOG.error("Duplicate config preference key: {}", arg);
                    continue;
                }
                put(arg.getPrefKey(), arg);
            }
        }
    }};

    /**
     * Updates a {@link ConfigSetter} with a preference. Default values don't really matter - if we're
     * setting this preference, it's because the device reported it, along with the current value, so we
     * shouldn't need a default unless there's a bug.
     *
     * @return true if the {@link ConfigSetter} was updated for this preference key
     */
    public boolean setConfig(final Prefs prefs, final String key, final ConfigSetter setter) {
        final ConfigArg configArg = PREF_TO_CONFIG.get(key);
        if (configArg == null) {
            LOG.error("Unknown pref key {}", key);
            return false;
        }

        switch (configArg.getConfigType(mGroupVersions)) {
            case BOOL:
                setter.setBoolean(configArg, prefs.getBoolean(key, false));
                return true;
            case STRING:
                final String encodedString = encodeString(configArg, prefs.getString(key, null));
                if (encodedString != null) {
                    setter.setString(configArg, encodedString);
                    return true;
                }
                break;
            case STRING_LIST:
                final String encodedStringList = encodeString(configArg, prefs.getString(key, null));
                if (encodedStringList != null) {
                    setter.setStringList(configArg, encodedStringList);
                    return true;
                }
                break;
            case SHORT:
                setter.setShort(configArg, (short) prefs.getInt(key, 0));
                return true;
            case INT:
                setter.setInt(configArg, prefs.getInt(key, 0));
                return true;
            case BYTE:
                final Byte encodedByte = encodeByte(configArg, prefs.getString(key, null));
                if (encodedByte != null) {
                    setter.setByte(configArg, encodedByte);
                    return true;
                }
                break;
            case BYTE_LIST:
                final Set<String> byteListString = prefs.getStringSet(key, Collections.emptySet());
                final byte[] encodedByteList = new byte[byteListString.size()];
                int i = 0;
                for (final String s : byteListString) {
                    encodedByteList[i++] = encodeByte(configArg, s);
                }
                setter.setByteList(configArg, encodedByteList);
                return true;
            case DATETIME_HH_MM:
                setter.setHourMinute(configArg, prefs.getTimePreference(key, "00:00"));
                return true;
        }

        LOG.warn("Failed to set {}", configArg);

        return false;
    }

    private static String encodeString(final ConfigArg configArg, final String value) {
        if (value == null) {
            return null;
        }

        switch (configArg) {
            case UPPER_BUTTON_LONG_PRESS:
            case LOWER_BUTTON_PRESS:
                return MapUtils.reverse(Huami2021MenuType.displayItemNameLookup).get(value);
            case DATE_FORMAT:
                return value.replace("/", ".");
        }

        return value; // passthrough
    }

    private static Byte encodeByte(final ConfigArg configArg, final String value) {
        if (value == null) {
            return null;
        }

        switch (configArg) {
            case ALWAYS_ON_DISPLAY_MODE:
                return encodeEnum(ALWAYS_ON_DISPLAY_MAP, value);
            case LIFT_WRIST_MODE:
                return encodeEnum(LIFT_WRIST_MAP, value);
            case LIFT_WRIST_RESPONSE_SENSITIVITY:
                return encodeEnum(LIFT_WRIST_SENSITIVITY_MAP, value);
            case LANGUAGE:
                return languageLocaleToByte(value);
            case HEART_RATE_ALL_DAY_MONITORING:
                return encodeHeartRateAllDayMonitoring(value);
            case SCREEN_TIMEOUT:
            case HEART_RATE_HIGH_ALERTS:
            case HEART_RATE_LOW_ALERTS:
            case SPO2_LOW_ALERT:
                return (byte) Integer.parseInt(value);
            case TIME_FORMAT:
                return encodeString(TIME_FORMAT_MAP, value);
            case DND_MODE:
                return encodeEnum(DND_MODE_MAP, value);
            case TEMPERATURE_UNIT:
                return encodeEnum(TEMPERATURE_UNIT_MAP, value);
            case NIGHT_MODE_MODE:
                return encodeString(NIGHT_MODE_MAP, value);
            case WEARING_DIRECTION_BUTTONS:
                return encodeString(WEARING_DIRECTION_MAP, value);
            case OFFLINE_VOICE_LANGUAGE:
                return encodeString(OFFLINE_VOICE_LANGUAGE_MAP, value);
            case WORKOUT_GPS_PRESET:
                return encodeEnum(GPS_PRESET_MAP, value);
            case WORKOUT_GPS_BAND:
                return encodeEnum(GPS_BAND_MAP, value);
            case WORKOUT_GPS_COMBINATION:
                return encodeEnum(GPS_COMBINATION_MAP, value);
            case WORKOUT_GPS_SATELLITE_SEARCH:
                return encodeEnum(GPS_SATELLITE_SEARCH_MAP, value);
            case WORKOUT_DETECTION_CATEGORY:
                return encodeEnum(WORKOUT_DETECTION_CATEGORY_MAP, value);
            case WORKOUT_DETECTION_SENSITIVITY:
                return encodeEnum(WORKOUT_DETECTION_SENSITIVITY_MAP, value);
        }

        LOG.error("No encoder for {}", configArg);

        return null;
    }

    /**
     * Returns the preference key where to save the minimum possible value for a preference.
     */
    public static String getPrefMinKey(final String key) {
        return String.format(Locale.ROOT, "%s_huami_2021_min", key);
    }

    /**
     * Returns the preference key where to save the maximum possible value for a preference.
     */
    public static String getPrefMaxKey(final String key) {
        return String.format(Locale.ROOT, "%s_huami_2021_max", key);
    }

    /**
     * Returns the preference key where to save the list of possible value for a preference, comma-separated.
     */
    public static String getPrefPossibleValuesKey(final String key) {
        return String.format(Locale.ROOT, "%s_huami_2021_possible_values", key);
    }

    /**
     * Returns the preference key where to that a config was reported as supported (boolean).
     */
    public static String getPrefKnownConfig(final ConfigArg pref) {
        return String.format(Locale.ROOT, "huami_2021_known_config_%s", pref.name());
    }

    public static boolean deviceHasConfig(final Prefs devicePrefs, final ZeppOsConfigService.ConfigArg config) {
        return devicePrefs.getBoolean(getPrefKnownConfig(config), false);
    }

    public ConfigSetter newSetter() {
        return new ConfigSetter();
    }

    public class ConfigSetter {
        private final Map<ConfigGroup, Map<ConfigArg, byte[]>> arguments = new LinkedHashMap<>();

        public ConfigSetter() {
        }

        public ConfigSetter setBoolean(final ConfigArg arg, final boolean value) {
            checkArg(arg, ConfigType.BOOL);

            putArgument(arg, new byte[]{(byte) (value ? 0x01 : 0x00)});

            return this;
        }

        public ConfigSetter setString(final ConfigArg arg, final String value) {
            checkArg(arg, ConfigType.STRING);

            putArgument(arg, (value + "\0").getBytes(StandardCharsets.UTF_8));

            return this;
        }

        public ConfigSetter setStringList(final ConfigArg arg, final String value) {
            checkArg(arg, ConfigType.STRING_LIST);

            putArgument(arg, (value + "\0").getBytes(StandardCharsets.UTF_8));

            return this;
        }

        public ConfigSetter setShort(final ConfigArg arg, final short value) {
            checkArg(arg, ConfigType.SHORT);

            putArgument(arg, BLETypeConversions.fromUint16(value));

            return this;
        }

        public ConfigSetter setInt(final ConfigArg arg, final int value) {
            checkArg(arg, ConfigType.INT);

            putArgument(arg, BLETypeConversions.fromUint32(value));

            return this;
        }

        public ConfigSetter setByte(final ConfigArg arg, final byte value) {
            checkArg(arg, ConfigType.BYTE);

            putArgument(arg, new byte[]{value});

            return this;
        }

        public ConfigSetter setByteList(final ConfigArg arg, final byte[] values) {
            checkArg(arg, ConfigType.BYTE_LIST);

            putArgument(arg, ArrayUtils.addAll(new byte[]{(byte) values.length}, values));

            return this;
        }

        public ConfigSetter setHourMinute(final ConfigArg arg, final Date date) {
            checkArg(arg, ConfigType.DATETIME_HH_MM);

            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(date);

            putArgument(arg, new byte[]{
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE)
            });

            return this;
        }

        private void putArgument(final ConfigArg arg, final byte[] encodedValue) {
            final Map<ConfigArg, byte[]> groupMap;
            if (arguments.containsKey(arg.getConfigGroup())) {
                groupMap = arguments.get(arg.getConfigGroup());
            } else {
                groupMap = new LinkedHashMap<>();
                arguments.put(arg.getConfigGroup(), groupMap);
            }

            groupMap.put(arg, encodedValue);
        }

        public byte[] encode(final ConfigGroup configGroup) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            final Map<ConfigArg, byte[]> configArgMap = arguments.get(configGroup);

            try {
                baos.write(CMD_SET);
                baos.write(configGroup.getValue());
                baos.write(configGroup.getVersion());
                baos.write(0x00); // ?
                baos.write(configArgMap.size());
                for (final Map.Entry<ConfigArg, byte[]> arg : configArgMap.entrySet()) {
                    final ConfigType configType = arg.getKey().getConfigType(mGroupVersions);
                    baos.write(arg.getKey().getCode());
                    baos.write(configType.getValue());
                    baos.write(arg.getValue());
                }
            } catch (final IOException e) {
                LOG.error("Failed to encode command", e);
            }

            return baos.toByteArray();
        }

        public void write(final TransactionBuilder builder) {
            // Write one command per config group
            for (final ConfigGroup configGroup : arguments.keySet()) {
                ZeppOsConfigService.this.write(builder, encode(configGroup));
            }
        }

        private void checkArg(final ConfigArg arg, final ConfigType expectedConfigType) {
            if (arg.getConfigType(mGroupVersions) == null) {
                // Some special cases (STEPS goal) do not have a config type
                return;
            }

            try {
                if (!expectedConfigType.equals(arg.getConfigType(mGroupVersions))) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Invalid arg type %s for %s, expected %s",
                                    expectedConfigType,
                                    arg,
                                    arg.getConfigType(mGroupVersions)
                            )
                    );
                }
            } catch (final IllegalArgumentException e) {
                if (!BuildConfig.DEBUG) {
                    // Crash
                    throw e;
                } else {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    public class ConfigParser {
        private final ConfigGroup configGroup;
        private final boolean includesConstraints;

        public ConfigParser(final ConfigGroup configGroup, final boolean includesConstraints) {
            this.configGroup = configGroup;
            this.includesConstraints = includesConstraints;
        }

        public Map<String, Object> parse(final int expectedNumConfigs, final byte[] bytes) {
            final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            final Map<String, Object> prefs = new HashMap<>();

            int configCount = 0;

            while (buf.position() < buf.limit()) {
                if (configCount > expectedNumConfigs) {
                    LOG.error("Got more configs than {}", expectedNumConfigs);
                    return null;
                }

                final byte configArgByte = buf.get();
                final ZeppOsConfigService.ConfigArg configArg = ZeppOsConfigService.ConfigArg.fromCode(configGroup, configArgByte);
                if (configArg == null) {
                    LOG.error("Unknown config {} for {}", String.format("0x%02x", configArgByte), configGroup);
                }

                final byte configTypeByte = buf.get();
                final ConfigType configType = ConfigType.fromValue(configTypeByte);
                if (configType == null) {
                    LOG.error("Unknown type {} for {}", String.format("0x%02x", configTypeByte), configArg);
                    // Abort, since we don't know how to parse this type or how many bytes it is
                    // Return whatever we parsed so far, since that's still valid
                    return prefs;
                }
                if (configArg != null) {
                    if (configType != configArg.getConfigType(mGroupVersions)) {
                        LOG.warn("Unexpected arg type {} for {}, expected {}", configType, configArg, configArg.getConfigType(mGroupVersions));
                    }
                }

                Map<String, Object> argPrefs = null;

                // FIXME this switch has a lot of repeated code that could be generalized...
                switch (configType) {
                    case BOOL:
                        final ConfigBoolean valBoolean = ConfigBoolean.consume(buf);
                        if (valBoolean == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valBoolean);
                        if (configArg != null) {
                            argPrefs = convertBooleanToPrefs(configArg, valBoolean);
                        }
                        break;
                    case STRING:
                        final ConfigString valString = ConfigString.consume(buf, includesConstraints);
                        if (valString == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valString);
                        if (configArg != null) {
                            argPrefs = convertStringToPrefs(configArg, valString);
                        }
                        break;
                    case STRING_LIST:
                        final ConfigStringList valStringList = ConfigStringList.consume(buf, includesConstraints);
                        if (valStringList == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valStringList);
                        if (configArg != null) {
                            argPrefs = convertStringListToPrefs(configArg, valStringList);
                        }
                        break;
                    case SHORT:
                        final ConfigShort valShort = ConfigShort.consume(buf, includesConstraints);
                        if (valShort == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valShort);
                        if (configArg != null) {
                            argPrefs = convertShortToPrefs(configArg, valShort);
                        }
                        break;
                    case INT:
                        final ConfigInt valInt = ConfigInt.consume(buf, includesConstraints);
                        if (valInt == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valInt);
                        if (configArg != null) {
                            argPrefs = convertIntToPrefs(configArg, valInt);
                        }
                        break;
                    case BYTE:
                        final ConfigByte valByte = ConfigByte.consume(buf, includesConstraints);
                        if (valByte == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valByte);
                        if (configArg != null) {
                            argPrefs = convertByteToPrefs(configArg, valByte);
                        }
                        break;
                    case BYTE_LIST:
                        final ConfigByteList valByteList = ConfigByteList.consume(buf, includesConstraints);
                        if (valByteList == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valByteList);
                        if (configArg != null) {
                            argPrefs = convertByteListToPrefs(configArg, valByteList);
                        }
                        break;
                    case DATETIME_HH_MM:
                        final ConfigDatetimeHhMm valHhMm = ConfigDatetimeHhMm.consume(buf);
                        if (valHhMm == null) {
                            LOG.error("Failed to parse {} for {}", configType, configArg);
                            return prefs;
                        }
                        LOG.info("Got {} ({}) = {}", configArg, String.format("0x%02x", configArgByte), valHhMm);
                        if (configArg != null) {
                            argPrefs = convertDatetimeHhMmToPrefs(configArg, valHhMm);
                        }
                        break;
                    default:
                        LOG.error("No parser for {}", configArg);
                        // Abort, since we don't know how to parse this type or how many bytes it is
                        // Return whatever we parsed so far, since that's still valid
                        return prefs;
                }

                if (argPrefs == null) {
                    LOG.warn("Unhandled {} pref of type {}", configType, configArg);
                }

                if (configArg != null && argPrefs != null && configType == configArg.getConfigType(mGroupVersions)) {
                    prefs.put(getPrefKnownConfig(configArg), true);

                    // Special cases for "follow phone" preferences. We need to ensure that "auto"
                    // always has precedence
                    if (argPrefs.containsKey(PREF_LANGUAGE) && prefs.containsKey(PREF_LANGUAGE)) {
                        if (Objects.equals(prefs.get(PREF_LANGUAGE), DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO)) {
                            argPrefs.remove(PREF_LANGUAGE);
                        }
                    }
                    if (argPrefs.containsKey(PREF_TIMEFORMAT) && prefs.containsKey(PREF_TIMEFORMAT)) {
                        if (Objects.equals(prefs.get(PREF_TIMEFORMAT), DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO)) {
                            argPrefs.remove(PREF_TIMEFORMAT);
                        }
                    }

                    prefs.putAll(argPrefs);
                }

                configCount++;
            }

            return prefs;
        }

        private Map<String, Object> convertBooleanToPrefs(final ConfigArg configArg, final ConfigBoolean value) {
            // Special cases
            switch (configArg) {
                case LANGUAGE_FOLLOW_PHONE:
                    if (value.getValue()) {
                        return singletonMap(PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO);
                    } else {
                        // If not following phone, we'll receive the actual value in LANGUAGE
                        return Collections.emptyMap();
                    }
                case TIME_FORMAT_FOLLOWS_PHONE:
                    if (value.getValue()) {
                        return singletonMap(PREF_TIMEFORMAT, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO);
                    } else {
                        // If not following phone, we'll receive the actual value in TIME_FORMAT
                        return Collections.emptyMap();
                    }
                default:
                    break;
            }

            if (configArg.getPrefKey() != null) {
                // The arg maps to a boolean pref directly
                return singletonMap(configArg.getPrefKey(), value.getValue());
            }

            return null;
        }

        private Map<String, Object> convertStringToPrefs(final ConfigArg configArg, final ConfigString str) {
            // Special cases
            switch (configArg) {
                case DATE_FORMAT:
                    return singletonMap(PREF_DATEFORMAT, str.getValue().replace(".", "/").toUpperCase(Locale.ROOT));
                default:
                    break;
            }

            if (configArg.getPrefKey() != null) {
                // The arg maps to a string pref directly
                return singletonMap(configArg.getPrefKey(), str.getValue());
            }

            return null;
        }

        private Map<String, Object> convertStringListToPrefs(final ConfigArg configArg, final ConfigStringList str) {
            final List<String> possibleValues = str.getPossibleValues();
            final boolean includesConstraints = !possibleValues.isEmpty();
            Map<String, Object> prefs = null;
            final ValueDecoder<String> decoder;

            switch (configArg) {
                case UPPER_BUTTON_LONG_PRESS:
                case LOWER_BUTTON_PRESS:
                    decoder = Huami2021MenuType.displayItemNameLookup::get;
                    break;
                default:
                    decoder = a -> a; // passthrough
            }

            if (configArg.getPrefKey() != null) {
                prefs = singletonMap(configArg.getPrefKey(), decoder.decode(str.getValue()));
                if (includesConstraints) {
                    prefs.put(
                            getPrefPossibleValuesKey(configArg.getPrefKey()),
                            decodeStringValues(possibleValues, decoder)
                    );
                }
            }

            return prefs;
        }

        private Map<String, Object> convertShortToPrefs(final ConfigArg configArg, final ConfigShort value) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a number pref directly
                final Map<String, Object> prefs = singletonMap(configArg.getPrefKey(), value.getValue());

                if (value.isMinMaxKnown()) {
                    prefs.put(getPrefMinKey(configArg.getPrefKey()), value.getMin());
                    prefs.put(getPrefMaxKey(configArg.getPrefKey()), value.getMax());
                }

                return prefs;
            }

            return null;
        }

        private Map<String, Object> convertIntToPrefs(final ConfigArg configArg, final ConfigInt value) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a number pref directly
                final Map<String, Object> prefs = singletonMap(configArg.getPrefKey(), value.getValue());

                if (value.isMinMaxKnown()) {
                    prefs.put(getPrefMinKey(configArg.getPrefKey()), value.getMin());
                    prefs.put(getPrefMaxKey(configArg.getPrefKey()), value.getMax());
                }

                return prefs;
            }

            return null;
        }

        private Map<String, Object> convertDatetimeHhMmToPrefs(final ConfigArg configArg, final ConfigDatetimeHhMm hhmm) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a hhmm pref directly
                return singletonMap(configArg.getPrefKey(), hhmm.getValue());
            }

            return null;
        }

        private Map<String, Object> convertByteListToPrefs(final ConfigArg configArg, final ConfigByteList value) {
            final byte[] possibleValues = value.getPossibleValues();
            final boolean includesConstraints = possibleValues != null && possibleValues.length > 0;
            Map<String, Object> prefs = null;
            final ValueDecoder<Byte> decoder;

            switch (configArg) {
                case WORKOUT_DETECTION_CATEGORY:
                    decoder = b -> decodeEnum(WORKOUT_DETECTION_CATEGORY_MAP, b);
                    break;
                default:
                    LOG.warn("No decoder for {}", configArg);
                    return null;
            }

            if (configArg.getPrefKey() != null) {
                final List<String> valuesList = decodeByteValues(value.getValues(), decoder);
                prefs = singletonMap(configArg.getPrefKey(), new HashSet<>(valuesList));
                if (includesConstraints) {
                    prefs.put(
                            getPrefPossibleValuesKey(configArg.getPrefKey()),
                            String.join(",", decodeByteValues(possibleValues, decoder))
                    );
                }
            }

            return prefs;
        }

        private Map<String, Object> convertByteToPrefs(final ConfigArg configArg, final ConfigByte value) {
            final byte[] possibleValues = value.getPossibleValues();
            final boolean includesConstraints = value.getPossibleValues().length > 0;
            Map<String, Object> prefs = null;
            final ValueDecoder<Byte> decoder;

            switch (configArg) {
                case ALWAYS_ON_DISPLAY_MODE:
                    decoder = b -> decodeEnum(ALWAYS_ON_DISPLAY_MAP, b);
                    break;
                case LIFT_WRIST_MODE:
                    decoder = b -> decodeEnum(LIFT_WRIST_MAP, b);
                    break;
                case LIFT_WRIST_RESPONSE_SENSITIVITY:
                    decoder = b -> decodeEnum(LIFT_WRIST_SENSITIVITY_MAP, b);
                    break;
                case LANGUAGE:
                    final String language = languageByteToLocale(value.getValue());
                    if (language != null) {
                        prefs = singletonMap(configArg.getPrefKey(), language);
                        if (includesConstraints) {
                            final List<String> possibleLanguages = new ArrayList<>();
                            possibleLanguages.add("auto");
                            for (final byte possibleValue : value.getPossibleValues()) {
                                possibleLanguages.add(languageByteToLocale(possibleValue));
                            }
                            possibleLanguages.removeAll(Collections.singleton(null));
                            prefs.put(getPrefPossibleValuesKey(configArg.getPrefKey()), String.join(",", possibleLanguages));
                        }
                    }
                    decoder = null;
                    break;
                case HEART_RATE_ALL_DAY_MONITORING:
                    decoder = ZeppOsConfigService::decodeHeartRateAllDayMonitoring;
                    break;
                case SCREEN_TIMEOUT:
                case HEART_RATE_HIGH_ALERTS:
                case HEART_RATE_LOW_ALERTS:
                case SPO2_LOW_ALERT:
                    decoder = a -> String.format(Locale.ROOT, "%d", a & 0xff);
                    break;
                case TIME_FORMAT:
                    decoder = b -> decodeString(TIME_FORMAT_MAP, b);
                    break;
                case DND_MODE:
                    decoder = b -> decodeEnum(DND_MODE_MAP, b);
                    break;
                case TEMPERATURE_UNIT:
                    // TODO: This should be per device...
                    decoder = b -> decodeEnum(TEMPERATURE_UNIT_MAP, b);
                    break;
                case NIGHT_MODE_MODE:
                    decoder = b -> decodeString(NIGHT_MODE_MAP, b);
                    break;
                case WEARING_DIRECTION_BUTTONS:
                    decoder = b -> decodeString(WEARING_DIRECTION_MAP, b);
                    break;
                case OFFLINE_VOICE_LANGUAGE:
                    decoder = b -> decodeString(OFFLINE_VOICE_LANGUAGE_MAP, b);
                    break;
                case WORKOUT_GPS_PRESET:
                    decoder = b -> decodeEnum(GPS_PRESET_MAP, b);
                    break;
                case WORKOUT_GPS_BAND:
                    decoder = b -> decodeEnum(GPS_BAND_MAP, b);
                    break;
                case WORKOUT_GPS_COMBINATION:
                    decoder = b -> decodeEnum(GPS_COMBINATION_MAP, b);
                    break;
                case WORKOUT_GPS_SATELLITE_SEARCH:
                    decoder = b -> decodeEnum(GPS_SATELLITE_SEARCH_MAP, b);
                    break;
                case WORKOUT_DETECTION_SENSITIVITY:
                    decoder = b -> decodeEnum(WORKOUT_DETECTION_SENSITIVITY_MAP, b);
                    break;
                default:
                    decoder = null;
            }

            if (decoder != null) {
                prefs = singletonMap(configArg.getPrefKey(), decoder.decode(value.getValue()));
                if (includesConstraints) {
                    prefs.put(
                            getPrefPossibleValuesKey(configArg.getPrefKey()),
                            String.join(",", decodeByteValues(possibleValues, decoder))
                    );
                }
            }

            return prefs;
        }

        private List<String> decodeByteValues(final byte[] values, final ValueDecoder<Byte> decoder) {
            final List<String> decoded = new ArrayList<>(values.length);
            for (final byte b : values) {
                final String decodedByte = decoder.decode(b);
                if (decodedByte != null) {
                    decoded.add(decodedByte);
                } else {
                    decoded.add(String.format("0x%x", b));
                }
            }
            decoded.removeAll(Collections.singleton(null));
            return decoded;
        }

        private String decodeStringValues(final List<String> values, final ValueDecoder<String> decoder) {
            final List<String> decoded = new ArrayList<>(values.size());
            for (final String str : values) {
                final String decodedStr = decoder.decode(str);
                if (decodedStr != null) {
                    decoded.add(decodedStr);
                } else {
                    decoded.add(str);
                }
            }
            if (decoded.isEmpty()) {
                return null;
            }
            return String.join(",", decoded);
        }

        private Map<String, Object> singletonMap(final String key, final Object value) {
            if (key == null) {
                LOG.error("Null key in prefs update");
                if (BuildConfig.DEBUG) {
                    // Crash
                    throw new IllegalStateException("Null key in prefs update");
                }
                return Collections.emptyMap();
            }

            return new HashMap<String, Object>() {{
                put(key, value);
            }};
        }
    }

    private static class ConfigBoolean {
        private final boolean value;

        public ConfigBoolean(final boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }

        private static ConfigBoolean consume(final ByteBuffer buf) {
            return new ConfigBoolean(buf.get() == 1);
        }

        @Override
        public String toString() {
            return String.format("ConfigBoolean{value=%s}", value);
        }
    }

    private static class ConfigString {
        private final String value;
        private final int maxLength;

        public ConfigString(final String value, final int maxLength) {
            this.value = value;
            this.maxLength = maxLength;
        }

        public String getValue() {
            return value;
        }

        public int getMaxLength() {
            return maxLength;
        }

        private static ConfigString consume(final ByteBuffer buf, final boolean includesConstraints) {
            final String value = StringUtils.untilNullTerminator(buf);
            if (value == null) {
                LOG.error("Null terminator not found in buffer");
                return null;
            }

            if (!includesConstraints) {
                return new ConfigString(value, -1);
            }

            final int maxLength = buf.get() & 0xff;

            return new ConfigString(value, maxLength);
        }

        @Override
        public String toString() {
            return String.format("ConfigString{value=%s}", value);
        }
    }

    private static class ConfigStringList {
        private final String value;
        private final List<String> possibleValues;

        public ConfigStringList(final String value, final List<String> possibleValues) {
            this.value = value;
            this.possibleValues = possibleValues;
        }

        public String getValue() {
            return value;
        }

        public List<String> getPossibleValues() {
            return possibleValues;
        }

        private static ConfigStringList consume(final ByteBuffer buf, final boolean includesConstraints) {
            final String value = StringUtils.untilNullTerminator(buf);
            if (value == null) {
                LOG.error("Null terminator not found in buffer");
                return null;
            }

            final List<String> possibleValues = new ArrayList<>();
            if (includesConstraints) {
                final int unknown1 = buf.get() & 0xff; // ?
                final int numPossibleValues = buf.get() & 0xff;

                for (int i = 0; i < numPossibleValues; i++) {
                    final String possibleValue = StringUtils.untilNullTerminator(buf);
                    possibleValues.add(possibleValue);
                }
            }

            return new ConfigStringList(value, possibleValues);
        }

        @Override
        public String toString() {
            return String.format("ConfigStringList{value=%s, possibleValues=%s}", value, possibleValues);
        }
    }

    private static class ConfigShort {
        private final short value;
        private final short min;
        private final short max;
        private final boolean minMaxKnown;

        public ConfigShort(final short value) {
            this.value = value;
            this.min = this.max = 0;
            minMaxKnown = false;
        }

        public ConfigShort(final short value, final short min, final short max) {
            this.value = value;
            this.min = min;
            this.max = max;
            this.minMaxKnown = true;
        }

        public short getValue() {
            return value;
        }

        public short getMin() {
            return min;
        }

        public short getMax() {
            return max;
        }

        public boolean isMinMaxKnown() {
            return minMaxKnown;
        }

        private static ConfigShort consume(final ByteBuffer buf, final boolean includesConstraints) {
            final short value = buf.getShort();

            if (!includesConstraints) {
                return new ConfigShort(value);
            }

            final short min = buf.getShort();
            final short max = buf.getShort();

            return new ConfigShort(value, min, max);
        }

        @Override
        public String toString() {
            if (isMinMaxKnown()) {
                return String.format(Locale.ROOT, "ConfigShort{value=%d, min=%d, max=%d}", value, min, max);
            } else {
                return String.format(Locale.ROOT, "ConfigShort{value=%d}", value);
            }
        }
    }

    private static class ConfigInt {
        private final int value;
        private final int min;
        private final int max;
        private final boolean minMaxKnown;

        public ConfigInt(final int value) {
            this.value = value;
            this.min = this.max = 0;
            minMaxKnown = false;
        }

        public ConfigInt(final int value, final int min, final int max) {
            this.value = value;
            this.min = min;
            this.max = max;
            this.minMaxKnown = true;
        }

        public int getValue() {
            return value;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public boolean isMinMaxKnown() {
            return minMaxKnown;
        }

        private static ConfigInt consume(final ByteBuffer buf, final boolean includesConstraints) {
            final int value = buf.getInt();

            if (!includesConstraints) {
                return new ConfigInt(value);
            }

            final int min = buf.getInt();
            final int max = buf.getInt();

            return new ConfigInt(value, min, max);
        }

        @Override
        public String toString() {
            if (isMinMaxKnown()) {
                return String.format(Locale.ROOT, "ConfigInt{value=%d, min=%d, max=%d}", value, min, max);
            } else {
                return String.format(Locale.ROOT, "ConfigInt{value=%d}", value);
            }
        }
    }

    private static class ConfigByte {
        private final byte value;
        private final byte[] possibleValues;

        public ConfigByte(final byte value, final byte[] possibleValues) {
            this.value = value;
            this.possibleValues = possibleValues;
        }

        public byte getValue() {
            return value;
        }

        public byte[] getPossibleValues() {
            return possibleValues;
        }

        private static ConfigByte consume(final ByteBuffer buf, final boolean includesConstraints) {
            final byte value = buf.get();

            if (includesConstraints) {
                final int numPossibleValues = buf.get() & 0xff;
                final byte[] possibleValues = new byte[numPossibleValues];

                for (int i = 0; i < numPossibleValues; i++) {
                    possibleValues[i] = buf.get();
                }

                return new ConfigByte(value, possibleValues);
            }

            return new ConfigByte(value, new byte[0]);
        }

        @Override
        public String toString() {
            return String.format("ConfigByte{value=0x%02x, possibleValues=%s}", value, GB.hexdump(possibleValues));
        }
    }

    private static class ConfigByteList {
        private final byte[] values;
        private final byte[] possibleValues;

        public ConfigByteList(final byte[] values, final byte[] possibleValues) {
            this.values = values;
            this.possibleValues = possibleValues;
        }

        public byte[] getValues() {
            return values;
        }

        @Nullable
        public byte[] getPossibleValues() {
            return possibleValues;
        }

        private static ConfigByteList consume(final ByteBuffer buf, final boolean includesConstraints) {
            final int numValues = buf.get() & 0xff;
            final byte[] values = new byte[numValues];
            for (int i = 0; i < numValues; i++) {
                values[i] = buf.get();
            }

            if (includesConstraints) {
                final int numPossibleValues = buf.get() & 0xff;
                final byte[] possibleValues = new byte[numPossibleValues];

                for (int i = 0; i < numPossibleValues; i++) {
                    possibleValues[i] = buf.get();
                }

                return new ConfigByteList(values, possibleValues);
            }

            return new ConfigByteList(values, null);
        }

        @Override
        public String toString() {
            if (possibleValues != null) {
                return String.format("ConfigByteList{values=%s, possibleValues=%s}", GB.hexdump(values), GB.hexdump(possibleValues));
            } else {
                return String.format("ConfigByteList{values=%s}", GB.hexdump(values));
            }
        }
    }

    private static class ConfigDatetimeHhMm {
        final String value;

        public ConfigDatetimeHhMm(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        private static ConfigDatetimeHhMm consume(final ByteBuffer buf) {
            final DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            final String hhmm = String.format(Locale.ROOT, "%02d:%02d", buf.get(), buf.get());
            try {
                df.parse(hhmm);
            } catch (final ParseException e) {
                LOG.error("Failed to parse HH:mm from {}", hhmm);
                return null;
            }
            return new ConfigDatetimeHhMm(hhmm);
        }

        @Override
        public String toString() {
            return String.format("ConfigDatetimeHhMm{value=%s}", value);
        }
    }

    private interface ValueDecoder<T> {
        String decode(T val);
    }

    public static String languageByteToLocale(final byte code) {
        final Map<Integer, String> localeLookup = MapUtils.reverse(HuamiLanguageType.idLookup);
        return localeLookup.get((int) code);
    }

    public static Byte languageLocaleToByte(final String locale) {
        if (HuamiLanguageType.idLookup.containsKey(locale)) {
            return (byte) (int) HuamiLanguageType.idLookup.get(locale);
        }

        return null;
    }

    public static String decodeHeartRateAllDayMonitoring(final byte b) {
        if (b > 0) {
            return String.format(Locale.ROOT, "%d", (b & 0xff) * 60);
        } else {
            return String.format(Locale.ROOT, "%d", b);
        }
    }

    public static byte encodeHeartRateAllDayMonitoring(final String val) {
        final int intVal = Integer.parseInt(val);
        if (intVal < 0) {
            return (byte) intVal;
        } else {
            return (byte) (intVal / 60);
        }
    }

    private static final Map<Byte, Enum<?>> ALWAYS_ON_DISPLAY_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, AlwaysOnDisplay.OFF);
        put((byte) 0x01, AlwaysOnDisplay.AUTOMATIC);
        put((byte) 0x02, AlwaysOnDisplay.SCHEDULED);
        put((byte) 0x03, AlwaysOnDisplay.ALWAYS);
    }};

    private static final Map<Byte, String> NIGHT_MODE_MAP = new HashMap<Byte, String>() {{
        put((byte) 0x00, MiBandConst.PREF_NIGHT_MODE_OFF);
        put((byte) 0x01, MiBandConst.PREF_NIGHT_MODE_SUNSET);
        put((byte) 0x02, MiBandConst.PREF_NIGHT_MODE_SCHEDULED);
    }};

    private static final Map<Byte, Enum<?>> DND_MODE_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, DoNotDisturb.OFF);
        put((byte) 0x01, DoNotDisturb.SCHEDULED);
        put((byte) 0x02, DoNotDisturb.AUTOMATIC);
        put((byte) 0x03, DoNotDisturb.ALWAYS);
    }};

    private static final Map<Byte, Enum<?>> TEMPERATURE_UNIT_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, MiBandConst.DistanceUnit.METRIC);
        put((byte) 0x01, MiBandConst.DistanceUnit.IMPERIAL);
    }};

    private static final Map<Byte, String> TIME_FORMAT_MAP = new HashMap<Byte, String>() {{
        put((byte) 0x00, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H);
        put((byte) 0x01, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H);
    }};

    private static final Map<Byte, Enum<?>> LIFT_WRIST_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, ActivateDisplayOnLift.OFF);
        put((byte) 0x01, ActivateDisplayOnLift.SCHEDULED);
        put((byte) 0x02, ActivateDisplayOnLift.ON);
    }};

    private static final Map<Byte, Enum<?>> LIFT_WRIST_SENSITIVITY_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, ActivateDisplayOnLiftSensitivity.NORMAL);
        put((byte) 0x01, ActivateDisplayOnLiftSensitivity.SENSITIVE);
    }};

    private static final Map<Byte, String> WEARING_DIRECTION_MAP = new HashMap<Byte, String>() {{
        put((byte) 0x00, "buttons_on_left");
        put((byte) 0x01, "buttons_on_right");
    }};

    private static final Map<Byte, String> OFFLINE_VOICE_LANGUAGE_MAP = new HashMap<Byte, String>() {{
        put((byte) 0x01, "zh_CN"); // TODO confirm
        put((byte) 0x02, "en_US");
        put((byte) 0x03, "de_DE");
        put((byte) 0x04, "es_ES");
    }};

    private static final Map<Byte, Enum<?>> GPS_PRESET_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, GpsCapability.Preset.ACCURACY);
        put((byte) 0x01, GpsCapability.Preset.BALANCED);
        put((byte) 0x02, GpsCapability.Preset.POWER_SAVING);
        put((byte) 0x04, GpsCapability.Preset.CUSTOM);
    }};

    private static final Map<Byte, Enum<?>> GPS_BAND_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, GpsCapability.Band.SINGLE_BAND);
        put((byte) 0x01, GpsCapability.Band.DUAL_BAND);
    }};

    private static final Map<Byte, Enum<?>> GPS_COMBINATION_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, GpsCapability.Combination.LOW_POWER_GPS);
        put((byte) 0x01, GpsCapability.Combination.GPS);
        put((byte) 0x02, GpsCapability.Combination.GPS_BDS);
        put((byte) 0x03, GpsCapability.Combination.GPS_GNOLASS);
        put((byte) 0x04, GpsCapability.Combination.GPS_GALILEO);
        put((byte) 0x05, GpsCapability.Combination.ALL_SATELLITES);
    }};

    private static final Map<Byte, Enum<?>> GPS_SATELLITE_SEARCH_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, GpsCapability.SatelliteSearch.SPEED_FIRST);
        put((byte) 0x01, GpsCapability.SatelliteSearch.ACCURACY_FIRST);
    }};

    private static final Map<Byte, Enum<?>> WORKOUT_DETECTION_CATEGORY_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x03, WorkoutDetectionCapability.Category.WALKING);
        put((byte) 0x28, WorkoutDetectionCapability.Category.INDOOR_WALKING);
        put((byte) 0x01, WorkoutDetectionCapability.Category.OUTDOOR_RUNNING);
        put((byte) 0x02, WorkoutDetectionCapability.Category.TREADMILL);
        put((byte) 0x04, WorkoutDetectionCapability.Category.OUTDOOR_CYCLING);
        put((byte) 0x06, WorkoutDetectionCapability.Category.POOL_SWIMMING);
        put((byte) 0x09, WorkoutDetectionCapability.Category.ELLIPTICAL);
        put((byte) 0x17, WorkoutDetectionCapability.Category.ROWING_MACHINE);
    }};

    private static final Map<Byte, Enum<?>> WORKOUT_DETECTION_SENSITIVITY_MAP = new HashMap<Byte, Enum<?>>() {{
        put((byte) 0x00, WorkoutDetectionCapability.Sensitivity.HIGH);
        put((byte) 0x01, WorkoutDetectionCapability.Sensitivity.STANDARD);
        put((byte) 0x02, WorkoutDetectionCapability.Sensitivity.LOW);
    }};

    public static String decodeEnum(final Map<Byte, Enum<?>> map, final byte b) {
        if (map.containsKey(b)) {
            return map.get(b).name().toLowerCase(Locale.ROOT);
        }

        return null;
    }

    public static String decodeString(final Map<Byte, String> map, final byte b) {
        return map.get(b);
    }

    public static Byte encodeEnum(final Map<Byte, Enum<?>> map, final String val) {
        final Map<Enum<?>, Byte> reverse = MapUtils.reverse(map);
        for (final Enum<?> anEnum : reverse.keySet()) {
            if (anEnum.name().toLowerCase(Locale.ROOT).equals(val)) {
                return reverse.get(anEnum);
            }
        }

        return null;
    }

    public static Byte encodeString(final Map<Byte, String> map, final String val) {
        final Map<String, Byte> reverse = MapUtils.reverse(map);
        for (final String aString : reverse.keySet()) {
            if (aString.equals(val)) {
                return reverse.get(aString);
            }
        }

        return null;
    }
}
