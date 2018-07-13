/*  Copyright (C) 2018 Benedikt Elser

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
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

/**
 * A content Provider, which publishes read only RAW @see ActivitySample to other applications
 * <p>
 */
public class HRContentProvider extends ContentProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HRContentProvider.class);

    private static final int DEVICES_LIST = 1;
    private static final int REALTIME = 2;
    private static final int ACTIVITY_START = 3;
    private static final int ACTIVITY_STOP = 4;

    enum provider_state {ACTIVE, CONNECTING, INACTIVE};
    provider_state state = provider_state.INACTIVE;

    private static final UriMatcher URI_MATCHER;
    private Timer punchTimer = new Timer();

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(HRContentProviderContract.AUTHORITY,
                "devices", DEVICES_LIST);
        URI_MATCHER.addURI(HRContentProviderContract.AUTHORITY,
                "realtime", REALTIME);
        URI_MATCHER.addURI(HRContentProviderContract.AUTHORITY,
                "activity_start", ACTIVITY_START);
        URI_MATCHER.addURI(HRContentProviderContract.AUTHORITY,
                "activity_stop", ACTIVITY_STOP);
    }

    private ActivitySample buffered_sample = null;

    private GBDevice mGBDevice = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //LOG.info("Received Event, aciton: " + action);

            switch (action) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    mGBDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    LOG.debug("ACTION DEVICE CHANGED Got Device " + mGBDevice);
                    // Rationale: If device was not connected
                    // it should show up here after being connected
                    // If the user wanted to switch on realtime traffic, but we first needed to connect it
                    // we do it here
                    if (mGBDevice.isConnected() && state == provider_state.CONNECTING) {
                        LOG.debug("Device connected now, enabling realtime " + mGBDevice);

                        state = provider_state.ACTIVE;
                        GBApplication.deviceService().onEnableRealtimeSteps(true);
                        GBApplication.deviceService().onEnableRealtimeHeartRateMeasurement(true);
                    }
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    ActivitySample tmp_sample = (ActivitySample) intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE);
                    //LOG.debug("Got new Sample " + tmp_sample.getHeartRate());
                    if (tmp_sample.getHeartRate() == -1)
                        break;

                    buffered_sample = tmp_sample;
                    // This notifies the observer
                    getContext().
                            getContentResolver().
                            notifyChange(Uri.parse(HRContentProviderContract.REALTIME_URL), null);
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    public boolean onCreate() {
        LOG.info("Creating...");
        IntentFilter filterLocal = new IntentFilter();

        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);

        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(mReceiver, filterLocal);

        //TODO: This crashes the app. Seems like device Manager is not here yet
        //mGBDevice = ((GBApplication) this.getContext()).getDeviceManager().getSelectedDevice();

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //LOG.info(HRContentProvider.class.getName(), "query uri " + uri.toString());
        MatrixCursor mc;

        DeviceManager deviceManager;
        switch (URI_MATCHER.match(uri)) {
            case DEVICES_LIST:
                deviceManager = ((GBApplication) (this.getContext())).getDeviceManager();
                List<GBDevice> l = deviceManager.getDevices();
                if (l == null) {
                    return null;
                }
                LOG.info(String.format("listing %d devices", l.size()));

                mc = new MatrixCursor(HRContentProviderContract.deviceColumnNames);
                for (GBDevice dev : l) {
                    mc.addRow(new Object[]{dev.getName(), dev.getModel(), dev.getAddress()});
                }
                return mc;
            case ACTIVITY_START:

                this.state = provider_state.CONNECTING;
                LOG.info("Get ACTIVTY START");
                GBDevice targetDevice = getDevice((selectionArgs != null) ? selectionArgs[0] : "");
                if (targetDevice != null && targetDevice.isConnected()) {
                    this.state = provider_state.ACTIVE;
                    GBApplication.deviceService().onEnableRealtimeSteps(true);
                    GBApplication.deviceService().onEnableRealtimeHeartRateMeasurement(true);
                    mc = new MatrixCursor(HRContentProviderContract.activityColumnNames);
                    mc.addRow(new String[]{"OK", "Connected"});
                } else {
                    GBApplication.deviceService().connect(targetDevice);
                    mc = new MatrixCursor(HRContentProviderContract.activityColumnNames);
                    mc.addRow(new String[]{"OK", "Connecting"});
                }

                punchTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        LOG.debug("punching the deviceService...");
                        // As seen in LiveActivityFragment:
                        // have to enable it again and again to keep it measureing
                        GBApplication.deviceService().onEnableRealtimeHeartRateMeasurement(true);
                    }

                }, 1000 * 10, 1000);
                // Start after 10 seconds, repeat each second

                return mc;
            case ACTIVITY_STOP:
                this.state = provider_state.INACTIVE;

                LOG.info("Get ACTIVITY STOP");
                GBApplication.deviceService().onEnableRealtimeSteps(false);
                GBApplication.deviceService().onEnableRealtimeHeartRateMeasurement(false);
                mc = new MatrixCursor(HRContentProviderContract.activityColumnNames);
                mc.addRow(new String[]{"OK", "No error"});
                punchTimer.cancel();
                punchTimer = new Timer();
                return mc;
            case REALTIME:
                //String sample_string = (buffered_sample == null) ? "" : buffered_sample.toString();
                //LOG.error(String.format("Get REALTIME buffered sample %s", sample_string));
                mc = new MatrixCursor(HRContentProviderContract.realtimeColumnNames);
                if (buffered_sample != null)
                    mc.addRow(new Object[]{"OK", buffered_sample.getHeartRate(), buffered_sample.getSteps(), mGBDevice != null ? mGBDevice.getBatteryLevel() : 99});
                return mc;
        }
        return null;
    }

    // Returns the requested device. If it is not found
    // it tries to return the "current" device (if i understand it correctly)
    @Nullable
    private GBDevice getDevice(String deviceAddress) {
        DeviceManager deviceManager;

        if (mGBDevice != null && mGBDevice.getAddress().equals(deviceAddress)) {
            LOG.info(String.format("Found device mGBDevice %s", mGBDevice));

            return mGBDevice;
        }

        deviceManager = ((GBApplication) (this.getContext())).getDeviceManager();
        for (GBDevice device : deviceManager.getDevices()) {
            if (deviceAddress.equals(device.getAddress())) {
                LOG.info(String.format("Found device device %s", device));
                return device;
            }
        }
        LOG.info(String.format("Did not find device returning selected %s", deviceManager.getSelectedDevice()));
        return deviceManager.getSelectedDevice();
    }

    @Override
    public String getType(@NonNull Uri uri) {
        LOG.error("getType uri " + uri);
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