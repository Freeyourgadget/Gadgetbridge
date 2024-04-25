/*  Copyright (C) 2022-2024 LukasEdl

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
import android.location.LocationManager;

import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.providers.MockLocationProvider;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.providers.PhoneLocationProvider;

public enum GBLocationProviderType {
    GPS {
        @Override
        public GBLocationProvider newInstance(final Context context, final GBLocationListener locationListener) {
            return new PhoneLocationProvider(context, locationListener, LocationManager.GPS_PROVIDER);
        }
    },
    NETWORK {
        @Override
        public GBLocationProvider newInstance(final Context context, final GBLocationListener locationListener) {
            return new PhoneLocationProvider(context, locationListener, LocationManager.NETWORK_PROVIDER);
        }
    },
    MOCK {
        @Override
        public GBLocationProvider newInstance(final Context context, final GBLocationListener locationListener) {
            return new MockLocationProvider(context, locationListener);
        }
    },
    ;

    public abstract GBLocationProvider newInstance(final Context context, final GBLocationListener locationListener);
}
