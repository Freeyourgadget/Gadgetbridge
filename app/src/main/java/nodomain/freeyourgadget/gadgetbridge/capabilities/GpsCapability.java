/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities;

public class GpsCapability {
    public enum Preset {
        ACCURACY,
        BALANCED,
        POWER_SAVING,
        CUSTOM
    }

    public enum Band {
        SINGLE_BAND,
        DUAL_BAND
    }

    public enum Combination {
        LOW_POWER_GPS,
        GPS,
        GPS_BDS,
        GPS_GLONASS,
        GPS_GALILEO,
        ALL_SATELLITES
    }

    public enum SatelliteSearch {
        SPEED_FIRST,
        ACCURACY_FIRST
    }
}
