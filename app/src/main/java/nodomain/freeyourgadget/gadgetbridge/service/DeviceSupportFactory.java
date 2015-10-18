package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.EnumSet;

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
        DeviceSupport deviceSupport;
        int indexFirstColon = deviceAddress.indexOf(":");
        if (indexFirstColon > 0) {
            if (indexFirstColon == deviceAddress.lastIndexOf(":")) { // only one colon
                deviceSupport = createTCPDeviceSupport(deviceAddress);
            } else {
                // multiple colons -- bt?
                deviceSupport = createBTDeviceSupport(deviceAddress);
            }
        } else {
            // no colon at all, maybe a class name?
            deviceSupport = createClassNameDeviceSupport(deviceAddress);
        }

        if (deviceSupport != null) {
            return deviceSupport;
        }

        // no device found, check transport availability and warn
        checkBtAvailability();
        return null;
    }

    private DeviceSupport createClassNameDeviceSupport(String className) throws GBException {
        try {
            Class<?> deviceSupportClass = Class.forName(className);
            Constructor<?> constructor = deviceSupportClass.getConstructor();
            DeviceSupport support = (DeviceSupport) constructor.newInstance();
            // has to create the device itself
            support.setContext(null, null, mContext);
            return support;
        } catch (ClassNotFoundException e) {
            return null; // not a class, or not known at least
        } catch (Exception e) {
            throw new GBException("Error creating DeviceSupport instance for " + className, e);
        }
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
                if (btDevice.getName() == null || btDevice.getName().startsWith("MI")) { //FIXME: workaround for Miband not being paired
                    gbDevice = new GBDevice(deviceAddress, "MI", DeviceType.MIBAND);
                    deviceSupport = new ServiceDeviceSupport(new MiBandSupport(), EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING));
                } else if (btDevice.getName().indexOf("Pebble") == 0) {
                    gbDevice = new GBDevice(deviceAddress, btDevice.getName(), DeviceType.PEBBLE);
                    deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                }
                if (deviceSupport != null) {
                    deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
                    return deviceSupport;
                }
            } catch (Exception e) {
                throw new GBException(mContext.getString(R.string.cannot_connect_bt_address_invalid_, e));
            }
        }
        return null;
    }

    private DeviceSupport createTCPDeviceSupport(String deviceAddress) throws GBException {
        try {
            GBDevice gbDevice = new GBDevice(deviceAddress, "Pebble qemu", DeviceType.PEBBLE); //FIXME, do not hardcode
            DeviceSupport deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
            deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
            return deviceSupport;
        } catch (Exception e) {
            throw new GBException("cannot connect to " + deviceAddress, e); // FIXME: localize
        }
    }

}
