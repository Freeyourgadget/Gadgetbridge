/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;

public class AndroidUtils {
    public static ParcelUuid[] toParcelUUids(Parcelable[] uuids) {
        if (uuids == null) {
            return null;
        }
        ParcelUuid[] uuids2 = new ParcelUuid[uuids.length];
        System.arraycopy(uuids, 0, uuids2, 0, uuids.length);
        return uuids2;
    }

    /**
     * Unregisters the given receiver from the given context.
     * @param context the context from which to unregister
     * @param receiver the receiver to unregister
     * @return true if it was successfully unregistered, or false if the receiver was not registered
     */
    public static boolean safeUnregisterBroadcastReceiver(Context context, BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Unregisters the given receiver from the given {@link LocalBroadcastManager}.
     * @param manager the manager  from which to unregister
     * @param receiver the receiver to unregister
     * @return true if it was successfully unregistered, or false if the receiver was not registered
     */
    public static boolean safeUnregisterBroadcastReceiver(LocalBroadcastManager manager, BroadcastReceiver receiver) {
        try {
            manager.unregisterReceiver(receiver);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
