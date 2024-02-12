/*  Copyright (C) 2020-2024 Andreas Shimokawa, FintasticMan, ITCactus,
    Patric Gruber, Stephan Lachnit, Taavi Eomäe, uli

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import java.util.UUID;

public class PineTimeJFConstants {
    public static final UUID UUID_SERVICE_MUSIC_CONTROL = UUID.fromString("00000000-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_CHARACTERISTICS_MUSIC_EVENT = UUID.fromString("00000001-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_STATUS = UUID.fromString("00000002-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_ARTIST = UUID.fromString("00000003-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_TRACK = UUID.fromString("00000004-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_ALBUM = UUID.fromString("00000005-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_POSITION = UUID.fromString("00000006-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_LENGTH_TOTAL = UUID.fromString("00000007-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_TRACK_NUMBER = UUID.fromString("00000008-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_TRACK_TOTAL = UUID.fromString("00000009-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_PLAYBACK_SPEED = UUID.fromString("0000000a-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_REPEAT = UUID.fromString("0000000b-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_MUSIC_SHUFFLE = UUID.fromString("0000000c-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_SERVICE_NAVIGATION = UUID.fromString("00010000-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_CHARACTERISTICS_NAVIGATION_FLAGS = UUID.fromString("00010001-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_NAVIGATION_NARRATIVE = UUID.fromString("00010002-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_NAVIGATION_MAN_DISTANCE = UUID.fromString("00010003-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTICS_NAVIGATION_PROGRESS = UUID.fromString("00010004-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT = UUID.fromString("00020001-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_SERVICE_WEATHER = UUID.fromString("00040000-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTIC_WEATHER_DATA = UUID.fromString("00040001-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTIC_WEATHER_CONTROL = UUID.fromString("00040002-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_SERVICE_SIMPLE_WEATHER = UUID.fromString("00050000-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTIC_SIMPLE_WEATHER_DATA = UUID.fromString("00050001-78fc-48fe-8e23-433b3a1942d0");

    // since 1.7. https://github.com/InfiniTimeOrg/InfiniTime/blob/develop/doc/MotionService.md
    public static final UUID UUID_SERVICE_MOTION = UUID.fromString("00030000-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTIC_MOTION_STEP_COUNT = UUID.fromString("00030001-78fc-48fe-8e23-433b3a1942d0");
    public static final UUID UUID_CHARACTERISTIC_MOTION_RAW_XYZ_VALUES = UUID.fromString("00030002-78fc-48fe-8e23-433b3a1942d0");

    // since https://github.com/InfiniTimeOrg/InfiniTime/pull/1454
    public static final UUID UUID_CHARACTERISTIC_WORLD_TIME = UUID.fromString("00050001-78fc-48fe-8e23-433b3a1942d0");

    public static final UUID UUID_SERVICE_HEART_RATE = GattService.UUID_SERVICE_HEART_RATE;
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

}
