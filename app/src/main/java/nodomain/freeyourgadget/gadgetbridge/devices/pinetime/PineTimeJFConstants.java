/*  Copyright (C) 2020-2021 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

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

    public static final UUID UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT = UUID.fromString("00020001-78fc-48fe-8e23-433b3a1942d0");
}
