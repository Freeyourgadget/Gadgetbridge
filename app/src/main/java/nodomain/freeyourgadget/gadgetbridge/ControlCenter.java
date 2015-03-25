package nodomain.freeyourgadget.gadgetbridge;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;

public class ControlCenter extends Activity {


    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.quit";

    public static final String ACTION_REFRESH_DEVICELIST
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.set_version";

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
                String deviceAddress = intent.getStringExtra("device_address");
                GBDevice.State state = GBDevice.State.values()[intent.getIntExtra("device_state", 0)];
                String firmwareVersion = intent.getStringExtra("firmware_version");
                if (deviceList.isEmpty()) {
                    refreshPairedDevices();
                    mGBDeviceAdapter.notifyDataSetChanged();
                }
                if (deviceAddress != null) {
                    for (GBDevice device : deviceList) {
                        if (device.getAddress().equals(deviceAddress)) {
                            device.setFirmwareVersion(firmwareVersion);
                            device.setState(state);
                            mGBDeviceAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenter);

        deviceListView = (ListView) findViewById(R.id.deviceListView);
        mGBDeviceAdapter = new GBDeviceAdapter(this, deviceList);
        deviceListView.setAdapter(this.mGBDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                if (deviceList.get(position).getState() == GBDevice.State.CONNECTED) {
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
        registerReceiver(mReceiver, filter);

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_debug) {
            Intent intent = new Intent(this, DebugActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_quit) {
            Intent stopIntent = new Intent(this, BluetoothCommunicationService.class);
            stopService(stopIntent);

            Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);
            sendBroadcast(quitIntent);
        } else if (id == R.id.action_refresh) {
            if (deviceList.isEmpty()) {
                refreshPairedDevices();
                mGBDeviceAdapter.notifyDataSetChanged();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void refreshPairedDevices() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
        } else if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
        } else {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().indexOf("Pebble") == 0) {
                    // Matching device found
                    deviceList.add(new GBDevice(device.getAddress(), device.getName()));
                }
            }
        }
    }

}
