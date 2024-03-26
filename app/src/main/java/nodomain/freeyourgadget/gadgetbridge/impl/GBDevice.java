/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Daniele Gobbetti, José Rebelo, Petr Vaněk, Taavi
    Eomäe, Uwe Hermann

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.impl;

import static nodomain.freeyourgadget.gadgetbridge.model.BatteryState.UNKNOWN;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
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
    public static final short BATTERY_ICON_DEFAULT = -1;
    public static final short BATTERY_LABEL_DEFAULT = -1;
    private static final short BATTERY_THRESHOLD_PERCENT = 10;
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_UUID = "extraUUID";
    public static final String EXTRA_UPDATE_SUBJECT = "EXTRA_UPDATE_SUBJECT";
    private static final String DEVINFO_HW_VER = "HW: ";
    private static final String DEVINFO_FW_VER = "FW: ";
    private static final String DEVINFO_FW2_VER = "FW2: ";
    private static final String DEVINFO_ADDR = "ADDR: ";
    private static final String DEVINFO_ADDR2 = "ADDR2: ";
    public static final String BATTERY_INDEX = "battery_index";
    private String mName;
    private String mAlias;
    private String parentFolder;
    private final String mAddress;
    private String mVolatileAddress;
    private final DeviceType mDeviceType;
    private String mFirmwareVersion;
    private String mFirmwareVersion2;
    private String mModel;
    private State mState = State.NOT_CONNECTED;

    // multiple battery support: at this point we support up to three batteries
    private int[] mBatteryLevel = {BATTERY_UNKNOWN, BATTERY_UNKNOWN, BATTERY_UNKNOWN};
    private float[] mBatteryVoltage = {BATTERY_UNKNOWN, BATTERY_UNKNOWN, BATTERY_UNKNOWN};
    private short mBatteryThresholdPercent = BATTERY_THRESHOLD_PERCENT;
    private BatteryState[] mBatteryState = {UNKNOWN, UNKNOWN, UNKNOWN};
    private int[] mBatteryIcons = {BATTERY_ICON_DEFAULT, BATTERY_ICON_DEFAULT, BATTERY_ICON_DEFAULT};
    private int[] mBatteryLabels = {BATTERY_LABEL_DEFAULT, BATTERY_LABEL_DEFAULT, BATTERY_LABEL_DEFAULT};

    private short mRssi = RSSI_UNKNOWN;
    private String mBusyTask;
    private List<ItemWithDetails> mDeviceInfos;
    private HashMap<String, Object> mExtraInfos;

    private int mNotificationIconConnected = R.drawable.ic_notification;
    private int mNotificationIconDisconnected = R.drawable.ic_notification_disconnected;
    private int mNotificationIconLowBattery = R.drawable.ic_notification_low_battery;

    public static enum DeviceUpdateSubject {
        UNKNOWN,
        NOTHING,
        CONNECTION_STATE,
        DEVICE_STATE,
    }

    public GBDevice(String address, String name, String alias, String parentFolder, DeviceType deviceType) {
        this(address, null, name, alias, parentFolder, deviceType);
    }

    public GBDevice(String address, String address2, String name, String alias, String parentFolder, DeviceType deviceType) {
        mAddress = address;
        mVolatileAddress = address2;
        mName = (name != null) ? name : mAddress;
        mAlias = alias;
        mDeviceType = deviceType;
        this.parentFolder = parentFolder;
        validate();
    }

    private GBDevice(Parcel in) {
        mName = in.readString();
        mAlias = in.readString();
        parentFolder = in.readString();
        mAddress = in.readString();
        mVolatileAddress = in.readString();
        mDeviceType = DeviceType.values()[in.readInt()];
        mFirmwareVersion = in.readString();
        mFirmwareVersion2 = in.readString();
        mModel = in.readString();
        mState = State.values()[in.readInt()];
        mBatteryLevel = in.createIntArray();
        mBatteryVoltage = in.createFloatArray();
        mBatteryThresholdPercent = (short) in.readInt();
        mBatteryState = ordinalsToEnums(in.createIntArray());
        mBatteryIcons = in.createIntArray();
        mBatteryLabels = in.createIntArray();
        mRssi = (short) in.readInt();
        mBusyTask = in.readString();
        mDeviceInfos = in.readArrayList(getClass().getClassLoader());
        mExtraInfos = (HashMap) in.readSerializable();
        mNotificationIconConnected = in.readInt();
        mNotificationIconDisconnected = in.readInt();
        mNotificationIconLowBattery = in.readInt();

        validate();
    }

    public void copyFromDevice(GBDevice device){
        if(!device.mAddress.equals(mAddress)){
            throw new RuntimeException("Cannot copy from device with other address");
        }

        mName = device.mName;
        mAlias = device.mAlias;
        parentFolder = device.parentFolder;
        mVolatileAddress = device.mVolatileAddress;
        mFirmwareVersion = device.mFirmwareVersion;
        mFirmwareVersion2 = device.mFirmwareVersion2;
        mModel = device.mModel;
        mState = device.mState;
        mBatteryLevel = device.mBatteryLevel;
        mBatteryVoltage = device.mBatteryVoltage;
        mBatteryThresholdPercent = device.mBatteryThresholdPercent;
        mBatteryState = device.mBatteryState;
        mBatteryIcons = device.mBatteryIcons;
        mBatteryLabels = device.mBatteryLabels;
        mRssi = device.mRssi;
        mBusyTask = device.mBusyTask;
        mDeviceInfos = device.mDeviceInfos;
        mExtraInfos = device.mExtraInfos;
        mNotificationIconConnected = device.mNotificationIconConnected;
        mNotificationIconDisconnected = device.mNotificationIconDisconnected;
        mNotificationIconLowBattery = device.mNotificationIconLowBattery;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mAlias);
        dest.writeString(parentFolder);
        dest.writeString(mAddress);
        dest.writeString(mVolatileAddress);
        dest.writeInt(mDeviceType.ordinal());
        dest.writeString(mFirmwareVersion);
        dest.writeString(mFirmwareVersion2);
        dest.writeString(mModel);
        dest.writeInt(mState.ordinal());
        dest.writeIntArray(mBatteryLevel);
        dest.writeFloatArray(mBatteryVoltage);
        dest.writeInt(mBatteryThresholdPercent);
        dest.writeIntArray(enumsToOrdinals(mBatteryState));
        dest.writeIntArray(mBatteryIcons);
        dest.writeIntArray(mBatteryLabels);
        dest.writeInt(mRssi);
        dest.writeString(mBusyTask);
        dest.writeList(mDeviceInfos);
        dest.writeSerializable(mExtraInfos);
        dest.writeInt(mNotificationIconConnected);
        dest.writeInt(mNotificationIconDisconnected);
        dest.writeInt(mNotificationIconLowBattery);
    }

    private void validate() {
        if (getAddress() == null) {
            throw new IllegalArgumentException("address must not be null");
        }
    }

    private int[] enumsToOrdinals(BatteryState[] arrayEnum) {
        int[] ordinals = new int[arrayEnum.length];
        for (int i = 0; i < arrayEnum.length; i++) {
            ordinals[i] = arrayEnum[i].ordinal();
        }
        return ordinals;
    }

    private BatteryState[] ordinalsToEnums(int[] arrayInt){
        BatteryState[] enums = new BatteryState[arrayInt.length];
        for(int i = 0; i<arrayInt.length; i++){
            enums[i]=BatteryState.values()[arrayInt[i]];
        }
        return enums;
    }

    public String getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(String parentFolder) {
        this.parentFolder = parentFolder;
    }

    public String getName() {
        return mName;
    }

    public String getAlias() {
        return mAlias;
    }

    public String getAliasOrName() {
        if (mAlias != null && !mAlias.equals("")) {
            return mAlias;
        }
        return mName;
    }

    public void setName(String name) {
        if (name == null) {
            LOG.warn("Ignoring setting of GBDevice name to null for " + this);
            return;
        }
        mName = name;
    }

    public void setAlias(String alias) {
        mAlias = alias;
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
        return mState.equalsOrHigherThan(State.CONNECTED);
    }

    public boolean isInitializing() {
        return mState == State.INITIALIZING;
    }

    public boolean isInitialized() {
        return mState.equalsOrHigherThan(State.INITIALIZED);
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

    public int getNotificationIconConnected() {
        return mNotificationIconConnected;
    }

    public void setNotificationIconConnected(int mNotificationIconConnected) {
        this.mNotificationIconConnected = mNotificationIconConnected;
    }

    public int getNotificationIconDisconnected() {
        return mNotificationIconDisconnected;
    }

    public void setNotificationIconDisconnected(int notificationIconDisconnected) {
        this.mNotificationIconDisconnected = notificationIconDisconnected;
    }

    public int getNotificationIconLowBattery() {
        return mNotificationIconLowBattery;
    }

    public void setNotificationIconLowBattery(int mNotificationIconLowBattery) {
        this.mNotificationIconLowBattery = mNotificationIconLowBattery;
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

        setBatteryLevel(BATTERY_UNKNOWN, 0);
        setBatteryLevel(BATTERY_UNKNOWN, 1);
        setBatteryLevel(BATTERY_UNKNOWN, 2);
        setBatteryState(UNKNOWN, 0);
        setBatteryState(UNKNOWN, 1);
        setBatteryState(UNKNOWN, 2);
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
        try{
            // TODO: not sure if this is really neccessary...
            if(simple){
                return GBApplication.getContext().getString(mState.getSimpleStringId());
            }
            return GBApplication.getContext().getString(mState.getStringId());
        }catch (Exception e){}

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

    @NonNull
    public DeviceCoordinator getDeviceCoordinator(){
        return mDeviceType.getDeviceCoordinator();
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
        sendDeviceUpdateIntent(context, DeviceUpdateSubject.UNKNOWN);
    }

    // TODO: this doesn't really belong here
    public void sendDeviceUpdateIntent(Context context, DeviceUpdateSubject subject) {
        Intent deviceUpdateIntent = new Intent(ACTION_DEVICE_CHANGED);
        deviceUpdateIntent.putExtra(EXTRA_DEVICE, this);
        deviceUpdateIntent.putExtra(EXTRA_UPDATE_SUBJECT, subject);
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
    public int getBatteryLevel() {
        return getBatteryLevel(0);
    }

    public int getBatteryLevel(int index) {
        return mBatteryLevel[index];
    }


    public void setBatteryLevel(int batteryLevel) {
        setBatteryLevel(batteryLevel, 0);
    }

    public void setBatteryLevel(int batteryLevel, int index) {
        if ((batteryLevel >= 0 && batteryLevel <= 100) || batteryLevel == BATTERY_UNKNOWN) {
            mBatteryLevel[index] = batteryLevel;
        } else {
            LOG.error("Battery level musts be within range 0-100: " + batteryLevel);
        }
    }

    public void setBatteryVoltage(float batteryVoltage) {
        setBatteryVoltage(batteryVoltage, 0);
    }


    public void setBatteryVoltage(float batteryVoltage, int index) {
        if (batteryVoltage >= 0 || batteryVoltage == BATTERY_UNKNOWN) {
            mBatteryVoltage[index] = batteryVoltage;
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
        return getBatteryVoltage(0);
    }

    public float getBatteryVoltage(int index) {
        return mBatteryVoltage[index];
    }

    public BatteryState getBatteryState() {
        return getBatteryState(0);
    }

    public BatteryState getBatteryState(int index) {
        return mBatteryState[index];
    }

    public void setBatteryState(BatteryState mBatteryState) {
        setBatteryState(mBatteryState, 0);
    }

    public void setBatteryState(BatteryState mBatteryState, int index) {
        this.mBatteryState[index] = mBatteryState;
    }

    public short getBatteryThresholdPercent() {
        return mBatteryThresholdPercent;
    }

    public void setBatteryThresholdPercent(short batteryThresholdPercent) {
        this.mBatteryThresholdPercent = batteryThresholdPercent;
    }

    public int getBatteryIcon(int index) {
        return this.mBatteryIcons[index];
    }

    public void setBatteryIcon(int icon, int index) {
        this.mBatteryIcons[index] = icon;
    }

    public int getBatteryLabel(int index) {
        return this.mBatteryLabels[index];
    }

    public void setBatteryLabel(int label, int index) {
        this.mBatteryLabels[index] = label;
    }

    public int getEnabledDisabledIconResource(){
        return isInitialized() ?
                getDeviceCoordinator().getDefaultIconResource() :
                getDeviceCoordinator().getDisabledIconResource();
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
            for (ItemWithDetails deviceInfo : mDeviceInfos){
                GenericItem item = new GenericItem(deviceInfo.getName() + ": ", deviceInfo.getDetails());
                item.setIcon(deviceInfo.getIcon());
                result.add(item);
            }
        }
        if (mModel != null) {
            result.add(new GenericItem(DEVINFO_HW_VER, mModel));
        }
        if (mFirmwareVersion != null) {
            result.add(new GenericItem(DEVINFO_FW_VER, mFirmwareVersion));
        }
        if (mFirmwareVersion2 != null) {
            result.add(new GenericItem(DEVINFO_FW2_VER, mFirmwareVersion2));
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
        NOT_CONNECTED(R.string.not_connected),
        WAITING_FOR_RECONNECT(R.string.waiting_for_reconnect),
        WAITING_FOR_SCAN(R.string.device_state_waiting_scan),
        CONNECTING(R.string.connecting),
        CONNECTED(R.string.connected, R.string.connecting),
        INITIALIZING(R.string.initializing, R.string.connecting),
        AUTHENTICATION_REQUIRED(R.string.authentication_required), // some kind of pairing is required by the device
        AUTHENTICATING(R.string.authenticating), // some kind of pairing is requested by the device
        /**
         * Means that the device is connected AND all the necessary initialization steps
         * have been performed. At the very least, this means that basic information like
         * device name, firmware version, hardware revision (as applicable) is available
         * in the GBDevice.
         */
        INITIALIZED(R.string.initialized, R.string.connected);


        private int stringId, simpleStringId;

        State(int stringId, int simpleStringId) {
            this.stringId = stringId;
            this.simpleStringId = simpleStringId;
        }

        State(int stringId) {
            this(stringId, stringId);
        }

        public int getStringId() {
            return stringId;
        }

        public int getSimpleStringId() {
            return simpleStringId;
        }

        public boolean equalsOrHigherThan(State otherState){
            return compareTo(otherState) >= 0;
        }
    }
}
