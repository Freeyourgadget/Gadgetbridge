/*  Copyright (C) 2018-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, maxirnilian, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.watchxplus;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DebugActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.DataType;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus.WatchXPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus.WatchXPlusDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus.WatchXPlusSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.WatchXPlusHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.operations.InitOperation;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class WatchXPlusDeviceSupport extends AbstractBTLEDeviceSupport {
    protected static Prefs prefs  = GBApplication.getPrefs();
    private boolean needsAuth;
    private int sequenceNumber = 0;
    private boolean isCalibrationActive = false;

    private Map<Integer, Integer> dataToFetch = new LinkedHashMap<>();
    private int requestedDataTimestamp;
    private int dataSlots = 0;
    private DataType currentDataType;

    private byte ACK_CALIBRATION = 0;

    private final GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();

    private static final Logger LOG = LoggerFactory.getLogger(WatchXPlusDeviceSupport.class);

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String broadcastAction = intent.getAction();
            switch (broadcastAction) {
                case WatchXPlusConstants.ACTION_CALIBRATION:
                    enableCalibration(intent.getBooleanExtra(WatchXPlusConstants.ACTION_ENABLE, false));
                    break;
                case WatchXPlusConstants.ACTION_CALIBRATION_SEND:
                    int hour = intent.getIntExtra(WatchXPlusConstants.VALUE_CALIBRATION_HOUR, -1);
                    int minute = intent.getIntExtra(WatchXPlusConstants.VALUE_CALIBRATION_MINUTE, -1);
                    int second = intent.getIntExtra(WatchXPlusConstants.VALUE_CALIBRATION_SECOND, -1);
                    if (hour != -1 && minute != -1 && second != -1) {
                        sendCalibrationData(hour, minute, second);
                    }
                    break;
                case WatchXPlusConstants.ACTION_CALIBRATION_HOLD:
                    holdCalibration();
                    break;
            }
        }
    };

    public WatchXPlusDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(WatchXPlusConstants.UUID_SERVICE_WATCHXPLUS);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WatchXPlusConstants.ACTION_CALIBRATION);
        intentFilter.addAction(WatchXPlusConstants.ACTION_CALIBRATION_SEND);
        intentFilter.addAction(WatchXPlusConstants.ACTION_CALIBRATION_HOLD);
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        try {
            boolean auth = needsAuth;
            needsAuth = false;
            new InitOperation(auth, this, builder).perform();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder;
    }

    @Override
    public boolean connectFirstTime() {
        needsAuth = true;
        return super.connect();
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        String message = StringUtils.truncate(senderOrTitle, 14) + "\0";
        if (notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 20) + ": ";
        }
        if (notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 64);
        }

        sendNotification(WatchXPlusConstants.NOTIFICATION_CHANNEL_DEFAULT, message);
    }

    /** Cancel notification
     * cancel watch notification - stop vibration and turn off screen
     * on watch - clear phone icon near bluetooth
    */
    private void cancelNotification() {
        try {
            getQueue().clear();
            TransactionBuilder builder = performInitialized("cancelNotification");
            byte[] bArr;
            int mPosition = 1024;   // all positions
            int mMessageId = 0xFF;  // all messages
            bArr = new byte[6];
            bArr[0] = (byte) ((int) (mPosition >> 24));
            bArr[1] = (byte) ((int) (mPosition >> 16));
            bArr[2] = (byte) ((int) (mPosition >> 8));
            bArr[3] = (byte) ((int) mPosition);
            bArr[4] = (byte) (mMessageId >> 8);
            bArr[5] = (byte) mMessageId;
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_NOTIFICATION_CANCEL,
                            WatchXPlusConstants.WRITE_VALUE,
                            bArr));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to cancel notification", e);
        }
    }

    /** Format text and send it to watch
     * @param notificationChannel - text or call
     * @param notificationText - text to show
     */
    private void sendNotification(int notificationChannel, String notificationText) {
        try {
            TransactionBuilder builder = performInitialized("showNotification");
            byte[] command = WatchXPlusConstants.CMD_NOTIFICATION_TEXT_TASK;
            byte[] text = notificationText.getBytes("UTF-8");
            byte[] messagePart;

            int messageLength = text.length;
            int parts = messageLength / 9;
            int remainder = messageLength % 9;

//            Increment parts quantity if message length is not multiple of 9
            if (remainder != 0) {
                parts++;
            }
            for (int messageIndex = 0; messageIndex < parts; messageIndex++) {
                if (messageIndex + 1 != parts || remainder == 0) {
                    messagePart = new byte[11];
                } else {
                    messagePart = new byte[remainder + 2];
                }

                System.arraycopy(text, messageIndex * 9, messagePart, 2, messagePart.length - 2);

                if (messageIndex + 1 == parts) {
                    messageIndex = 0xFF;
                }
                messagePart[0] = (byte) notificationChannel;
                messagePart[1] = (byte) messageIndex;
                builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                          WatchXPlusConstants.KEEP_ALIVE,
                          messagePart));
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to send notification", e);
        }
    }

    /** enable notification channels on watch
     * @param builder
     * enable all notification channels
     * TODO add settings to choose notification channels
     */
    private WatchXPlusDeviceSupport enableNotificationChannels(TransactionBuilder builder) {
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_NOTIFICATION_SETTINGS,
                        WatchXPlusConstants.WRITE_VALUE,
                        new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));

        return this;
    }

    public WatchXPlusDeviceSupport authorizationRequest(TransactionBuilder builder, boolean firstConnect) {
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_AUTHORIZATION_TASK,
                        WatchXPlusConstants.TASK,
                        new byte[]{(byte) (firstConnect ? 0x00 : 0x01)})); //possibly not the correct meaning

        return this;
    }

    private void enableCalibration(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("enableCalibration");
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_CALIBRATION_INIT_TASK,
                            WatchXPlusConstants.TASK,
                            new byte[]{(byte) (enable ? 0x01 : 0x00)}));
            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to start/stop calibration mode", e);
        }
    }

    private void holdCalibration() {
        try {
            TransactionBuilder builder = performInitialized("holdCalibration");
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_CALIBRATION_KEEP_ALIVE,
                            WatchXPlusConstants.KEEP_ALIVE));
            performImmediately(builder);
        } catch (IOException e) {
            LOG.warn("Unable to keep calibration mode alive", e);
        }
    }

    private void sendCalibrationData(@IntRange(from = 0, to = 23) int hour, @IntRange(from = 0, to = 59) int minute, @IntRange(from = 0, to = 59) int second) {
        try {
            isCalibrationActive = true;
            TransactionBuilder builder = performInitialized("calibrate");
            int handsPosition = ((hour % 12) * 60 + minute) * 60 + second;
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_CALIBRATION_TASK,
                            WatchXPlusConstants.TASK,
                            Conversion.toByteArr16(handsPosition)));
            performImmediately(builder);
        } catch (IOException e) {
            isCalibrationActive = false;
            LOG.warn("Unable to send calibration data", e);
        }
    }

    private void getTime() {
        try {
            TransactionBuilder builder = performInitialized("getTime");
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_TIME_SETTINGS,
                            WatchXPlusConstants.READ_VALUE));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to get device time", e);
        }
    }

    private void handleTime(byte[] time) {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        GregorianCalendar nowDevice = BLETypeConversions.createCalendar();
        int year = (nowDevice.get(Calendar.YEAR) / 100) * 100 + Conversion.fromBcd8(time[8]);
        nowDevice.set(year,
                Conversion.fromBcd8(time[9]) - 1,
                Conversion.fromBcd8(time[10]),
                Conversion.fromBcd8(time[11]),
                Conversion.fromBcd8(time[12]),
                Conversion.fromBcd8(time[13]));
        nowDevice.set(Calendar.DAY_OF_WEEK, Conversion.fromBcd8(time[16]) + 1);

        long timeDiff = (Math.abs(now.getTimeInMillis() - nowDevice.getTimeInMillis())) / 1000;
        if (10 < timeDiff && timeDiff < 120) {
            enableCalibration(true);
            setTime(BLETypeConversions.createCalendar());
            enableCalibration(false);
        }
    }

    private void setTime(Calendar calendar) {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            int timezoneOffsetMinutes = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
            int timezoneOffsetIndustrialMinutes = Math.round((Math.abs(timezoneOffsetMinutes) % 60) * 100f / 60f);
            byte[] time = new byte[]{Conversion.toBcd8(calendar.get(Calendar.YEAR) % 100),
                    Conversion.toBcd8(calendar.get(Calendar.MONTH) + 1),
                    Conversion.toBcd8(calendar.get(Calendar.DAY_OF_MONTH)),
                    Conversion.toBcd8(calendar.get(Calendar.HOUR_OF_DAY)),
                    Conversion.toBcd8(calendar.get(Calendar.MINUTE)),
                    Conversion.toBcd8(calendar.get(Calendar.SECOND)),
                    (byte) (timezoneOffsetMinutes / 60),
                    (byte) timezoneOffsetIndustrialMinutes,
                    (byte) (calendar.get(Calendar.DAY_OF_WEEK) - 1)
            };
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_TIME_SETTINGS,
                            WatchXPlusConstants.WRITE_VALUE,
                            time));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to set time", e);
        }
    }

    /** send command to request watch firmware version
     * @param builder
     */
    public WatchXPlusDeviceSupport getFirmwareVersion(TransactionBuilder builder) {
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_FIRMWARE_INFO,
                        WatchXPlusConstants.READ_VALUE));

        return this;
    }

    /** send command to request watch battery state
     * @param builder
     */
    private WatchXPlusDeviceSupport getBatteryState(TransactionBuilder builder) {
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_BATTERY_INFO,
                        WatchXPlusConstants.READ_VALUE));

        return this;
    }

    /** initialize device on connect
     * @param builder
     */
    public WatchXPlusDeviceSupport initialize(TransactionBuilder builder) {
        getFirmwareVersion(builder)
                .getBatteryState(builder)
                .enableNotificationChannels(builder)
                .setFitnessGoal(builder)                        // set steps per day
                .getBloodPressureCalibrationStatus(builder)     // request blood pressure calibration
                .syncPreferences(builder);                      // read preferences from app and set them to watch
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        builder.setGattCallback(this);
        return this;
    }

    @Override
    public void onDeleteNotification(int id) {
        isMissedCall = false;
        cancelNotification();
    }

    @Override
    public void onSetTime() {
        getTime();
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("setAlarms");
            for (Alarm alarm : alarms) {
                setAlarm(alarm, alarm.getPosition() + 1, builder);
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to set alarms", e);
        }
    }

    // No useful use case at the moment, used to clear alarm slots for testing.
    private void deleteAlarm(TransactionBuilder builder, int index) {
        if (0 < index && index < 4) {
            byte[] alarmValue = new byte[]{(byte) index, 0x00, 0x00, 0x00, 0x00, 0x00};
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_ALARM_SETTINGS,
                            WatchXPlusConstants.WRITE_VALUE,
                            alarmValue));
        }
    }

    private void setAlarm(Alarm alarm, int index, TransactionBuilder builder) {
        // Shift the GB internal repetition mask to match the device specific one.
        byte repetitionMask = (byte) ((alarm.getRepetition() << 1) | (alarm.isRepetitive() ? 0x80 : 0x00));
        repetitionMask |= (alarm.getRepetition(Alarm.ALARM_SUN) ? 0x01 : 0x00);
        if (0 < index && index < 4) {
            byte[] alarmValue = new byte[]{(byte) index,
                    Conversion.toBcd8(AlarmUtils.toCalendar(alarm).get(Calendar.HOUR_OF_DAY)),
                    Conversion.toBcd8(AlarmUtils.toCalendar(alarm).get(Calendar.MINUTE)),
                    repetitionMask,
                    (byte) (alarm.getEnabled() ? 0x01 : 0x00),
                    0x00 // TODO: Unknown
            };
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_ALARM_SETTINGS,
                            WatchXPlusConstants.WRITE_VALUE,
                            alarmValue));
        }
    }

    /** send notification on watch when phone rings
     * @param callSpec - phone state
     * send notification on incoming call, cancel notification when call is answered, ignored or rejected
     * send missed call notification (if enabled from settings) when phone state changed from ringing to end call
     * TODO add missed call reminder (send notification to watch at desired period)
     */
    // variables to handle ring notifications
    boolean isRinging = false;  // store ringing state
    boolean outCall = false;    // store outgoing call state
    boolean isMissedCall = false;    // missed call state
    int remainingRepeats = 0;   // initialize call notification reminds
    int remainingMissedRepeats = 0;   // initialize missed call notification reminds
    @Override
    public void onSetCallState(final CallSpec callSpec) {
        final int repeatDelay = 5000;       // repeat delay of 5 sec (watch show call notifications for about 5 sec.)
        final int repeatMissedDelay = 60000;       // repeat missed call delay of 60 sec
        // get settings for continuous vibration while phone rings
        final boolean continuousRing = WatchXPlusDeviceCoordinator.getContiniousVibrationOnCall(getDevice().getAddress());
        // set settings for missed call
        boolean missedCall = WatchXPlusDeviceCoordinator.getMissedCallReminder(getDevice().getAddress());
        int repeatCount = WatchXPlusDeviceCoordinator.getRepeatOnCall(getDevice().getAddress());
        int repeatCountMissed = WatchXPlusDeviceCoordinator.getMissedCallRepeat(getDevice().getAddress());
        // check if repeatCount is in boundaries min=0, max=10
        if (repeatCount < 0) repeatCount = 0;
        if (repeatCount > 10) repeatCount = 10;   // limit repeats to 10
        // check if repeatCountMissed is in boundaries min=0, max=10
        if (repeatCountMissed < 0) repeatCountMissed = 0;
        if (repeatCountMissed > 10) repeatCountMissed = 10;   // limit repeats to 10

        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                isRinging = true;
                isMissedCall = false;
                remainingRepeats = repeatCount;
                if (("Phone".equals(callSpec.name)) || (callSpec.name.contains("ropusn")) || (callSpec.name.contains("issed"))) {
                    // do nothing for notifications without caller name, e.g. system call event
                } else {
                    // send first notification
                    sendNotification(WatchXPlusConstants.NOTIFICATION_CHANNEL_PHONE_CALL, callSpec.name);
                    // init repeat handler
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            // Actions to do after repeatDelay seconds
                            if (((isRinging) && (remainingRepeats > 0)) || ((isRinging) && (continuousRing))) {
                                remainingRepeats = remainingRepeats - 1;
                                sendNotification(WatchXPlusConstants.NOTIFICATION_CHANNEL_PHONE_CALL, callSpec.name);
                                // re-run handler
                                handler.postDelayed(this, repeatDelay);
                            } else {
                                remainingRepeats = 0;
                                // stop handler
                                handler.removeCallbacks(this);
                            }
                        }
                    }, repeatDelay);
                }
                break;
            case CallSpec.CALL_START:
                isRinging = false;
                outCall = false;
                isMissedCall = false;
                cancelNotification();
                break;
            case CallSpec.CALL_REJECT:
                isRinging = false;
                outCall = false;
                isMissedCall = false;
                cancelNotification();
                break;
            case CallSpec.CALL_ACCEPT:
                isRinging = false;
                outCall = false;
                isMissedCall = false;
                cancelNotification();
                break;
            case CallSpec.CALL_OUTGOING:
                outCall = true;
                isRinging = false;
                isMissedCall = false;
                cancelNotification();
                break;
            case CallSpec.CALL_END:
                if ((isRinging) && (!outCall)) {
                    // it's a missed call, don't clear notification to preserve small icon near bluetooth
                    isRinging = false;
                    outCall = false;
                    isMissedCall = true;
                    remainingMissedRepeats = repeatCountMissed;
                    // send missed call notification if enabled in settings
                    if (missedCall) {
                        sendNotification(WatchXPlusConstants.NOTIFICATION_CHANNEL_PHONE_CALL, "Missed call");
                        // repeat missed call notification
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                // Actions to do after repeatDelay seconds
                                if ((isMissedCall) && (remainingMissedRepeats > 0)) {
                                    remainingMissedRepeats = remainingMissedRepeats - 1;
                                    sendNotification(WatchXPlusConstants.NOTIFICATION_CHANNEL_PHONE_CALL, "Missed call");
                                    // re-run handler
                                    handler.postDelayed(this, repeatMissedDelay);
                                } else {
                                    remainingMissedRepeats = 0;
                                    isMissedCall = false;
                                    // stop handler
                                    handler.removeCallbacks(this);
                                }
                            }
                        }, repeatMissedDelay);
                    }
                } else {
                    isRinging = false;
                    outCall = false;
                    isMissedCall = false;
                    cancelNotification();
                }
                break;
            default:
                isRinging = false;
                isMissedCall = false;
                cancelNotification();
                break;
        }
    }

    /** handle button press while ringing
     * @param value - reply from watch
     *  while phone rings choose what to do when watch button is pressed
     */
    private void handleButtonWhenRing(byte[] value) {
        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
        // get saved settings if true - reject call, otherwise ignore call
        boolean buttonReject = WatchXPlusDeviceCoordinator.getButtonReject(getDevice().getAddress());
        if (buttonReject) {
            LOG.info(" call rejected ");
            isRinging = false;
            callCmd.event = GBDeviceEventCallControl.Event.REJECT;
            evaluateGBDeviceEvent(callCmd);
            cancelNotification();
        } else {
            LOG.info(" call ignored ");
            isRinging = false;
            callCmd.event = GBDeviceEventCallControl.Event.IGNORE;
            evaluateGBDeviceEvent(callCmd);
            cancelNotification();
        }
    }

    private WatchXPlusDeviceSupport setFitnessGoal(TransactionBuilder builder) {
        int fitnessGoal = new ActivityUser().getStepsGoal();
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_FITNESS_GOAL_SETTINGS,
                        WatchXPlusConstants.WRITE_VALUE,
                        Conversion.toByteArr16(fitnessGoal)));
        return this;
    }

    /** set personal info - read it from About me
     * @param builder
     * @param height - user height in meters
     * @param weight - user weight in kg
     * @param age    - user age
     * @param gender - user age
     * send personal information on watch
     */
    private WatchXPlusDeviceSupport setPersonalInformation(TransactionBuilder builder, int height, int weight, int age, int gender) {
        LOG.warn(" Setting Personal Information... height:"+height+" weight:"+weight+" age:"+age+" gender:"+gender);
        byte[] command = WatchXPlusConstants.CMD_SET_PERSONAL_INFO;

        byte[] bArr = new byte[4];
        bArr[0] = (byte) height;        // byte[08]
        bArr[1] = (byte) weight;        // byte[09]
        bArr[2] = (byte) age;           // byte[10]
        bArr[3] = (byte) gender;        // byte[11]

        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(command,
                        WatchXPlusConstants.WRITE_VALUE,
                        bArr));
        return this;
    }

    /** handle get/set personal info
     * @param value - reply from watch
     * actual do nothing (for test purposes only)
     */
    private void handlePersonalInfo(byte[] value) {
        int height = Conversion.fromByteArr16(value[8]);
        int weight = Conversion.fromByteArr16(value[9]);
        int age = Conversion.fromByteArr16(value[10]);
        int gender = Conversion.fromByteArr16(value[11]);
        LOG.info(" Personal info - height:" + height + ", weight:" + weight + ", age:" + age + ", gender:" + gender);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

        TransactionBuilder builder = null;
        try {
            builder = performInitialized("fetchData");

            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_DAY_STEPS_INFO,
                            WatchXPlusConstants.READ_VALUE));

