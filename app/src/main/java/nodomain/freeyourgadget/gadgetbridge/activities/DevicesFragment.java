/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapterv2;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DevicesFragment extends Fragment {

    private DeviceManager deviceManager;
    private GBDeviceAdapterv2 mGBDeviceAdapter;
    private RecyclerView deviceListView;
    private FloatingActionButton fab;
    List<GBDevice> deviceList;
    private  HashMap<String,long[]> deviceActivityHashMap = new HashMap();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case DeviceManager.ACTION_DEVICES_CHANGED:
                case GBApplication.ACTION_NEW_DATA:
                    final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (action.equals(GBApplication.ACTION_NEW_DATA)) {
                        createRefreshTask("get activity data", requireContext(), device).execute();
                    }
                    if (device != null) {
                        // Refresh only this device
                        refreshSingleDevice(device);
                    } else {
                        refreshPairedDevices();
                    }

                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
            }
        }
    };

    private void handleRealtimeSample(Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
                refreshPairedDevices();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View currentView = inflater.inflate(R.layout.fragment_devices, container, false);

        deviceManager = ((GBApplication) getActivity().getApplication()).getDeviceManager();

        deviceListView = currentView.findViewById(R.id.deviceListView);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(new LinearLayoutManager(currentView.getContext()));

        deviceList = deviceManager.getDevices();
        mGBDeviceAdapter = new GBDeviceAdapterv2(currentView.getContext(), deviceList, deviceActivityHashMap);
        mGBDeviceAdapter.setHasStableIds(true);

        deviceListView.setAdapter(this.mGBDeviceAdapter);

        // get activity data asynchronously, this fills the deviceActivityHashMap
        // and calls refreshPairedDevices() → notifyDataSetChanged
        deviceListView.post(new Runnable() {
            @Override
            public void run() {
                if (getContext() != null) {
                    createRefreshTask("get activity data", getContext(), null).execute();
                }
            }
        });

        fab = currentView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        showFabIfNeccessary();

        /* uncomment to enable fixed-swipe to reveal more actions

        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT , ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(dX>50)
                    dX = 50;
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                GB.toast(getBaseContext(), "onMove", Toast.LENGTH_LONG, GB.ERROR);

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                GB.toast(getBaseContext(), "onSwiped", Toast.LENGTH_LONG, GB.ERROR);

            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {
            }
        });

        swipeToDismissTouchHelper.attachToRecyclerView(deviceListView);
        */

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        refreshPairedDevices();

        if (GB.isBluetoothEnabled() && deviceList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startActivity(new Intent(getActivity(), DiscoveryActivityV2.class));
        }

        return currentView;
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(getActivity(), DiscoveryActivityV2.class));
    }

    private void showFabIfNeccessary() {
        if (GBApplication.getPrefs().getBoolean("display_add_device_fab", true)) {
            fab.show();
        } else {
            if (deviceManager.getDevices().size() < 1) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (deviceListView != null) unregisterForContextMenu(deviceListView);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private long[] getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();

        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db);
    }

    public void refreshPairedDevices() {
        if (mGBDeviceAdapter != null) {
            mGBDeviceAdapter.rebuildFolders();
            mGBDeviceAdapter.notifyDataSetChanged();
        }
    }

    public void refreshSingleDevice(final GBDevice device) {
        if (mGBDeviceAdapter != null) {
            mGBDeviceAdapter.refreshSingleDevice(device);
        }
    }

    public RefreshTask createRefreshTask(String task, Context context, GBDevice device) {
        return new RefreshTask(task, context, device);
    }

    public class RefreshTask extends DBAccess {
        private final GBDevice device;

        public RefreshTask(final String task, final Context context, final GBDevice device) {
            super(task, context);
            this.device = device;
        }

        @Override
        protected void doInBackground(final DBHandler db) {
            if (device != null) {
                updateDevice(db, device);
            } else {
                for (GBDevice gbDevice : deviceList) {
                    updateDevice(db, gbDevice);
                }
            }
        }

        private void updateDevice(final DBHandler db, final GBDevice gbDevice) {
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            final boolean showActivityCard = GBApplication.getDevicePrefs(gbDevice).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD, true);
            if (coordinator.supportsActivityTracking() && showActivityCard) {
                final long[] stepsAndSleepData = getSteps(gbDevice, db);
                deviceActivityHashMap.put(gbDevice.getAddress(), stepsAndSleepData);
            }
        }

        @Override
        protected void onPostExecute(final Object o) {
            if (device != null) {
                refreshSingleDevice(device);
            } else {
                refreshPairedDevices();
            }
        }
    }
}
