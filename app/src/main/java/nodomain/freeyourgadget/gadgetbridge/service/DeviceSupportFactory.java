/*  Copyright (C) 2015-2018 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, João Paulo Barraca, ladbsoft, protomors, Quallenauge,
    Sami Alaoui, Sergey Trofimov, tiparega

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
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitcor.AmazfitCorSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3.MiBand3Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.id115.ID115Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.liveview.LiveviewSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.no1f1.No1F1Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vibratissimo.VibratissimoSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hplus.HPlusSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.TeclastH30Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xwatch.XWatchSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.zetime.ZeTimeDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DeviceSupportFactory {
    private final BluetoothAdapter mBtAdapter;
    private final Context mContext;

    public DeviceSupportFactory(Context context) {
        mContext = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized DeviceSupport createDeviceSupport(GBDevice device) throws GBException {
        DeviceSupport deviceSupport = null;
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

    private DeviceSupport createBTDeviceSupport(GBDevice gbDevice) throws GBException {
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            DeviceSupport deviceSupport = null;

            try {
                switch (gbDevice.getType()) {
                    case PEBBLE:
                        deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case MIBAND:
                        deviceSupport = new ServiceDeviceSupport(new MiBandSupport(), EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case MIBAND2:
                        deviceSupport = new ServiceDeviceSupport(new HuamiSupport(), EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case MIBAND3:
                        deviceSupport = new ServiceDeviceSupport(new MiBand3Support(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case AMAZFITBIP:
                        deviceSupport = new ServiceDeviceSupport(new AmazfitBipSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case AMAZFITCOR:
                        deviceSupport = new ServiceDeviceSupport(new AmazfitCorSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case VIBRATISSIMO:
                        deviceSupport = new ServiceDeviceSupport(new VibratissimoSupport(), EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case LIVEVIEW:
                        deviceSupport = new ServiceDeviceSupport(new LiveviewSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case HPLUS:
                        deviceSupport = new ServiceDeviceSupport(new HPlusSupport(DeviceType.HPLUS), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case MAKIBESF68:
                        deviceSupport = new ServiceDeviceSupport(new HPlusSupport(DeviceType.MAKIBESF68), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case EXRIZUK8:
                        deviceSupport = new ServiceDeviceSupport(new HPlusSupport(DeviceType.EXRIZUK8), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case Q8:
                        deviceSupport = new ServiceDeviceSupport(new HPlusSupport(DeviceType.Q8), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case NO1F1:
                        deviceSupport = new ServiceDeviceSupport(new No1F1Support(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case TECLASTH30:
                        deviceSupport = new ServiceDeviceSupport(new TeclastH30Support(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case XWATCH:
                        deviceSupport = new ServiceDeviceSupport(new XWatchSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case FOSSILQHYBRID:
                        deviceSupport = new ServiceDeviceSupport(new QHybridSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case ZETIME:
                        deviceSupport = new ServiceDeviceSupport(new ZeTimeDeviceSupport(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                    case ID115:
                        deviceSupport = new ServiceDeviceSupport(new ID115Support(), EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING));
                        break;
                }
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
