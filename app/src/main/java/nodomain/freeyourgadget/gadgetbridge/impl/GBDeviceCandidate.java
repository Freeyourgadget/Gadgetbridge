package nodomain.freeyourgadget.gadgetbridge.impl;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

/**
 * A device candidate is a Bluetooth device that is not yet managed by
 * Gadgetbridge. Only if a DeviceCoordinator steps up and confirms to
 * support this candidate, will the candidate be promoted to a GBDevice.
 */
public class GBDeviceCandidate implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceCandidate.class);

    private final BluetoothDevice device;
    private final short rssi;
    private DeviceType deviceType = DeviceType.UNKNOWN;

    public GBDeviceCandidate(BluetoothDevice device, short rssi) {
        this.device = device;
        this.rssi = rssi;
    }

    private GBDeviceCandidate(Parcel in) {
        device = in.readParcelable(getClass().getClassLoader());
        rssi = (short) in.readInt();
        deviceType = DeviceType.valueOf(in.readString());

        if (device == null || deviceType == null) {
            throw new IllegalStateException("Unable to read state from Parcel");
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, 0);
        dest.writeInt(rssi);
        dest.writeString(deviceType.name());
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getMacAddress() {
        return device != null ? device.getAddress() : GBApplication.getContext().getString(R.string._unknown_);
    }

    public boolean supportsService(UUID aService) {
        ParcelUuid[] uuids = device.getUuids();
        if (uuids == null) {
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
        String name = null;
        if (device != null) {
            name = device.getName();
        }
        if (name == null || name.length() == 0) {
            name = GBApplication.getContext().getString(R.string._unknown_);
        }
        return name;
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
}
