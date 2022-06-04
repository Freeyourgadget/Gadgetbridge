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
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * A static location manager, which keeps track of what providers are currently running. A notification is kept
 * while there is at least one provider runnin.
 */
public class GBLocationManager {
    private static final Logger LOG = LoggerFactory.getLogger(GBLocationManager.class);

    /**
     * The current number of running listeners.
     */
    private static Map<EventHandler, AbstractLocationProvider> providers = new HashMap<>();

    public static void start(final Context context, final EventHandler eventHandler) {
        if (providers.containsKey(eventHandler)) {
            LOG.warn("EventHandler already registered");
            return;
        }

        GB.createGpsNotification(context, providers.size() + 1);

        final GBLocationListener locationListener = new GBLocationListener(eventHandler);
        final AbstractLocationProvider locationProvider = new PhoneGpsLocationProvider(locationListener);

        locationProvider.start(context);

        providers.put(eventHandler, locationProvider);
    }

    public static void stop(final Context context, final EventHandler eventHandler) {
        final AbstractLocationProvider locationProvider = providers.remove(eventHandler);

        if (locationProvider != null) {
            LOG.warn("EventHandler not registered");

            locationProvider.stop(context);
        }

        if (!providers.isEmpty()) {
            GB.createGpsNotification(context, providers.size());
        } else {
            GB.removeGpsNotification(context);
        }
    }

    public static void stopAll(final Context context) {
        for (EventHandler eventHandler : providers.keySet()) {
            stop(context, eventHandler);
        }
    }
}
