package nodomain.freeyourgadget.gadgetbridge.contentprovider;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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

    private GBDevice mGBDevice = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                mGBDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            }
        }
    };

    @Override
    public boolean onCreate() {
        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(mReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.equals(CONTENT_URI)) {
            MatrixCursor mc = new MatrixCursor(columnNames);
            int connected = 0;
            int pebbleKit = 0;
            Prefs prefs = GBApplication.getPrefs();
            if (prefs.getBoolean("pebble_enable_pebblekit", false)) {
                pebbleKit = 1;
            }
            String fwString = "unknown";
            if (mGBDevice != null && mGBDevice.getType() == DeviceType.PEBBLE && mGBDevice.isInitialized()) {
                connected = 1;
                fwString = mGBDevice.getFirmwareVersion();
            }
            mc.addRow(new Object[]{connected, pebbleKit, pebbleKit, 3, 8, 2, fwString});

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