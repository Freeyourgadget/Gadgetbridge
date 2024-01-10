/*  Copyright (C) 2022-2024 Ganblejs

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
