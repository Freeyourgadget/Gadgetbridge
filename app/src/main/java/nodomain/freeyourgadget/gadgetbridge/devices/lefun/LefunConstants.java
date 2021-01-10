/*  Copyright (C) 2017-2021 Andreas Shimokawa, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

/**
 * Constants used with Lefun device support
 */
public class LefunConstants {
    // BLE UUIDs
    public static final UUID UUID_SERVICE_LEFUN = UUID.fromString(String.format(BASE_UUID, "18D0"));
    public static final UUID UUID_CHARACTERISTIC_LEFUN_WRITE = UUID.fromString(String.format(BASE_UUID, "2D01"));
    public static final UUID UUID_CHARACTERISTIC_LEFUN_NOTIFY = UUID.fromString(String.format(BASE_UUID, "2D00"));

    // Coordinator constants
    public static final String ADVERTISEMENT_NAME = "Lefun";
    public static final String MANUFACTURER_NAME = "Teng Jin Da";
    // Commands
    public static final byte CMD_REQUEST_ID = (byte) 0xab;
    public static final byte CMD_RESPONSE_ID = 0x5a;
    public static final int CMD_MAX_LENGTH = 20;
    // 3 header bytes plus checksum
    public static final int CMD_HEADER_LENGTH = 4;
    public static final byte CMD_FIRMWARE_INFO = 0x00;
    public static final byte CMD_BONDING_REQUEST = 0x01;
    public static final byte CMD_SETTINGS = 0x02;
    public static final byte CMD_BATTERY_LEVEL = 0x03;
    public static final byte CMD_TIME = 0x04;
    public static final byte CMD_ALARM = 0x05;
    public static final byte CMD_PROFILE = 0x06;
    public static final byte CMD_UI_PAGES = 0x07;
    public static final byte CMD_FEATURES = 0x08;
    public static final byte CMD_FIND_DEVICE = 0x09;
    public static final byte CMD_FIND_PHONE = 0x0a;
    public static final byte CMD_SEDENTARY_REMINDER_INTERVAL = 0x0b;
    public static final byte CMD_HYDRATION_REMINDER_INTERVAL = 0x0c;
    public static final byte CMD_REMOTE_CAMERA = 0x0d;
    public static final byte CMD_REMOTE_CAMERA_TRIGGERED = 0x0e;
    public static final byte CMD_PPG_START = 0x0f;
    public static final byte CMD_PPG_RESULT = 0x10;
    public static final byte CMD_PPG_DATA = 0x11;
    public static final byte CMD_STEPS_DATA = 0x12;
    public static final byte CMD_ACTIVITY_DATA = 0x13;
    public static final byte CMD_SLEEP_TIME_DATA = 0x14;
    public static final byte CMD_SLEEP_DATA = 0x15;
    public static final byte CMD_NOTIFICATION = 0x17;
    public static final byte CMD_LANGUAGE = 0x21;
    public static final byte CMD_UNKNOWN_22 = 0x22;
    public static final byte CMD_UNKNOWN_25 = 0x25;
    public static final byte CMD_UNKNOWN_80 = (byte) 0x80;
    public static final int PPG_TYPE_INVALID = -1;
    public static final int PPG_TYPE_HEART_RATE = 0;
    public static final int PPG_TYPE_BLOOD_PRESSURE = 1;
    public static final int PPG_TYPE_BLOOD_OXYGEN = 2;
    public static final int PPG_TYPE_COUNT = 3;
    // DB activity kinds
    public static final int DB_ACTIVITY_KIND_UNKNOWN = 0;
    public static final int DB_ACTIVITY_KIND_ACTIVITY = 1;
    public static final int DB_ACTIVITY_KIND_HEART_RATE = 2;
    public static final int DB_ACTIVITY_KIND_LIGHT_SLEEP = 3;
    public static final int DB_ACTIVITY_KIND_DEEP_SLEEP = 4;
    // Pseudo-intensity
    public static final int INTENSITY_MIN = 0;
    public static final int INTENSITY_DEEP_SLEEP = 1;
    public static final int INTENSITY_LIGHT_SLEEP = 2;
    public static final int INTENSITY_AWAKE = 3;
    public static final int INTENSITY_MAX = 4;
    public static int NUM_ALARM_SLOTS = 5;
}
