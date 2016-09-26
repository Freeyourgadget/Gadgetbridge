package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class DiscoveryActivity extends GBActivity implements AdapterView.OnItemClickListener {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivity.class);
    private static final long SCAN_DURATION = 60000; // 60s

    private ScanCallback newLeScanCallback = null;

    private final Handler handler = new Handler();

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    discoveryStarted(Scanning.SCANNING_BT);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // continue with LE scan, if available
                    if (isScanning == Scanning.SCANNING_BT) {
                        checkAndRequestLocationPermission();
                        if (GBApplication.isRunningLollipopOrLater()) {
                            startDiscovery(Scanning.SCANNING_NEW_BTLE);
                        } else {
                            startDiscovery(Scanning.SCANNING_BTLE);
                        }
                    } else {
                        discoveryFinished();
                    }
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    bluetoothStateChanged(oldState, newState);
                    break;
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    handleDeviceFound(device, rssi);
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && device.getAddress().equals(bondingAddress)) {
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            GB.toast(DiscoveryActivity.this, "Successfully bonded with: " + bondingAddress, Toast.LENGTH_SHORT, GB.INFO);
                            finish();
                        }
                    }
                }
            }
        }
    };

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            LOG.warn(device.getName() + ": " + ((scanRecord != null) ? scanRecord.length : -1));
            logMessageContent(scanRecord);
            handleDeviceFound(device, (short) rssi);
        }
    };


    // why use a method to to get callback?
    // because this callback need API >= 21
    // we cant add @TARGETAPI("Lollipop") at class header
    // so use a method woth SDK check to return this callback
    private ScanCallback getScanCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            newLeScanCallback = new ScanCallback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    try {
                        LOG.warn(result.getDevice().getName() + ": " +
                                ((result.getScanRecord() != null) ? result.getScanRecord().getBytes().length : -1));
                        //logMessageContent(result.getScanRecord().getBytes());
                        handleDeviceFound(result.getDevice(), (short) result.getRssi());
                    } catch (NullPointerException e) {
                        LOG.warn("Error handling scan result", e);
                    }
                }
            };
        }
        return newLeScanCallback;
    }

    public void logMessageContent(byte[] value) {
        if (value != null) {
            for (byte b : value) {
                LOG.warn("DATA: " + String.format("0x%2x", b) + " - " + (char) (b & 0xff));
            }
        }
    }

    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopDiscovery();
        }
    };

    private ProgressBar progressView;
    private BluetoothAdapter adapter;
    private final ArrayList<GBDeviceCandidate> deviceCandidates = new ArrayList<>();
    private DeviceCandidateAdapter cadidateListAdapter;
    private Button startButton;
    private Scanning isScanning = Scanning.SCANNING_OFF;
    private String bondingAddress;

    private enum Scanning {
        SCANNING_BT,
        SCANNING_BTLE,
        SCANNING_NEW_BTLE,
        SCANNING_OFF
    }

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
        ListView deviceCandidatesView = (ListView) findViewById(R.id.discovery_deviceCandidatesView);

        cadidateListAdapter = new DeviceCandidateAdapter(this, deviceCandidates);
        deviceCandidatesView.setAdapter(cadidateListAdapter);
        deviceCandidatesView.setOnItemClickListener(this);

        IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
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
                deviceCandidates.add((GBDeviceCandidate) p);
            }
        }
    }

    public void onStartButtonClick(View button) {
        LOG.debug("Start Button clicked");
        if (isScanning()) {
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

    private void handleDeviceFound(BluetoothDevice device, short rssi) {
        LOG.debug("found device: " + device.getName() + ", " + device.getAddress());
        if (LOG.isDebugEnabled()) {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids != null && uuids.length > 0) {
                for (ParcelUuid uuid : uuids) {
                    LOG.debug("  supports uuid: " + uuid.toString());
                }
            }
        }
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            return; // ignore already bonded devices
        }

        GBDeviceCandidate candidate = new GBDeviceCandidate(device, rssi);
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

    /**
     * Pre: bluetooth is available, enabled and scanning is off.
     * Post: BT is discovering
     */
    private void startDiscovery() {
        if (isScanning()) {
            LOG.warn("Not starting discovery, because already scanning.");
            return;
        }
        startDiscovery(Scanning.SCANNING_BT);
    }

    private void startDiscovery(Scanning what) {
        LOG.info("Starting discovery: " + what);
        discoveryStarted(what); // just to make sure
        if (ensureBluetoothReady()) {
            if (what == Scanning.SCANNING_BT) {
                startBTDiscovery();
            } else if (what == Scanning.SCANNING_BTLE) {
                if (GB.supportsBluetoothLE()) {
                    startBTLEDiscovery();
                } else {
                    discoveryFinished();
                }
            } else if (what == Scanning.SCANNING_NEW_BTLE) {
                if (GB.supportsBluetoothLE()) {
                    startNEWBTLEDiscovery();
                } else  {
                    discoveryFinished();
                }
            }
        } else {
            discoveryFinished();
            Toast.makeText(this, "Enable Bluetooth to discover devices.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isScanning() {
        return isScanning != Scanning.SCANNING_OFF;
    }

    private void stopDiscovery() {
        LOG.info("Stopping discovery");
        if (isScanning()) {
            Scanning wasScanning = isScanning;
            // unfortunately, we don't always get a call back when stopping the scan, so
            // we do it manually; BEFORE stopping the scan!
            discoveryFinished();

            if (wasScanning == Scanning.SCANNING_BT) {
                stopBTDiscovery();
            } else if (wasScanning == Scanning.SCANNING_BTLE) {
                stopBTLEDiscovery();
            } else if (wasScanning == Scanning.SCANNING_NEW_BTLE) {
                stopNewBTLEDiscovery();
            }
            handler.removeMessages(0, stopRunnable);
        }
    }

    private void stopBTLEDiscovery() {
        adapter.stopLeScan(leScanCallback);
    }

    private void stopBTDiscovery() {
        adapter.cancelDiscovery();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopNewBTLEDiscovery() {
        adapter.getBluetoothLeScanner().stopScan(newLeScanCallback);
    }

    private void bluetoothStateChanged(int oldState, int newState) {
        discoveryFinished();
        if (newState == BluetoothAdapter.STATE_ON) {
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            startButton.setEnabled(true);
        } else {
            this.adapter = null;
            startButton.setEnabled(false);
        }
    }

    private void discoveryFinished() {
        isScanning = Scanning.SCANNING_OFF;
        progressView.setVisibility(View.GONE);
        startButton.setText(getString(R.string.discovery_start_scanning));
    }

    private void discoveryStarted(Scanning what) {
        isScanning = what;
        progressView.setVisibility(View.VISIBLE);
        startButton.setText(getString(R.string.discovery_stop_scanning));
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
            LOG.warn("No bluetooth available");
            this.adapter = null;
            return false;
        }
        BluetoothAdapter adapter = bluetoothService.getAdapter();
        if (adapter == null) {
            LOG.warn("No bluetooth available");
            this.adapter = null;
            return false;
        }
        if (!adapter.isEnabled()) {
            LOG.warn("Bluetooth not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            this.adapter = null;
            return false;
        }
        this.adapter = adapter;
        return true;
    }

    // New BTLE Discovery use startScan (List<ScanFilter> filters,
    //                                  ScanSettings settings,
    //                                  ScanCallback callback)
    // Its added on API21
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startNEWBTLEDiscovery() {
        // Only use new APi when user use Lollipop+ device
            LOG.info("Start New BTLE Discovery");
            handler.removeMessages(0, stopRunnable);
            handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);
            adapter.getBluetoothLeScanner().startScan(null, getScanSettings(), getScanCallback());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings getScanSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new ScanSettings.Builder()
                    .setScanMode(SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(MATCH_MODE_STICKY)
                    .build();
        } else {
            return new ScanSettings.Builder()
                    .setScanMode(SCAN_MODE_LOW_LATENCY)
                    .build();
        }
    }

    private void startBTLEDiscovery() {
        LOG.info("Starting BTLE Discovery");
        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);
        adapter.startLeScan(leScanCallback);
    }

    private void startBTDiscovery() {
        LOG.info("Starting BT Discovery");
        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);
        adapter.startDiscovery();
    }

    private void checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    private Message getPostMessage(Runnable runnable) {
        Message m = Message.obtain(handler, runnable);
        m.obj = runnable;
        return m;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return;
        }

        stopDiscovery();
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        Class<? extends Activity> pairingActivity = coordinator.getPairingActivity();
        if (pairingActivity != null) {
            Intent intent = new Intent(this, pairingActivity);
            intent.putExtra(DeviceCoordinator.EXTRA_DEVICE_MAC_ADDRESS, deviceCandidate.getMacAddress());
            startActivity(intent);
        } else {
            try {
                BluetoothDevice btDevice = adapter.getRemoteDevice(deviceCandidate.getMacAddress());
                if (btDevice.createBond()) {
                    // async, wait for bonding event to finish this activity
                    bondingAddress = btDevice.getAddress();
                }
            } catch (Exception e) {
                LOG.error("Error pairing device: " + deviceCandidate.getMacAddress());
            }
        }
    }
}
