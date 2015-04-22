package nodomain.freeyourgadget.gadgetbridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ControlCenter extends Activity {


    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.quit";

    public static final String ACTION_REFRESH_DEVICELIST
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.set_version";

    TextView hintTextView;
    ListView deviceListView;
    GBDeviceAdapter mGBDeviceAdapter;
    final List<GBDevice> deviceList = new ArrayList<>();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_QUIT)) {
                finish();
            } else if (action.equals(ACTION_REFRESH_DEVICELIST)) {
                refreshPairedDevices();
            } else if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice dev = intent.getParcelableExtra("device");
                if (dev.getAddress() != null) {
                    int index = deviceList.indexOf(dev); // search by address
                    if (index >= 0) {
                        deviceList.set(index, dev);
                    } else {
                        deviceList.add(dev);
                    }
                }
                refreshPairedDevices();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenter);
        hintTextView = (TextView) findViewById(R.id.hintTextView);
        deviceListView = (ListView) findViewById(R.id.deviceListView);
        mGBDeviceAdapter = new GBDeviceAdapter(this, deviceList);
        deviceListView.setAdapter(this.mGBDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                if (deviceList.get(position).isConnected()) {
                    Intent startIntent = new Intent(ControlCenter.this, AppManagerActivity.class);
                    startActivity(startIntent);
                } else {
                    Intent startIntent = new Intent(ControlCenter.this, BluetoothCommunicationService.class);
                    startIntent.setAction(BluetoothCommunicationService.ACTION_CONNECT);
                    startIntent.putExtra("device_address", deviceList.get(position).getAddress());

                    startService(startIntent);
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_QUIT);
        filter.addAction(ACTION_REFRESH_DEVICELIST);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        refreshPairedDevices();
        /*
         * Ask for permission to intercept notifications on first run.
         */
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("firstrun", true)) {
            sharedPrefs.edit().putBoolean("firstrun", false).commit();
            Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(enableIntent);
        }
        Intent startIntent = new Intent(this, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_START);
        startService(startIntent);

        Intent versionInfoIntent = new Intent(this, BluetoothCommunicationService.class);
        versionInfoIntent.setAction(BluetoothCommunicationService.ACTION_REQUEST_VERSIONINFO);
        startService(versionInfoIntent);
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
                Intent stopIntent = new Intent(this, BluetoothCommunicationService.class);
                stopService(stopIntent);

                Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);
                LocalBroadcastManager.getInstance(this).sendBroadcast(quitIntent);
                return true;
            case R.id.action_refresh:
                refreshPairedDevices();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void refreshPairedDevices() {
        boolean connected = false;
        List<GBDevice> availableDevices = new ArrayList<>();
        for (GBDevice device : deviceList) {
            if (device.isConnected() || device.isConnecting()) {
                connected = true;
                availableDevices.add(device);
            }
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
        } else if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
        } else {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice pairedDevice : pairedDevices) {
                GBDevice.Type deviceType;
                if (pairedDevice.getName().indexOf("Pebble") == 0) {
                    deviceType = GBDevice.Type.PEBBLE;
                } else if (pairedDevice.getName().equals("MI")) {
                    deviceType = GBDevice.Type.MIBAND;
                } else {
                    continue;
                }
                GBDevice device = new GBDevice(pairedDevice.getAddress(), pairedDevice.getName(), deviceType);
                if (!availableDevices.contains(device)) {
                    availableDevices.add(device);
                }
            }

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String miAddr = sharedPrefs.getString("development_miaddr", null);
            if (miAddr != null && miAddr.length() > 0) {
                GBDevice miDevice = new GBDevice(miAddr, "MI", GBDevice.Type.MIBAND);
                if (!availableDevices.contains(miDevice)) {
                    availableDevices.add(miDevice);
                }
            }
            deviceList.retainAll(availableDevices);
            for (GBDevice dev : availableDevices) {
                if (!deviceList.contains(dev)) {
                    deviceList.add(dev);
                }
            }

            if (connected) {
                hintTextView.setText("tap connected device for App Mananger");
            } else if (!deviceList.isEmpty()) {
                hintTextView.setText("tap a device to connect");
            }
        }
        mGBDeviceAdapter.notifyDataSetChanged();
    }
}
