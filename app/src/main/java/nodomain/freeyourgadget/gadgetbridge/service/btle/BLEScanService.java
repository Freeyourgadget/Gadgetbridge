package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class BLEScanService extends Service {
    public final String ACTION_SCAN_DEVICE = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.SCAN_FOR_DEVICE";

    public final String EXTRA_DEVICE = "EXTRA_DEVICE";

    private BluetoothLeScanner scanner;
    private ArrayList<ScanFilter> currentFilters = new ArrayList<>();
    private boolean isScanning = false;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            // device found, attempt connection
            // stop scanning for device for now
            // will restart when connection attempt fails
            stopScanningForDevice(device.getAddress());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        scanner = manager.getAdapter().getBluetoothLeScanner();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null){
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if(action == null){
         return START_NOT_STICKY;
        }
        switch (action) {
            case ACTION_SCAN_DEVICE:
                handleScanDevice(intent);
                break;

            default:
                return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void handleScanDevice(Intent intent){
        GBDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        if(device == null){
            return;
        }
    }

    private boolean isDeviceIncludedInCurrentFilters(GBDevice device){
        for(ScanFilter currentFilter : currentFilters){
            if(device.getAddress() == currentFilter.getDeviceAddress()){
                return true;
            }
        }
        return false;
    }

    private void stopScanningForDevice(GBDevice device){

    }

    private void stopScanningForDevice(String deviceAddress){
        currentFilters.removeIf(scanFilter -> scanFilter
                .getDeviceAddress()
                .equals(deviceAddress)
        );

        restartScan();
    }

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
        restartScan();
    }

    private void restartScan(){
        if(isScanning){
            scanner.stopScan(scanCallback);
        }

        if(currentFilters.size() == 0){
            // not sure if this is the right place to check for this
            // yet, if there is not device to scan for, there is no need to start a new scan
            isScanning = false;
            return;
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // enforced anyway in background
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setLegacy(false)
                .build();
        scanner.startScan(currentFilters, scanSettings, scanCallback);
        isScanning = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}