//            Fetch heart rate data samples count
            requestDataCount(DataType.HEART_RATE);

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to retrieve recorded data", e);
        }
    }

    @Override
    public void onReset(int flags) {
       // testNewCommands();
    }

    @Override
    public void onHeartRateTest() {
        //requestHeartRateMeasurement();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {
        sendBloodPressureCalibration();
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
        try {
            builder = performInitialized("sendConfig: " + config);
            LOG.info(" config changed:" + config);
            switch (config) {
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    setFitnessGoal(builder);
                    break;
                case WatchXPlusConstants.PREF_ACTIVATE_DISPLAY:
                    setHeadsUpScreen(builder, sharedPreferences);
                    getShakeStatus(builder);
                    break;
                case WatchXPlusConstants.PREF_DISCONNECT_REMIND:
                    setDisconnectReminder(builder, sharedPreferences);
                    getDisconnectReminderStatus(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                    setLanguageAndTimeFormat(builder, sharedPreferences);
                    break;
                case WatchXPlusConstants.PREF_DO_NOT_DISTURB:
                case WatchXPlusConstants.PREF_DO_NOT_DISTURB_START:
                case WatchXPlusConstants.PREF_DO_NOT_DISTURB_END:
                    setQuiteHours(builder, sharedPreferences);
                    break;
                case "BP_CAL":
                    sendBloodPressureCalibration();
                    break;
                case "LONG_SIT":
                    setLongSitHours(builder, sharedPreferences);
                    break;
                case "WXP_POWER_MODE":
                    setPowerMode(config);
                    break;
                case "WXP_LANGUAGE":
                    setLanguageAndTimeFormat(builder, sharedPreferences);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {
        requestBloodPressureMeasurement();
    }


    /** set long sit reminder time
     * @param builder
     * @param enable        - state (true - enabled or false - disabled)
     * @param hourStart     - begin hour
     * @param minuteStart   - begin minute
     * @param hourEnd       - end hour
     * @param minuteEnd     - end minute
     * set long sit reminder (inactivity reminder) on watch
     */
    private WatchXPlusDeviceSupport setLongSitHours(TransactionBuilder builder, boolean enable, int hourStart, int minuteStart, int hourEnd, int minuteEnd, int period) {
        LOG.warn(" Setting Long sit reminder... Enabled:"+enable+" Period:"+period);
        LOG.warn(" Setting Long sit time... Hs:"+hourEnd+" Ms:"+minuteEnd+" He:"+hourStart+" Me:"+minuteStart);
        LOG.warn(" Setting Long sit DND time... Hs:"+hourStart+" Ms:"+minuteStart+" He:"+hourEnd+" Me:"+minuteEnd);
        // set Long Sit reminder time
        byte[] command = WatchXPlusConstants.CMD_INACTIVITY_REMINDER_SET;

        byte[] bArr = new byte[10];
        bArr[0] = (byte) hourEnd;            // byte[08]
        bArr[1] = (byte) minuteEnd;          // byte[09]
        bArr[2] = (byte) hourStart;          // byte[10]
        bArr[3] = (byte) minuteStart;        // byte[11]
        bArr[4] = (byte) hourStart;          // byte[12]
        bArr[5] = (byte) minuteStart;        // byte[13]
        bArr[6] = (byte) hourEnd;            // byte[14]
        bArr[7] = (byte) minuteEnd;          // byte[15]
        bArr[8] = (byte) (period >> 8);      // byte[16]
        bArr[9] = (byte) period;             // byte[17]

        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(command, WatchXPlusConstants.WRITE_VALUE, bArr));
        // set long sit reminder state (enabled, disabled)
        setLongSitSwitch(builder, enable);
        return this;
    }

    /** get Long sit settings from app, and send it to watch
     * @param builder
     * @param sharedPreferences
     * @return
     */
    private WatchXPlusDeviceSupport setLongSitHours(TransactionBuilder builder, SharedPreferences sharedPreferences) {
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        boolean enable = WatchXPlusDeviceCoordinator.getLongSitHours(sharedPreferences, start, end);
        if (enable) {
            int period = prefs.getInt(WatchXPlusConstants.PREF_LONGSIT_PERIOD, 60);
            return this.setLongSitHours(builder, enable,
                    start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                    end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE),
                    period);
        } else {
            // disable Long sit reminder
            LOG.info(" Long sit reminder are disabled");
            return this.setLongSitSwitch(builder, enable);
        }
    }

    /** set long sit reminder switch
     * @param tbuilder
     * @param enable - true or false
     * enabled or disables long sit reminder (inactivity reminder) on watch
     */
    private WatchXPlusDeviceSupport setLongSitSwitch(TransactionBuilder tbuilder, boolean enable) {
        LOG.warn("Setting Long sit reminder switch to" + enable);
        tbuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_INACTIVITY_REMINDER_SWITCH,
                        WatchXPlusConstants.WRITE_VALUE,
                        new byte[]{(byte) (enable ? 0x01 : 0x00)}));
        return this;
    }



    /** set do not disturb time
     * @param builder
     * @param enable        - state (true - enabled or false - disabled)
     * @param hourStart     - begin hour
     * @param minuteStart   - begin minute
     * @param hourEnd       - end hour
     * @param minuteEnd     - end minute
     * set do not disturb on watch
     */
    private WatchXPlusDeviceSupport setQuiteHours(TransactionBuilder builder, boolean enable, int hourStart, int minuteStart, int hourEnd, int minuteEnd) {
         LOG.warn(" Setting DND time... Hs:"+hourStart+" Ms:"+minuteStart+" He:"+hourEnd+" Me:"+minuteEnd);
         // set DND time
         byte[] command = WatchXPlusConstants.CMD_SET_QUITE_HOURS_TIME;

         byte[] bArr = new byte[4];
         bArr[0] = (byte) hourStart;        // byte[08]
         bArr[1] = (byte) minuteStart;      // byte[09]
         bArr[2] = (byte) hourEnd;          // byte[10]
         bArr[3] = (byte) minuteEnd;        // byte[11]
         builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
              buildCommand(command, WatchXPlusConstants.WRITE_VALUE, bArr));
         // set DND state (enabled, disabled)
         setQuiteHoursSwitch(builder, enable);
         return this;
    }

    /** set do not disturb switch
     * @param tbuilder
     * @param enable - true or false
     * enabled or disables DND on watch
     */
    private WatchXPlusDeviceSupport setQuiteHoursSwitch(TransactionBuilder tbuilder, boolean enable) {
            LOG.warn("Setting DND switch to" + enable);
            tbuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_SET_QUITE_HOURS_SWITCH,
                        WatchXPlusConstants.WRITE_VALUE,
                        new byte[]{(byte) (enable ? 0x01 : 0x00)}));
        return this;
    }

    /** get DND settings from app, and send it to watch
     * @param builder
     * @param sharedPreferences
     * @return
     */
    private WatchXPlusDeviceSupport setQuiteHours(TransactionBuilder builder, SharedPreferences sharedPreferences) {
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        boolean enable = WatchXPlusDeviceCoordinator.getQuiteHours(sharedPreferences, start, end);
        if (enable) {
            return this.setQuiteHours(builder, enable,
                    start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                    end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
        } else {
            // disable DND
            LOG.info(" Quiet hours are disabled");
            return this.setQuiteHoursSwitch(builder, enable);
        }
    }

    /** set watch power
     * @param config
     * switch watch power mode
     * modes (0- normal, 1- energysaving, 2- only watch)
     */
    private WatchXPlusDeviceSupport setPowerMode(String config) {
        int settingRead = prefs.getInt("wxp_power_mode", 0);
        byte[] bArr = new byte[1];
        bArr[0] = (byte) settingRead;
        LOG.info(" setting: " + config);
        try {
            TransactionBuilder builder = performInitialized("setPowerMode");
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_POWER_MODE,
                            WatchXPlusConstants.TASK,
                            bArr));
            builder.queue(getQueue());
        }   catch (IOException e) {
            LOG.warn("Unable to set power mode", e);
        }
        return this;
    }

    /** request status of blood pressure calibration
     * @param builder
     */
    private WatchXPlusDeviceSupport getBloodPressureCalibrationStatus(TransactionBuilder builder) {
        builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_IS_BP_CALIBRATED,
                        WatchXPlusConstants.READ_VALUE));

        return this;
    }

    /** send blood pressure calibration to watch
     * TODO add better error handling if blood pressure calibration is failed
     */
    private WatchXPlusDeviceSupport sendBloodPressureCalibration() {
        try {
            int beginCalibration = prefs.getInt(WatchXPlusConstants.PREF_BP_CAL_SWITCH, 0);
            if (beginCalibration == 1) {
                LOG.warn(" Calibrating BP - cancel " + beginCalibration);
                return this;
            }
            int mLowP = prefs.getInt(WatchXPlusConstants.PREF_BP_CAL_LOW, 80);
            int mHighP = prefs.getInt(WatchXPlusConstants.PREF_BP_CAL_HIGH, 130);
            LOG.warn(" Calibrating BP ... LowP=" + mLowP + " HighP="+mHighP);
            GB.toast("Calibrating BP...", Toast.LENGTH_LONG, GB.INFO);

            TransactionBuilder builder = performInitialized("bpCalibrate");

            byte[] command = WatchXPlusConstants.CMD_BP_CALIBRATION;
            byte mStart = 0x01; // initiate calibration

            byte[] bArr = new byte[5];
            bArr[0] = (byte) mStart;               // byte[08]
            bArr[1] = (byte) (mHighP >> 8);        // byte[09]
            bArr[2] = (byte) mHighP;              // byte[10]
            bArr[3] = (byte) (mLowP >> 8);        // byte[11]
            bArr[4] = (byte) mLowP;               // byte[12]

            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.TASK,
                            bArr));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to send BP Calibration", e);
        }
        return this;
    }

    /** handle watch response if blood pressure is calibrated
     * @param value - watch response
     * save result to global variable (uses for BP measurement)
     */
    private void handleBloodPressureCalibrationStatus(byte[] value) {
        if (Conversion.fromByteArr16(value[8]) != 0) {
            WatchXPlusDeviceCoordinator.isBPCalibrated = false;
        } else {
            WatchXPlusDeviceCoordinator.isBPCalibrated = true;
        }
    }

    /** handle watch response for result of blood pressure calibration
     * @param value - watch response
     */
    private void handleBloodPressureCalibrationResult(byte[] value) {
        if (Conversion.fromByteArr16(value[8]) != 0x00) {
            WatchXPlusDeviceCoordinator.isBPCalibrated = false;
            GB.toast("Calibrating BP fail", Toast.LENGTH_LONG, GB.ERROR);
        } else {
            WatchXPlusDeviceCoordinator.isBPCalibrated = true;
            int high = Conversion.fromByteArr16(value[9], value[10]);
            int low = Conversion.fromByteArr16(value[11], value[12]);
            GB.toast("OK. Measured Low:"+low+" high:"+high, Toast.LENGTH_LONG, GB.INFO);
        }
    }

    /** request blood pressure measurement
     * first check if blood pressure is calibrated
     */
    private void requestBloodPressureMeasurement() {
        if (!WatchXPlusDeviceCoordinator.isBPCalibrated) {
            LOG.warn("BP is NOT calibrated");
            GB.toast("BP is not calibrated", Toast.LENGTH_LONG, GB.WARN);
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("bpMeasure");

            byte[] command = WatchXPlusConstants.CMD_BLOOD_PRESSURE_MEASURE;

            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.TASK, new byte[]{0x01}));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to request BP Measure", e);
        }
    }



    // not working!!!
    private void testNewCommands() {
        try {
            TransactionBuilder builder = performInitialized("test");

            int first = prefs.getInt("wxp_newcmd_first", 0);
            int second = prefs.getInt("wxp_newcmd_second", 0);
            byte[] command = new byte[]{(byte) first, (byte) second};

            LOG.info("testing new command " + command);
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.READ_VALUE));

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to request HR Measure", e);
        }
    }


    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        try {
            TransactionBuilder builder = performInitialized("setWeather");
            int currentTemp = 0;
            int todayMinTemp = 0;
            int todayMaxTemp = 0;
            byte[] command = WatchXPlusConstants.CMD_WEATHER_SET;
            byte[] weatherInfo = new byte[5];
            String currentCondition = weatherSpec.currentCondition;
// set weather icon
            int currentConditionCode = 0; // 0 is sunny
            switch (currentCondition) {
                case "Overcast clouds":
                    currentConditionCode = 1;
                case "Broken clouds":
                    currentConditionCode = 2;
                    break;
            }
            if ((currentCondition.contains("Fog")) || (currentCondition.contains("fog"))) { currentConditionCode = 3; }
            if ((currentCondition.contains("Rain")) || (currentCondition.contains("rain"))) { currentConditionCode = 257; }
            if ((currentCondition.contains("Snow")) || (currentCondition.contains("snow"))) { currentConditionCode = 514; }
            if ((currentCondition.contains("Sand")) || (currentCondition.contains("sand"))) { currentConditionCode = 769; }
            if ((currentCondition.contains("Dust")) || (currentCondition.contains("dust"))) { currentConditionCode = 769; }
            if ((currentCondition.contains("Sleet")) || (currentCondition.contains("sleet"))) { currentConditionCode = 1026; }
// calculate for temps under 0
            currentTemp = (Math.abs(weatherSpec.currentTemp)) - 273;
            if (currentTemp < 0) {
                currentTemp = (Math.abs(currentTemp) ^ 255) + 1;
            }
            todayMinTemp = (Math.abs(weatherSpec.todayMinTemp)) - 273;
            if (todayMinTemp < 0) {
                todayMinTemp = (Math.abs(todayMinTemp) ^ 255) + 1;
            }
            todayMaxTemp = (Math.abs(weatherSpec.todayMaxTemp)) - 273;
            if (todayMaxTemp < 0) {
                todayMaxTemp = (Math.abs(todayMaxTemp) ^ 255) + 1;
            }
            LOG.warn(" Set weather min: " + todayMinTemp + " max: " + todayMaxTemp + " current: " + currentTemp + " icon: " + currentCondition);
//            First two bytes are controlling the icon
            weatherInfo[0] = (byte )(currentConditionCode >> 8);
            weatherInfo[1] = (byte )currentConditionCode;
            weatherInfo[2] = (byte) todayMinTemp;
            weatherInfo[3] = (byte) todayMaxTemp;
            weatherInfo[4] = (byte) currentTemp;
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.KEEP_ALIVE,
                            weatherInfo));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to set weather", e);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        byte[] value = characteristic.getValue();
        if (WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE.equals(characteristicUUID)) {
            if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_FIRMWARE_INFO, 5)) {
                handleFirmwareInfo(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_SHAKE_SWITCH, 5)) {
                handleShakeState(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_SET_PERSONAL_INFO, 5)) {
                handlePersonalInfo(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_BUTTON_WHILE_RING, 5)) {
                handleButtonWhenRing(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DISCONNECT_REMIND, 5)) {
                handleDisconnectReminderState(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_BATTERY_INFO, 5)) {
                handleBatteryState(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_GOAL_AIM_STATUS, 5)) {
                handleSportAimStatus(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_TIME_SETTINGS, 5)) {
                handleTime(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_IS_BP_CALIBRATED, 5)) {
                handleBloodPressureCalibrationStatus(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_BP_CALIBRATION, 5)) {
                handleBloodPressureCalibrationResult(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_BUTTON_INDICATOR, 5)) {
                this.onReverseFindDevice(true);
//                It looks like WatchXPlus doesn't send this action
//                WRONG: WatchXPlus send this on find phone
                LOG.info(" Unhandled action: Button pressed");
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_ALARM_INDICATOR, 5)) {
                LOG.info(" Alarm active: id=" + value[8]);
            } else if (isCalibrationActive && value.length == 7 && value[4] == ACK_CALIBRATION) {
                setTime(BLETypeConversions.createCalendar());
                isCalibrationActive = false;
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DAY_STEPS_INDICATOR, 5)) {
                handleStepsInfo(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DATA_COUNT, 5)) {
                LOG.info(" Received data count");
                handleDataCount(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DATA_DETAILS, 5)) {
                LOG.info(" Received data details");
                handleDataDetails(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DATA_CONTENT, 5)) {
                LOG.info(" Received data content");
                handleDataContentAck(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_BP_MEASURE_STARTED, 5)) {
                handleBpMeasureResult(value);
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_DATA_CONTENT_REMOVE, 5)) {
                handleDataContentRemove(value);
            } else if (value.length == 7 && value[5] == 0) {
                LOG.info(" Received ACK");
//                Not sure if that's necessary. There is no response for ACK in original app logs
//                handleAck();
            } else if (ArrayUtils.equals(value, WatchXPlusConstants.RESP_NOTIFICATION_SETTINGS, 5)) {
                LOG.info(" Received notification settings status");
            } else {
                LOG.info(" Unhandled value change for characteristic: " + characteristicUUID);
                logMessageContent(characteristic.getValue());
            }

            return true;
        } else if (WatchXPlusConstants.UUID_CHARACTERISTIC_DATABASE_READ.equals(characteristicUUID)) {
            LOG.info(" Value change for characteristic DATABASE: " + characteristicUUID + " value " + value);
            handleContentDataChunk(value);
            return true;
        } else {
            LOG.info(" Unhandled characteristic changed: " + characteristicUUID + " value " + value);
            logMessageContent(characteristic.getValue());
        }

        return false;
    }

    private void handleDataContentRemove(byte[] value) {
        int dataType = Conversion.fromByteArr16(value[8], value[9]);
        int timestamp = Conversion.fromByteArr16(value[10], value[11], value[12], value[13]);
        int removed = value[14];
        DataType type = DataType.getType(dataType);
        if( removed == 0) {
            LOG.info(" Removed " + type + " data for timestamp " + timestamp);
        } else {
            LOG.info(" Unsuccessful removal of " + type + " data for timestamp " + timestamp);
        }
    }

    /**
     * Heart rate history retrieve flow:
     * 1. Request for heart rate data slots count. CMD_RETRIEVE_DATA_COUNT, {@link WatchXPlusDeviceSupport#requestDataCount}
     * 2. Extract data count from response. RESP_DATA_COUNT, {@link WatchXPlusDeviceSupport#handleDataCount}
     * 3. Request for N data slot details. CMD_RETRIEVE_DATA_DETAILS, {@link WatchXPlusDeviceSupport#requestDataDetails}
     * 4. Timestamp of slot is returned, save it for later use. RESP_DATA_DETAILS, {@link WatchXPlusDeviceSupport#handleDataDetails}
     * 5. Repeat step 3-4 until all slots details retrieved.
     * 6. Request for M data content by timestamp. CMD_RETRIEVE_DATA_CONTENT, {@link WatchXPlusDeviceSupport#requestDataContentForTimestamp}
     * 7. Receive kind of pre-flight response. RESP_DATA_CONTENT, {@link WatchXPlusDeviceSupport#handleDataContentAck}
     * 8. Receive frames with content. They are different than other frames, {@link WatchXPlusDeviceSupport#handleContentDataChunk}
     *      ie. 0000000255-4F4C48-434241434444454648474747, 0001000247-474645-434240FFFFFFFFFFFFFFFFFF
     */
    private void requestDataCount(DataType dataType) {

        TransactionBuilder builder;
        try {
            builder = performInitialized("requestDataCount");

            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_RETRIEVE_DATA_COUNT,
                            WatchXPlusConstants.READ_VALUE,
                            dataType.getValue()));

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to send request to retrieve recorded data", e);
        }
    }

    private void handleDataCount(byte[] value) {

        int dataType = Conversion.fromByteArr16(value[8], value[9]);
        int dataCount = Conversion.fromByteArr16(value[10], value[11]);

        DataType type = DataType.getType(dataType);
        LOG.info("Watch contains " + dataCount + " " + type + " entries");
        dataSlots = dataCount;
        dataToFetch.clear();
        if (dataCount != 0) {
            requestDataDetails(dataToFetch.size(), type);
        }
    }

    private void requestDataDetails(int i, DataType dataType) {
        LOG.info(" Requesting " + dataType + " details");
        try {
            TransactionBuilder builder = performInitialized("requestDataDetails");

            byte[] index = Conversion.toByteArr16(i);
            byte[] req = BLETypeConversions.join(dataType.getValue(), index);
            currentDataType = dataType;
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(WatchXPlusConstants.CMD_RETRIEVE_DATA_DETAILS,
                            WatchXPlusConstants.READ_VALUE,
                            req));

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to request data details", e);
        }
    }

    private void handleDataDetails(byte[] value) {
        LOG.info("Got data details");
        int timestamp = Conversion.fromByteArr16(value[8], value[9], value[10], value[11]);
        int dataLength = Conversion.fromByteArr16(value[12], value[13]);
        int samplingInterval = (int) onSamplingInterval(value[14] >> 4, Conversion.fromByteArr16((byte) (value[14] & 15), value[15]));
        int mtu = Conversion.fromByteArr16(value[16]);
        int parts = dataLength / 16;
        if (dataLength % 16 > 0) {
            parts++;
        }

        LOG.info("timestamp (UTC): " + timestamp);
        LOG.info("timestamp (UTC): " + new Date((long) timestamp * 1000));
        LOG.info("dataLength (data length): " + dataLength);
        LOG.info("samplingInterval (per time): " + samplingInterval);
        LOG.info("mtu (mtu): " + mtu);
        LOG.info("parts: " + parts);

        dataToFetch.put(timestamp, parts);

        if (dataToFetch.size() == dataSlots) {
            Map.Entry<Integer, Integer> currentValue = dataToFetch.entrySet().iterator().next();
            requestedDataTimestamp = currentValue.getKey();
            requestDataContentForTimestamp(requestedDataTimestamp, currentDataType);
        } else {
            requestDataDetails(dataToFetch.size(), currentDataType);
        }
    }

    private void requestDataContentForTimestamp(int timestamp, DataType dataType) {
        byte[] command = WatchXPlusConstants.CMD_RETRIEVE_DATA_CONTENT;

        try {
            TransactionBuilder builder = performInitialized("requestDataContentForTimestamp");
            byte[] ts = Conversion.toByteArr32(timestamp);
            byte[] req = BLETypeConversions.join(dataType.getValue(), ts);
            req = BLETypeConversions.join(req, Conversion.toByteArr16(0));
            requestedDataTimestamp = timestamp;
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.READ_VALUE,
                            req));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to request data content", e);
        }
    }

    private void removeDataContentForTimestamp(int timestamp, DataType dataType) {
        byte[] command = WatchXPlusConstants.CMD_REMOVE_DATA_CONTENT;

        try {
            TransactionBuilder builder = performInitialized("removeDataContentForTimestamp");
            byte[] ts = Conversion.toByteArr32(timestamp);
            byte[] req = BLETypeConversions.join(dataType.getValue(), ts);
            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand(command,
                            WatchXPlusConstants.TASK,
                            req));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to remove data content", e);
        }
    }

    private void handleDataContentAck(byte[] value) {
        LOG.info(" Received data content start");
//        To verify: Chunks are sent if value[8] == 0, if value[8] == 1 they are not sent by watch
    }

    private void handleContentDataChunk(byte[] value) {
        int chunkNo = Conversion.fromByteArr16(value[0], value[1]);
        int dataType = Conversion.fromByteArr16(value[2], value[3]);
        int timezoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000;
        DataType type = DataType.getType(dataType);
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            WatchXPlusSampleProvider provider = new WatchXPlusSampleProvider(getDevice(), dbHandler.getDaoSession());
            List<WatchXPlusActivitySample> samples = new ArrayList<>();

            if (DataType.SLEEP.equals(type)) {
                WatchXPlusHealthActivityOverlayDao overlayDao = dbHandler.getDaoSession().getWatchXPlusHealthActivityOverlayDao();
                List<WatchXPlusHealthActivityOverlay> overlayList = new ArrayList<>();

                for (int i = 4; i < value.length; i+= 2) {

                    int val = Conversion.fromByteArr16(value[i], value[i+1]);
                    if (65535 == val) {
                        break;
                    }

                    int tsWithOffset = requestedDataTimestamp + (((((chunkNo * 16) / 2) + ((i - 4) / 2)) *5) * 60) - timezoneOffset;
                    LOG.debug(" requested timestamp " + requestedDataTimestamp + " chunkNo " + chunkNo + " Got data: " + new Date((long) tsWithOffset * 1000) + ", value: " + val);
                    WatchXPlusActivitySample sample = createSample(dbHandler, tsWithOffset);
                    sample.setTimestamp(tsWithOffset);
                    sample.setProvider(provider);
                    sample.setRawIntensity(val);
                    sample.setRawKind(val == 0 ? ActivityKind.TYPE_DEEP_SLEEP : ActivityKind.TYPE_LIGHT_SLEEP);
                    samples.add(sample);
                    overlayList.add(new WatchXPlusHealthActivityOverlay(sample.getTimestamp(), sample.getTimestamp()+300, sample.getRawKind(), sample.getDeviceId(), sample.getUserId(), sample.getRawWatchXPlusHealthData()));
                }
                overlayDao.insertOrReplaceInTx(overlayList);
                provider.addGBActivitySamples(samples.toArray(new WatchXPlusActivitySample[0]));

                handleEndOfDataChunks(chunkNo, type);
            } else if (DataType.HEART_RATE.equals(type)) {

                for (int i = 4; i < value.length; i++) {

                    int val = Conversion.fromByteArr16(value[i]);
                    if (255 == val) {
                        break;
                    }
                    int tsWithOffset = requestedDataTimestamp + (((((chunkNo * 16) + i) - 4) * 2) * 60) - timezoneOffset;
//                    LOG.debug(" requested timestamp " + requestedDataTimestamp + " chunkNo " + chunkNo + " Got data: " + new Date((long) tsWithOffset * 1000) + ", value: " + val);
                    WatchXPlusActivitySample sample = createSample(dbHandler, tsWithOffset);
                    sample.setTimestamp(tsWithOffset);
                    sample.setHeartRate(val);
                    sample.setProvider(provider);
                    sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
                    samples.add(sample);
                }
                provider.addGBActivitySamples(samples.toArray(new WatchXPlusActivitySample[0]));

                handleEndOfDataChunks(chunkNo, type);
            } else {
                LOG.warn(" Got unsupported data package type: " + type);
            }
        } catch (GBException ex) {
            LOG.info((ex.getMessage()));
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

    }

    private void handleEndOfDataChunks(int chunkNo, DataType type) {
        if(!dataToFetch.isEmpty() && chunkNo == dataToFetch.get(requestedDataTimestamp) - 1) {
            dataToFetch.remove(requestedDataTimestamp);
            removeDataContentForTimestamp(requestedDataTimestamp, currentDataType);
            if (!dataToFetch.isEmpty()) {
                Map.Entry<Integer, Integer> currentValue = dataToFetch.entrySet().iterator().next();
                requestedDataTimestamp = currentValue.getKey();
                requestDataContentForTimestamp(requestedDataTimestamp, type);
            } else {
                dataSlots = 0;
                if(type.equals(DataType.HEART_RATE)) {
                    currentDataType = DataType.SLEEP;
                    requestDataCount(currentDataType);
                }
            }
        } else if (dataToFetch.isEmpty()) {
            dataSlots = 0;
            if(type.equals(DataType.HEART_RATE)) {
                currentDataType = DataType.SLEEP;
                requestDataCount(currentDataType);
            }
        }
    }


    private void handleBpMeasureResult(byte[] value) {

        if (value.length < 11) {
            LOG.info(" BP Measure started. Waiting for result");
            GB.toast("BP Measure started. Waiting for result...", Toast.LENGTH_LONG, GB.INFO);
        } else {
            LOG.info(" Received BP live data");
            int high = Conversion.fromByteArr16(value[8], value[9]);
            int low = Conversion.fromByteArr16(value[10], value[11]);
            int timestamp = Conversion.fromByteArr16(value[12], value[13], value[14], value[15]);
            GB.toast("Calculated BP data: low: " + low + ", high: " + high, Toast.LENGTH_LONG, GB.INFO);
            LOG.info(" Calculated BP data: timestamp: " + timestamp + ", high: " + high + ", low: " + low);
        }
    }

    private void handleAck() {
        try {
            TransactionBuilder builder = performInitialized("handleAck");

            builder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                    buildCommand((byte) 0x00));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("Unable to response to ACK", e);
        }
    }

    //    This is only for ACK response
    private byte[] buildCommand(byte action) {
        byte[] result = new byte[7];
        System.arraycopy(WatchXPlusConstants.CMD_HEADER, 0, result, 0, 5);

        result[2] = (byte) (result.length - 6);
        result[3] = WatchXPlusConstants.REQUEST;
        result[4] = (byte) sequenceNumber++;
        result[5] = action;
        result[result.length - 1] = calculateChecksum(result);

        return result;
    }

    /** handle watch response for steps goal (show steps setting)
     * @param value - watch reply
     * for test purposes only
     */
    private void handleSportAimStatus(byte[] value) {
        int stepsAim = Conversion.fromByteArr16(value[8], value[9]);
        LOG.debug(" Received goal stepsAim: " + stepsAim);
    }

    private void handleStepsInfo(byte[] value) {
        int steps = Conversion.fromByteArr16(value[8], value[9]);
        LOG.debug(" Received steps count: " + steps);

        // This code is from MakibesHR3DeviceSupport
        Calendar date = GregorianCalendar.getInstance();
        int timestamp = (int) (date.getTimeInMillis() / 1000);

        // We need to subtract the day's total step count thus far.
        int dayStepCount = this.getStepsOnDay(timestamp);

        int newSteps = (steps - dayStepCount);

        if (newSteps > 0) {
            LOG.debug("adding " + newSteps + " steps");

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                WatchXPlusSampleProvider provider = new WatchXPlusSampleProvider(getDevice(), dbHandler.getDaoSession());

                WatchXPlusActivitySample sample = createSample(dbHandler, timestamp);
                sample.setTimestamp(timestamp);
//            sample.setRawKind(record.type);
                sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
                sample.setSteps(newSteps);
//            sample.setDistance(record.distance);
//            sample.setCalories(record.calories);
//            sample.setDistance(record.distance);
//            sample.setHeartRate((record.maxHeartRate - record.minHeartRate) / 2); //TODO: Find an alternative approach for Day Summary Heart Rate
//            sample.setRawHPlusHealthData(record.getRawData());

                sample.setProvider(provider);
                provider.addGBActivitySample(sample);
            } catch (GBException ex) {
                LOG.info((ex.getMessage()));
            } catch (Exception ex) {
                LOG.info(ex.getMessage());
            }
        }
    }

    /**
     * @param timeStamp Time stamp at some point during the requested day.
     */
    private int getStepsOnDay(int timeStamp) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            Calendar dayStart = new GregorianCalendar();
            Calendar dayEnd = new GregorianCalendar();

            this.getDayStartEnd(timeStamp, dayStart, dayEnd);

            WatchXPlusSampleProvider provider = new WatchXPlusSampleProvider(this.getDevice(), dbHandler.getDaoSession());

            List<WatchXPlusActivitySample> samples = provider.getAllActivitySamples(
                    (int) (dayStart.getTimeInMillis() / 1000L),
                    (int) (dayEnd.getTimeInMillis() / 1000L));

            int totalSteps = 0;

            for (WatchXPlusActivitySample sample : samples) {
                totalSteps += sample.getSteps();
            }

            return totalSteps;

        } catch (Exception ex) {
            LOG.error(ex.getMessage());

            return 0;
        }
    }

    /**
     * @param timeStamp seconds
     */
    private void getDayStartEnd(int timeStamp, Calendar start, Calendar end) {
        final int DAY = (24 * 60 * 60);

        int timeStampStart = ((timeStamp / DAY) * DAY);
        int timeStampEnd = (timeStampStart + DAY);

        start.setTimeInMillis(timeStampStart * 1000L);
        end.setTimeInMillis(timeStampEnd * 1000L);
    }

    private WatchXPlusActivitySample createSample(DBHandler dbHandler, int timestamp) {
        Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
        Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
        WatchXPlusActivitySample sample = new WatchXPlusActivitySample(
                timestamp,                      // ts
                deviceId, userId,               // User id
                null,            // Raw Data
                ActivityKind.TYPE_UNKNOWN,      // rawKind
                ActivitySample.NOT_MEASURED,      // rawIntensity
                ActivitySample.NOT_MEASURED,     // Steps
                ActivitySample.NOT_MEASURED,    // HR
                ActivitySample.NOT_MEASURED,  // Distance
                ActivitySample.NOT_MEASURED     // Calories
        );

        return sample;
    }

    private byte[] buildCommand(byte[] command, byte action) {
        return buildCommand(command, action, null);
    }

    private byte[] buildCommand(byte[] command, byte action, byte[] value) {
        if (Arrays.equals(command, WatchXPlusConstants.CMD_CALIBRATION_TASK)) {
            ACK_CALIBRATION = (byte) sequenceNumber;
        }
        command = BLETypeConversions.join(command, value);
        byte[] result = new byte[7 + command.length];
        System.arraycopy(WatchXPlusConstants.CMD_HEADER, 0, result, 0, 5);
        System.arraycopy(command, 0, result, 6, command.length);
        result[2] = (byte) (command.length + 1);
        result[3] = WatchXPlusConstants.REQUEST;
        result[4] = (byte) sequenceNumber++;
        result[5] = action;
        result[result.length - 1] = calculateChecksum(result);

        return result;
    }

    private byte calculateChecksum(byte[] bytes) {
        byte checksum = 0x00;
        for (int i = 0; i < bytes.length - 1; i++) {
            checksum += (bytes[i] ^ i) & 0xFF;
        }
        return (byte) (checksum & 0xFF);
    }

    /** handle watch response for firmware version
     * @param value - watch response
     */
    private void handleFirmwareInfo(byte[] value) {
        versionInfo.fwVersion = String.format(Locale.US, "%d.%d.%d", value[8], value[9], value[10]);
        handleGBDeviceEvent(versionInfo);
    }

    /** handle watch response for battery level
     * @param value
     */
    private void handleBatteryState(byte[] value) {
        batteryInfo.state = value[8] == 1 ? BatteryState.BATTERY_NORMAL : BatteryState.BATTERY_LOW;
        batteryInfo.level = value[9];
        handleGBDeviceEvent(batteryInfo);
    }

    /** handle watch response for lift wrist, and shake to refuse/ignore call
     * @param value - watch response
     * for test purposes only
     */
    private void handleShakeState(byte[] value) {
        boolean z = true;
        String light = "lightScreen";
        if ((value[11] & 1) == 1) {
            light = light + " on";
        } else {
            light = light + " off";
        }
        String refuse = "refuseCall";
        if ((((value[11] & 2) >> 1) & 1) != 1) {
           //z = false;
            refuse = refuse + " off";
        } else {
            refuse = refuse + " on";
        }
        LOG.info(" handleShakeState: " + light + " " + refuse);
    }

    /** handle disconnect reminder (lost device) status
     * @param value - watch response
     * for test purposes only
     */
    private void handleDisconnectReminderState(byte[] value) {
        boolean z = true;
        if (1 != value[8]) {
            z = false;
        }
        LOG.info(" disconnectReminder: " + Boolean.valueOf(z) + " val: " + value[8]);
        return;
    }

