package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
    private static final String TAG = GBDevice.class.getSimpleName();
    private final String mName;
    private final String mAddress;
    private final Type mType;
    private String mFirmwareVersion = null;
    private String mHardwareVersion = null;
    private State mState = State.NOT_CONNECTED;
    private short mBatteryLevel = 50; // unknown
    private String mBatteryState;

    public GBDevice(String address, String name, Type type) {
        mAddress = address;
        mName = name;
        mType = type;
        validate();
    }

    private GBDevice(Parcel in) {
        mName = in.readString();
        mAddress = in.readString();
        mType = Type.values()[in.readInt()];
        mFirmwareVersion = in.readString();
        mHardwareVersion = in.readString();
        mState = State.values()[in.readInt()];
        mBatteryLevel = (short) in.readInt();
        mBatteryState = in.readString();
        validate();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mAddress);
        dest.writeInt(mType.ordinal());
        dest.writeString(mFirmwareVersion);
        dest.writeString(mHardwareVersion);
        dest.writeInt(mState.ordinal());
        dest.writeInt(mBatteryLevel);
        dest.writeString(mBatteryState);
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
    }

    String getStateString() {
        switch (mState) {
            case NOT_CONNECTED:
                return GBApplication.getContext().getString(R.string.not_connected);
            case CONNECTING:
                return GBApplication.getContext().getString(R.string.connecting);
            case CONNECTED:
                return GBApplication.getContext().getString(R.string.connected);
            case INITIALIZED:
                return GBApplication.getContext().getString(R.string.initialized);
        }
        return GBApplication.getContext().getString(R.string.unknown_state);
    }

    public String getInfoString() {
        if (mFirmwareVersion != null) {
            if (mHardwareVersion != null) {
                return GBApplication.getContext().getString(R.string.connectionstate_hw_fw, getStateString(), mHardwareVersion, mFirmwareVersion);
            }
            return GBApplication.getContext().getString(R.string.connectionstate_fw, getStateString(), mFirmwareVersion);
        } else {
            return getStateString();
        }
    }

    public Type getType() {
        return mType;
    }

    // TODO: this doesn't really belong here
    public void sendDeviceUpdateIntent(Context context) {
        Intent deviceUpdateIntent = new Intent(ACTION_DEVICE_CHANGED);
        deviceUpdateIntent.putExtra("device", this);
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
     * Ranges from 0-100 (percent)
     *
     * @return the battery level in range 0-100
     */
    public short getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(short batteryLevel) {
        if (mBatteryLevel >= 0 && mBatteryLevel <= 100) {
            mBatteryLevel = batteryLevel;
        } else {
            Log.e(TAG, "Battery level musts be within range 0-100: " + batteryLevel);
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
        INITIALIZED
    }

    public enum Type {
        UNKNOWN,
        PEBBLE,
        MIBAND
    }
}
