/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

abstract class DatalogSessionPebbleHealth extends DatalogSession {

    private final GBDevice mDevice;

    DatalogSessionPebbleHealth(byte id, UUID uuid, int timestamp, int tag, byte itemType, short itemSize, GBDevice device) {
        super(id, uuid, timestamp, tag, itemType, itemSize);
        mDevice = device;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    boolean isPebbleHealthEnabled() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean("pebble_sync_health", true);
    }

    boolean storePebbleHealthRawRecord() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean("pebble_health_store_raw", true);
    }
}