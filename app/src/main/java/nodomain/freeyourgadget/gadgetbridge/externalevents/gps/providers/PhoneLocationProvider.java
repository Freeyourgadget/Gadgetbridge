/*  Copyright (C) 2022-2024 José Rebelo, LukasEdl

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
package nodomain.freeyourgadget.gadgetbridge.externalevents.gps.providers;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * A location provider that uses the phone GPS, using {@link LocationManager}.
 */
public class PhoneLocationProvider extends GBLocationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneLocationProvider.class);

    private final String provider;

    private static final int INTERVAL_MIN_DISTANCE = 0;

    public PhoneLocationProvider(final Context context, final LocationListener locationListener, final String provider) {
        super(context, locationListener);
        this.provider = provider;
    }

    @Override
    public void start(final int interval) {
        LOG.info("Starting phone gps location provider");

        if (!GB.checkPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) && !GB.checkPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            GB.toast("Location permission not granted", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }

        final LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(getLocationListener());
        locationManager.requestLocationUpdates(
                provider,
                interval > 0 ? interval : 1_000,
                INTERVAL_MIN_DISTANCE,
                getLocationListener(),
                Looper.getMainLooper()
        );

        final Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LOG.debug("Last known gps location: {}", lastKnownLocation);
    }

    @Override
    public void stop() {
        LOG.info("Stopping phone gps location provider");

        final LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(getLocationListener());
    }
}
