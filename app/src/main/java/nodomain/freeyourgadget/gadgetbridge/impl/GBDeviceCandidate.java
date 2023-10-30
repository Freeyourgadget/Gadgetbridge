/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Taavi Eomäe

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

/**
 * A device candidate is a Bluetooth device that is not yet managed by
 * Gadgetbridge. Only if a DeviceCoordinator steps up and confirms to
 * support this candidate, will the candidate be promoted to a GBDevice.
 */
public class GBDeviceCandidate implements Parcelable, Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceCandidate.class);

    private BluetoothDevice device;
    private short rssi;
    private ParcelUuid[] serviceUuids;
    // Cached values for device name and bond status, to avoid querying the remote bt device
    private String deviceName;
    private Boolean isBonded = null;

    public GBDeviceCandidate(BluetoothDevice device, short rssi, ParcelUuid[] serviceUuids) {
        this.device = device;
        this.rssi = rssi;
        this.serviceUuids = serviceUuids != null ? serviceUuids : new ParcelUuid[0];
    }

    private GBDeviceCandidate(Parcel in) {
        device = in.readParcelable(getClass().getClassLoader());
        if (device == null) {
            throw new IllegalStateException("Unable to read state from Parcel");
        }
        rssi = (short) in.readInt();

        serviceUuids = AndroidUtils.toParcelUuids(in.readParcelableArray(getClass().getClassLoader()));

        deviceName = in.readString();
        final int isBondedInt = in.readInt();
        if (isBondedInt != -1) {
            isBonded = (isBondedInt == 1);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, 0);
        dest.writeInt(rssi);
        dest.writeParcelableArray(serviceUuids, 0);
        dest.writeString(deviceName);
        if (isBonded == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(isBonded ? 1 : 0);
        }
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

    public String getMacAddress() {
        return device != null ? device.getAddress() : GBApplication.getContext().getString(R.string._unknown_);
    }

    private ParcelUuid[] mergeServiceUuids(ParcelUuid[] serviceUuids, ParcelUuid[] deviceUuids) {
        Set<ParcelUuid> uuids = new LinkedHashSet<>();
        if (serviceUuids != null) {
            uuids.addAll(Arrays.asList(serviceUuids));
        }
        if (deviceUuids != null) {
            uuids.addAll(Arrays.asList(deviceUuids));
        }
        return uuids.toArray(new ParcelUuid[0]);
    }

    public void addUuids(ParcelUuid[] newUuids) {
        this.serviceUuids = mergeServiceUuids(serviceUuids, newUuids);
    }

    public void setRssi(short rssi) {
        this.rssi = rssi;
    }

    public boolean isBonded() {
        if (isBonded == null) {
            try {
                isBonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
            } catch (final SecurityException e) {
                /* This should never happen because we need all the permissions
                    to get to the point where we can even scan, but 'SecurityException' check
                    is added to stop Android Studio errors */
                LOG.error("SecurityException on getBonded");
                isBonded = false;
            }
        }

        return isBonded;
    }

    @NonNull
    public ParcelUuid[] getServiceUuids() {
        return serviceUuids;
    }

    public boolean supportsService(UUID aService) {
        ParcelUuid[] uuids = getServiceUuids();
        if (uuids == null || uuids.length == 0) {
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
        if (isNameKnown()) {
            return deviceName;
        }
        return "(unknown)";
    }

    public void refreshNameIfUnknown() {
        if (isNameKnown()) {
            return;
        }

        try {
            final Method method = device.getClass().getMethod("getAliasName");
            deviceName = (String) method.invoke(device);
        } catch (final NoSuchMethodException ignore) {
            // ignored
        } catch (final IllegalAccessException | InvocationTargetException ignore) {
            LOG.warn("Could not get device alias for {}", device.getAddress());
        }
        if (deviceName == null || deviceName.isEmpty()) {
            try {
                deviceName = device.getName();
            } catch (final SecurityException e) {
                // Should never happen
                LOG.error("SecurityException on device.getName");
            }
        }
    }

    public boolean isNameKnown() {
        return deviceName != null && !deviceName.isEmpty();
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
        return getName() + ": " + getMacAddress();
    }

    @NonNull
    @Override
    public GBDeviceCandidate clone() {
        try {
            final GBDeviceCandidate clone = (GBDeviceCandidate) super.clone();
            clone.device = this.device;
            clone.rssi = this.rssi;
            clone.serviceUuids = this.serviceUuids;
            clone.deviceName = this.deviceName;
            clone.isBonded = this.isBonded;
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
