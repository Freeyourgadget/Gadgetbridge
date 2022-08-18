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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import static org.apache.commons.lang3.ArrayUtils.subarray;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALWAYS_ON_DISPLAY_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALWAYS_ON_DISPLAY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALWAYS_ON_DISPLAY_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BT_CONNECTED_ADVERTISEMENT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_SENSITIVITY;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_RELAXATION_REMINDER;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_END;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_START;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SCREEN_TIMEOUT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_PASSWORD;
import static nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_PASSWORD_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service.CHUNKED2021_ENDPOINT_CONFIG;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service.CONFIG_CMD_SET;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_EXPOSE_HR_THIRDPARTY;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_START;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLift;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLiftSensitivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.AlwaysOnDisplay;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DoNotDisturb;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class Huami2021Config {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021Config.class);

    public enum ConfigType {
        DISPLAY(0x01, 0x02),
        LOCKSCREEN(0x04, 0x01),
        LANGUAGE(0x07, 0x02),
        HEALTH(0x08, 0x02),
        SYSTEM(0x0a, 0x01),
        BLUETOOTH(0x0b, 0x01),
        ;

        private final byte value;
        private final byte nextByte; // FIXME what does this byte mean?

        ConfigType(int value, int nextByte) {
            this.value = (byte) value;
            this.nextByte = (byte) nextByte;
        }

        public byte getValue() {
            return value;
        }

        public byte getNextByte() {
            return nextByte;
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

    public enum ArgType {
        BOOL(0x0b),
        STRING(0x20),
        SHORT(0x01),
        INT(0x03),
        BYTE(0x10),
        DATETIME_HH_MM(0x30),
        ;

        private final byte value;

        ArgType(int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum ConfigArg {
        // Display
        SCREEN_BRIGHTNESS(ConfigType.DISPLAY, ArgType.SHORT, 0x02, PREF_SCREEN_BRIGHTNESS),
        SCREEN_TIMEOUT(ConfigType.DISPLAY, ArgType.BYTE, 0x03, PREF_SCREEN_TIMEOUT),
        ALWAYS_ON_DISPLAY_MODE(ConfigType.DISPLAY, ArgType.BYTE, 0x04, PREF_ALWAYS_ON_DISPLAY_MODE),
        ALWAYS_ON_DISPLAY_SCHEDULED_START(ConfigType.DISPLAY, ArgType.DATETIME_HH_MM, 0x05, PREF_ALWAYS_ON_DISPLAY_START),
        ALWAYS_ON_DISPLAY_SCHEDULED_END(ConfigType.DISPLAY, ArgType.DATETIME_HH_MM, 0x06, PREF_ALWAYS_ON_DISPLAY_END),
        LIFT_WRIST_MODE(ConfigType.DISPLAY, ArgType.BYTE, 0x08, PREF_ACTIVATE_DISPLAY_ON_LIFT),
        LIFT_WRIST_SCHEDULED_START(ConfigType.DISPLAY, ArgType.DATETIME_HH_MM, 0x09, PREF_DISPLAY_ON_LIFT_START),
        LIFT_WRIST_SCHEDULED_END(ConfigType.DISPLAY, ArgType.DATETIME_HH_MM, 0x0a, PREF_DISPLAY_ON_LIFT_END),
        LIFT_WRIST_RESPONSE_SENSITIVITY(ConfigType.DISPLAY, ArgType.BYTE, 0x0b, PREF_DISPLAY_ON_LIFT_SENSITIVITY),
        SCREEN_ON_ON_NOTIFICATIONS(ConfigType.DISPLAY, ArgType.BOOL, 0x0c, PREF_SCREEN_ON_ON_NOTIFICATIONS),

        // Lock Screen
        PASSWORD_ENABLED(ConfigType.LOCKSCREEN, ArgType.BOOL, 0x01, PREF_PASSWORD_ENABLED),
        PASSWORD_TEXT(ConfigType.LOCKSCREEN, ArgType.STRING, 0x02, PREF_PASSWORD),

        // Language
        LANGUAGE(ConfigType.LANGUAGE, ArgType.BYTE, 0x01, PREF_LANGUAGE),
        LANGUAGE_FOLLOW_PHONE(ConfigType.LANGUAGE, ArgType.BOOL, 0x02, null),

        // Health
        HEART_RATE_ALL_DAY_MONITORING(ConfigType.HEALTH, ArgType.BYTE, 0x01, PREF_HEARTRATE_MEASUREMENT_INTERVAL),
        HEART_RATE_HIGH_ALERTS(ConfigType.HEALTH, ArgType.BYTE, 0x02, PREF_HEARTRATE_ALERT_HIGH_THRESHOLD),
        HEART_RATE_LOW_ALERTS(ConfigType.HEALTH, ArgType.BYTE, 0x03, PREF_HEARTRATE_ALERT_LOW_THRESHOLD),
        THIRD_PARTY_HR_SHARING(ConfigType.HEALTH, ArgType.BOOL, 0x05, PREF_EXPOSE_HR_THIRDPARTY),
        SLEEP_HIGH_ACCURACY_MONITORING(ConfigType.HEALTH, ArgType.BOOL, 0x11, PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION),
        SLEEP_BREATHING_QUALITY_MONITORING(ConfigType.HEALTH, ArgType.BOOL, 0x12, PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING),
        STRESS_MONITORING(ConfigType.HEALTH, ArgType.BOOL, 0x13, PREF_HEARTRATE_STRESS_MONITORING),
        STRESS_RELAXATION_REMINDER(ConfigType.HEALTH, ArgType.BOOL, 0x14, PREF_HEARTRATE_STRESS_RELAXATION_REMINDER),
        SPO2_ALL_DAY_MONITORING(ConfigType.HEALTH, ArgType.BOOL, 0x31, PREF_SPO2_ALL_DAY_MONITORING),
        SPO2_LOW_ALERT(ConfigType.HEALTH, ArgType.BYTE, 0x32, PREF_SPO2_LOW_ALERT_THRESHOLD),
        FITNESS_GOAL_NOTIFICATION(ConfigType.HEALTH, ArgType.BOOL, 0x51, PREF_USER_FITNESS_GOAL_NOTIFICATION), // TODO is it?
        FITNESS_GOAL_STEPS(ConfigType.HEALTH, ArgType.INT, 0x52, null), // TODO needs to be handled globally
        INACTIVITY_WARNINGS_ENABLED(ConfigType.HEALTH, ArgType.BOOL, 0x41, PREF_INACTIVITY_ENABLE),
        INACTIVITY_WARNINGS_SCHEDULED_START(ConfigType.HEALTH, ArgType.DATETIME_HH_MM, 0x42, PREF_INACTIVITY_START),
        INACTIVITY_WARNINGS_SCHEDULED_END(ConfigType.HEALTH, ArgType.DATETIME_HH_MM, 0x43, PREF_INACTIVITY_END),
        INACTIVITY_WARNINGS_DND_ENABLED(ConfigType.HEALTH, ArgType.BOOL, 0x44, PREF_INACTIVITY_DND),
        INACTIVITY_WARNINGS_DND_SCHEDULED_START(ConfigType.HEALTH, ArgType.DATETIME_HH_MM, 0x45, PREF_INACTIVITY_DND_START),
        INACTIVITY_WARNINGS_DND_SCHEDULED_END(ConfigType.HEALTH, ArgType.DATETIME_HH_MM, 0x46, PREF_INACTIVITY_DND_END),

        // System
        TIME_FORMAT(ConfigType.SYSTEM, ArgType.BYTE, 0x01, PREF_TIMEFORMAT),
        DATE_FORMAT(ConfigType.SYSTEM, ArgType.STRING, 0x02, null),
        DND_MODE(ConfigType.SYSTEM, ArgType.BYTE, 0x0a, PREF_DO_NOT_DISTURB),
        DND_SCHEDULED_START(ConfigType.SYSTEM, ArgType.DATETIME_HH_MM, 0x0b, PREF_DO_NOT_DISTURB_START),
        DND_SCHEDULED_END(ConfigType.SYSTEM, ArgType.DATETIME_HH_MM, 0x0c, PREF_DO_NOT_DISTURB_END),
        TEMPERATURE_UNIT(ConfigType.SYSTEM, ArgType.BYTE, 0x12, null),
        TIME_FORMAT_FOLLOWS_PHONE(ConfigType.SYSTEM, ArgType.BOOL, 0x13, null),
        DISPLAY_CALLER(ConfigType.SYSTEM, ArgType.BOOL, 0x18, null), // TODO Handle
        NIGHT_MODE_MODE(ConfigType.SYSTEM, ArgType.BYTE, 0x1b, PREF_NIGHT_MODE),
        NIGHT_MODE_SCHEDULED_START(ConfigType.SYSTEM, ArgType.DATETIME_HH_MM, 0x1c, PREF_NIGHT_MODE_START),
        NIGHT_MODE_SCHEDULED_END(ConfigType.SYSTEM, ArgType.DATETIME_HH_MM, 0x1d, PREF_NIGHT_MODE_END),

        // Bluetooth
        BLUETOOTH_CONNECTED_ADVERTISING(ConfigType.BLUETOOTH, ArgType.BOOL, 0x02, PREF_BT_CONNECTED_ADVERTISEMENT),
        ;

        private final ConfigType configType;
        private final ArgType argType;
        private final byte code;
        private final String prefKey;

        ConfigArg(final ConfigType configType, final ArgType argType, final int code, final String prefKey) {
            this.configType = configType;
            this.argType = argType;
            this.code = (byte) code;
            this.prefKey = prefKey;
        }

        public ConfigType getConfigType() {
            return configType;
        }

        public ArgType getArgType() {
            return argType;
        }

        public byte getCode() {
            return code;
        }

        public String getPrefKey() {
            return prefKey;
        }

        public static ConfigArg fromCode(final ConfigType configType, final byte code) {
            for (final Huami2021Config.ConfigArg arg : values()) {
                if (arg.getConfigType().equals(configType) && arg.getCode() == code) {
                    return arg;
                }
            }
            return null;
        }

        public static List<ConfigArg> getAllArgsForConfigType(final ConfigType configType) {
            final List<Huami2021Config.ConfigArg> configArgs = new ArrayList<>();
            for (final Huami2021Config.ConfigArg arg : values()) {
                if (arg.getConfigType().equals(configType)) {
                    configArgs.add(arg);
                }
            }
            return configArgs;
        }
    }

    public static class ConfigSetter {
        private final ConfigType configType;
        private final Map<ConfigArg, byte[]> arguments = new LinkedHashMap<>();

        public ConfigSetter(final ConfigType configType) {
            this.configType = configType;
        }

        public ConfigSetter setBoolean(final ConfigArg arg, final boolean value) {
            checkArg(arg, ArgType.BOOL);

            arguments.put(arg, new byte[]{(byte) (value ? 0x01 : 0x00)});

            return this;
        }

        public ConfigSetter setString(final ConfigArg arg, final String value) {
            checkArg(arg, ArgType.STRING);

            arguments.put(arg, (value + "\0").getBytes(StandardCharsets.UTF_8));

            return this;
        }

        public ConfigSetter setShort(final ConfigArg arg, final short value) {
            checkArg(arg, ArgType.SHORT);

            arguments.put(arg, BLETypeConversions.fromUint16(value));

            return this;
        }

        public ConfigSetter setInt(final ConfigArg arg, final int value) {
            checkArg(arg, ArgType.INT);

            arguments.put(arg, BLETypeConversions.fromUint32(value));

            return this;
        }

        public ConfigSetter setByte(final ConfigArg arg, final byte value) {
            checkArg(arg, ArgType.BYTE);

            arguments.put(arg, new byte[]{value});

            return this;
        }

        public ConfigSetter setHourMinute(final ConfigArg arg, final Date date) {
            checkArg(arg, ArgType.DATETIME_HH_MM);

            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(date);

            arguments.put(arg, new byte[]{
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE)
            });

            return this;
        }

        public byte[] encode() {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                baos.write(CONFIG_CMD_SET);
                baos.write(configType.getValue());
                baos.write(configType.getNextByte());
                baos.write(0x00); // ?
                baos.write(arguments.size());
                for (final Map.Entry<ConfigArg, byte[]> arg : arguments.entrySet()) {
                    final ArgType argType = arg.getKey().getArgType();
                    baos.write(arg.getKey().getCode());
                    baos.write(argType.getValue());
                    baos.write(arg.getValue());
                }
            } catch (final IOException e) {
                LOG.error("Failed to encode command", e);
            }

            return baos.toByteArray();
        }

        public void write(final Huami2021Support support, final TransactionBuilder builder) {
            support.writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CONFIG, encode(), true);
        }

        public void write(final Huami2021Support support) {
            try {
                final TransactionBuilder builder = support.performInitialized("write config");
                support.writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CONFIG, encode(), true);
                builder.queue(support.getQueue());
            } catch (final Exception e) {
                LOG.error("Failed to write config", e);
            }
        }

        private void checkArg(final ConfigArg arg, final ArgType expectedArgType) {
            try {
                if (!configType.equals(arg.getConfigType())) {
                    throw new IllegalArgumentException("Unexpected config type " + arg.getConfigType());
                }

                if (!expectedArgType.equals(arg.getArgType())) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Invalid arg type %s for %s, expected %s",
                                    expectedArgType,
                                    arg,
                                    arg.getArgType()
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

    public static class ConfigParser {
        private static final Logger LOG = LoggerFactory.getLogger(ConfigParser.class);

        private final ConfigType configType;

        public ConfigParser(final ConfigType configType) {
            this.configType = configType;
        }

        public Map<String, Object> parse(final int expectedNumConfigs, final byte[] bytes) {
            final Map<String, Object> prefs = new HashMap<>();

            int configCount = 0;
            int pos = 0;

            while (pos < bytes.length) {
                if (configCount > expectedNumConfigs) {
                    LOG.error("Got more configs than {}", expectedNumConfigs);
                    return null;
                }

                final Huami2021Config.ConfigArg configArg = Huami2021Config.ConfigArg.fromCode(configType, bytes[pos]);
                if (configArg == null) {
                    LOG.error("Unknown config {} for {} at {}", String.format("0x%02x", bytes[pos]), configType, pos);
                    return null;
                }

                pos++;

                final boolean unexpectedType = (bytes[pos] != configArg.getArgType().getValue());
                if (unexpectedType) {
                    LOG.warn("Unexpected arg type {} for {}, expected {}", String.format("0x%02x", bytes[pos]), configArg, configArg.getArgType());
                }

                pos++;

                final Map<String, Object> argPrefs;

                switch (configArg.getArgType()) {
                    case BOOL:
                        final boolean valBoolean = bytes[pos] == 1;
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, valBoolean);
                        argPrefs = convertBooleanToPrefs(configArg, valBoolean);
                        pos += 1;
                        break;
                    case STRING:
                        final String valString = StringUtils.untilNullTerminator(bytes, pos);
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, valString);
                        argPrefs = convertStringToPrefs(configArg, valString);
                        pos += valString.length() + 1;
                        break;
                    case SHORT:
                        final int valShort = BLETypeConversions.toUint16(subarray(bytes, pos, pos + 2));
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, valShort);
                        argPrefs = convertNumberToPrefs(configArg, valShort);
                        pos += 2;
                        break;
                    case INT:
                        final int valInt = BLETypeConversions.toUint32(subarray(bytes, pos, pos + 4));
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, valInt);
                        argPrefs = convertNumberToPrefs(configArg, valInt);
                        pos += 4;
                        break;
                    case BYTE:
                        final byte valByte = bytes[pos];
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, valByte);
                        argPrefs = convertByteToPrefs(configArg, valByte);
                        pos += 1;
                        break;
                    case DATETIME_HH_MM:
                        final DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        final String hhmm = String.format(Locale.ROOT, "%02d:%02d", bytes[pos], bytes[pos + 1]);
                        try {
                            df.parse(hhmm);
                        } catch (final ParseException e) {
                            LOG.error("Failed to parse HH:mm from {}", hhmm);
                            return null;
                        }
                        LOG.info("Got {} for {} = {}", configArg.getArgType(), configArg, hhmm);
                        argPrefs = convertDatetimeHhMmToPrefs(configArg, hhmm);
                        pos += 2;
                        break;
                    default:
                        LOG.error("Unknown arg type {}", configArg);
                        configCount++;
                        continue;
                }

                if (argPrefs != null && !unexpectedType) {
                    // Special cases for "follow phone" preferences. We need to ensure that "auto"
                    // always has precedence
                    if (argPrefs.containsKey(PREF_LANGUAGE) && prefs.containsKey(PREF_LANGUAGE)) {
                        if (prefs.get(PREF_LANGUAGE).equals(DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO)) {
                            argPrefs.remove(PREF_LANGUAGE);
                        }
                    }
                    if (argPrefs.containsKey(PREF_TIMEFORMAT) && prefs.containsKey(PREF_TIMEFORMAT)) {
                        if (prefs.get(PREF_TIMEFORMAT).equals(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO)) {
                            argPrefs.remove(PREF_TIMEFORMAT);
                        }
                    }

                    prefs.putAll(argPrefs);
                }

                configCount++;
            }

            return prefs;
        }

        private static Map<String, Object> convertBooleanToPrefs(final ConfigArg configArg, final boolean value) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a boolean pref directly
                return singletonMap(configArg.getPrefKey(), value);
            }

            switch(configArg) {
                case LANGUAGE_FOLLOW_PHONE:
                    if (value) {
                        return singletonMap(PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO);
                    } else {
                        // If not following phone, we'll receive the actual value in LANGUAGE
                        return Collections.emptyMap();
                    }
                case TIME_FORMAT_FOLLOWS_PHONE:
                    if (value) {
                        return singletonMap(PREF_TIMEFORMAT, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO);
                    } else {
                        // If not following phone, we'll receive the actual value in TIME_FORMAT
                        return Collections.emptyMap();
                    }
                default:
                    break;
            }

            LOG.warn("Unhandled Boolean pref {}", configArg);
            return null;
        }

        private static Map<String, Object> convertStringToPrefs(final ConfigArg configArg, final String str) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a string pref directly
                return singletonMap(configArg.getPrefKey(), str);
            }

            switch(configArg) {
                case DATE_FORMAT:
                    return singletonMap(PREF_DATEFORMAT, str.replace(".", "/").toUpperCase(Locale.ROOT));
                default:
                    break;
            }

            LOG.warn("Unhandled String pref {}", configArg);
            return null;
        }

        private static Map<String, Object> convertNumberToPrefs(final ConfigArg configArg, final int value) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a number pref directly
                return singletonMap(configArg.getPrefKey(), value);
            }

            LOG.warn("Unhandled number pref {}", configArg);
            return null;
        }

        private static Map<String, Object> convertDatetimeHhMmToPrefs(final ConfigArg configArg, final String hhmm) {
            if (configArg.getPrefKey() != null) {
                // The arg maps to a hhmm pref directly
                return singletonMap(configArg.getPrefKey(), hhmm);
            }

            LOG.warn("Unhandled datetime pref {}", configArg);
            return null;
        }

        private static Map<String, Object> convertByteToPrefs(final ConfigArg configArg, final byte b) {
            switch(configArg) {
                case ALWAYS_ON_DISPLAY_MODE:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), AlwaysOnDisplay.OFF.name().toLowerCase(Locale.ROOT));
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), AlwaysOnDisplay.AUTO.name().toLowerCase(Locale.ROOT));
                        case 0x02:
                            return singletonMap(configArg.getPrefKey(), AlwaysOnDisplay.SCHEDULED.name().toLowerCase(Locale.ROOT));
                        case 0x03:
                            return singletonMap(configArg.getPrefKey(), AlwaysOnDisplay.ALWAYS.name().toLowerCase(Locale.ROOT));
                    }
                    break;
                case LIFT_WRIST_MODE:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), ActivateDisplayOnLift.OFF.name().toLowerCase(Locale.ROOT));
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), ActivateDisplayOnLift.SCHEDULED.name().toLowerCase(Locale.ROOT));
                        case 0x02:
                            return singletonMap(configArg.getPrefKey(), ActivateDisplayOnLift.ON.name().toLowerCase(Locale.ROOT));
                    }
                    break;
                case LIFT_WRIST_RESPONSE_SENSITIVITY:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), ActivateDisplayOnLiftSensitivity.NORMAL.name().toLowerCase(Locale.ROOT));
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), ActivateDisplayOnLiftSensitivity.SENSITIVE.name().toLowerCase(Locale.ROOT));
                    }
                    break;
                case LANGUAGE:
                    final Map<Integer, String> reverseLanguageLookup = MapUtils.reverse(HuamiLanguageType.idLookup);
                    final String language = reverseLanguageLookup.get(b & 0xff);
                    if (language != null) {
                        return singletonMap(configArg.getPrefKey(), language);
                    }
                    break;
                case HEART_RATE_ALL_DAY_MONITORING:
                    if (b > 0) {
                        return singletonMap(configArg.getPrefKey(), String.format("%d", (b & 0xff) * 60));
                    } else {
                        return singletonMap(configArg.getPrefKey(), String.format("%d", b));
                    }
                case SCREEN_TIMEOUT:
                case HEART_RATE_HIGH_ALERTS:
                case HEART_RATE_LOW_ALERTS:
                case SPO2_LOW_ALERT:
                    return singletonMap(configArg.getPrefKey(), String.format("%d", b & 0xff));
                case TIME_FORMAT:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H);
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H);
                    }
                    break;
                case DND_MODE:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), DoNotDisturb.OFF.name().toLowerCase(Locale.ROOT));
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), DoNotDisturb.SCHEDULED.name().toLowerCase(Locale.ROOT));
                        case 0x02:
                            return singletonMap(configArg.getPrefKey(), DoNotDisturb.AUTOMATIC.name().toLowerCase(Locale.ROOT));
                        case 0x03:
                            return singletonMap(configArg.getPrefKey(), DoNotDisturb.ALWAYS.name().toLowerCase(Locale.ROOT));
                    }
                    break;
                case TEMPERATURE_UNIT:
                    // TODO: This should be per device...
                    //switch(b) {
                    //    case 0x00:
                    //        return singletonMap(SettingsActivity.PREF_MEASUREMENT_SYSTEM, METRIC);
                    //    case 0x01:
                    //        return singletonMap(SettingsActivity.PREF_MEASUREMENT_SYSTEM, IMPERIAL);
                    //}
                    break;
                case NIGHT_MODE_MODE:
                    switch(b) {
                        case 0x00:
                            return singletonMap(configArg.getPrefKey(), MiBandConst.PREF_NIGHT_MODE_OFF);
                        case 0x01:
                            return singletonMap(configArg.getPrefKey(), MiBandConst.PREF_NIGHT_MODE_SUNSET);
                        case 0x02:
                            return singletonMap(configArg.getPrefKey(), MiBandConst.PREF_NIGHT_MODE_SCHEDULED);
                    }
                    break;
                default:
                    break;
            }

            LOG.warn("Unhandled byte pref {}", configArg);
            return null;
        }

        private static Map<String, Object> singletonMap(final String key, final Object value) {
            if (key == null && BuildConfig.DEBUG) {
                // Crash
                throw new IllegalStateException("Null key in prefs update");
            }

            return Collections.singletonMap(key, value);
        }
    }
}
