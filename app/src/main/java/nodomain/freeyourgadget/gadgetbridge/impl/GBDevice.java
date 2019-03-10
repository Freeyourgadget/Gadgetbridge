/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Taavi Eomäe, Uwe Hermann

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

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;

public class GBDevice implements Parcelable {
    public static final String ACTION_DEVICE_CHANGED
            = "nodomain.freeyourgadget.gadgetbridge.gbdevice.action.device_changed";
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
    private static final short BATTERY_THRESHOLD_PERCENT = 10;
    public static final String EXTRA_DEVICE = "device";
    private static final String DEVINFO_HW_VER = "HW: ";
    private static final String DEVINFO_FW_VER = "FW: ";
    private static final String DEVINFO_HR_VER = "HR: ";
    private static final String DEVINFO_GPS_VER = "GPS: ";
    private static final String DEVINFO_ADDR = "ADDR: ";
    private static final String DEVINFO_ADDR2 = "ADDR2: ";
    private String mName;
    private final String mAddress;
    private String mVolatileAddress;
    private final DeviceType mDeviceType;
    private String mFirmwareVersion;
    private String mFirmwareVersion2;
    private String mModel;
    private State mState = State.NOT_CONNECTED;
    private short mBatteryLevel = BATTERY_UNKNOWN;
    private float mBatteryVoltage = BATTERY_UNKNOWN;
    private short mBatteryThresholdPercent = BATTERY_THRESHOLD_PERCENT;
    private BatteryState mBatteryState;
    private short mRssi = RSSI_UNKNOWN;
    private String mBusyTask;
    private List<ItemWithDetails> mDeviceInfos;
    private HashMap<String, Object> mExtraInfos;

    public GBDevice(String address, String name, DeviceType deviceType) {
        this(address, null, name, deviceType);
    }

    public GBDevice(String address, String address2, String name, DeviceType deviceType) {
        mAddress = address;
        mVolatileAddress = address2;
        mName = (name != null) ? name : mAddress;
        mDeviceType = deviceType;
        validate();
    }

