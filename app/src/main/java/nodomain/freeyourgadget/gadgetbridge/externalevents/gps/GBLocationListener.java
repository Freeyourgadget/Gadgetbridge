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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An implementation of a {@link LocationListener} that forwards the location updates to the
 * provided {@link EventHandler}.
 */
public class GBLocationListener implements LocationListener {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationListener.class);

    private final EventHandler eventHandler;

    private Location previousLocation;

    public GBLocationListener(final EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void onLocationChanged(final Location location) {
        LOG.info("Location changed: {}", location);

        // The location usually doesn't contain speed, compute it from the previous location
        if (previousLocation != null && !location.hasSpeed()) {
            long timeInterval = (location.getTime() - previousLocation.getTime()) / 1000L;
            float distanceInMeters = previousLocation.distanceTo(location);
            location.setSpeed(distanceInMeters / timeInterval);
        }

        previousLocation = location;

        eventHandler.onSetGpsLocation(location);
    }

    @Override
    public void onProviderDisabled(final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onProviderEnabled(final String provider) {
        LOG.info("onProviderDisabled: {}", provider);
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
        LOG.info("onStatusChanged: {}", provider, status);
    }
}
