/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxWaypoint extends GPSCoordinate {
    @Nullable
    private final String name;

    public GpxWaypoint(final double longitude, final double latitude, final double altitude, @Nullable final String name) {
        super(longitude, latitude, altitude);
        this.name = name;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public static class Builder {
        private double longitude;
        private double latitude;
        private double altitude;
        private String name;

        public Builder withLongitude(final double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder withLatitude(final double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder withAltitude(final double altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public GpxWaypoint build() {
            return new GpxWaypoint(longitude, latitude, altitude, name);
        }
    }
}