// read preferences
    private void syncPreferences(TransactionBuilder transaction) {
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
        this.setHeadsUpScreen(transaction, sharedPreferences);              // lift wirst to screen on
        this.setQuiteHours(transaction, sharedPreferences);                // DND
        this.setDisconnectReminder(transaction, sharedPreferences);         // disconnect reminder
        this.setLanguageAndTimeFormat(transaction, sharedPreferences);                   // set time mode 12/24h
        this.setAltitude(transaction);                                      // set altitude calibration
        this.setLongSitHours(transaction, sharedPreferences);                    // set Long sit reminder
        ActivityUser activityUser = new ActivityUser();
        this.setPersonalInformation(transaction, activityUser.getHeightCm(), activityUser.getWeightKg(),
                activityUser.getAge(),activityUser.getGender());
    }

    private Handler mFindPhoneHandler = new Handler();

    private void onReverseFindDevice(boolean start) {
        if (start) {
            SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(
                    this.getDevice().getAddress());

            int findPhone = WatchXPlusDeviceCoordinator.getFindPhone(sharedPreferences);

            if (findPhone != WatchXPlusDeviceCoordinator.FindPhone_OFF) {
                GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;

                evaluateGBDeviceEvent(findPhoneEvent);

                if (findPhone > 0) {
                    this.mFindPhoneHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onReverseFindDevice(false);
                        }
                    }, findPhone * 1000);
                }
            }
        } else {
            // Always send stop, ignore preferences.
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;

            evaluateGBDeviceEvent(findPhoneEvent);
        }
    }
    // Set Lift Wrist to Light Screen based on saved preferences
    private WatchXPlusDeviceSupport setHeadsUpScreen(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setHeadsUpScreen(transactionBuilder,
                WatchXPlusDeviceCoordinator.shouldEnableHeadsUpScreen(sharedPreferences));
    }

    // Command to toggle Lift Wrist to Light Screen, and shake to ignore/reject call
    private WatchXPlusDeviceSupport setHeadsUpScreen(TransactionBuilder transactionBuilder, boolean enable) {
        boolean shakeReject = WatchXPlusDeviceCoordinator.getShakeReject(getDevice().getAddress());
        byte refuseCall = 0x00; // force shake wrist to ignore/reject call to OFF
                                // returned characteristic is equal with button press while ringing
        if (shakeReject) refuseCall = 0x01;
        byte lightScreen = 0x00;
        if (enable) {
            lightScreen = 0x01;
        }
        byte b = (byte) (lightScreen + (refuseCall << 1));
        byte[] liftScreen = new byte[4];
        liftScreen[0] = 0x00;
        liftScreen[1] = 0x00;
        liftScreen[2] = 0x00;
        liftScreen[3] = b;              //byte[11]
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_SHAKE_SWITCH,
                             WatchXPlusConstants.WRITE_VALUE,
                             liftScreen));
        return this;
    }

    private WatchXPlusDeviceSupport setDisconnectReminder(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setDisconnectReminder(transactionBuilder,
                WatchXPlusDeviceCoordinator.shouldEnableDisconnectReminder(sharedPreferences));
    }

    private WatchXPlusDeviceSupport setDisconnectReminder(TransactionBuilder transactionBuilder, boolean enable) {
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_DISCONNECT_REMIND,
                        WatchXPlusConstants.WRITE_VALUE,
                        new byte[]{(byte) (enable ? 0x01 : 0x00)}));
        return this;
    }

