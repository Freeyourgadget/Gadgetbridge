/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.externalevents.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A location provider that uses the phone GPS, using {@link LocationManager}.
 */
public class PhoneGpsLocationProvider extends AbstractLocationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneGpsLocationProvider.class);

    private static final int INTERVAL_MIN_TIME = 1000;
    private static final int INTERVAL_MIN_DISTANCE = 0;

    public PhoneGpsLocationProvider(LocationListener locationListener) {
        super(locationListener);
    }

    @Override
    void start(final Context context) {
        LOG.info("Starting phone gps location provider");

        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(getLocationListener());
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                INTERVAL_MIN_TIME,
                INTERVAL_MIN_DISTANCE,
                getLocationListener(),
                Looper.getMainLooper()
        );

        final Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LOG.debug("Last known location: {}", lastKnownLocation);
    }

    @Override
    void stop(final Context context) {
        LOG.info("Stopping phone gps location provider");

        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(getLocationListener());
    }
}
