package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GBDevice implements Parcelable {
    public static final String ACTION_DEVICE_CHANGED
            = "nodomain.freeyourgadget.gadgetbride.gbdevice.action.device_changed";
    public static final Creator<GBDevice> CREATOR = new Creator<GBDevice>() {
        @Override
        public GBDevice createFromParcel(Parcel source) {
            return new GBDevice(source);
        }

        @Override
        public GBDevice[] newArray(int size) {
            return new GBDevice[size];
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(GBDevice.class);
    public static final short RSSI_UNKNOWN = 0;
    public static final short BATTERY_UNKNOWN = -1;
    public static final String EXTRA_DEVICE = "device";
    private final String mName;
    private final String mAddress;
    private final DeviceType mDeviceType;
    private String mFirmwareVersion = null;
    private String mHardwareVersion = null;
    private State mState = State.NOT_CONNECTED;
    private short mBatteryLevel = BATTERY_UNKNOWN;
    private String mBatteryState;
    private short mRssi = RSSI_UNKNOWN;

    public GBDevice(String address, String name, DeviceType deviceType) {
        mAddress = address;
        mName = name;
        mDeviceType = deviceType;
        validate();
    }

    private GBDevice(Parcel in) {
        mName = in.readString();
        mAddress = in.readString();
        mDeviceType = DeviceType.values()[in.readInt()];
        mFirmwareVersion = in.readString();
        mHardwareVersion = in.readString();
        mState = State.values()[in.readInt()];
        mBatteryLevel = (short) in.readInt();
        mBatteryState = in.readString();
        mRssi = (short) in.readInt();
        validate();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mAddress);
        dest.writeInt(mDeviceType.ordinal());
        dest.writeString(mFirmwareVersion);
        dest.writeString(mHardwareVersion);
        dest.writeInt(mState.ordinal());
        dest.writeInt(mBatteryLevel);
        dest.writeString(mBatteryState);
        dest.writeInt(mRssi);
    }

    private void validate() {
        if (getAddress() == null) {
            throw new IllegalArgumentException("address must not be null");
        }
    }

    public String getName() {
        return mName;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        mFirmwareVersion = firmwareVersion;
    }

    public String getHardwareVersion() {
        return mHardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        mHardwareVersion = hardwareVersion;
    }

    public boolean isConnected() {
        return mState.ordinal() >= State.CONNECTED.ordinal();
    }

    public boolean isInitializing() {
        return mState == State.INITIALIZING;
    }

    public boolean isInitialized() {
        return mState.ordinal() >= State.INITIALIZED.ordinal();
    }

    public boolean isConnecting() {
        return mState == State.CONNECTING;
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
        if (state.ordinal() <= State.CONNECTED.ordinal()) {
            unsetDynamicState();
        }
    }

    private void unsetDynamicState() {
        setBatteryLevel(BATTERY_UNKNOWN);
        setBatteryState(null);
        setFirmwareVersion(null);
        setRssi(RSSI_UNKNOWN);
    }

    public String getStateString() {
        switch (mState) {
            case NOT_CONNECTED:
                return GBApplication.getContext().getString(R.string.not_connected);
            case CONNECTING:
                return GBApplication.getContext().getString(R.string.connecting);
            case CONNECTED:
                return GBApplication.getContext().getString(R.string.connected);
            case INITIALIZING:
                return GBApplication.getContext().getString(R.string.initializing);
            case INITIALIZED:
                return GBApplication.getContext().getString(R.string.initialized);
        }
        return GBApplication.getContext().getString(R.string.unknown_state);
    }


    public String getInfoString() {
        if (mFirmwareVersion != null) {
            if (mHardwareVersion != null) {
                return GBApplication.getContext().getString(R.string.connectionstate_hw_fw, mHardwareVersion, mFirmwareVersion);
            }
            return GBApplication.getContext().getString(R.string.connectionstate_fw, mFirmwareVersion);
        } else {
            return "";
        }
    }

    public DeviceType getType() {
        return mDeviceType;
    }

    public void setRssi(short rssi) {
        if (rssi < 0) {
            LOG.warn("illegal rssi value " + rssi + ", setting to RSSI_UNKNOWN");
            mRssi = RSSI_UNKNOWN;
        } else {
            mRssi = rssi;
        }
    }

    /**
     * Returns the device specific signal strength value, or #RSSI_UNKNOWN
     */
    public short getRssi() {
        return mRssi;
    }

    // TODO: this doesn't really belong here
    public void sendDeviceUpdateIntent(Context context) {
        Intent deviceUpdateIntent = new Intent(ACTION_DEVICE_CHANGED);
        deviceUpdateIntent.putExtra(EXTRA_DEVICE, this);
        LocalBroadcastManager.getInstance(context).sendBroadcast(deviceUpdateIntent);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GBDevice)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (((GBDevice) obj).getAddress().equals(this.mAddress)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode() ^ 37;
    }

    /**
     * Ranges from 0-100 (percent), or -1 if unknown
     *
     * @return the battery level in range 0-100, or -1 if unknown
     */
    public short getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(short batteryLevel) {
        if ((batteryLevel >= 0 && batteryLevel <= 100) || batteryLevel == BATTERY_UNKNOWN) {
            mBatteryLevel = batteryLevel;
        } else {
            LOG.error("Battery level musts be within range 0-100: " + batteryLevel);
        }
    }

    /**
     * Returns a string representation of the battery state.
     */
    public String getBatteryState() {
        return mBatteryState != null ? mBatteryState : GBApplication.getContext().getString(R.string._unknown_);
    }

    public void setBatteryState(String batteryState) {
        mBatteryState = batteryState;
    }

    public enum State {
        // Note: the order is important!
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        INITIALIZING,
        INITIALIZED
    }

}