// Request status of Disconnect reminder
    public WatchXPlusDeviceSupport getDisconnectReminderStatus(TransactionBuilder transactionBuilder) {
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_DISCONNECT_REMIND,
                        WatchXPlusConstants.READ_VALUE));
        return this;
    }
// Request status of Lift Wrist to Light Screen, and Shake to Ignore/Reject Call
    public WatchXPlusDeviceSupport getShakeStatus(TransactionBuilder transactionBuilder) {
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_SHAKE_SWITCH,
                        WatchXPlusConstants.READ_VALUE));
        return this;
    }

// calibrate altitude
    private WatchXPlusDeviceSupport setAltitude(TransactionBuilder transactionBuilder) {
        int value = WatchXPlusDeviceCoordinator.getAltitude(getDevice().getAddress());
        int mAltitude = value;
        if (mAltitude < 0) {
            mAltitude = (Math.abs(mAltitude) ^ 65535) + 1;
        }
        int mAirPressure = Math.abs(0); // air pressure 0 ???
        byte[] bArr = new byte[4];
        bArr[0] = (byte) (mAltitude >> 8);      // bytr[8]
        bArr[1] = (byte) mAltitude;             // bytr[9]
        bArr[2] = (byte) (mAirPressure >> 8);   // bytr[10]
        bArr[3] = (byte) mAirPressure;          // bytr[11]
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_ALTITUDE,
                        WatchXPlusConstants.WRITE_VALUE,
                        bArr));
        //LOG.info(" setAltitude: " + mAltitude);
        return this;
    }

    // set time format
    private WatchXPlusDeviceSupport setLanguageAndTimeFormat(TransactionBuilder transactionBuilder, byte timeMode, byte language) {
        byte[] bArr = new byte[2];
        bArr[0] = language;           //byte[08] language
        bArr[1] = timeMode;           //byte[09] time
        transactionBuilder.write(getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE),
                buildCommand(WatchXPlusConstants.CMD_TIME_LANGUAGE,
                        WatchXPlusConstants.WRITE_VALUE,
                        bArr));
        return this;
    }

    private WatchXPlusDeviceSupport setLanguageAndTimeFormat(TransactionBuilder transactionBuilder, SharedPreferences sharedPreferences) {
        return this.setLanguageAndTimeFormat(transactionBuilder,
                WatchXPlusDeviceCoordinator.getTimeMode(sharedPreferences),
                WatchXPlusDeviceCoordinator.getLanguage(sharedPreferences));
    }

    @Override
    public void dispose() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(broadcastReceiver);
        super.dispose();
    }

    private static double onSamplingInterval(int i, int i2) {
        switch (i) {
            case 1:
                return 1.0d * Math.pow(10.0d, -6.0d) * ((double) i2);
            case 2:
                return 1.0d * Math.pow(10.0d, -3.0d) * ((double) i2);
            case 3:
                return (double) (1 * i2);
            case 4:
                return 10.0d * Math.pow(10.0d, -6.0d) * ((double) i2);
            case 5:
                return 10.0d * Math.pow(10.0d, -3.0d) * ((double) i2);
            case 6:
                return (double) (10 * i2);
            default:
                return (double) (10 * i2);
        }
    }



    private static class Conversion {
        static byte toBcd8(@IntRange(from = 0, to = 99) int value) {
            int high = (value / 10) << 4;
            int low = value % 10;
            return (byte) (high | low);
        }

        static int fromBcd8(byte value) {
            int high = ((value & 0xF0) >> 4) * 10;
            int low = value & 0x0F;
            return high + low;
        }

        static byte[] toByteArr16(int value) {
            return new byte[]{(byte) (value >> 8), (byte) value};
        }

        static int fromByteArr16(byte... value) { // equals calculateHigh
            int intValue = 0;
            for (int i2 = 0; i2 < value.length; i2++) {
                intValue += (value[i2] & 255) << (((value.length - 1) - i2) * 8);
            }
            return intValue;
        }

        static byte[] toByteArr32(int value) {
            return new byte[]{(byte) (value >> 24),
                    (byte) (value >> 16),
                    (byte) (value >> 8),
                    (byte) value};
        }

        static int calculateLow(byte... bArr) {
            int i = 0;
            int i2 = 0;
            while (i < bArr.length) {
                i2 += (bArr[i] & 255) << (i * 8);
                i++;
            }
            return i2;
        }
    }
}