    private GBDevice(Parcel in) {
        mName = in.readString();
        mAddress = in.readString();
        mVolatileAddress = in.readString();
        mDeviceType = DeviceType.values()[in.readInt()];
        mFirmwareVersion = in.readString();
        mFirmwareVersion2 = in.readString();
        mModel = in.readString();
        mState = State.values()[in.readInt()];
        mBatteryLevel = (short) in.readInt();
        mBatteryThresholdPercent = (short) in.readInt();
        mBatteryState = (BatteryState) in.readSerializable();
        mRssi = (short) in.readInt();
        mBusyTask = in.readString();
        mDeviceInfos = in.readArrayList(getClass().getClassLoader());
        mExtraInfos = (HashMap) in.readSerializable();

        validate();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mAddress);
        dest.writeString(mVolatileAddress);
        dest.writeInt(mDeviceType.ordinal());
        dest.writeString(mFirmwareVersion);
        dest.writeString(mFirmwareVersion2);
        dest.writeString(mModel);
        dest.writeInt(mState.ordinal());
        dest.writeInt(mBatteryLevel);
        dest.writeInt(mBatteryThresholdPercent);
        dest.writeSerializable(mBatteryState);
        dest.writeInt(mRssi);
        dest.writeString(mBusyTask);
        dest.writeList(mDeviceInfos);
        dest.writeSerializable(mExtraInfos);
    }

    private void validate() {
        if (getAddress() == null) {
            throw new IllegalArgumentException("address must not be null");
        }
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if (name == null) {
            LOG.warn("Ignoring setting of GBDevice name to null for " + this);
            return;
        }
        mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getVolatileAddress() {
        return mVolatileAddress;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }
    public String getFirmwareVersion2() {
        return mFirmwareVersion2;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        mFirmwareVersion = firmwareVersion;
    }

    /**
     * Sets the second firmware version (HR or GPS or other component)
     * @param firmwareVersion2
     */
    public void setFirmwareVersion2(String firmwareVersion2) {
        mFirmwareVersion2 = firmwareVersion2;
    }

    public void setVolatileAddress(String volatileAddress) {
        mVolatileAddress = volatileAddress;
    }

    /**
     * Returns the specific model/hardware revision of this device.
     * This information is not always available, typically only when the device is initialized
     * @return the model/hardware revision of this device
     * @see #getType()
     */
    @Nullable
    public String getModel() {
        return mModel;
    }

    public void setModel(String model) {
        mModel = model;
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

    public boolean isBusy() {
        return mBusyTask != null;
    }

    public String getBusyTask() {
        return mBusyTask;
    }

    /**
     * Marks the device as busy, performing a certain task. While busy, no other operations will
     * be performed on the device.
     * <p/>
     * Note that nested busy tasks are not supported, every single call to #setBusyTask()
     * or unsetBusy() has an effect.
     *
     * @param task a textual name of the task to be performed, possibly displayed to the user
     */
    public void setBusyTask(String task) {
        if (task == null) {
            throw new IllegalArgumentException("busy task must not be null");
        }
        if (mBusyTask != null) {
            LOG.warn("Attempt to mark device as busy with: " + task + ", but is already busy with: " + mBusyTask);
        }
        LOG.info("Mark device as busy: " + task);
        mBusyTask = task;
    }

    /**
     * Marks the device as not busy anymore.
     */
    public void unsetBusyTask() {
        if (mBusyTask == null) {
            LOG.error("Attempt to mark device as not busy anymore, but was not busy before.");
            return;
        }
        LOG.info("Mark device as NOT busy anymore: " + mBusyTask);
        mBusyTask = null;
    }

    public State getState() {
        return mState;
    }

    public int getStateOrdinal() {
        return mState.ordinal();
    }

    public void setState(State state) {
        mState = state;
        if (state.ordinal() <= State.CONNECTED.ordinal()) {
            unsetDynamicState();
        }
    }

    private void unsetDynamicState() {
        setBatteryLevel(BATTERY_UNKNOWN);
        setBatteryState(BatteryState.UNKNOWN);
        setFirmwareVersion(null);
        setFirmwareVersion2(null);
        setRssi(RSSI_UNKNOWN);
        resetExtraInfos();
        if (mBusyTask != null) {
            unsetBusyTask();
        }
    }

    public String getStateString() {
        return getStateString(true);
    }

    /**
     * for simplicity the user won't see all internal states, just connecting -> connected
     * instead of connecting->connected->initializing->initialized
     * Set simple to true to get this behavior.
     */
    private String getStateString(boolean simple) {
        switch (mState) {
            case NOT_CONNECTED:
                return GBApplication.getContext().getString(R.string.not_connected);
            case WAITING_FOR_RECONNECT:
                return GBApplication.getContext().getString(R.string.waiting_for_reconnect);
            case CONNECTING:
                return GBApplication.getContext().getString(R.string.connecting);
            case CONNECTED:
                if (simple) {
                    return GBApplication.getContext().getString(R.string.connecting);
                }
                return GBApplication.getContext().getString(R.string.connected);
            case INITIALIZING:
                if (simple) {
                    return GBApplication.getContext().getString(R.string.connecting);
                }
                return GBApplication.getContext().getString(R.string.initializing);
            case AUTHENTICATION_REQUIRED:
                return GBApplication.getContext().getString(R.string.authentication_required);
            case AUTHENTICATING:
                return GBApplication.getContext().getString(R.string.authenticating);
            case INITIALIZED:
                if (simple) {
                    return GBApplication.getContext().getString(R.string.connected);
                }
                return GBApplication.getContext().getString(R.string.initialized);
        }
        return GBApplication.getContext().getString(R.string.unknown_state);
    }

    /**
     * Returns the general type of this device. For more detailed information,
     * soo #getModel()
     * @return the general type of this device
     */
    @NonNull
    public DeviceType getType() {
        return mDeviceType;
    }

    public void setRssi(short rssi) {
        if (rssi < 0) {
            LOG.warn("Illegal RSSI value " + rssi + ", setting to RSSI_UNKNOWN");
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
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof GBDevice)) {
            return false;
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
     * Returns the extra info value if it is set, null otherwise
     * @param key the extra info key
     * @return the extra info value if set, null otherwise
     */
    public Object getExtraInfo(String key) {
        if (mExtraInfos == null) {
            return null;
        }

        return mExtraInfos.get(key);
    }

    /**
     * Sets an extra info value, overwriting the current one, if any
     * @param key the extra info key
     * @param info the extra info value
     */
    public void setExtraInfo(String key, Object info) {
        if (mExtraInfos == null) {
            mExtraInfos = new HashMap<>();
        }

        mExtraInfos.put(key, info);
    }

    /**
     * Deletes all the extra infos
     */
    public void resetExtraInfos() {
        mExtraInfos = null;
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

    public void setBatteryVoltage(float batteryVoltage) {
        if (batteryVoltage >= 0 || batteryVoltage == BATTERY_UNKNOWN) {
            mBatteryVoltage = batteryVoltage;
        } else {
            LOG.error("Battery voltage must be > 0: " + batteryVoltage);
        }
    }

    /**
     * Voltage greater than zero (unit: Volt), or -1 if unknown
     *
     * @return the battery voltage, or -1 if unknown
     */
    public float getBatteryVoltage() {
        return mBatteryVoltage;
    }

    public BatteryState getBatteryState() {
        return mBatteryState;
    }

    public void setBatteryState(BatteryState mBatteryState) {
        this.mBatteryState = mBatteryState;
    }

    public short getBatteryThresholdPercent() {
        return mBatteryThresholdPercent;
    }

    public void setBatteryThresholdPercent(short batteryThresholdPercent) {
        this.mBatteryThresholdPercent = batteryThresholdPercent;
    }

    @Override
    public String toString() {
        return "Device " + getName() + ", " + getAddress() + ", " + getStateString(false);
    }

    /**
     * Returns a shortened form of the device's address, in order to form a
     * unique name in companion with #getName().
     */
    @NonNull
    public String getShortAddress() {
        String address = getAddress();
        if (address != null) {
            if (address.length() > 5) {
                return address.substring(address.length() - 5);
            }
            return address;
        }
        return "";
    }

    public boolean hasDeviceInfos() {
        return getDeviceInfos().size() > 0;
    }

    public ItemWithDetails getDeviceInfo(String name) {
        for (ItemWithDetails item : getDeviceInfos()) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    public List<ItemWithDetails> getDeviceInfos() {
        List<ItemWithDetails> result = new ArrayList<>();
        if (mDeviceInfos != null) {
            result.addAll(mDeviceInfos);
        }
        if (mModel != null) {
            result.add(new GenericItem(DEVINFO_HW_VER, mModel));
        }
        if (mFirmwareVersion != null) {
            result.add(new GenericItem(DEVINFO_FW_VER, mFirmwareVersion));
        }
        if (mFirmwareVersion2 != null) {
            // FIXME: This is ugly
            if (mDeviceType == DeviceType.AMAZFITBIP) {
                result.add(new GenericItem(DEVINFO_GPS_VER, mFirmwareVersion2));
            } else {
                result.add(new GenericItem(DEVINFO_HR_VER, mFirmwareVersion2));
            }
        }
        if (mAddress != null) {
            result.add(new GenericItem(DEVINFO_ADDR, mAddress));
        }
        if (mVolatileAddress != null) {
            result.add(new GenericItem(DEVINFO_ADDR2, mVolatileAddress));
        }
        Collections.sort(result);
        return result;
    }

    public void setDeviceInfos(List<ItemWithDetails> deviceInfos) {
        this.mDeviceInfos = deviceInfos;
    }

    public void addDeviceInfo(ItemWithDetails info) {
        if (mDeviceInfos == null) {
            mDeviceInfos = new ArrayList<>();
        } else {
            int index = mDeviceInfos.indexOf(info);
            if (index >= 0) {
                mDeviceInfos.set(index, info); // replace item with new one
                return;
            }
        }
        mDeviceInfos.add(info);
    }

    public boolean removeDeviceInfo(ItemWithDetails info) {
        if (mDeviceInfos == null) {
            return false;
        }
        return mDeviceInfos.remove(info);
    }

    public enum State {
        // Note: the order is important!
        NOT_CONNECTED,
        WAITING_FOR_RECONNECT,
        CONNECTING,
        CONNECTED,
        INITIALIZING,
        AUTHENTICATION_REQUIRED, // some kind of pairing is required by the device
        AUTHENTICATING, // some kind of pairing is requested by the device
        /**
         * Means that the device is connected AND all the necessary initialization steps
         * have been performed. At the very least, this means that basic information like
         * device name, firmware version, hardware revision (as applicable) is available
         * in the GBDevice.
         */
        INITIALIZED,
    }
}
