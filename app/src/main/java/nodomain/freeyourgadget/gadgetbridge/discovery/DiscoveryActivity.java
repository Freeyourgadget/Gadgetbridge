package nodomain.freeyourgadget.gadgetbridge.discovery;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;

public class DiscoveryActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = "DiscoveryAct";

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    discoveryStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    discoveryFinished();
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    bluetoothStateChanged(oldState, newState);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    break;
            }
        }
    };

    private void bluetoothStateChanged(int oldState, int newState) {
        discoveryFinished();
        startButton.setEnabled(newState == BluetoothAdapter.STATE_ON);
    }

    private void discoveryFinished() {
        isScanning = false;
        progressView.setVisibility(View.GONE);
        startButton.setText(getString(R.string.discovery_start_scanning));
    }

    private void discoveryStarted() {
        isScanning = true;
        progressView.setVisibility(View.VISIBLE);
        startButton.setText(getString(R.string.discovery_stop_scanning));
    }

    private ProgressBar progressView;
    private BluetoothAdapter adapter;
    private ArrayList<DeviceCandidate> deviceCandidates = new ArrayList<>();
    private ListView deviceCandidatesView;
    private DeviceCandidateAdapter cadidateListAdapter;
    private Button startButton;
    private boolean isScanning;
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            DeviceCandidate candidate = new DeviceCandidate(device, (short) rssi);
            if (DeviceHelper.getInstance().isSupported(candidate)) {
                int index = deviceCandidates.indexOf(candidate);
                if (index >= 0) {
                    deviceCandidates.set(index, candidate); // replace
                } else {
                    deviceCandidates.add(candidate);
                }
                cadidateListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_discovery);
        startButton = (Button) findViewById(R.id.discovery_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartButtonClick(startButton);
            }
        });

        progressView = (ProgressBar) findViewById(R.id.discovery_progressbar);
        progressView.setProgress(0);
        progressView.setIndeterminate(true);
        progressView.setVisibility(View.GONE);
        deviceCandidatesView = (ListView) findViewById(R.id.discovery_deviceCandidatesView);

        cadidateListAdapter = new DeviceCandidateAdapter(this, deviceCandidates);
        deviceCandidatesView.setAdapter(cadidateListAdapter);
        deviceCandidatesView.setOnItemClickListener(this);

        IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(bluetoothReceiver, bluetoothIntents);

        startDiscovery();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("deviceCandidates", deviceCandidates);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Parcelable> restoredCandidates = savedInstanceState.getParcelableArrayList("deviceCandidates");
        if (restoredCandidates != null) {
            deviceCandidates.clear();
            for (Parcelable p : restoredCandidates) {
                deviceCandidates.add((DeviceCandidate) p);
            }
        }
    }

    public void onStartButtonClick(View button) {
        Log.d(TAG, "Start Button clicked");
        if (isScanning) {
            stopDiscovery();
        } else {
            startDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bluetoothReceiver);
        super.onDestroy();
    }

    /**
     * Pre: bluetooth is available, enabled and scanning is off
     */
    private void startDiscovery() {
        Log.i(TAG, "Starting discovery...");
        discoveryStarted(); // just to make sure
        if (ensureBluetoothReady()) {
            startBLEDiscovery();
        } else {
            discoveryFinished();
            Toast.makeText(this, "Enable Bluetooth to discover devices.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopDiscovery() {
        if (isScanning) {
            adapter.stopLeScan(leScanCallback);
            // unfortunately, we never get a call back when stopping the scan, so
            // we do it manually:
            discoveryFinished();
        }
    }

    private boolean ensureBluetoothReady() {
        boolean available = checkBluetoothAvailable();
        startButton.setEnabled(available);
        if (available) {
            adapter.cancelDiscovery();
            // must not return the result of cancelDiscovery()
            // appears to return false when currently not scanning
            return true;
        }
        return false;
    }

    private boolean checkBluetoothAvailable() {
        BluetoothManager bluetoothService = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothService == null) {
            Log.w(TAG, "No bluetooth available");
            this.adapter = null;
            return false;
        }
        BluetoothAdapter adapter = bluetoothService.getAdapter();
        if (!adapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled");
            this.adapter = null;
            return false;
        }
        this.adapter = adapter;
        return true;
    }

    private void startBLEDiscovery() {
        adapter.startLeScan(leScanCallback);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            Log.e(TAG, "Device candidate clicked, but item not found");
            return;
        }

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        Intent intent = new Intent(this, coordinator.getPairingActivity());
        intent.putExtra(DeviceCoordinator.EXTRA_DEVICE_MAC_ADDRESS, deviceCandidate.getMacAddress());
        startActivity(intent);
    }
}
