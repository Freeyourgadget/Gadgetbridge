package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

// TODO: support option for a single reminder notification when notifications could not be delivered?
// conditions: app was running and received notifications, but device was not connected.
// maybe need to check for "unread notifications" on device for that.
public abstract class AbstractDeviceSupport implements DeviceSupport {
    private GBDevice gbDevice;
    private BluetoothAdapter btAdapter;
    private Context context;

    public void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    @Override
    public boolean isConnected() {
        return gbDevice.isConnected();
    }
    
    protected boolean isInitialized() {
        return gbDevice.isInitialized();
    }
    
    @Override
    public GBDevice getDevice() {
        return gbDevice;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    @Override
    public Context getContext() {
        return context;
    }
}
