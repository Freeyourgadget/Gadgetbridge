package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public interface DeviceSupport extends EventHandler {
    public void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context);

    public boolean connect();

    public GBDevice getDevice();

    public BluetoothAdapter getBluetoothAdapter();

    public Context getContext();

    public void dispose();
}
