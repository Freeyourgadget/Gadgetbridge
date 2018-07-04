/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.contentprovider;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_BOOLEAN_ENABLE;

/**
 * A content Provider, which publishes read only RAW @see ActivitySample to other applications
 * <p>
 * TODO:
 * For this to work there must be an additional api which switches the GadgetBridge to "start activity"
 */
public class HRContentProvider extends ContentProvider {

    private static final int DEVICES_LIST = 1;
    private static final int REALTIME = 2;
    private static final int ACTIVITY_START = 3;
    private static final int ACTIVITY_STOP = 4;

    private static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(HRContentProvider.AUTHORITY,
                "devices", DEVICES_LIST);
        URI_MATCHER.addURI(HRContentProvider.AUTHORITY,
                "realtime", REALTIME);
        URI_MATCHER.addURI(HRContentProvider.AUTHORITY,
                "activity_start", ACTIVITY_START);
        URI_MATCHER.addURI(HRContentProvider.AUTHORITY,
                "activity_stop", ACTIVITY_STOP);
    }

    private ActivitySample buffered_sample = null;

    // this is only needed for the MatrixCursor constructor
    public static final String[] deviceColumnNames = new String[]{"Name", "Model", "Address"};
    public static final String[] activityColumnNames = new String[]{"Status", "Message"};
    public static final String[] realtimeColumnNames = new String[]{"Status", "Message"};

    static final String AUTHORITY = "com.gadgetbridge.heartrate.provider";
    static final String ACTIVITY_START_URL = "content://" + AUTHORITY + "/activity_start";
    static final String ACTIVITY_STOP_URL = "content://" + AUTHORITY + "/activity_stop";

    static final String REALTIME_URL = "content://" + AUTHORITY + "/realtime";
    static final String DEVICES_URL = "content://" + AUTHORITY + "/devices";

    public static final Uri ACTIVITY_START_URI = Uri.parse(ACTIVITY_START_URL);
    public static final Uri ACTIVITY_STOP_URI = Uri.parse(ACTIVITY_STOP_URL);
    public static final Uri REALTIME_URI = Uri.parse(REALTIME_URL);
    public static final Uri DEVICES_URI = Uri.parse(DEVICES_URL);

    private GBDevice mGBDevice = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.i(HRContentProvider.class.getName(), "Received Event, aciton: " + action);

            switch (action) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    mGBDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    buffered_sample = (ActivitySample) intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE);
                    // This notifies the observer
                    getContext().
                            getContentResolver().
                            notifyChange(REALTIME_URI, null);
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    public boolean onCreate() {
        Log.i(HRContentProvider.class.getName(), "Creating...");
        IntentFilter filterLocal = new IntentFilter();

        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);

        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(mReceiver, filterLocal);

        //TODO Do i need only for testing or also in production?
        this.getContext().registerReceiver(mReceiver, filterLocal);

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Log.i(HRContentProvider.class.getName(), "query uri " + uri.toString());
        MatrixCursor mc;
        Intent intent;

        switch (URI_MATCHER.match(uri)) {
            case DEVICES_LIST:
                DeviceManager deviceManager = ((GBApplication) GBApplication.getContext()).getDeviceManager();
                List<GBDevice> l = deviceManager.getDevices();
                Log.i(HRContentProvider.class.getName(), String.format("listing %d devices", l.size()));

                 mc = new MatrixCursor(deviceColumnNames);
                for (GBDevice dev : l) {
                    mc.addRow(new Object[]{dev.getName(), dev.getModel(), dev.getAddress()});
                }
                return mc;
            case ACTIVITY_START:
                Log.i(HRContentProvider.class.getName(), String.format("Get ACTIVTY START"));
                intent =
                        new Intent()
                        .setAction(ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT)
                        .putExtra(EXTRA_BOOLEAN_ENABLE, true);

                GBApplication.getContext().startService(intent);
                mc = new MatrixCursor(activityColumnNames);
                mc.addRow(new String[]{"OK", "No error"});
                return mc;
            case ACTIVITY_STOP:
                Log.i(HRContentProvider.class.getName(), String.format("Get ACTIVITY STOP"));
                intent =
                        new Intent()
                                .setAction(ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT)
                                .putExtra(EXTRA_BOOLEAN_ENABLE, false);

                GBApplication.getContext().startService(intent);
                mc = new MatrixCursor(activityColumnNames);
                mc.addRow(new String[]{"OK", "No error"});
                return mc;
            case REALTIME:
                String sample_string = (buffered_sample == null) ? "" : buffered_sample.toString();
                //Log.e(HRContentProvider.class.getName(), String.format("Get REALTIME buffered sample %s", sample_string));
                mc = new MatrixCursor(realtimeColumnNames);
                if (buffered_sample == null)
                    mc.addRow(new Object[]{"NO_DATA", 0});
                else
                    mc.addRow(new Object[]{"OK", buffered_sample.getHeartRate()});
                return mc;
        }
        return new MatrixCursor(deviceColumnNames);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        Log.e(HRContentProvider.class.getName(), "getType uri " + uri);
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
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[]
            selectionArgs) {
        return 0;
    }

    // Das ist eine debugging funktion
    @Override
    public void shutdown() {
        LocalBroadcastManager.getInstance(this.getContext()).unregisterReceiver(mReceiver);
        super.shutdown();
    }

}