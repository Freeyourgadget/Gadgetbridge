/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import java.util.Date;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxTrackPoint extends GPSCoordinate {
    private final Date time;

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time) {
        super(longitude, latitude, altitude);
        this.time = time;
    }

    public Date getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GpxTrackPoint)) return false;
        if (!super.equals(o)) return false;
        final GpxTrackPoint that = (GpxTrackPoint) o;
        return Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), time);
    }

    public static class Builder {
        private double longitude;
        private double latitude;
        private double altitude;
        private Date time;

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

        public Builder withTime(final Date time) {
            this.time = time;
            return this;
        }

        public GpxTrackPoint build() {
            return new GpxTrackPoint(longitude, latitude, altitude, time);
        }
    }
}
