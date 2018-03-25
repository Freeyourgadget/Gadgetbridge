/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Felix
    Konstantin Maurer

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
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import java.net.URISyntaxException;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class AndroidUtils {
    public static ParcelUuid[] toParcelUuids(Parcelable[] uuids) {
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

    /**
     * As seen on stackoverflow https://stackoverflow.com/a/36714242/1207186
     * Try to find the file path of a document uri
     * @param context the application context
     * @param uri the Uri for which the path should be resolved
     * @return the path corresponding to the Uri as a String
     * @throws IllegalArgumentException on any problem decoding the uri to a path
     */
    public static @NonNull String getFilePath(@NonNull Context context, @NonNull Uri uri) throws IllegalArgumentException {
        try {
            String path = internalGetFilePath(context, uri);
            if (TextUtils.isEmpty(path)) {
                throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri);
            }
            return path;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri, ex);
        }
    }

    /**
     * As seen on stackoverflow https://stackoverflow.com/a/36714242/1207186
     * Try to find the file path of a document uri
     * @param context the application context
     * @param uri the Uri for which the path should be resolved
     * @return the path corresponding to the Uri as a String
     * @throws URISyntaxException
    */
    private static @Nullable String internalGetFilePath(@NonNull Context context, @NonNull Uri uri) throws URISyntaxException {
        String selection = null;
        String[] selectionArgs = null;

        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                }
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            cursor = context.getContentResolver()
                    .query(uri, projection, selection, selectionArgs, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                return cursor.getString(column_index);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri);
    }
}
