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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;

/**
 * A mock location provider which keeps updating the location at a constant speed, starting from the
 * last known location. Useful for local tests.
 */
public class MockLocationProvider extends GBLocationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MockLocationProvider.class);

    private Location previousLocation = new CurrentPosition().getLastKnownLocation();

    /**
     * Interval between location updates, in milliseconds.
     */
    private static final int DEFAULT_INTERVAL = 1000;

    /**
     * Difference between location updates, in degrees.
     */
    private static final float COORD_DIFF = 0.0002f;

    /**
     * Whether the handler is running.
     */
    private boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public MockLocationProvider(final Context context, final LocationListener locationListener) {
        super(context, locationListener);
    }

    @Override
    public void start(final int interval) {
        LOG.info("Starting mock location provider");

        running = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }

                final Location newLocation = new Location(previousLocation);
                newLocation.setLatitude(previousLocation.getLatitude() + COORD_DIFF);
                newLocation.setTime(System.currentTimeMillis());
                newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                getLocationListener().onLocationChanged(newLocation);

                previousLocation = newLocation;

                if (running) {
                    handler.postDelayed(this, interval);
                }
            }
        }, interval > 0 ? interval : DEFAULT_INTERVAL);
    }

    @Override
    public void stop() {
        LOG.info("Stopping mock location provider");

        running = false;
        handler.removeCallbacksAndMessages(null);
    }
}
