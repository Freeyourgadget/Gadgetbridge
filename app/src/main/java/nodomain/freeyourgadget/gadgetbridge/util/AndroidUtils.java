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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

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

    public static void setLanguage(Context context, Locale language) {
        Configuration config = new Configuration();
        config.setLocale(language);

        // FIXME: I have no idea what I am doing
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    /**
     * Returns the theme dependent text color as a css-style hex string.
     * @param context the context to access the colour
     */
    public static String getTextColorHex(Context context) {
        int color;
        if (GBApplication.isDarkThemeEnabled()) {
            color = context.getResources().getColor(R.color.primarytext_dark);
        } else {
            color = context.getResources().getColor(R.color.primarytext_light);
        }
        return colorToHex(color);
    }

    /**
     * Returns the theme dependent background color as a css-style hex string.
     * @param context the context to access the colour
     */
    public static String getBackgroundColorHex(Context context) {
        int color;
        if (GBApplication.isDarkThemeEnabled()) {
            color = context.getResources().getColor(R.color.cardview_dark_background);
        } else {
            color = context.getResources().getColor(R.color.cardview_light_background);
        }
        return colorToHex(color);
    }

    private static String colorToHex(int color) {
        return "#"
                + Integer.toHexString(Color.red(color))
                + Integer.toHexString(Color.green(color))
                + Integer.toHexString(Color.blue(color));
    }
}
