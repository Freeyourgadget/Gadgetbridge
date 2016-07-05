package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ControlCenter extends GBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ControlCenter.class);

    private TextView hintTextView;
    private FloatingActionButton fab;
    private ImageView background;

    private SwipeRefreshLayout swipeLayout;
    private GBDeviceAdapter mGBDeviceAdapter;
    private DeviceManager deviceManager;
    /**
     * Temporary field for the context menu
     */
    private GBDevice selectedDevice;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
                case DeviceManager.ACTION_DEVICES_CHANGED:
                    refreshPairedDevices();
                    GBDevice selectedDevice = deviceManager.getSelectedDevice();
                    if (selectedDevice != null) {
                        refreshBusyState(selectedDevice);
                        enableSwipeRefresh(selectedDevice);
                    }
                    break;
            }
        }
    };

    private void refreshBusyState(GBDevice dev) {
        if (dev.isBusy()) {
            swipeLayout.setRefreshing(true);
        } else {
            boolean wasBusy = swipeLayout.isRefreshing();
            if (wasBusy) {
                swipeLayout.setRefreshing(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenter);

        deviceManager = GBApplication.getDeviceManager();

        hintTextView = (TextView) findViewById(R.id.hintTextView);
        ListView deviceListView = (ListView) findViewById(R.id.deviceListView);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        background = (ImageView) findViewById(R.id.no_items_bg);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        final List<GBDevice> deviceList = deviceManager.getDevices();
        mGBDeviceAdapter = new GBDeviceAdapter(this, deviceList);
        deviceListView.setAdapter(this.mGBDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                GBDevice gbDevice = mGBDeviceAdapter.getItem(position);
                if (gbDevice.isInitialized()) {
                    DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
                    Class<? extends Activity> primaryActivity = coordinator.getPrimaryActivity();
                    if (primaryActivity != null) {
                        Intent startIntent = new Intent(ControlCenter.this, primaryActivity);
                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice);
                        startActivity(startIntent);
                    }
                } else {
                    GBApplication.deviceService().connect(gbDevice);
                }
            }
        });

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.controlcenter_swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchActivityData();
            }
        });

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        refreshPairedDevices();
        /*
         * Ask for permission to intercept notifications on first run.
         */
        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("firstrun", true)) {
            prefs.getPreferences().edit().putBoolean("firstrun", false).apply();
            Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(enableIntent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }

        GBApplication.deviceService().start();

        enableSwipeRefresh(deviceManager.getSelectedDevice());
        if (GB.isBluetoothEnabled() && deviceList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startActivity(new Intent(this, DiscoveryActivity.class));
        } else {
            GBApplication.deviceService().requestDeviceInfo();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedDevice = mGBDeviceAdapter.getItem(acmi.position);
        if (selectedDevice != null && selectedDevice.isBusy()) {
            // no context menu when device is busy
            return;
        }
        getMenuInflater().inflate(R.menu.controlcenter_context, menu);

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(selectedDevice);
        if (!coordinator.supportsActivityDataFetching()) {
            menu.removeItem(R.id.controlcenter_fetch_activity_data);
        }
        if (!coordinator.supportsScreenshots()) {
            menu.removeItem(R.id.controlcenter_take_screenshot);
        }
        if (!coordinator.supportsAlarmConfiguration()) {
            menu.removeItem(R.id.controlcenter_configure_alarms);
        }

        if (selectedDevice.getState() == GBDevice.State.NOT_CONNECTED) {
            menu.removeItem(R.id.controlcenter_disconnect);
        }
        if (!selectedDevice.isInitialized()) {
            menu.removeItem(R.id.controlcenter_find_device);
            menu.removeItem(R.id.controlcenter_fetch_activity_data);
            menu.removeItem(R.id.controlcenter_configure_alarms);
            menu.removeItem(R.id.controlcenter_take_screenshot);
        }

        menu.setHeaderTitle(selectedDevice.getName());
    }

    private void enableSwipeRefresh(GBDevice device) {
        if (device == null) {
            swipeLayout.setEnabled(false);
        } else {
            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
            boolean enable = coordinator.allowFetchActivityData(device);
            swipeLayout.setEnabled(enable);
        }
    }

    private void fetchActivityData() {
        GBDevice selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            return;
        }
        if (selectedDevice.isInitialized()) {
            GBApplication.deviceService().onFetchActivityData();
        } else {
            swipeLayout.setRefreshing(false);
            GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.controlcenter_start_sleepmonitor:
                if (selectedDevice != null) {
                    Intent startIntent;
                    startIntent = new Intent(ControlCenter.this, ChartsActivity.class);
                    startIntent.putExtra(GBDevice.EXTRA_DEVICE, selectedDevice);
                    startActivity(startIntent);
                }
                return true;
            case R.id.controlcenter_fetch_activity_data:
                fetchActivityData();
                return true;
            case R.id.controlcenter_disconnect:
                if (selectedDevice != null) {
                    selectedDevice = null;
                    GBApplication.deviceService().disconnect();
                }
                return true;
            case R.id.controlcenter_find_device:
                if (selectedDevice != null) {
                    findDevice(true);
                    ProgressDialog.show(
                            this,
                            getString(R.string.control_center_find_lost_device),
                            getString(R.string.control_center_cancel_to_stop_vibration),
                            true, true,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    findDevice(false);
                                }
                            });
                }
                return true;
            case R.id.controlcenter_configure_alarms:
                if (selectedDevice != null) {
                    Intent startIntent;
                    startIntent = new Intent(ControlCenter.this, ConfigureAlarms.class);
                    startActivity(startIntent);
                }
                return true;
            case R.id.controlcenter_take_screenshot:
                if (selectedDevice != null) {
                    GBApplication.deviceService().onScreenshotReq();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void findDevice(boolean start) {
        GBApplication.deviceService().onFindDevice(start);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_debug:
                Intent debugIntent = new Intent(this, DebugActivity.class);
                startActivity(debugIntent);
                return true;
            case R.id.action_quit:
                GBApplication.deviceService().quit();

                Intent quitIntent = new Intent(GBApplication.ACTION_QUIT);
                LocalBroadcastManager.getInstance(this).sendBroadcast(quitIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(this, DiscoveryActivity.class));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void refreshPairedDevices() {
        List<GBDevice> deviceList = deviceManager.getDevices();
        GBDevice connectedDevice = null;

        for (GBDevice device : deviceList) {
            if (device.isConnected() || device.isConnecting()) {
                connectedDevice = device;
                break;
            }
        }

        if (deviceList.isEmpty()) {
            background.setVisibility(View.VISIBLE);
        } else {
            background.setVisibility(View.INVISIBLE);
        }

        if (connectedDevice != null) {
            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(connectedDevice);
            hintTextView.setText(coordinator.getTapString());
        } else if (!deviceList.isEmpty()) {
            hintTextView.setText(R.string.tap_a_device_to_connect);
        }

        mGBDeviceAdapter.notifyDataSetChanged();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermissions() {
        List<String> wantedPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.BLUETOOTH);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_CONTACTS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.CALL_PHONE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_PHONE_STATE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add(Manifest.permission.READ_CALENDAR);
        if (ContextCompat.checkSelfPermission(this, "com.fsck.k9.permission.READ_MESSAGES") == PackageManager.PERMISSION_DENIED)
            wantedPermissions.add("com.fsck.k9.permission.READ_MESSAGES");

        if (!wantedPermissions.isEmpty())
            ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[wantedPermissions.size()]), 0);
    }


}
