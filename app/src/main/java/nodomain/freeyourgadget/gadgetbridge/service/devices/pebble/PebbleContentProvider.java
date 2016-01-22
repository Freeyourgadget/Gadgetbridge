package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;

public class PebbleContentProvider extends ContentProvider {

    public static final int COLUMN_CONNECTED = 0;
    public static final int COLUMN_APPMSG_SUPPORT = 1;
    public static final int COLUMN_DATALOGGING_SUPPORT = 2;
    public static final int COLUMN_VERSION_MAJOR = 3;
    public static final int COLUMN_VERSION_MINOR = 4;
    public static final int COLUMN_VERSION_POINT = 5;
    public static final int COLUMN_VERSION_TAG = 6;

    // this is only needed for the MatrixCursor constructor
    public static final String[] columnNames = new String[]{"0", "1", "2", "3", "4", "5", "6"};

    static final String PROVIDER_NAME = "com.getpebble.android.provider";
    static final String URL = "content://" + PROVIDER_NAME + "/state";
    static final Uri CONTENT_URI = Uri.parse(URL);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.equals(CONTENT_URI)) {
            MatrixCursor mc = new MatrixCursor(columnNames);
            mc.addRow(new Object[]{1, 1, 0, 3, 8, 0, "Gadgetbridge"});
            return mc;
        } else {
            return null;
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}