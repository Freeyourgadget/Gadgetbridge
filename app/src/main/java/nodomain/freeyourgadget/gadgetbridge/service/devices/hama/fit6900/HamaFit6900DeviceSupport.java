/*
Copyright (C) 2024 enoint

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
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nodomain.freeyourgadget.gadgetbridge.service.devices.hama.fit6900;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.os.Handler;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900.HamaFit6900Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900.Message;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public final class HamaFit6900DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(HamaFit6900DeviceSupport.class);

    private BluetoothGattCharacteristic writeCharacteristic;

    private int notificationCount = 0;
    private final Handler findPhoneStopNotificationHandler = new Handler();

    public HamaFit6900DeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(HamaFit6900Constants.UUID_SERVICE_RXTX);
    }

    @Override
    public TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        writeCharacteristic = getCharacteristic(HamaFit6900Constants.UUID_CHARACTERISTIC_TX);

        builder.notify(getCharacteristic(HamaFit6900Constants.UUID_CHARACTERISTIC_RX), true);
        builder.setCallback(this);

        builder.write(writeCharacteristic, Message.encodeGetBatteryStatus());
        builder.write(writeCharacteristic, Message.encodeGetFirmwareVersion());

        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            builder.write(writeCharacteristic, makeSetDateTimeMessage());
        }
        // sync all preferences to device
        builder.write(writeCharacteristic, makeSetSystemDataMessage());
        builder.write(writeCharacteristic, makeSetUnitMessage());
        builder.write(writeCharacteristic, makeSetUserInfoMessage());
        builder.write(writeCharacteristic, makeSetAutoHeartRate());
        builder.write(writeCharacteristic, makeSetDoNotDisturbMessage());
        builder.write(writeCharacteristic, makeSetHydrationReminderMessage());
        builder.write(writeCharacteristic, makeSetLiftWristMessage());
        builder.write(writeCharacteristic, Message.encodeSetAlarms(new ArrayList(DBHelper.getAlarms(gbDevice))));

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @Override
    public void onSendConfiguration(final String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                sendMessage("update-language+timeformat", makeSetSystemDataMessage());
                return;

            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                sendMessage("update-units", makeSetUnitMessage());
                return;

            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_GENDER:
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
            case DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL:
                sendMessage("update-user-info", makeSetUserInfoMessage());
                return;

            case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_START:
            case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_END:
            case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_INTERVAL: {
                byte[] msg = makeSetAutoHeartRate();
                if (msg != null) {
                    sendMessage("update-auto-heart-rate", msg);
                }
                return;
            }

            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END: {
                byte[] msg = makeSetDoNotDisturbMessage();
                if (msg != null)
                    sendMessage("update-do-not-disturb", msg);
                return;
            }

            case DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD: {
                byte[] msg = makeSetHydrationReminderMessage();
                if (msg != null)
                    sendMessage("update-hydration-reminder", msg);
                return;
            }

            case DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
                sendMessage("update-lift-wrist", makeSetLiftWristMessage());
                return;
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        if (!characteristic.getUuid().equals(HamaFit6900Constants.UUID_CHARACTERISTIC_RX)) {
            return false;
        }

        byte[] receivedData = characteristic.getValue();

        Message.CommandMessage cmdMsg = Message.decodeCommandMessage(receivedData);
        if (cmdMsg == null) {
            return false;
        }

        final byte[] cmdArgs = cmdMsg.cmdArgs;
        switch (cmdMsg.cmd) {
            case 1:
                switch (cmdMsg.key) {
                    case 19: // response to GetFirmwareVersion
                        final String fwVersion = String.format("%d.%d.%d", cmdArgs[0] & 0xFF, cmdArgs[1] & 0xFF, cmdArgs[2] & 0xFF);
                        // data[3]: bracelet type
                        // data[>3]: ?
                        handleFirmwareVersion(fwVersion);
                        return true;
                }
                break;

            case 2:
                switch (cmdMsg.key) {
                    case 1: // response to SetUnit; no args
                    case 32: // response to SetDateTime; no args
                    case 33: // response to SetAlarms; no args
                    case 35: // response to SetUserInfo; no args
                    case 39: // response to SetSystemData; no args
                    case 40: // response to SetHydrationReminder; no args
                        return true;
                }
                break;

            case 4:
                switch (cmdMsg.key) {
                    case 70: // notification msg: camera - open
                        handleCameraRemote(GBDeviceEventCameraRemote.Event.OPEN_CAMERA);
                        return true;
                    case 71: // notification msg: camera - capture
                        handleCameraRemote(GBDeviceEventCameraRemote.Event.TAKE_PICTURE);
                        return true;
                    case 72: // notification msg: camera - close
                        handleCameraRemote(GBDeviceEventCameraRemote.Event.CLOSE_CAMERA);
                        return true;

                    case 74: // response to SetLiftWriteDisplayOn; no args
                        return true;

                    case 65: // response to GetBatteryStatus
                        handleBatteryStatus(cmdArgs);
                        return true;
                }
                break;

            case 5:
                switch (cmdMsg.key) {
                    case 80: // response to FindDevice; length=1, [0]
                        return true;
                    case 81: // notification msg: find phone; watch sends no notification to stop it
                        handleFindPhone(true);
                        return true;
                }
                break;

            case 6:
                switch (cmdMsg.key) {
                    case 96: // response to ShowNotification; no args
                    case 100: // response to SetDoNotDisturb; no args
                        return true;
                }
                break;

            case 9:
                switch (cmdMsg.key) {
                    case 146: // response to SetAutoHeartRate; no args
                        return true;
                }
                break;

            case 10:
                switch (cmdMsg.key) {
                    case 171: // notification msg: heart rate update
                        // int heartRate = cmdArgs[0] & 0xFF;
                        break;
                    case 172: // notification msg: steps update
                        // int32 steps; float calories [kCal]; float distance [km]
                        //int steps = Message.decodeInt32(cmdArgs, 0);
                        break;
                }
                break;

            case 13:
                switch (cmdMsg.key) {
                    case 2: // notification msg: hang up call
                        // in case of ShowNotification(INCOMING_CALL,..) the watch shows a hang up
                        // button which triggers this notification. Accepting a call is not supported.
                        handleCallReject();
                        return true;

                    case 4: // notification msg: media player - play
                    case 5: // notification msg: media player - pause
                        // Watch only shows a single play/pause button and has no idea of media player playing state.
                        // So just toggle between play and pause.
                        handleMusicControl(GBDeviceEventMusicControl.Event.PLAYPAUSE);
                        return true;
                    case 6: // notification msg: media player - previous
                        handleMusicControl(GBDeviceEventMusicControl.Event.PREVIOUS);
                        return true;
                    case 7: // notification msg: media player - next
                        handleMusicControl(GBDeviceEventMusicControl.Event.NEXT);
                        return true;
                }
                break;
        }

        //LOG.debug(" command {},{} | NOT HANDLED", cmdMsg.cmd, cmdMsg.key);

        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (!getDevicePrefsNotificationEnabled()) {
            return;
        }

        Message.NotificationType type = Message.NotificationType.UNKNOWN;
        switch (notificationSpec.type) {
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                type = Message.NotificationType.FACEBOOK;
                break;
            case GENERIC_SMS:
                type = Message.NotificationType.SMS;
                break;
            case INSTAGRAM:
                type = Message.NotificationType.INSTAGRAM;
                break;
            case LINKEDIN:
                type = Message.NotificationType.LINKEDIN;
                break;
            case TWITTER:
                type = Message.NotificationType.TWITTER;
                break;
            case WHATSAPP:
                type = Message.NotificationType.WHATSAPP;
                break;
        }
        final String notificationMsg = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        final String uniqueTaskName = "notification" + notificationCount;
        notificationCount++;

        sendMessage(uniqueTaskName, Message.encodeShowNotification(type, notificationMsg));
    }

    @Override
    public void onSetTime() {
        sendMessage("set-datetime", makeSetDateTimeMessage());
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        sendMessage("set-alarms", Message.encodeSetAlarms(alarms));
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING: {
                if (getDevicePrefsNotificationEnabled()) {
                    final String text = StringUtils.getFirstOf(callSpec.name, callSpec.number);
                    sendMessage("notification-call-incoming",
                            Message.encodeShowNotification(Message.NotificationType.INCOMING_CALL, text));
                }
                break;
            }
            case CallSpec.CALL_ACCEPT:
                // aborts INCOMING_CALL notification
                sendMessage("notification-call-accept",
                        Message.encodeShowNotification(Message.NotificationType.CALL_ACCEPT, ""));
                break;
            case CallSpec.CALL_REJECT:
            case CallSpec.CALL_END:
                // aborts INCOMING_CALL notification
                sendMessage("notification-call-reject",
                        Message.encodeShowNotification(Message.NotificationType.CALL_REJECT, ""));
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            sendMessage("find-device", Message.encodeFindDevice());
        }
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private boolean sendMessage(String taskName, byte[] message) {
        try {
            TransactionBuilder builder = performInitialized(taskName);
            builder.write(writeCharacteristic, message);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error(taskName, ex);
            return false;
        }
        return true;
    }

    private boolean getDevicePrefsNotificationEnabled() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        return prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
    }

    private Message.TimeFormat getDevicePrefsTimeFormat() {
        Message.TimeFormat timeFormat = null;
        switch (getDevicePrefs().getTimeFormat()) {
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H:
                timeFormat = Message.TimeFormat.Format24H;
                break;
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H:
                timeFormat = Message.TimeFormat.Format12H;
                break;
        }
        return timeFormat;
    }

    private byte[] makeSetDateTimeMessage() {
        return Message.encodeSetDateTime(Calendar.getInstance(), getDevicePrefsTimeFormat());
    }

    private byte[] makeSetSystemDataMessage() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        final String localeString = prefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO);

        String language;
        String country;
        if (localeString.equals(DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO)) {
            language = Locale.getDefault().getLanguage();
            country = Locale.getDefault().getCountry();
        } else {
            language = localeString.substring(0, 2);
            country = localeString.substring(3, 5);
        }

        return Message.encodeSetSystemData(language, country, getDevicePrefsTimeFormat());
    }

    private byte[] makeSetUnitMessage() {
        final Prefs prefs = GBApplication.getPrefs();
        String unit = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric");

        return Message.encodeSetUnit(unit.equals("metric"));
    }

    private byte[] makeSetUserInfoMessage() {
        final ActivityUser activityUser = new ActivityUser();
        return Message.encodeSetUserInfo(
                (activityUser.getGender() == ActivityUser.GENDER_MALE) ? Message.Gender.MALE : Message.Gender.FEMALE,
                activityUser.getAge(),
                activityUser.getHeightCm(),
                activityUser.getWeightKg(),
                activityUser.getStepsGoal());
    }

    private byte[] makeSetDoNotDisturbMessage() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        final String enabled = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, "off");
        if (!enabled.equals(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED)) {
            return Message.encodeSetDoNotDisturb(false, 0, 0, 0, 0);
        }

        final String start = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, "22:00");
        final String end = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, "6:00");

        final Calendar startCalendar = GregorianCalendar.getInstance();
        final Calendar endCalendar = GregorianCalendar.getInstance();
        final DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            return null;
        }

        return Message.encodeSetDoNotDisturb(true, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE),
                endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE));
    }

    private byte[] makeSetAutoHeartRate() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SWITCH, false);
        if (!enabled) {
            return Message.encodeSetAutoHeartRate(false, 0, 0, 0, 0, 0);
        }

        // PREF_AUTOHEARTRATE_SLEEP is not supported
        final String start = prefs.getString(DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_START, "22:00");
        final String end = prefs.getString(DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_END, "6:00");
        final String intervalStr = prefs.getString(DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_INTERVAL, "2");

        final Calendar startCalendar = GregorianCalendar.getInstance();
        final Calendar endCalendar = GregorianCalendar.getInstance();
        final DateFormat df = new SimpleDateFormat("HH:mm");
        final int intervalMinutes;

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            return null;
        }

        try {
            intervalMinutes = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            return null;
        }

        return Message.encodeSetAutoHeartRate(true, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE),
                endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), intervalMinutes);
    }

    private byte[] makeSetHydrationReminderMessage() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, false);
        final String intervalStr = prefs.getString(DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD, "60");
        int intervalMinutes;
        try {
            intervalMinutes = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (intervalMinutes == 0) {
            enabled = false;
        }

        // Drink reminder notifications honor the do-not-disturb time range.
        // Start time must be > then end time; e.g. start=19, end=1 won't work.
        // enable=true and start=end=0 behaves as disabled.

        int startHour = 0;
        int startMin = 0;
        int endHour = 23;
        int endMin = 59;

        return Message.encodeSetHydrationReminder(enabled, startHour, startMin, endHour, endMin, intervalMinutes);
    }

    private byte[] makeSetLiftWristMessage() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        final String enabled = prefs.getString(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, "off");

        final boolean isEnabled = !enabled.equals("off");
        return Message.encodeSetLiftWristDisplayOn(isEnabled);
    }

    private void handleFirmwareVersion(String fwVersion) {
        final GBDeviceEventVersionInfo event = new GBDeviceEventVersionInfo();
        event.fwVersion = fwVersion;
        event.fwVersion2 = null;
        event.hwVersion = null;
        evaluateGBDeviceEvent(event);
    }

    private void handleCallReject() {
        final GBDeviceEventCallControl event = new GBDeviceEventCallControl();
        event.event = GBDeviceEventCallControl.Event.REJECT;
        evaluateGBDeviceEvent(event);
    }

    private void handleCameraRemote(GBDeviceEventCameraRemote.Event eventType) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CAMERA_REMOTE, false))
            return;

        final GBDeviceEventCameraRemote event = new GBDeviceEventCameraRemote();
        event.event = eventType;
        evaluateGBDeviceEvent(event);
    }

    private void handleMusicControl(GBDeviceEventMusicControl.Event eventType) {
        final GBDeviceEventMusicControl event = new GBDeviceEventMusicControl();
        event.event = eventType;
        evaluateGBDeviceEvent(event);
    }

    private void handleFindPhone(boolean start) {
        if (start) {
            SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
            String findPhone = sharedPreferences.getString(PREF_FIND_PHONE, getContext().getString(R.string.p_off));
            if (findPhone.equals("off"))
                return;

            String durationSecStr = sharedPreferences.getString(PREF_FIND_PHONE_DURATION, "");
            int durationSec;
            try {
                durationSec = Integer.parseInt(durationSecStr);
            } catch (Exception ex) {
                durationSec = 60;
            }
            if (durationSec <= 0)
                return;

            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
            evaluateGBDeviceEvent(findPhoneEvent);

            try {
                this.findPhoneStopNotificationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handleFindPhone(false);
                    }
                }, durationSec * 1000);

            } catch (Exception ex) {
                handleFindPhone(false);
            }
        } else {
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
            evaluateGBDeviceEvent(findPhoneEvent);
        }
    }

    private void handleBatteryStatus(byte[] cmdArgs) {
        /*
        final int level = cmdArgs[0] & 0xFF;
        final int type = cmdArgs[1] & 0xFF;

        Disabled since watch always returns a level value of 60
        Also missing: status needs to be polled regularly

        GBDeviceEventBatteryInfo event = new GBDeviceEventBatteryInfo();
        event.level = level;
        evaluateGBDeviceEvent(event); */
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
