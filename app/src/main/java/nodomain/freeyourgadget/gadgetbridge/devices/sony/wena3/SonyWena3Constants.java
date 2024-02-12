/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import android.graphics.Color;

import java.util.UUID;

public class SonyWena3Constants {
    public static final String BT_DEVICE_NAME = "WNW-21A";
    private static final String uuidTemplate = "4EFD%s-A6C1-16F0-062F-F196CF496695";

    public static final UUID COMMON_SERVICE_UUID = UUID.fromString(String.format(uuidTemplate, "1501"));
    public static final UUID COMMON_SERVICE_CHARACTERISTIC_MODE_UUID = UUID.fromString(String.format(uuidTemplate, "1503"));
    public static final UUID COMMON_SERVICE_CHARACTERISTIC_CONTROL_UUID = UUID.fromString(String.format(uuidTemplate, "1514"));
    public static final UUID COMMON_SERVICE_CHARACTERISTIC_INFO_UUID = UUID.fromString(String.format(uuidTemplate, "1520"));
    public static final UUID COMMON_SERVICE_CHARACTERISTIC_STATE_UUID = UUID.fromString(String.format(uuidTemplate, "1521"));

    public static final UUID NOTIFICATION_SERVICE_UUID = UUID.fromString(String.format(uuidTemplate, "4001"));
    public static final UUID NOTIFICATION_SERVICE_CHARACTERISTIC_UUID = UUID.fromString(String.format(uuidTemplate, "4002"));

    public static final UUID ACTIVITY_LOG_SERVICE_UUID = UUID.fromString(String.format(uuidTemplate, "3001"));
    public static final UUID ACTIVITY_LOG_CHARACTERISTIC_UUID = UUID.fromString(String.format(uuidTemplate, "3002"));


    public static int[] LED_PRESETS = {
            Color.rgb(255, 0, 0),
            Color.rgb(255, 255, 0),
            Color.rgb(0, 255, 0),
            Color.rgb(0, 255, 255),
            Color.rgb(0, 0, 255),
            Color.rgb(255, 0, 255),
            Color.rgb(255, 255, 255)
    };

    public static final long EPOCH_START = 1577836800000L;

    public static final int ALARM_SLOTS = 9;
    public static final int ALARM_DEFAULT_SMART_WAKEUP_MARGIN_MINUTES = 10;

    public static final String DB_FIELD_VO2_DATAPOINT_NUMBER = "datapoint";
    public static final String DB_FIELD_VO2_VALUE = "vo2";
    public static final String DB_FIELD_STRESS = "stress";
    public static final String DB_FIELD_STEPS = "steps";
    public static final String DB_FIELD_HEART_RATE = "heartRate";
    public static final String DB_FIELD_ENERGY = "energy";
    public static final String DB_FIELD_CALORIES = "calories";

}

