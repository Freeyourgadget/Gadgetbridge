package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BLEScanService extends Service {
    public static final String COMMAND_SCAN_DEVICE = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.START_SCAN_FOR_DEVICE";
    public static final String COMMAND_START_SCAN_ALL = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.START_SCAN_ALL";
    public static final String COMMAND_STOP_SCAN_ALL = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.STOP_SCAN_ALL";

    public static final String EVENT_DEVICE_FOUND = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.event.DEVICE_FOUND";

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";

    // 5 minutes scan restart interval
    private final int DELAY_SCAN_RESTART = 5 * 60 * 1000;

    private LocalBroadcastManager localBroadcastManager;
    private NotificationManager notificationManager;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner scanner;

    private Logger LOG = LoggerFactory.getLogger(getClass());
    // private final ArrayList<ScanFilter> currentFilters = new ArrayList<>();

    private enum ScanningState {
        NOT_SCANNING,
        SCANNING_WITHOUT_FILTERS,
        SCANNING_WITH_FILTERS;

        public boolean isDoingAnyScan(){
            return ordinal() > NOT_SCANNING.ordinal();
        }

        public boolean shouldDiscardAfterFirstMatch(){
            return this == SCANNING_WITH_FILTERS;
        }
    };
    private ScanningState currentState = ScanningState.NOT_SCANNING;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            LOG.debug("onScanResult: " + result);

            Intent intent = new Intent(EVENT_DEVICE_FOUND);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
            localBroadcastManager.sendBroadcast(intent);

            // device found, attempt connection
            // stop scanning for device for now
            // will restart when connection attempt fails
            if(currentState.shouldDiscardAfterFirstMatch()) {
                // stopScanningForDevice(device.getAddress());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            LOG.error("onScanFailed: " + errorCode);

            updateNotification("Scan failed: " + errorCode);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        scanner = bluetoothManager.getAdapter().getBluetoothLeScanner();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        registerReceivers();

        this.startForeground();

        if(scanner == null){
            updateNotification("Waiting for bluetooth...");
        }else{
            restartScan(true);
        }

        // schedule after 5 seconds to fix weird timing of both services
        scheduleRestartScan(5000);
    }

    private void scheduleRestartScan(){
        scheduleRestartScan(DELAY_SCAN_RESTART);
    }

    private void scheduleRestartScan(long millis){
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            LOG.debug("restarting scan...");
            try {
                restartScan(true);
            }catch (Exception e){
                LOG.error("error during scheduled scan restart", e);
            }
            scheduleRestartScan();
        }, millis);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    private void updateNotification(boolean isScanning, int scannedDeviceCount){
        notificationManager.notify(
                GB.NOTIFICATION_ID_SCAN,
                createNotification(isScanning, scannedDeviceCount)
        );
    }

    private void updateNotification(String content){
        notificationManager.notify(
                GB.NOTIFICATION_ID_SCAN,
                createNotification(content, R.drawable.ic_bluetooth)
        );
    }

    private Notification createNotification(boolean isScanning, int scannedDevicesCount){
        int icon = R.drawable.ic_bluetooth;
        String content = "Not scanning";
        if(isScanning){
            icon = R.drawable.ic_bluetooth_searching;
            if(scannedDevicesCount == 1) {
                content = String.format("Scanning %d device", scannedDevicesCount);
            }else if(scannedDevicesCount > 1){
                content = String.format("Scanning %d devices", scannedDevicesCount);
            }else{
                content = "Scanning all devices";
            }
        }

        return createNotification(content, icon);
    }

    private Notification createNotification(String content, int icon){

        return new NotificationCompat
                .Builder(this, GB.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Scan service")
                .setContentText(content)
                .setSmallIcon(icon)
                .build();
    }

    private void startForeground(){
        Notification serviceNotification = createNotification(false, 0);

        super.startForeground(GB.NOTIFICATION_ID_SCAN, serviceNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null){
            return START_STICKY;
        }
        String action = intent.getAction();
        if(action == null){
            return START_STICKY;
        }
        switch (action) {
            case COMMAND_SCAN_DEVICE:
                handleScanDevice(intent);
                break;
            case COMMAND_START_SCAN_ALL:
                handleScanAll(intent);
                break;
            case COMMAND_STOP_SCAN_ALL:
                handleStopScanAll(intent);
                break;
            default:
                return START_STICKY;
        }
        return START_STICKY;
    }

    private void handleStopScanAll(Intent intent){
        restartScan(true);
    }

    private void handleScanAll(Intent intent){
        if(currentState != ScanningState.SCANNING_WITHOUT_FILTERS){
            restartScan(false);
        }
    }

    private void handleScanDevice(Intent intent){
        /*
        GBDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        if(device == null){
            return;
        }
        scanForDevice(device);
         */
        restartScan(true);
    }


    /*private boolean isDeviceIncludedInCurrentFilters(GBDevice device){
        for(ScanFilter currentFilter : currentFilters){
            if(device.getAddress().equals(currentFilter.getDeviceAddress())){
                return true;
            }
        }
        return false;
    }
    */

    /*
    private void stopScanningForDevice(GBDevice device){
        this.stopScanningForDevice(device.getAddress());
    }
     */

    /*
    private void stopScanningForDevice(String deviceAddress){
        currentFilters.removeIf(scanFilter -> scanFilter
                .getDeviceAddress()
                .equals(deviceAddress)
        );

        restartScan(true);
    }
    */

    /*
    private void scanForDevice(GBDevice device){
        if(isDeviceIncludedInCurrentFilters(device)){
            // already scanning for device
            return;
        }
        ScanFilter deviceFilter = new ScanFilter.Builder()
                .setDeviceAddress(device.getAddress())
                .build();

        currentFilters.add(deviceFilter);

        // restart scan here
        restartScan(true);
    }
    */

    BroadcastReceiver deviceStateUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice.DeviceUpdateSubject subject =
                        (GBDevice.DeviceUpdateSubject)
                                intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

                if (subject != GBDevice.DeviceUpdateSubject.CONNECTION_STATE) {
                    return;
                }
                restartScan(true);
                return;
            }
            if(GBApplication.ACTION_QUIT.equals(intent.getAction())){
                LOG.debug("stopping scan service...");
                if(currentState.isDoingAnyScan()){
                    scanner.stopScan(scanCallback);
                }
                stopSelf();
                return;
            }
        }
    };

    BroadcastReceiver bluetoothStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null){
                return;
            }
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch(state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    updateNotification("Waiting for bluetooth...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    restartScan(true);
                    break;
            }
        }
    };

    private void registerReceivers(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filter.addAction(GBApplication.ACTION_QUIT);
        localBroadcastManager.registerReceiver(
                deviceStateUpdateReceiver,
                filter
        );

        registerReceiver(
                bluetoothStateChangedReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        );
    }

    private void unregisterReceivers(){
        localBroadcastManager.unregisterReceiver(deviceStateUpdateReceiver);

        unregisterReceiver(bluetoothStateChangedReceiver);
    }

    private void restartScan(boolean applyFilters){
        if(scanner == null){
            scanner = bluetoothManager.getAdapter().getBluetoothLeScanner();
        }
        if(scanner == null){
            // at this point we should already be waiting for bluetooth to turn back on
            LOG.debug("cannot enable scan since bluetooth seems off (scanner == null)");
            return;
        }
        if(bluetoothManager.getAdapter().getState() != BluetoothAdapter.STATE_ON){
            // again, we should be waiting for the adapter to turn on again
            LOG.debug("Bluetooth adapter state off");
            return;
        }
        if(currentState.isDoingAnyScan()){
            scanner.stopScan(scanCallback);
        }
        ArrayList<ScanFilter> scanFilters = null;

        if(applyFilters) {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();

            scanFilters = new ArrayList<>(devices.size());

            for (GBDevice device : devices) {
                if (device.getState() == GBDevice.State.WAITING_FOR_SCAN) {
                    scanFilters.add(new ScanFilter.Builder()
                            .setDeviceAddress(device.getAddress())
                            .build()
                    );
                }
            }

            if(scanFilters.size() == 0){
                // no need to start scanning
                LOG.debug("restartScan: stopping BLE scan, no devices");
                currentState = ScanningState.NOT_SCANNING;
                updateNotification(false, 0);
                return;
            }
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // enforced anyway in background
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setLegacy(false)
                .build();

        scanner.startScan(scanFilters, scanSettings, scanCallback);
        if(applyFilters) {
            LOG.debug("restartScan: started scan for " + scanFilters.size() + " devices");
            updateNotification(true, scanFilters.size());
            currentState = ScanningState.SCANNING_WITH_FILTERS;
        }else{
            LOG.debug("restartScan: started scan for all devices");
            updateNotification(true, 0);
            currentState = ScanningState.SCANNING_WITHOUT_FILTERS;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}