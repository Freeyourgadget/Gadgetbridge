package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public  class PendingIntentUtils {
    
    public static PendingIntent getBroadcast(Context context,
                                             int requestCode,
                                             Intent intent,
                                             int flags,
                                             boolean makeMutable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (makeMutable) {
            flags |= PendingIntent.FLAG_MUTABLE;
          } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
          }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags);
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
    
    public static PendingIntent getActivity(Context context,
                                             int requestCode,
                                             Intent intent,
                                             int flags,
                                             boolean makeMutable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (makeMutable) {
            flags |= PendingIntent.FLAG_MUTABLE;
          } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
          }
            return PendingIntent.getActivity(context, requestCode, intent, flags);
        }

        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }
    
    public static PendingIntent getService(Context context,
                                             int requestCode,
                                             Intent intent,
                                             int flags,
                                             boolean makeMutable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (makeMutable) {
            flags |= PendingIntent.FLAG_MUTABLE;
          } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
          }
            return PendingIntent.getService(context, requestCode, intent, flags);
        }

        return PendingIntent.getService(context, requestCode, intent, flags);
    }
    
}
