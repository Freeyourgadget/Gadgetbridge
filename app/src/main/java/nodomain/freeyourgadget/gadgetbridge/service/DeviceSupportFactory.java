/*  Copyright (C) 2015-2021 0nse, 115ek, Andreas Böhler, Andreas Shimokawa,
    angelpup, Carsten Pfeiffer, Cre3per, criogenic, DanialHanif, Daniel Dakhno,
    Daniele Gobbetti, Dmytro Bielik, Gordon Williams, Jean-François Greffier,
    João Paulo Barraca, José Rebelo, ladbsoft, Manuel Ruß, maxirnilian,
    mkusnierz, odavo32nof, opavlov, pangwalla, Pavel Elagin, protomors,
    Quallenauge, Sami Alaoui, Sebastian Kranz, Sergey Trofimov, Sophanimus,
    Taavi Eomäe, tiparega, Vadim Kaushan, Yukai Li

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;

public class DeviceSupportFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSupportFactory.class);

    private final BluetoothAdapter mBtAdapter;
    private final Context mContext;

    DeviceSupportFactory(Context context) {
        mContext = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized DeviceSupport createDeviceSupport(GBDevice device) throws GBException {
        DeviceSupport deviceSupport;
        String deviceAddress = device.getAddress();
        int indexFirstColon = deviceAddress.indexOf(":");
        if (indexFirstColon > 0) {
            if (indexFirstColon == deviceAddress.lastIndexOf(":")) { // only one colon
                deviceSupport = createTCPDeviceSupport(device);
            } else {
                // multiple colons -- bt?
                deviceSupport = createBTDeviceSupport(device);
            }
        } else {
            // no colon at all, maybe a class name?
            deviceSupport = createClassNameDeviceSupport(device);
        }

        if (deviceSupport != null) {
            return deviceSupport;
        }

        // no device found, check transport availability and warn
        checkBtAvailability();
        return null;
    }

    private DeviceSupport createClassNameDeviceSupport(GBDevice device) throws GBException {
        String className = device.getAddress();
        try {
            Class<?> deviceSupportClass = Class.forName(className);
            Constructor<?> constructor = deviceSupportClass.getConstructor();
            DeviceSupport support = (DeviceSupport) constructor.newInstance();
            // has to create the device itself
            support.setContext(device, null, mContext);
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

    private ServiceDeviceSupport createServiceDeviceSupport(GBDevice device) throws GBException {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        Class supportClass = coordinator.getDeviceSupportClass();

        try {
            Constructor supportConstructor = supportClass.getConstructor(DeviceType.class);
            DeviceSupport supportInstance = (DeviceSupport) supportConstructor.newInstance(device.getType());
            return new ServiceDeviceSupport(supportInstance, coordinator.getInitialFlags());
        } catch (NoSuchMethodException e) {
            // ignore, let next call get the default, zero-argument constructor
        } catch (ReflectiveOperationException e) {
            LOG.error("error calling DeviceSupport constructor with argument 'DeviceType'");
            throw new GBException(e);
        }

        try {
            DeviceSupport supportInstance = (DeviceSupport) supportClass.newInstance();
            return new ServiceDeviceSupport(supportInstance, coordinator.getInitialFlags());
        } catch (ReflectiveOperationException e) {
            LOG.error("error calling DeviceSupport constructor with zero arguments");
            throw new GBException(e);
        }
    }

    private DeviceSupport createBTDeviceSupport(GBDevice gbDevice) throws GBException {
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            try {
                DeviceSupport deviceSupport = createServiceDeviceSupport(gbDevice);
                if (deviceSupport != null) {
                    deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
                    return deviceSupport;
                }
            } catch (Exception e) {
                throw new GBException(mContext.getString(R.string.cannot_connect_bt_address_invalid_), e);
            }
        }
        return null;
    }

    private DeviceSupport createTCPDeviceSupport(GBDevice gbDevice) throws GBException {
        try {
            DeviceSupport deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
            deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
            return deviceSupport;
        } catch (Exception e) {
            throw new GBException("cannot connect to " + gbDevice, e); // FIXME: localize
        }
    }

}
