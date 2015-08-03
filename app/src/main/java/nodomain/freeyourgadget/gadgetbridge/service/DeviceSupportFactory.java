package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DeviceSupportFactory {
    private final BluetoothAdapter mBtAdapter;
    private Context mContext;

    public DeviceSupportFactory(Context context) {
        mContext = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized DeviceSupport createDeviceSupport(String deviceAddress) throws GBException {
        DeviceSupport deviceSupport = createBTDeviceSupport(deviceAddress);
        if (deviceSupport != null) {
            return deviceSupport;
        }
        // support for other kinds of transports

        // no device found, check transport availability and warn
        checkBtAvailability();
        return null;
    }

    private void checkBtAvailability() {
        if (mBtAdapter == null) {
            GB.toast(mContext.getString(R.string.bluetooth_is_not_supported_), Toast.LENGTH_SHORT, GB.WARN);
        } else if (!mBtAdapter.isEnabled()) {
            GB.toast(mContext.getString(R.string.bluetooth_is_disabled_), Toast.LENGTH_SHORT, GB.WARN);
        }
    }

    private DeviceSupport createBTDeviceSupport(String deviceAddress) throws GBException {
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            GBDevice gbDevice = null;
            DeviceSupport deviceSupport = null;

            try {
                BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(deviceAddress);
                if (btDevice.getName() == null || btDevice.getName().equals("MI")) { //FIXME: workaround for Miband not being paired
                    gbDevice = new GBDevice(deviceAddress, "MI", DeviceType.MIBAND);
                    deviceSupport = new MiBandSupport();
                } else if (btDevice.getName().indexOf("Pebble") == 0) {
                    gbDevice = new GBDevice(deviceAddress, btDevice.getName(), DeviceType.PEBBLE);
                    deviceSupport = new PebbleSupport();
                }
                if (deviceSupport != null) {
                    deviceSupport = new ServiceDeviceSupport(deviceSupport);
                    deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
                    return deviceSupport;
                }
            } catch (Exception e) {
                throw new GBException(mContext.getString(R.string.cannot_connect_bt_address_invalid_, e));
            }
        }
        return null;
    }
}
