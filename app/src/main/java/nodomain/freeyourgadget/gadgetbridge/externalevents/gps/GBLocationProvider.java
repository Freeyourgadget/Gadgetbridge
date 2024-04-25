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
package nodomain.freeyourgadget.gadgetbridge.externalevents.gps;

import android.content.Context;
import android.location.LocationListener;

/**
 * An abstract location provider, which periodically sends a location update to the provided {@link LocationListener}.
 */
public abstract class GBLocationProvider {
    private final Context context;
    private final LocationListener locationListener;

    public GBLocationProvider(final Context context, final LocationListener locationListener) {
        this.context = context;
        this.locationListener = locationListener;
    }

    public final Context getContext() {
        return this.context;
    }

    public final LocationListener getLocationListener() {
        return this.locationListener;
    }

    /**
     * Start sending periodic location updates.
     */
    public abstract void start(final int interval);

    /**
     * Stop sending periodic location updates.
     */
    public abstract void stop();
}
