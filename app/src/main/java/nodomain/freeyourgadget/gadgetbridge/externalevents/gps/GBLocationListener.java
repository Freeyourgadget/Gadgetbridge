/*  Copyright (C) 2022-2024 illis, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.externalevents.gps;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * An implementation of a {@link LocationListener} that forwards the location updates to the
 * provided {@link EventHandler}.
 */
public class GBLocationListener implements LocationListener {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationListener.class);

    private final GBDevice device;

    private Location previousLocation;
    // divide by 3.6 to get km/h to m/s
    private static final double SPEED_THRESHOLD = 1.0 / 3.6;

    public GBLocationListener(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        LOG.info("Location changed: {}", location);

        // Correct the location time
        location.setTime(getLocationTimestamp(location));

        // The location usually doesn't contain speed, compute it from the previous location
        // Some devices report hasSpeed() as true, and yet only return a 0 value, so we have to check against a speed threshold
        boolean hasValidSpeed = location.hasSpeed() && (location.getSpeed() > SPEED_THRESHOLD);
        if (previousLocation != null && !hasValidSpeed) {
            long timeInterval = (location.getTime() - previousLocation.getTime());
            float distanceInMeters = previousLocation.distanceTo(location);
            location.setSpeed(distanceInMeters / timeInterval * 1000L);
        }

        previousLocation = location;

        GBApplication.deviceService(device).onSetGpsLocation(location);
    }

    @Override
    public void onProviderDisabled(@NonNull final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onProviderEnabled(@NonNull final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
        LOG.info("onStatusChanged: {}, {}", provider, status);
    }

    private static long getLocationTimestamp(final Location location) {
        long nanosSinceLocation = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
        return System.currentTimeMillis() - (nanosSinceLocation / 100_000L);
    }
}
