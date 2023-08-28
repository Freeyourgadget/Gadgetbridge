/*  Copyright (C) 2023-2024 Andreas Böhler, foxstidious, Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gbx100.CasioGBX100SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOREMOVE_MESSAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_CALL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_EMAIL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_OTHER;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_SMS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FAKE_RING_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_KEY_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_PREVIEW_MESSAGE_IN_TITLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_ACTIVETIME_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEPS_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_YEAR_OF_BIRTH;

public class CasioGBX100DeviceSupport extends Casio2C2DSupport implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGBX100DeviceSupport.class);

    private boolean mGetConfigurationPending = false;
    private boolean mRingNotificationPending = false;
    private final ArrayList<Integer> mSyncedNotificationIDs = new ArrayList<>();
    private int mLastCallId = new AtomicInteger((int) (System.currentTimeMillis()/1000)).incrementAndGet();
    private int mFakeRingDurationCounter = 0;
    private final Handler mFindPhoneHandler = new Handler();
    private final Handler mFakeRingDurationHandler = new Handler();
    private final Handler mAutoRemoveMessageHandler = new Handler();
    private final Handler mReconnectHandler = new Handler();
    private boolean mNeedsGetConfiguration = false;

    public CasioGBX100DeviceSupport() {
        super(LOG);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {

        try {
            new InitOperation(this, builder, mFirstConnect, mNeedsGetConfiguration).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Casio watch failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");


        //preferences.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);

        if(mFirstConnect) {
            SharedPreferences preferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString(DeviceSettingsPreferenceConst.PREFS_DEVICE_CHARTS_TABS, "activity,activitylist,stepsweek");
            editor.apply();
        }

        return builder;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if(data.length == 0)
            return true;

        return super.onCharacteristicRead(gatt, characteristic, status);
    }

    public CasioGBX100ActivitySample getSumWithinRange(int timestamp_from, int timestamp_to) {
        int steps = 0;
        int calories = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());

            CasioGBX100SampleProvider provider = new CasioGBX100SampleProvider(this.getDevice(), dbHandler.getDaoSession());
            List<CasioGBX100ActivitySample> samples = provider.getActivitySamples(timestamp_from, timestamp_to);
            for(CasioGBX100ActivitySample sample : samples) {
                if(sample.getDevice().equals(device) &&
                        sample.getUser().equals(user)) {
                    steps += sample.getSteps();
                    calories += sample.getCalories();
                }
            }
        } catch (Exception e) {
            LOG.error("Error fetching activity data.");
        }

        CasioGBX100ActivitySample ret = new CasioGBX100ActivitySample();
        ret.setCalories(calories);
        ret.setSteps(steps);
        LOG.debug("Fetched for today: " + calories + " cals and " + steps + " steps.");
        return ret;
    }

    private void addGBActivitySamples(ArrayList<CasioGBX100ActivitySample> samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());

            CasioGBX100SampleProvider provider = new CasioGBX100SampleProvider(this.getDevice(), dbHandler.getDaoSession());

            for (CasioGBX100ActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);

                provider.addGBActivitySample(sample);
            }

        } catch (Exception ex) {
            // Why is this a toast? The user doesn't care about the error.
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());

            LOG.error(ex.getMessage());
        }
    }

    public void stepCountDataFetched(int totalCount, int totalCalories, ArrayList<CasioGBX100ActivitySample> data) {
        LOG.info("Got the following step count data: ");
        LOG.info("Total Count: " + totalCount);
        LOG.info("Total Calories: " + totalCalories);

        addGBActivitySamples(data);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == FEATURE_ALERT_LEVEL) {
                if(data[1] == 0x02) {
                    onReverseFindDevice(true);
                } else {
                    onReverseFindDevice(false);
                }
                return true;
            } else if(data[0] == FEATURE_CURRENT_TIME_MANAGER) {
                if(data[1] == 0x00) {
                    try {
                        TransactionBuilder builder = performInitialized("writeCurrentTime");
                        writeCurrentTime(builder, ZonedDateTime.now());
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        LOG.warn("writing current time failed: " + e.getMessage());
                    }
                }
            }

        }

        LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    public void syncProfile() {
        try {
            new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_ALL).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Sending Casio configuration failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    /**
     * Sends a notification to the watch module. Overloaded - this method does not specify a subtitle
     * since subtitle isn't really used yet.
     * @param icon Icon to use - see {@link CasioConstants}
     * @param sender Sender (null if none)
     * @param title Notification title (null if none)
     * @param message Message body (null if none)
     * @param id Notification id
     * @param delete true if sending a delete of notification to the device
     */
    private void showNotification(byte icon, String sender, String title, String message, int id, boolean delete) {
        showNotification(icon, sender, title, null, message, id, delete);
    }

    /**
     * Sends a notification to the watch module. Overloaded - this method does not specify a subtitle
     * @param icon Icon to use - see {@link CasioConstants}
     * @param sender Sender (null if none)
     * @param title Notification title (null if none)
     * @param subtitle Message subtitle (null if none)
     * @param message Message body (null if none)
     * @param id Notification id
     */
    private void showNotification(byte icon, String sender, String title, String subtitle, String message, int id, boolean delete) {
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        boolean showMessagePreview = sharedPreferences.getBoolean(PREF_PREVIEW_MESSAGE_IN_TITLE, true);

        boolean shouldAlert = true;
        switch (icon) {
            case CasioConstants.CATEGORY_EMAIL:
                shouldAlert = sharedPreferences.getBoolean(PREF_CASIO_ALERT_EMAIL, true);
                break;
            case CasioConstants.CATEGORY_OTHER:
                shouldAlert = sharedPreferences.getBoolean(PREF_CASIO_ALERT_OTHER, true);
                break;
            case CasioConstants.CATEGORY_SNS:
                shouldAlert = sharedPreferences.getBoolean(PREF_CASIO_ALERT_SMS, true);
                break;
            case CasioConstants.CATEGORY_INCOMING_CALL:
                shouldAlert = sharedPreferences.getBoolean(PREF_CASIO_ALERT_CALL, true);
                break;
            case CasioConstants.CATEGORY_SCHEDULE_AND_ALARM:
                shouldAlert = sharedPreferences.getBoolean(PREF_CASIO_ALERT_CALENDAR, true);
                break;
        }

        // If not a call or email, check the sender and if null, promote the title and message preview
        // as subtitle
        if (showMessagePreview && icon != CasioConstants.CATEGORY_INCOMING_CALL && icon != CasioConstants.CATEGORY_EMAIL) {
            if (sender == null) {
                // Shift title to sender slot
                sender = title;
            }
            //Shift content to title
            if (message != null) {
                title = message.substring(0, Math.min(message.length(), 18)) + "..";
            }
        }

        // Make sure title and sender are less than 32 characters
        byte[] titleBytes = new byte[0];
        if(title != null) {
            if (title.length() > 32) {
                title = title.substring(0, 30) + "..";
            }
            titleBytes = title.getBytes(StandardCharsets.UTF_8);
        }

        byte[] messageBytes = new byte[0];
        if(message != null) {
            messageBytes = message.getBytes(StandardCharsets.UTF_8);
        }

        byte[] senderBytes = new byte[0];
        if(sender != null) {
            if (sender.length() > 32) {
                sender = sender.substring(0, 30) + "..";
            }
            senderBytes = sender.getBytes(StandardCharsets.UTF_8);
        }

        byte[] subtitleBytes = new byte[0];
        if (subtitle != null) {
            subtitleBytes = subtitle.getBytes(StandardCharsets.UTF_8);
        }

        byte[] arr = new byte[22];
        arr[0] = (byte)(id & 0xff);
        arr[1] = (byte) ((id >> 8) & 0xff);
        arr[2] = (byte) ((id >> 16) & 0xff);
        arr[3] = (byte) ((id >> 24) & 0xff);
        arr[4] = delete ? (byte) 0x02 : (byte) 0x00;
        if (shouldAlert) {
            arr[5] = (byte) 0x01; // Set to 0x0 to vibrate/ring for this notification
        } else {
            arr[5] = (byte) 0x00; // Set to 0x00 to not vibrate/ring for this notification
        }

        arr[6] = icon;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        String timestamp = dateFormat.format(new Date());
        System.arraycopy(timestamp.getBytes(), 0, arr, 7, 15);

        byte[] copy = Arrays.copyOf(arr, arr.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if(senderBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + senderBytes.length);
            copy[copy.length-2-senderBytes.length] = (byte)(senderBytes.length & 0xff);
            copy[copy.length-1-senderBytes.length] = (byte)((senderBytes.length >> 8) & 0xff);
            System.arraycopy(senderBytes, 0, copy, copy.length - senderBytes.length, senderBytes.length);
        }
        copy = Arrays.copyOf(copy, copy.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if(titleBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + titleBytes.length);
            copy[copy.length-2-titleBytes.length] = (byte)(titleBytes.length & 0xff);
            copy[copy.length-1-titleBytes.length] = (byte)((titleBytes.length >> 8) & 0xff);
            System.arraycopy(titleBytes, 0, copy, copy.length - titleBytes.length, titleBytes.length);
        }
        copy = Arrays.copyOf(copy, copy.length + 2);

        // Subtitle
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if (subtitleBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + subtitleBytes.length);
            copy[copy.length-2-subtitleBytes.length] = (byte)(subtitleBytes.length & 0xff);
            copy[copy.length-1-subtitleBytes.length] = (byte)((subtitleBytes.length >> 8) & 0xff);
            System.arraycopy(subtitleBytes, 0, copy, copy.length - subtitleBytes.length, subtitleBytes.length);
        }

        copy = Arrays.copyOf(copy, copy.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if(messageBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + messageBytes.length);
            copy[copy.length-2-messageBytes.length] = (byte)(messageBytes.length & 0xff);
            copy[copy.length-1-messageBytes.length] = (byte)((messageBytes.length >> 8) & 0xff);
            System.arraycopy(messageBytes, 0, copy, copy.length - messageBytes.length, messageBytes.length);
        }
        for(int i=0; i<copy.length; i++) {
            copy[i] = (byte)(~copy[i]);
        }

        try {
            TransactionBuilder builder = performInitialized("showNotification");
            builder.write(getCharacteristic(CasioConstants.CASIO_NOTIFICATION_CHARACTERISTIC_UUID), copy);
            LOG.info("Showing notification, title: " + title + " message (not sent): " + message);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("showNotification failed: " + e.getMessage());
        }
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        byte icon;
        boolean autoremove = false;
        switch (notificationSpec.type) {
            case GENERIC_CALENDAR:
                icon = CasioConstants.CATEGORY_SCHEDULE_AND_ALARM;
                break;
            case GENERIC_EMAIL:
            case GMAIL:
            case GOOGLE_INBOX:
                icon = CasioConstants.CATEGORY_EMAIL;
                break;
            case GENERIC_SMS:
                icon = CasioConstants.CATEGORY_SNS;
                SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                autoremove = sharedPreferences.getBoolean(PREF_AUTOREMOVE_MESSAGE, false);
                break;
            case GENERIC_PHONE:
                icon = CasioConstants.CATEGORY_INCOMING_CALL;
                break;
            default:
                icon = CasioConstants.CATEGORY_OTHER;
                break;
        }
        LOG.info("onNotification id=" + notificationSpec.getId());
        showNotification(icon, notificationSpec.sender, notificationSpec.title, notificationSpec.body, notificationSpec.getId(), false);
        mSyncedNotificationIDs.add(notificationSpec.getId());
        if(autoremove) {
            mAutoRemoveMessageHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onDeleteNotification(notificationSpec.getId());
                }
            }, CasioConstants.CASIO_AUTOREMOVE_MESSAGE_DELAY);
        }
        // The watch only holds up to 10 notifications. However, the user might have deleted
        // some notifications in the meantime, so to be sure, we keep the last 100 IDs.
        if(mSyncedNotificationIDs.size() > 100) {
            mSyncedNotificationIDs.remove(0);
        }
    }

    @Override
    public void onDeleteNotification(int id) {
        LOG.info("onDeleteNofication id=" + id);
        Integer idInt = id;
        if(mSyncedNotificationIDs.contains(idInt)) {
            showNotification(CasioConstants.CATEGORY_OTHER, null, null, null, id, true);
            mSyncedNotificationIDs.remove(idInt);
        }
    }

    private void onReverseFindDevice(boolean start) {
        if (start) {
            SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

            String findPhone = sharedPreferences.getString(PREF_FIND_PHONE, getContext().getString(R.string.p_off));

            if(findPhone.equals(getContext().getString(R.string.p_off)))
                return;

            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
            evaluateGBDeviceEvent(findPhoneEvent);

            if(findPhone.equals(getContext().getString(R.string.p_on))) {
                String duration = sharedPreferences.getString(PREF_FIND_PHONE_DURATION, "0");

                try {
                    int iDuration;

                    try {
                        iDuration = Integer.parseInt(duration);
                    } catch (Exception ex) {
                        LOG.warn(ex.getMessage());
                        iDuration = 60;
                    }
                    if(iDuration > 0) {
                        this.mFindPhoneHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                                evaluateGBDeviceEvent(findPhoneEvent);
                            }
                        }, iDuration * 1000);
                    }
                } catch (Exception e) {
                    LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
                }
            }
        } else {
            // Always send stop, ignore preferences.
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
            evaluateGBDeviceEvent(findPhoneEvent);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        byte[] data1 = new byte[5];
        byte[] data2 = new byte[17];

        if(!isConnected())
            return;

        data1[0] = FEATURE_SETTING_FOR_ALM;
        data2[0] = FEATURE_SETTING_FOR_ALM2;

        for(int i=0; i<alarms.size(); i++)
        {
            byte[] settings = new byte[4];
            Alarm alm = alarms.get(i);
            if(alm.getEnabled()) {
                settings[0] = 0x40;
                if (alm.getSnooze()) {
                    settings[0] = 0x50;
                }
            } else {
                settings[0] = 0;
            }
            settings[1] = 0x40;
            settings[2] = (byte)alm.getHour();
            settings[3] = (byte)alm.getMinute();
            if(i == 0) {
                System.arraycopy(settings, 0, data1, 1, settings.length);
            } else {
                System.arraycopy(settings, 0, data2, 1 + (i-1)*4, settings.length);
            }
        }
        try {
            TransactionBuilder builder = performInitialized("setAlarm");
            writeAllFeatures(builder, data1);
            writeAllFeatures(builder, data2);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.error("Error setting alarm: " + e.getMessage());
        }
    }

    @Override
    public void onSetTime() {
        LOG.debug("onSetTime called");
        try {
            TransactionBuilder builder = performInitialized("onSetTime");
            writeCurrentTime(builder, ZonedDateTime.now());
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("onSetTime failed: " + e.getMessage());
        }
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(CasioConstants.CATEGORY_INCOMING_CALL, callSpec.name, callSpec.number, "Phone Call", mLastCallId, false);
                SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                boolean fakeRingDuration = sharedPreferences.getBoolean(PREF_FAKE_RING_DURATION, false);
                if(fakeRingDuration && mFakeRingDurationCounter < CasioConstants.CASIO_FAKE_RING_RETRIES) {
                    mFakeRingDurationCounter++;
                    mFakeRingDurationHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNotification(CasioConstants.CATEGORY_INCOMING_CALL, null, null, null, mLastCallId, true);
                            onSetCallState(callSpec);
                        }
                    }, CasioConstants.CASIO_FAKE_RING_SLEEP_DURATION);
                } else {
                    mFakeRingDurationCounter = 0;
                }
                mRingNotificationPending = true;
                break;
            default:
                if(mRingNotificationPending) {
                    mFakeRingDurationHandler.removeCallbacksAndMessages(null);
                    mFakeRingDurationCounter = 0;
                    showNotification(CasioConstants.CATEGORY_INCOMING_CALL, null, null, null, mLastCallId, true);
                    mLastCallId = new AtomicInteger((int) (System.currentTimeMillis() / 1000)).incrementAndGet();
                }
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            new FetchStepCountDataOperation(this).perform();
        } catch(IOException e) {
            GB.toast(getContext(), "Error fetching data", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        LOG.info("onSendConfiguration" + config);
        onSharedPreferenceChanged(null, config);
    }

    public void onGetConfigurationFinished() {
        mGetConfigurationPending = false;
    }

    @Override
    public void onReadConfiguration(String config) {
        LOG.info("onReadConfiguration" + config);
        // This is called upon pairing to retrieve the current watch settings, if any
        if(config == null) {
            try {
                mGetConfigurationPending = true;
                mNeedsGetConfiguration = false;
                new GetConfigurationOperation(this, true).perform();
            } catch (IOException e) {
                mGetConfigurationPending = false;
                GB.toast(getContext(), "Reading Casio configuration failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            }
        }
    }

    @Override
    public void onTestNewFunction() {
        byte[] data = new byte[2];
        data[0] = (byte)0x2e;
        data[1] = (byte)0x03;
        try {
            TransactionBuilder builder = performInitialized("onTestNewFunction");
            writeAllFeaturesRequest(builder, data);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.error("Error setting alarm: " + e.getMessage());
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOG.debug(key + " changed");

        if (!this.isConnected()) {
            LOG.debug("ignoring change, we're disconnected");
            return;
        }

        if(mGetConfigurationPending) {
            LOG.debug("Preferences are being fetched right now");
            return;
        }
        try {
            switch (key) {
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_WRIST).perform();
                    break;
                case PREF_USER_STEPS_GOAL:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_STEP_GOAL).perform();
                    break;
                case PREF_USER_ACTIVETIME_MINUTES:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_ACTIVITY_GOAL).perform();
                    break;
                case PREF_USER_DISTANCE_METERS:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_DISTANCE_GOAL).perform();
                    break;
                case PREF_USER_GENDER:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_GENDER).perform();
                    break;
                case PREF_USER_HEIGHT_CM:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_HEIGHT).perform();
                    break;
                case PREF_USER_WEIGHT_KG:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_WEIGHT).perform();
                    break;
                case PREF_USER_YEAR_OF_BIRTH:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_BIRTHDAY).perform();
                    break;
                case PREF_TIMEFORMAT:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_TIMEFORMAT).perform();
                    break;
                case PREF_KEY_VIBRATION:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_KEY_VIBRATION).perform();
                    break;
                case PREF_AUTOLIGHT:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_AUTOLIGHT).perform();
                    break;
                case PREF_OPERATING_SOUNDS:
                    new SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_OPERATING_SOUNDS).perform();
                    break;
                case PREF_FAKE_RING_DURATION:
                case PREF_FIND_PHONE:
                case PREF_FIND_PHONE_DURATION:
                    // No action, we check the shared preferences when the device tries to ring the phone.
                    break;
                case PREF_PREVIEW_MESSAGE_IN_TITLE:
                case PREF_CASIO_ALERT_CALENDAR:
                case PREF_CASIO_ALERT_CALL:
                case PREF_CASIO_ALERT_EMAIL:
                case PREF_CASIO_ALERT_OTHER:
                case PREF_CASIO_ALERT_SMS:
                    // No action, we check it when message is received
                    break;
                default:
            }
        } catch (IOException e) {
            LOG.info("Error sending configuration change to watch");
        }
    }

    public void reconnectDelayed() {
        setAutoReconnect(true);
        mNeedsGetConfiguration = true;
        mReconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }, CasioConstants.CASIO_FAKE_RING_SLEEP_DURATION);
    }
}
