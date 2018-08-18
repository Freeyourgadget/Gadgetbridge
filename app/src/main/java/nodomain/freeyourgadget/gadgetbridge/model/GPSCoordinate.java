/*  Copyright (C) 2017-2018 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GPSCoordinate {
    private final double latitude;
    private final double longitude;
    private final double altitude;

    public static final int GPS_DECIMAL_DEGREES_SCALE = 6; // precise to 111.132mm at equator: https://en.wikipedia.org/wiki/Decimal_degrees

    public GPSCoordinate(double longitude, double latitude, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GPSCoordinate that = (GPSCoordinate) o;

        if (Double.compare(that.getLatitude(), getLatitude()) != 0) return false;
        if (Double.compare(that.getLongitude(), getLongitude()) != 0) return false;
        return Double.compare(that.getAltitude(), getAltitude()) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getLatitude());
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getLongitude());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getAltitude());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    private String formatLocation(double value) {
        return new BigDecimal(value).setScale(8, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public String toString() {
        return "lon: " + formatLocation(longitude) + ", lat: " + formatLocation(latitude) + ", alt: " + formatLocation(altitude) + "m";
    }
}
