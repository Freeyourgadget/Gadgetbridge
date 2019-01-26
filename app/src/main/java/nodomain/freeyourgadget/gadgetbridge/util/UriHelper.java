/*  Copyright (C) 2016-2018 Carsten Pfeiffer

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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UriHelper {
    @NonNull
    private final Uri uri;
    @NonNull
    private final Context context;
    private String fileName;
    private long fileSize;
    @Nullable
    private File file;

    private UriHelper(@NonNull Uri uri, @NonNull Context context) {
        this.uri = uri;
        this.context = context;
    }

    /**
     * Returns the uri as passed to #get(Uri, Context)
     */
    @NonNull
    public Uri getUri() {
        return uri;
    }

    /**
     * Returns the context as passed to #get(Uri, Context)
     */
    @NonNull
    public Context getContext() {
        return context;
    }

    /**
     * Returns an immutable helper to access the given Uri. In case the uri cannot be read/resolved
     * an IOException is thrown.
     * @param uri the uri to access
     * @param context the context for accessing uris
     * @throws IOException
     */
    @NonNull
    public static UriHelper get(@NonNull Uri uri, @NonNull Context context) throws FileNotFoundException, IOException {
        UriHelper helper = new UriHelper(uri, context);
        helper.resolveMetadata();
        return helper;
    }

    /**
     * Opens a stream to read the contents of the uri.
     * Note: the caller has to close the stream after usage.
     * Every invocation of this method will open a new stream.
     * FIXME: make sure that every caller actually closes the returned stream!
     * @throws FileNotFoundException
     */
    @NonNull
    public InputStream openInputStream() throws FileNotFoundException {
        ContentResolver cr = context.getContentResolver();
        InputStream inputStream = cr.openInputStream(uri);
        if (inputStream != null) {
            return new BufferedInputStream(inputStream);
        }
        throw new FileNotFoundException("Unable to open inputstream for " + uri);
    }

    /**
     * Returns the content length (file size) in bytes
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Returns the name of the file referenced by the Uri. Does not include the path.
     */
    @NonNull
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file behind the uri, or null in case it is not a file:/ Uri.
     * @return the file or null
     */
    @Nullable
    public File getFile() {
        return file;
    }

    private void resolveMetadata() throws IOException {
        String uriScheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(uriScheme)) {
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] {
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.SIZE
            }, null, null, null);
            if (cursor == null) {
                throw new IOException("Unable to query metadata for: " + uri);
            }
            try {
                if (cursor.moveToFirst()) {
                    int name_index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (name_index == -1) {
                        throw new IOException("Unable to retrieve name for: " + uri);
                    }
                    int size_index = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (size_index == -1) {
                        throw new IOException("Unable to retrieve size for: " + uri);
                    }
                    try {
                        fileName = cursor.getString(name_index);
                        if (fileName == null) {
                            throw new IOException("Unable to retrieve name for: " + uri);
                        }
                        fileSize = cursor.getLong(size_index);
                        if (fileSize < 0) {
                            throw new IOException("Unable to retrieve size for: " + uri);
                        }
                    } catch (Exception ex) {
                        throw new IOException("Unable to retrieve metadata for: " + uri + ": " + ex.getMessage());
                    }
                }
            } finally {
                cursor.close();
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uriScheme)) {
            file = new File(uri.getPath());
            if (!file.exists()) {
                throw new FileNotFoundException("Does not exist: " + file);
            }
            fileName = file.getName();
            fileSize = file.length();
        } else if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uriScheme)) {
            // we could actually read it, but I don't see how we can determine the file size
            throw new IOException("Unsupported scheme for uri: " + uri);
        } else {
            throw new IOException("Unsupported scheme for uri: " + uri);
        }
    }
}
