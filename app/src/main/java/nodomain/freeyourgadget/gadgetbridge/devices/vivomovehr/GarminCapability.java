/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum GarminCapability {
    CONNECT_MOBILE_FIT_LINK,
    GOLF_FIT_LINK,
    VIVOKID_JR_FIT_LINK,
    SYNC,
    DEVICE_INITIATES_SYNC,
    HOST_INITIATED_SYNC_REQUESTS,
    GNCS,
    ADVANCED_MUSIC_CONTROLS,
    FIND_MY_PHONE,
    FIND_MY_WATCH,
    CONNECTIQ_HTTP,
    CONNECTIQ_SETTINGS,
    CONNECTIQ_WATCH_APP_DOWNLOAD,
    CONNECTIQ_WIDGET_DOWNLOAD,
    CONNECTIQ_WATCH_FACE_DOWNLOAD,
    CONNECTIQ_DATA_FIELD_DOWNLOAD,
    CONNECTIQ_APP_MANAGEMENT,
    COURSE_DOWNLOAD,
    WORKOUT_DOWNLOAD,
    GOLF_COURSE_DOWNLOAD,
    DELTA_SOFTWARE_UPDATE_FILES,
    FITPAY,
    LIVETRACK,
    LIVETRACK_AUTO_START,
    LIVETRACK_MESSAGING,
    GROUP_LIVETRACK,
    WEATHER_CONDITIONS,
    WEATHER_ALERTS,
    GPS_EPHEMERIS_DOWNLOAD,
    EXPLICIT_ARCHIVE,
    SWING_SENSOR,
    SWING_SENSOR_REMOTE,
    INCIDENT_DETECTION,
    TRUEUP,
    INSTANT_INPUT,
    SEGMENTS,
    AUDIO_PROMPT_LAP,
    AUDIO_PROMPT_PACE_SPEED,
    AUDIO_PROMPT_HEART_RATE,
    AUDIO_PROMPT_POWER,
    AUDIO_PROMPT_NAVIGATION,
    AUDIO_PROMPT_CADENCE,
    SPORT_GENERIC,
    SPORT_RUNNING,
    SPORT_CYCLING,
    SPORT_TRANSITION,
    SPORT_FITNESS_EQUIPMENT,
    SPORT_SWIMMING,
    STOP_SYNC_AFTER_SOFTWARE_UPDATE,
    CALENDAR,
    WIFI_SETUP,
    SMS_NOTIFICATIONS,
    BASIC_MUSIC_CONTROLS,
    AUDIO_PROMPTS_SPEECH,
    DELTA_SOFTWARE_UPDATES,
    GARMIN_DEVICE_INFO_FILE_TYPE,
    SPORT_PROFILE_SETUP,
    HSA_SUPPORT,
    SPORT_STRENGTH,
    SPORT_CARDIO,
    UNION_PAY,
    IPASS,
    CIQ_AUDIO_CONTENT_PROVIDER,
    UNION_PAY_INTERNATIONAL,
    REQUEST_PAIR_FLOW,
    LOCATION_UPDATE,
    LTE_SUPPORT,
    DEVICE_DRIVEN_LIVETRACK_SUPPORT,
    CUSTOM_CANNED_TEXT_LIST_SUPPORT,
    EXPLORE_SYNC,
    INCIDENT_DETECT_AND_ASSISTANCE,
    CURRENT_TIME_REQUEST_SUPPORT,
    CONTACTS_SUPPORT,
    LAUNCH_REMOTE_CIQ_APP_SUPPORT,
    DEVICE_MESSAGES,
    WAYPOINT_TRANSFER,
    MULTI_LINK_SERVICE,
    OAUTH_CREDENTIALS,
    GOLF_9_PLUS_9,
    ANTI_THEFT_ALARM,
    INREACH,
    EVENT_SHARING;

    public static final Set<GarminCapability> ALL_CAPABILITIES = new HashSet<>(values().length);
    private static final Map<Integer, GarminCapability> FROM_ORDINAL = new HashMap<>(values().length);

    static {
        for (GarminCapability cap : values()) {
            FROM_ORDINAL.put(cap.ordinal(), cap);
            ALL_CAPABILITIES.add(cap);
        }
    }

    public static Set<GarminCapability> setFromBinary(byte[] bytes) {
        final Set<GarminCapability> result = new HashSet<>(GarminCapability.values().length);
        int current = 0;
        for (int b : bytes) {
            for (int curr = 1; curr < 0x100; curr <<= 1) {
                if ((b & curr) != 0) {
                    result.add(FROM_ORDINAL.get(current));
                }
                ++current;
            }
        }
        return result;
    }

    public static byte[] setToBinary(Set<GarminCapability> capabilities) {
        final GarminCapability[] values = values();
        final byte[] result = new byte[(values.length + 7) / 8];
        int bytePos = 0;
        int bitPos = 0;
        for (int i = 0; i < values.length; ++i) {
            if (capabilities.contains(FROM_ORDINAL.get(i))) {
                result[bytePos] |= (1 << bitPos);
            }
            ++bitPos;
            if (bitPos >= 8) {
                bitPos = 0;
                ++bytePos;
            }
        }
        return result;
    }

    public static String setToString(Set<GarminCapability> capabilities) {
        final StringBuilder result = new StringBuilder();
        for (GarminCapability cap : capabilities) {
            if (result.length() > 0) result.append(", ");
            result.append(cap.name());
        }
        return result.toString();
    }
}
