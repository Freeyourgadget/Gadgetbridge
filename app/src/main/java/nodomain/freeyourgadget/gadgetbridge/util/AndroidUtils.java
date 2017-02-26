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
