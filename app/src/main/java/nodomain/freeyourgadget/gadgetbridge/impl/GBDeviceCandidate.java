/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.impl;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;

/**
 * A device candidate is a Bluetooth device that is not yet managed by
 * Gadgetbridge. Only if a DeviceCoordinator steps up and confirms to
 * support this candidate, will the candidate be promoted to a GBDevice.
 */
public class GBDeviceCandidate implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceCandidate.class);

    private final BluetoothDevice device;
    private final short rssi;
    private final ParcelUuid[] serviceUuds;
    private DeviceType deviceType = DeviceType.UNKNOWN;

    public GBDeviceCandidate(BluetoothDevice device, short rssi, ParcelUuid[] serviceUuds) {
        this.device = device;
        this.rssi = rssi;
        this.serviceUuds = mergeServiceUuids(serviceUuds, device.getUuids());
    }

    private GBDeviceCandidate(Parcel in) {
        device = in.readParcelable(getClass().getClassLoader());
        if (device == null) {
            throw new IllegalStateException("Unable to read state from Parcel");
        }
        rssi = (short) in.readInt();
        deviceType = DeviceType.valueOf(in.readString());

        ParcelUuid[] uuids = AndroidUtils.toParcelUUids(in.readParcelableArray(getClass().getClassLoader()));
        serviceUuds = mergeServiceUuids(uuids, device.getUuids());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, 0);
        dest.writeInt(rssi);
        dest.writeString(deviceType.name());
        dest.writeParcelableArray(serviceUuds, 0);
    }

    public static final Creator<GBDeviceCandidate> CREATOR = new Creator<GBDeviceCandidate>() {
        @Override
        public GBDeviceCandidate createFromParcel(Parcel in) {
            return new GBDeviceCandidate(in);
        }

        @Override
        public GBDeviceCandidate[] newArray(int size) {
            return new GBDeviceCandidate[size];
        }
    };

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDeviceType(DeviceType type) {
        deviceType = type;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getMacAddress() {
        return device != null ? device.getAddress() : GBApplication.getContext().getString(R.string._unknown_);
    }

    private ParcelUuid[] mergeServiceUuids(ParcelUuid[] serviceUuds, ParcelUuid[] deviceUuids) {
        Set<ParcelUuid> uuids = new HashSet<>();
        if (serviceUuds != null) {
            uuids.addAll(Arrays.asList(serviceUuds));
        }
        if (deviceUuids != null) {
            uuids.addAll(Arrays.asList(deviceUuids));
        }
        return uuids.toArray(new ParcelUuid[0]);
    }

    @NonNull
    public ParcelUuid[] getServiceUuids() {
        return serviceUuds;
    }

    public boolean supportsService(UUID aService) {
        ParcelUuid[] uuids = getServiceUuids();
        if (uuids.length == 0) {
            LOG.warn("no cached services available for " + this);
            return false;
        }

        for (ParcelUuid uuid : uuids) {
            if (uuid != null && aService.equals(uuid.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        String deviceName = null;
        try {
            Method method = device.getClass().getMethod("getAliasName");
            if (method != null) {
                deviceName = (String) method.invoke(device);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
            LOG.info("Could not get device alias for " + device.getName());
        }
        if (deviceName == null || deviceName.length() == 0) {
            deviceName = device.getName();
        }
        if (deviceName == null || deviceName.length() == 0) {
            deviceName = "(unknown)";
        }
        return deviceName;
    }

    public short getRssi() {
        return rssi;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GBDeviceCandidate that = (GBDeviceCandidate) o;
        return device.getAddress().equals(that.device.getAddress());
    }

    @Override
    public int hashCode() {
        return device.getAddress().hashCode() ^ 37;
    }

    @Override
    public String toString() {
        return getName() + ": " + getMacAddress() + " (" + getDeviceType() + ")";
    }
}
