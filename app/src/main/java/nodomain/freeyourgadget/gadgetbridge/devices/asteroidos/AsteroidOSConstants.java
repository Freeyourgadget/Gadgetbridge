/*  Copyright (C) 2022-2024 Noodlez

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
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import java.util.UUID;

/**
 * A class to hold all the constants needed by the AsteroidOS devices
 */
public class AsteroidOSConstants {
    /**
     * A list of all the known supported codenames
     */
    public static final String[] KNOWN_DEVICE_CODENAMES = {
        "bass", "sturgeon", "catfish", "catfish_ext",
        "catshark", "lenok", "smelt", "carp",
        "sparrow", "wren", "anthias", "beluga",
        "dory", "firefish", "harmony", "inharmony",
        "narwhal", "ray", "sawfish", "sawshark",
        "skipjack", "tunny", "mooneye", "swift",
        "minnow", "sprat", "tetra", "pike", "hoki",
        "koi", "ayu"
    };

    /**
     * AsteroidOS Service Watch Filter UUID
     */
    public static final UUID SERVICE_UUID               = UUID.fromString("00000000-0000-0000-0000-00A57E401D05");

    /**
     * Battery level service
     */
    public static final UUID BATTERY_SERVICE_UUID       = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    /**
     * Battery level characteristic
     */
    public static final UUID BATTERY_UUID               = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    /**
     * Time service
     */
    public static final UUID TIME_SERVICE_UUID          = UUID.fromString("00005071-0000-0000-0000-00A57E401D05");
    /**
     * Time characteristic
     */
    public static final UUID TIME_SET_CHAR              = UUID.fromString("00005001-0000-0000-0000-00A57E401D05");

    /**
     * Screenshot service
     */
    public static final UUID SCREENSHOT_SERVICE_UUID    = UUID.fromString("00006071-0000-0000-0000-00A57E401D05");
    /**
     * Screenshot request characteristic
     */
    public static final UUID SCREENSHOT_REQUEST_CHAR     = UUID.fromString("00006001-0000-0000-0000-00A57E401D05");
    /**
     * Screenshot content characteristic
     */
    public static final UUID SCREENSHOT_CONTENT_CHAR         = UUID.fromString("00006002-0000-0000-0000-00A57E401D05");

    /**
     * Media service
     */
    public static final UUID MEDIA_SERVICE_UUID         = UUID.fromString("00007071-0000-0000-0000-00A57E401D05");
    /**
     * Media title characteristic
     */
    public static final UUID MEDIA_TITLE_CHAR           = UUID.fromString("00007001-0000-0000-0000-00A57E401D05");
    /**
     * Media album characteristic
     */
    public static final UUID MEDIA_ALBUM_CHAR           = UUID.fromString("00007002-0000-0000-0000-00A57E401D05");
    /**
     * Media artist characteristic
     */
    public static final UUID MEDIA_ARTIST_CHAR          = UUID.fromString("00007003-0000-0000-0000-00A57E401D05");
    /**
     * Media playing status characteristic
     */
    public static final UUID MEDIA_PLAYING_CHAR         = UUID.fromString("00007004-0000-0000-0000-00A57E401D05");
    /**
     * Media command characteristic
     */
    public static final UUID MEDIA_COMMANDS_CHAR        = UUID.fromString("00007005-0000-0000-0000-00A57E401D05");
    /**
     * Media volume characteristic
     */
    public static final UUID MEDIA_VOLUME_CHAR          = UUID.fromString("00007006-0000-0000-0000-00A57E401D05");

    /**
     * Weather service
     */
    public static final UUID WEATHER_SERVICE_UUID       = UUID.fromString("00008071-0000-0000-0000-00A57E401D05");
    /**
     * Weather city name characteristic
     */
    public static final UUID WEATHER_CITY_CHAR          = UUID.fromString("00008001-0000-0000-0000-00A57E401D05");
    /**
     * Weather condition codes characteristic
     */
    public static final UUID WEATHER_IDS_CHAR           = UUID.fromString("00008002-0000-0000-0000-00A57E401D05");
    /**
     * Weather minimum temps characteristic
     */
    public static final UUID WEATHER_MIN_TEMPS_CHAR     = UUID.fromString("00008003-0000-0000-0000-00A57E401D05");
    /**
     * Weather maximum temps characteristic
     */
    public static final UUID WEATHER_MAX_TEMPS_CHAR     = UUID.fromString("00008004-0000-0000-0000-00A57E401D05");

    /**
     * Notification service
     */
    public static final UUID NOTIFICATION_SERVICE_UUID  = UUID.fromString("00009071-0000-0000-0000-00A57E401D05");
    /**
     * Notification update characteristic
     */
    public static final UUID NOTIFICATION_UPDATE_CHAR   = UUID.fromString("00009001-0000-0000-0000-00A57E401D05");
    /**
     * Notification feedback characteristic
     */
    public static final UUID NOTIFICATION_FEEDBACK_CHAR = UUID.fromString("00009002-0000-0000-0000-00A57E401D05");
}
