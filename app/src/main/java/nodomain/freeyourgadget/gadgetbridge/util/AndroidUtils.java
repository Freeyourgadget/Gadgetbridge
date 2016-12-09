package nodomain.freeyourgadget.gadgetbridge.util;

import android.os.ParcelUuid;
import android.os.Parcelable;

public class AndroidUtils {
    public static ParcelUuid[] toParcelUUids(Parcelable[] uuids) {
        if (uuids == null) {
            return null;
        }
        ParcelUuid[] uuids2 = new ParcelUuid[uuids.length];
        System.arraycopy(uuids, 0, uuids2, 0, uuids.length);
        return uuids2;
    }
}
