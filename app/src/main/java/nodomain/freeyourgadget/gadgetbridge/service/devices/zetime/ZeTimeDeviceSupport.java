/*  Copyright (C) 2015-2019 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Julien Pivotto, Kranz, Sebastian Kranz, Steffen Liebergeld

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.zetime;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.zetime.ZeTimeConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.zetime.ZeTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ZeTimeActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Created by Kranz on 08.02.2018.
 */

public class ZeTimeDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ZeTimeDeviceSupport.class);
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
    private final int sevenHourOffset = 25200;
    private byte[] lastMsg;
    private byte msgPart;
    private int availableSleepData;
    private int availableStepsData;
    private int availableHeartRateData;
    private int progressSteps;
    private int progressSleep;
    private int progressHeartRate;
    private final int maxMsgLength = 20;
    private boolean callIncoming = false;
    private String songtitle = null;
    private byte musicState = -1;
    public byte[] music = null;
    public byte volume = 50;
    public byte[][] remindersOnWatch = new byte[3][10];

    public BluetoothGattCharacteristic notifyCharacteristic = null;
    public BluetoothGattCharacteristic writeCharacteristic = null;
    public BluetoothGattCharacteristic ackCharacteristic = null;
    public BluetoothGattCharacteristic replyCharacteristic = null;

    public ZeTimeDeviceSupport(){
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(ZeTimeConstants.UUID_SERVICE_BASE);
        addSupportedService(ZeTimeConstants.UUID_SERVICE_EXTEND);
        addSupportedService(ZeTimeConstants.UUID_SERVICE_HEART_RATE);
    }
    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");
        msgPart = 0;
        availableStepsData = 0;
        availableHeartRateData = 0;
        availableSleepData = 0;
        progressSteps = 0;
        progressSleep = 0;
        progressHeartRate = 0;
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        notifyCharacteristic = getCharacteristic(ZeTimeConstants.UUID_NOTIFY_CHARACTERISTIC);
        writeCharacteristic = getCharacteristic(ZeTimeConstants.UUID_WRITE_CHARACTERISTIC);
        ackCharacteristic = getCharacteristic(ZeTimeConstants.UUID_ACK_CHARACTERISTIC);
        replyCharacteristic = getCharacteristic(ZeTimeConstants.UUID_REPLY_CHARACTERISTIC);

        builder.notify(ackCharacteristic, true);
        builder.notify(notifyCharacteristic, true);
        requestDeviceInfo(builder);
        requestBatteryInfo(builder);
        setUserInfo(builder);
        setUserGoals(builder);
        requestActivityInfo(builder);
        synchronizeTime(builder);
        initMusicVolume(builder);
        onReadReminders(builder);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        LOG.info("Initialization Done");
        return builder;
    }

    @Override
    public void onSendConfiguration(String config) {
        try {
            TransactionBuilder builder = performInitialized("sendConfiguration");
            switch(config)
            {
                case ZeTimeConstants.PREF_WRIST:
                    setWrist(builder);
                    break;
                case ZeTimeConstants.PREF_SCREENTIME:
                    setScreenTime(builder);
                    break;
                case ZeTimeConstants.PREF_ANALOG_MODE:
                    setAnalogMode(builder);
                    break;
                case ZeTimeConstants.PREF_ACTIVITY_TRACKING:
                    setActivityTracking(builder);
                    break;
                case ZeTimeConstants.PREF_HANDMOVE_DISPLAY:
                    setDisplayOnMovement(builder);
                    break;
                case ZeTimeConstants.PREF_DO_NOT_DISTURB:
                case ZeTimeConstants.PREF_DO_NOT_DISTURB_START:
                case ZeTimeConstants.PREF_DO_NOT_DISTURB_END:
                    setDoNotDisturb(builder);
                    break;
                case ZeTimeConstants.PREF_CALORIES_TYPE:
                    setCaloriesType(builder);
                    break;
                case ZeTimeConstants.PREF_TIME_FORMAT:
                    setTimeFormate(builder);
                    break;
                case ZeTimeConstants.PREF_DATE_FORMAT:
                    setDateFormate(builder);
                    break;
                case ZeTimeConstants.PREF_INACTIVITY_KEY:
                case ZeTimeConstants.PREF_INACTIVITY_ENABLE:
                case ZeTimeConstants.PREF_INACTIVITY_START:
                case ZeTimeConstants.PREF_INACTIVITY_END:
                case ZeTimeConstants.PREF_INACTIVITY_THRESHOLD:
                case ZeTimeConstants.PREF_INACTIVITY_MO:
                case ZeTimeConstants.PREF_INACTIVITY_TU:
                case ZeTimeConstants.PREF_INACTIVITY_WE:
                case ZeTimeConstants.PREF_INACTIVITY_TH:
                case ZeTimeConstants.PREF_INACTIVITY_FR:
                case ZeTimeConstants.PREF_INACTIVITY_SA:
                case ZeTimeConstants.PREF_INACTIVITY_SU:
                    setInactivityAlert(builder);
                    break;
                case ZeTimeConstants.PREF_SMS_SIGNALING:
                case ZeTimeConstants.PREF_CALL_SIGNALING:
                case ZeTimeConstants.PREF_MISSED_CALL_SIGNALING:
                case ZeTimeConstants.PREF_EMAIL_SIGNALING:
                case ZeTimeConstants.PREF_SOCIAL_SIGNALING:
                case ZeTimeConstants.PREF_CALENDAR_SIGNALING:
                case ZeTimeConstants.PREF_INACTIVITY_SIGNALING:
                case ZeTimeConstants.PREF_LOW_POWER_SIGNALING:
                case ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING:
                    setSignaling(builder, config);
                    break;
                case ZeTimeConstants.PREF_SHOCK_STRENGTH:
                    setShockStrength(builder);
                case ZeTimeConstants.PREF_ZETIME_HEARTRATE_ALARM:
                case ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE:
                case ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE:
                    setHeartRateLimits(builder);
                    break;
                case ZeTimeConstants.PREF_USER_FITNESS_GOAL:
                case ZeTimeConstants.PREF_USER_SLEEP_GOAL:
                case ZeTimeConstants.PREF_USER_CALORIES_GOAL:
                case ZeTimeConstants.PREF_USER_DISTANCE_GOAL:
                case ZeTimeConstants.PREF_USER_ACTIVETIME_GOAL:
                    setUserGoals(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("onFindDevice");
            byte[] testSignaling = {
                    ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_TEST_SIGNALING,
                    ZeTimeConstants.CMD_SEND,
                    (byte)0x1,
                    (byte)0x0,
                    (byte)(start ? 1 : 0),
                    ZeTimeConstants.CMD_END
            };
            sendMsgToWatch(builder, testSignaling);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error on function onFindDevice: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        int heartRateMeasurementIntervall = 0; // 0 means off
        heartRateMeasurementIntervall = seconds/60; // zetime accepts only minutes

        byte[] heartrate = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_AUTO_HEARTRATE,
                ZeTimeConstants.CMD_SEND,
                (byte)0x1,
                (byte)0x0,
                (byte)heartRateMeasurementIntervall,
                ZeTimeConstants.CMD_END};

        try {
            TransactionBuilder builder = performInitialized("enableAutoHeartRate");
            sendMsgToWatch(builder, heartrate);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error enable auto heart rate measurement: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        byte[] alarmMessage = null;
        try {
            TransactionBuilder builder = performInitialized("setAlarms");

            Prefs prefs = GBApplication.getPrefs();

            for (Alarm alarm : alarms) {
                if(remindersOnWatch[alarm.getPosition()][0] == 0)
                {
                    alarmMessage = new byte[]{
                            ZeTimeConstants.CMD_PREAMBLE,
                            ZeTimeConstants.CMD_REMINDERS,
                            ZeTimeConstants.CMD_SEND,
                            (byte) 0xb,
                            (byte) 0x0,
                            (byte) 0x0,//(byte) alarm.getPosition(), // index
                            ZeTimeConstants.REMINDER_ALARM,
                            (byte) 0x0, // year low byte
                            (byte) 0x0, // year high byte
                            (byte) 0x0, // month
                            (byte) 0x0, // day
                            (byte) AlarmUtils.toCalendar(alarm).get(Calendar.HOUR_OF_DAY),
                            (byte) AlarmUtils.toCalendar(alarm).get(Calendar.MINUTE),
                            (byte) alarm.getRepetition(),
                            (byte) (alarm.getEnabled() ? 1 : 0),
                            (byte) prefs.getInt(ZeTimeConstants.PREF_ALARM_SIGNALING, 11), // reminder signaling
                            ZeTimeConstants.CMD_END
                    };
                    System.arraycopy(alarmMessage, 6, remindersOnWatch[alarm.getPosition()], 0, 10);
                } else {
                    alarmMessage = new byte[]{
                            ZeTimeConstants.CMD_PREAMBLE,
                            ZeTimeConstants.CMD_REMINDERS,
                            ZeTimeConstants.CMD_SEND,
                            (byte) 0x15,
                            (byte) 0x0,
                            (byte) 0x1, // edit alarm
                            remindersOnWatch[alarm.getPosition()][0],
                            remindersOnWatch[alarm.getPosition()][1],
                            remindersOnWatch[alarm.getPosition()][2],
                            remindersOnWatch[alarm.getPosition()][3],
                            remindersOnWatch[alarm.getPosition()][4],
                            remindersOnWatch[alarm.getPosition()][5],
                            remindersOnWatch[alarm.getPosition()][6],
                            remindersOnWatch[alarm.getPosition()][7],
                            remindersOnWatch[alarm.getPosition()][8],
                            remindersOnWatch[alarm.getPosition()][9],
                            ZeTimeConstants.REMINDER_ALARM,
                            (byte) 0x0, // year low byte
                            (byte) 0x0, // year high byte
                            (byte) 0x0, // month
                            (byte) 0x0, // day
                            (byte) AlarmUtils.toCalendar(alarm).get(Calendar.HOUR_OF_DAY),
                            (byte) AlarmUtils.toCalendar(alarm).get(Calendar.MINUTE),
                            (byte) alarm.getRepetition(),
                            (byte) (alarm.getEnabled() ? 1 : 0),
                            (byte) prefs.getInt(ZeTimeConstants.PREF_ALARM_SIGNALING, 11), // reminder signaling
                            ZeTimeConstants.CMD_END
                    };
                    System.arraycopy(alarmMessage, 16, remindersOnWatch[alarm.getPosition()], 0, 10);
                }
                sendMsgToWatch(builder, alarmMessage);
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error set alarms: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        songtitle = musicSpec.track;
        if(musicState != -1) {
            music = new byte[songtitle.getBytes(StandardCharsets.UTF_8).length + 7]; // 7 bytes for status and overhead
            music[0] = ZeTimeConstants.CMD_PREAMBLE;
            music[1] = ZeTimeConstants.CMD_MUSIC_CONTROL;
            music[2] = ZeTimeConstants.CMD_REQUEST_RESPOND;
            music[3] = (byte) ((songtitle.getBytes(StandardCharsets.UTF_8).length + 1) & 0xff);
            music[4] = (byte) ((songtitle.getBytes(StandardCharsets.UTF_8).length + 1) >> 8);
            music[5] = musicState;
            System.arraycopy(songtitle.getBytes(StandardCharsets.UTF_8), 0, music, 6, songtitle.getBytes(StandardCharsets.UTF_8).length);
            music[music.length - 1] = ZeTimeConstants.CMD_END;
            try {
                TransactionBuilder builder = performInitialized("setMusicStateInfo");
                replyMsgToWatch(builder, music);
                builder.queue(getQueue());
            } catch (IOException e) {
                GB.toast(getContext(), "Error setting music state and info: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        int subject_length = 0;
        int notification_length = 0;
        byte[] subject = null;
        byte[] notification = null;
        Calendar time = GregorianCalendar.getInstance();
        // convert every single digit of the date to ascii characters
        // we do it like so: use the base chrachter of '0' and add the digit
        byte[] datetimeBytes = new byte[]{
                (byte) ((time.get(Calendar.YEAR) / 1000) + '0'),
                (byte) (((time.get(Calendar.YEAR) / 100)%10) + '0'),
                (byte) (((time.get(Calendar.YEAR) / 10)%10) + '0'),
                (byte) ((time.get(Calendar.YEAR)%10) + '0'),
                (byte) (((time.get(Calendar.MONTH)+1)/10) + '0'),
                (byte) (((time.get(Calendar.MONTH)+1)%10) + '0'),
                (byte) ((time.get(Calendar.DAY_OF_MONTH)/10) + '0'),
                (byte) ((time.get(Calendar.DAY_OF_MONTH)%10) + '0'),
                (byte) 'T',
                (byte) ((time.get(Calendar.HOUR_OF_DAY)/10) + '0'),
                (byte) ((time.get(Calendar.HOUR_OF_DAY)%10) + '0'),
                (byte) ((time.get(Calendar.MINUTE)/10) + '0'),
                (byte) ((time.get(Calendar.MINUTE)%10) + '0'),
                (byte) ((time.get(Calendar.SECOND)/10) + '0'),
                (byte) ((time.get(Calendar.SECOND)%10) + '0'),
        };

        if(callIncoming || (callSpec.command == CallSpec.CALL_INCOMING)) {
            if (callSpec.command == CallSpec.CALL_INCOMING) {
            if (callSpec.name != null) {
                notification_length += callSpec.name.getBytes(StandardCharsets.UTF_8).length;
                subject_length = callSpec.name.getBytes(StandardCharsets.UTF_8).length;
                subject = new byte[subject_length];
                System.arraycopy(callSpec.name.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
            } else if (callSpec.number != null) {
                notification_length += callSpec.number.getBytes(StandardCharsets.UTF_8).length;
                subject_length = callSpec.number.getBytes(StandardCharsets.UTF_8).length;
                subject = new byte[subject_length];
                System.arraycopy(callSpec.number.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
            }
            notification_length += datetimeBytes.length + 10; // add message overhead
            notification = new byte[notification_length];
            notification[0] = ZeTimeConstants.CMD_PREAMBLE;
            notification[1] = ZeTimeConstants.CMD_PUSH_EX_MSG;
            notification[2] = ZeTimeConstants.CMD_SEND;
            notification[3] = (byte) ((notification_length - 6) & 0xff);
            notification[4] = (byte) ((notification_length - 6) >> 8);
                notification[5] = ZeTimeConstants.NOTIFICATION_INCOME_CALL;
                notification[6] = 1;
                notification[7] = (byte) subject_length;
                notification[8] = (byte) 0;
                System.arraycopy(subject, 0, notification, 9, subject_length);
                System.arraycopy(datetimeBytes, 0, notification, 9 + subject_length, datetimeBytes.length);
                notification[notification_length - 1] = ZeTimeConstants.CMD_END;
                callIncoming = true;
            } else {
                notification_length = datetimeBytes.length + 10; // add message overhead
                notification = new byte[notification_length];
                notification[0] = ZeTimeConstants.CMD_PREAMBLE;
                notification[1] = ZeTimeConstants.CMD_PUSH_EX_MSG;
                notification[2] = ZeTimeConstants.CMD_SEND;
                notification[3] = (byte) ((notification_length - 6) & 0xff);
                notification[4] = (byte) ((notification_length - 6) >> 8);
                notification[5] = ZeTimeConstants.NOTIFICATION_CALL_OFF;
                notification[6] = 1;
                notification[7] = (byte) 0;
                notification[8] = (byte) 0;
                System.arraycopy(datetimeBytes, 0, notification, 9, datetimeBytes.length);
                notification[notification_length - 1] = ZeTimeConstants.CMD_END;
                callIncoming = false;
            }
            if(notification != null)
            {
                try {
                    TransactionBuilder builder = performInitialized("setCallState");
                    sendMsgToWatch(builder, notification);
                    builder.queue(getQueue());
                } catch (IOException e) {
                    GB.toast(getContext(), "Error set call state: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                }
            }
        }

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            TransactionBuilder builder = performInitialized("fetchActivityData");
            requestActivityInfo(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error on fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onTestNewFunction() {
//        byte[] strength = {
//                ZeTimeConstants.CMD_PREAMBLE,
//                ZeTimeConstants.CMD_SHOCK_STRENGTH,
//                ZeTimeConstants.CMD_REQUEST,
//                (byte)0x1,
//                (byte)0x0,
//                (byte)0x0,
//                ZeTimeConstants.CMD_END
//        };
//        try {
//            TransactionBuilder builder = performInitialized("testNewFunction");
//            sendMsgToWatch(builder, strength);
//            builder.queue(getQueue());
//        } catch (IOException e) {
//            GB.toast(getContext(), "Error on testing new function: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
//        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        musicState = stateSpec.state;
        if(songtitle != null) {
            music = new byte[songtitle.getBytes(StandardCharsets.UTF_8).length + 7]; // 7 bytes for status and overhead
            music[0] = ZeTimeConstants.CMD_PREAMBLE;
            music[1] = ZeTimeConstants.CMD_MUSIC_CONTROL;
            music[2] = ZeTimeConstants.CMD_REQUEST_RESPOND;
            music[3] = (byte) ((songtitle.getBytes(StandardCharsets.UTF_8).length + 1) & 0xff);
            music[4] = (byte) ((songtitle.getBytes(StandardCharsets.UTF_8).length + 1) >> 8);
            if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
                music[5] = 0;
            } else {
                music[5] = 1;
            }
            System.arraycopy(songtitle.getBytes(StandardCharsets.UTF_8), 0, music, 6, songtitle.getBytes(StandardCharsets.UTF_8).length);
            music[music.length - 1] = ZeTimeConstants.CMD_END;
            try {
                TransactionBuilder builder = performInitialized("setMusicStateInfo");
                replyMsgToWatch(builder, music);
                builder.queue(getQueue());
            } catch (IOException e) {
                GB.toast(getContext(), "Error setting music state and info: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        Calendar time = GregorianCalendar.getInstance();
        byte[] CalendarEvent = new byte[calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length + 16]; // 26 bytes for calendar and overhead
        time.setTimeInMillis(calendarEventSpec.timestamp);
        CalendarEvent[0] = ZeTimeConstants.CMD_PREAMBLE;
        CalendarEvent[1] = ZeTimeConstants.CMD_PUSH_CALENDAR_DAY;
        CalendarEvent[2] = ZeTimeConstants.CMD_SEND;
        CalendarEvent[3] = (byte)((calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length + 10) & 0xff);
        CalendarEvent[4] = (byte)((calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length + 10) >> 8);
        CalendarEvent[5] = (byte)(calendarEventSpec.type + 0x1);
        CalendarEvent[6] = (byte)(time.get(Calendar.YEAR) & 0xff);
        CalendarEvent[7] = (byte)(time.get(Calendar.YEAR) >> 8);
        CalendarEvent[8] = (byte)(time.get(Calendar.MONTH)+1);
        CalendarEvent[9] = (byte)time.get(Calendar.DAY_OF_MONTH);
        CalendarEvent[10] = (byte) (time.get(Calendar.HOUR_OF_DAY) & 0xff);
        CalendarEvent[11] = (byte) (time.get(Calendar.HOUR_OF_DAY) >> 8);
        CalendarEvent[12] = (byte) (time.get(Calendar.MINUTE) & 0xff);
        CalendarEvent[13] = (byte) (time.get(Calendar.MINUTE) >> 8);
        CalendarEvent[14] = (byte) calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length;
        System.arraycopy(calendarEventSpec.title.getBytes(StandardCharsets.UTF_8), 0, CalendarEvent, 15, calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length);
        CalendarEvent[CalendarEvent.length-1] = ZeTimeConstants.CMD_END;
        try {
            TransactionBuilder builder = performInitialized("sendCalendarEvenr");
            sendMsgToWatch(builder, CalendarEvent);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending calendar event: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("synchronizeTime");
            synchronizeTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error setting the time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        String buildnumber = versionCmd.fwVersion.substring(versionCmd.fwVersion.length() - 4);
        byte[] weather = new byte[weatherSpec.location.getBytes(StandardCharsets.UTF_8).length + 26]; // 26 bytes for weatherdata and overhead
        weather[0] = ZeTimeConstants.CMD_PREAMBLE;
        weather[1] = ZeTimeConstants.CMD_PUSH_WEATHER_DATA;
        weather[2] = ZeTimeConstants.CMD_SEND;
        weather[3] = (byte)((weatherSpec.location.getBytes(StandardCharsets.UTF_8).length + 20) & 0xff);
        weather[4] = (byte)((weatherSpec.location.getBytes(StandardCharsets.UTF_8).length + 20) >> 8);
        weather[5] = 0; // celsius
        weather[6] = (byte)(weatherSpec.currentTemp - 273);
        weather[7] = (byte)(weatherSpec.todayMinTemp - 273);
        weather[8] = (byte)(weatherSpec.todayMaxTemp - 273);

        if (buildnumber.compareTo("B4.1") >= 0) // if using firmware 1.7 Build 41 and above use newer icons
        {
            weather[9] = Weather.mapToZeTimeCondition(weatherSpec.currentConditionCode);
        } else
        {
            weather[9] = Weather.mapToZeTimeConditionOld(weatherSpec.currentConditionCode);
        }
        for(int forecast = 0; forecast < 3; forecast++) {
            weather[10+(forecast*5)] = 0; // celsius
            weather[11+(forecast*5)] = (byte) 0xff;
            weather[12+(forecast*5)] = (byte) (weatherSpec.forecasts.get(forecast).minTemp - 273);
            weather[13+(forecast*5)] = (byte) (weatherSpec.forecasts.get(forecast).maxTemp - 273);
            weather[14+(forecast*5)] = Weather.mapToZeTimeCondition(weatherSpec.forecasts.get(forecast).conditionCode);
        }
        System.arraycopy(weatherSpec.location.getBytes(StandardCharsets.UTF_8), 0, weather, 25, weatherSpec.location.getBytes(StandardCharsets.UTF_8).length);
        weather[weather.length-1] = ZeTimeConstants.CMD_END;
        try {
            TransactionBuilder builder = performInitialized("sendWeahter");
            sendMsgToWatch(builder, weather);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending weather: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

        int subject_length = 0;
        int body_length = notificationSpec.body.getBytes(StandardCharsets.UTF_8).length;
        if(body_length > 256)
        {
            body_length = 256;
        }
        int notification_length = body_length;
        byte[] subject = null;
        byte[] notification = null;
        Calendar time = GregorianCalendar.getInstance();
        // convert every single digit of the date to ascii characters
        // we do it like so: use the base chrachter of '0' and add the digit
        byte[] datetimeBytes = new byte[]{
                (byte) ((time.get(Calendar.YEAR) / 1000) + '0'),
                (byte) (((time.get(Calendar.YEAR) / 100)%10) + '0'),
                (byte) (((time.get(Calendar.YEAR) / 10)%10) + '0'),
                (byte) ((time.get(Calendar.YEAR)%10) + '0'),
                (byte) (((time.get(Calendar.MONTH)+1)/10) + '0'),
                (byte) (((time.get(Calendar.MONTH)+1)%10) + '0'),
                (byte) ((time.get(Calendar.DAY_OF_MONTH)/10) + '0'),
                (byte) ((time.get(Calendar.DAY_OF_MONTH)%10) + '0'),
                (byte) 'T',
                (byte) ((time.get(Calendar.HOUR_OF_DAY)/10) + '0'),
                (byte) ((time.get(Calendar.HOUR_OF_DAY)%10) + '0'),
                (byte) ((time.get(Calendar.MINUTE)/10) + '0'),
                (byte) ((time.get(Calendar.MINUTE)%10) + '0'),
                (byte) ((time.get(Calendar.SECOND)/10) + '0'),
                (byte) ((time.get(Calendar.SECOND)%10) + '0'),
        };

        if (notificationSpec.sender != null)
        {
            notification_length += notificationSpec.sender.getBytes(StandardCharsets.UTF_8).length;
            subject_length = notificationSpec.sender.getBytes(StandardCharsets.UTF_8).length;
            subject = new byte[subject_length];
            System.arraycopy(notificationSpec.sender.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
        } else if(notificationSpec.phoneNumber != null)
        {
            notification_length += notificationSpec.phoneNumber.getBytes(StandardCharsets.UTF_8).length;
            subject_length = notificationSpec.phoneNumber.getBytes(StandardCharsets.UTF_8).length;
            subject = new byte[subject_length];
            System.arraycopy(notificationSpec.phoneNumber.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
        } else if(notificationSpec.subject != null)
        {
            notification_length += notificationSpec.subject.getBytes(StandardCharsets.UTF_8).length;
            subject_length = notificationSpec.subject.getBytes(StandardCharsets.UTF_8).length;
            subject = new byte[subject_length];
            System.arraycopy(notificationSpec.subject.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
        } else if(notificationSpec.title != null)
        {
            notification_length += notificationSpec.title.getBytes(StandardCharsets.UTF_8).length;
            subject_length = notificationSpec.title.getBytes(StandardCharsets.UTF_8).length;
            subject = new byte[subject_length];
            System.arraycopy(notificationSpec.title.getBytes(StandardCharsets.UTF_8), 0, subject, 0, subject_length);
        }
        notification_length += datetimeBytes.length + 10; // add message overhead
        notification = new byte[notification_length];
        notification[0] = ZeTimeConstants.CMD_PREAMBLE;
        notification[1] = ZeTimeConstants.CMD_PUSH_EX_MSG;
        notification[2] = ZeTimeConstants.CMD_SEND;
        notification[3] = (byte)((notification_length-6) & 0xff);
        notification[4] = (byte)((notification_length-6) >> 8);
        notification[6] = 1;
        notification[7] = (byte)subject_length;
        notification[8] = (byte)body_length;
        System.arraycopy(subject, 0, notification, 9, subject_length);
        System.arraycopy(notificationSpec.body.getBytes(StandardCharsets.UTF_8), 0, notification, 9+subject_length, body_length);
        System.arraycopy(datetimeBytes, 0, notification, 9+subject_length+body_length, datetimeBytes.length);
        notification[notification_length-1] = ZeTimeConstants.CMD_END;

        switch(notificationSpec.type)
        {
            case GENERIC_SMS:
                notification[5] = ZeTimeConstants.NOTIFICATION_SMS;
                break;
            case GENERIC_PHONE:
                notification[5] = ZeTimeConstants.NOTIFICATION_MISSED_CALL;
                break;
            case GMAIL:
            case GOOGLE_INBOX:
            case MAILBOX:
            case OUTLOOK:
            case YAHOO_MAIL:
            case GENERIC_EMAIL:
                notification[5] = ZeTimeConstants.NOTIFICATION_EMAIL;
                break;
            case WECHAT:
                notification[5] = ZeTimeConstants.NOTIFICATION_WECHAT;
                break;
            case VIBER:
                notification[5] = ZeTimeConstants.NOTIFICATION_VIBER;
                break;
            case WHATSAPP:
                notification[5] = ZeTimeConstants.NOTIFICATION_WHATSAPP;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                notification[5] = ZeTimeConstants.NOTIFICATION_FACEBOOK;
                break;
            case GOOGLE_HANGOUTS:
                notification[5] = ZeTimeConstants.NOTIFICATION_HANGOUTS;
                break;
            case LINE:
                notification[5] = ZeTimeConstants.NOTIFICATION_LINE;
                break;
            case SKYPE:
                notification[5] = ZeTimeConstants.NOTIFICATION_SKYPE;
                break;
            case CONVERSATIONS:
            case RIOT:
            case SIGNAL:
            case TELEGRAM:
            case THREEMA:
            case KONTALK:
            case ANTOX:
            case GOOGLE_MESSENGER:
            case HIPCHAT:
            case KIK:
            case KAKAO_TALK:
            case SLACK:
                notification[5] = ZeTimeConstants.NOTIFICATION_MESSENGER;
                break;
            case SNAPCHAT:
                notification[5] = ZeTimeConstants.NOTIFICATION_SNAPCHAT;
                break;
            case INSTAGRAM:
                notification[5] = ZeTimeConstants.NOTIFICATION_INSTAGRAM;
                break;
            case TWITTER:
                notification[5] = ZeTimeConstants.NOTIFICATION_TWITTER;
                break;
            case LINKEDIN:
                notification[5] = ZeTimeConstants.NOTIFICATION_LINKEDIN;
                break;
            case GENERIC_CALENDAR:
                notification[5] = ZeTimeConstants.NOTIFICATION_CALENDAR;
                break;
            default:
                notification[5] = ZeTimeConstants.NOTIFICATION_SOCIAL;
                break;
        }
        try {
            TransactionBuilder builder = performInitialized("sendNotification");
            sendMsgToWatch(builder, notification);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending notification: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (ZeTimeConstants.UUID_ACK_CHARACTERISTIC.equals(characteristicUUID)) {
            byte[] data = receiveCompleteMsg(characteristic.getValue());
            if(isMsgFormatOK(data)) {
                switch (data[1]) {
                    case ZeTimeConstants.CMD_WATCH_ID:
                        break;
                    case ZeTimeConstants.CMD_DEVICE_VERSION:
                        handleDeviceInfo(data);
                        break;
                    case ZeTimeConstants.CMD_BATTERY_POWER:
                        handleBatteryInfo(data);
                        break;
                    case ZeTimeConstants.CMD_AVAIABLE_DATA:
                        handleActivityFetching(data);
                        break;
                    case ZeTimeConstants.CMD_GET_STEP_COUNT:
                        handleStepsData(data);
                        break;
                    case ZeTimeConstants.CMD_GET_SLEEP_DATA:
                        handleSleepData(data);
                        break;
                    case ZeTimeConstants.CMD_GET_HEARTRATE_EXDATA:
                        handleHeartRateData(data);
                        break;
                    case ZeTimeConstants.CMD_MUSIC_CONTROL:
                        handleMusicControl(data);
                        break;
                    case ZeTimeConstants.CMD_TIME_SURFACE_SETTINGS:
                        getDateTimeFormat(data);
                        break;
                    case ZeTimeConstants.CMD_SHOCK_MODE:
                        getSignaling(data);
                        break;
                    case ZeTimeConstants.CMD_DO_NOT_DISTURB:
                        getDoNotDisturb(data);
                        break;
                    case ZeTimeConstants.CMD_ANALOG_MODE:
                        getAnalogMode(data);
                        break;
                    case ZeTimeConstants.CMD_CONTROL_DEVICE:
                        getActivityTracking(data);
                        break;
                    case ZeTimeConstants.CMD_DISPLAY_TIMEOUT:
                        getScreenTime(data);
                        break;
                    case ZeTimeConstants.CMD_USAGE_HABITS:
                        getWrist(data);
                        break;
                    case ZeTimeConstants.CMD_AUTO_HEARTRATE:
                        getHeartRateMeasurement(data);
                        break;
                    case ZeTimeConstants.CMD_HEARTRATE_ALARM_LIMITS:
                        getHeartRateLimits(data);
                        break;
                    case ZeTimeConstants.CMD_INACTIVITY_ALERT:
                        getInactivityAlert(data);
                        break;
                    case ZeTimeConstants.CMD_CALORIES_TYPE:
                        getCaloriesType(data);
                        break;
                    case ZeTimeConstants.CMD_SWITCH_SETTINGS:
                        getDisplayOnMovement(data);
                        break;
                    case ZeTimeConstants.CMD_REMINDERS:
                        storeActualReminders(data);
                        break;
                }
            }
            return true;
        } else if (ZeTimeConstants.UUID_NOTIFY_CHARACTERISTIC.equals(characteristicUUID))
        {
            byte[] data = receiveCompleteMsg(characteristic.getValue());
            if(isMsgFormatOK(data)) {
                switch (data[1])
                {
                    case ZeTimeConstants.CMD_MUSIC_CONTROL:
                        handleMusicControl(data);
                        break;
                }
                return true;
            }
        }
        else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            logMessageContent(characteristic.getValue());
        }
        return false;
    }

    private boolean isMsgFormatOK(byte[] msg)
    {
        if(msg != null) {
            if (msg[0] == ZeTimeConstants.CMD_PREAMBLE) {
                if ((msg[3] != 0) || (msg[4] != 0)) {
                    int payloadSize = (msg[4] << 8)&0xff00 | (msg[3]&0xff);
                    int msgLength = payloadSize + 6;
                    if (msgLength == msg.length) {
                        if (msg[msgLength - 1] == ZeTimeConstants.CMD_END) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private byte[] receiveCompleteMsg(byte[] msg)
    {
        if(msgPart == 0) {
            int payloadSize = (msg[4] << 8)&0xff00 | (msg[3]&0xff);
            if (payloadSize > 14) {
                lastMsg = new byte[msg.length];
                System.arraycopy(msg, 0, lastMsg, 0, msg.length);
                msgPart++;
                return null;
            } else {
                return msg;
            }
        } else
        {
            byte[] completeMsg = new byte[lastMsg.length + msg.length];
            System.arraycopy(lastMsg, 0, completeMsg, 0, lastMsg.length);
            System.arraycopy(msg, 0, completeMsg, lastMsg.length, msg.length);
            msgPart = 0;
            return completeMsg;
        }
    }

    private ZeTimeDeviceSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info!");
        builder.write(writeCharacteristic,new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                                ZeTimeConstants.CMD_BATTERY_POWER,
                                                ZeTimeConstants.CMD_REQUEST,
                                                0x01,
                                                0x00,
                                                0x00,
                                                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
        return this;
    }

    private ZeTimeDeviceSupport requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        builder.write(writeCharacteristic,new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                                ZeTimeConstants.CMD_WATCH_ID,
                                                ZeTimeConstants.CMD_REQUEST,
                                                0x01,
                                                0x00,
                                                0x00,
                                                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});

        builder.write(writeCharacteristic,new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                                ZeTimeConstants.CMD_DEVICE_VERSION,
                                                ZeTimeConstants.CMD_REQUEST,
                                                0x01,
                                                0x00,
                                                0x05,
                                                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});

        builder.write(writeCharacteristic,new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                                ZeTimeConstants.CMD_DEVICE_VERSION,
                                                ZeTimeConstants.CMD_REQUEST,
                                                0x01,
                                                0x00,
                                                0x02,
                                                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
        return this;
    }

    private ZeTimeDeviceSupport requestActivityInfo(TransactionBuilder builder) {
        builder.write(writeCharacteristic, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_AVAIABLE_DATA,
                ZeTimeConstants.CMD_REQUEST,
                0x01,
                0x00,
                0x00,
                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
        return this;
    }

    private ZeTimeDeviceSupport requestShockStrength(TransactionBuilder builder) {
        builder.write(writeCharacteristic, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_SHOCK_STRENGTH,
                ZeTimeConstants.CMD_REQUEST,
                0x01,
                0x00,
                0x00,
                ZeTimeConstants.CMD_END});
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
        return this;
    }

    private void handleBatteryInfo(byte[] value) {
            batteryCmd.level = ((short) value[5]);
            if(batteryCmd.level <= 25)
            {
                batteryCmd.state = BatteryState.BATTERY_LOW;
            } else
            {
                batteryCmd.state = BatteryState.BATTERY_NORMAL;
            }
        evaluateGBDeviceEvent(batteryCmd);
    }

    private void handleDeviceInfo(byte[] value) {
            value[value.length-1] = 0; // convert the end to a String end
            byte[] string = Arrays.copyOfRange(value,6, value.length-1);
            if(value[5] == 5)
            {
                versionCmd.fwVersion = new String(string);
            } else{
                versionCmd.hwVersion = new String(string);
            }
        evaluateGBDeviceEvent(versionCmd);
    }

    private void handleActivityFetching(byte[] msg)
    {
        availableStepsData = (int) ((msg[5]&0xff) | (msg[6] << 8)&0xff00);
        availableSleepData = (int) ((msg[7]&0xff) | (msg[8] << 8)&0xff00);
        availableHeartRateData= (int) ((msg[9]&0xff) | (msg[10] << 8)&0xff00);
        if(availableStepsData > 0){
            getStepData();
        } else if(availableHeartRateData > 0)
        {
            getHeartRateData();
        } else if(availableSleepData > 0)
        {
            getSleepData();
        }
    }

    private void getStepData()
    {
        try {
            TransactionBuilder builder = performInitialized("fetchStepData");
            builder.write(writeCharacteristic, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                ZeTimeConstants.CMD_GET_STEP_COUNT,
                                ZeTimeConstants.CMD_REQUEST,
                                0x02,
                                0x00,
                                0x00,
                                0x00,
                                ZeTimeConstants.CMD_END});
            builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void deleteStepData()
    {
        try {
            TransactionBuilder builder = performInitialized("deleteStepData");
            sendMsgToWatch(builder, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_DELETE_STEP_COUNT,
                    ZeTimeConstants.CMD_SEND,
                    0x01,
                    0x00,
                    0x00,
                    ZeTimeConstants.CMD_END});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error deleting activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void getHeartRateData()
    {
        try {
            TransactionBuilder builder = performInitialized("fetchHeartRateData");
            builder.write(writeCharacteristic, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_GET_HEARTRATE_EXDATA,
                ZeTimeConstants.CMD_REQUEST,
                0x01,
                0x00,
                0x00,
                ZeTimeConstants.CMD_END});
            builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching heart rate data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void deleteHeartRateData()
    {
        try {
            TransactionBuilder builder = performInitialized("deleteHeartRateData");
            sendMsgToWatch(builder, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_DELETE_HEARTRATE_DATA,
                    ZeTimeConstants.CMD_SEND,
                    0x01,
                    0x00,
                    0x00,
                    ZeTimeConstants.CMD_END});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error deleting heart rate data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void getSleepData()
    {
        try {
            TransactionBuilder builder = performInitialized("fetchSleepData");
            builder.write(writeCharacteristic, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_GET_SLEEP_DATA,
                ZeTimeConstants.CMD_REQUEST,
                0x02,
                0x00,
                0x00,
                0x00,
                ZeTimeConstants.CMD_END});
            builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching sleep data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void deleteSleepData()
    {
        try {
            TransactionBuilder builder = performInitialized("deleteSleepData");
            sendMsgToWatch(builder, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_DELETE_SLEEP_DATA,
                    ZeTimeConstants.CMD_SEND,
                    0x01,
                    0x00,
                    0x00,
                    ZeTimeConstants.CMD_END});
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error deleting sleep data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void handleStepsData(byte[] msg)
    {
        ZeTimeActivitySample sample = new ZeTimeActivitySample();
        int timestamp = (msg[10] << 24)&0xff000000 | (msg[9] << 16)&0xff0000 | (msg[8] << 8)&0xff00 | (msg[7]&0xff);
        timestamp += sevenHourOffset; // the timestamp from the watch has an offset of seven hours, do not know why...
        sample.setTimestamp(timestamp);
        sample.setSteps((msg[14] << 24)&0xff000000 | (msg[13] << 16)&0xff0000 | (msg[12] << 8)&0xff00 | (msg[11]&0xff));
        sample.setCaloriesBurnt((msg[18] << 24)&0xff000000 | (msg[17] << 16)&0xff0000 | (msg[16] << 8)&0xff00 | (msg[15]&0xff));
        sample.setDistanceMeters((msg[22] << 24)&0xff000000 | (msg[21] << 16)&0xff0000 | (msg[20] << 8)&0xff00 | (msg[19]&0xff));
        sample.setActiveTimeMinutes((msg[26] << 24)&0xff000000 | (msg[25] << 16)&0xff0000 | (msg[24] << 8)&0xff00 | (msg[23]&0xff));
        sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
        sample.setRawIntensity(sample.getSteps());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            sample.setUserId(DBHelper.getUser(dbHandler.getDaoSession()).getId());
            sample.setDeviceId(DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId());
            ZeTimeSampleProvider provider = new ZeTimeSampleProvider(getDevice(), dbHandler.getDaoSession());
            provider.addGBActivitySample(sample);
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
        }

        progressSteps = (msg[5]&0xff) | ((msg[6] << 8)&0xff00);
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, (int) (progressSteps *100 / availableStepsData), getContext());
        if (progressSteps == availableStepsData) {
            Prefs prefs = GBApplication.getPrefs();
            progressSteps = 0;
            availableStepsData = 0;
            GB.updateTransferNotification(null,"", false, 100, getContext());
            if (getDevice().isBusy()) {
                getDevice().unsetBusyTask();
                getDevice().sendDeviceUpdateIntent(getContext());
            }
            if (!prefs.getBoolean(ZeTimeConstants.PREF_ZETIME_DONT_DEL_ACTDATA, false)) {
                deleteStepData();
            }
            if(availableHeartRateData > 0) {
                getHeartRateData();
            } else if(availableSleepData > 0)
            {
                getSleepData();
            }
        }
    }

    private void handleSleepData(byte[] msg)
    {
        ZeTimeActivitySample sample = new ZeTimeActivitySample();
        int timestamp = (msg[10] << 24)&0xff000000 | (msg[9] << 16)&0xff0000 | (msg[8] << 8)&0xff00 | (msg[7]&0xff);
        timestamp += sevenHourOffset; // the timestamp from the watch has an offset of seven hours, do not know why...
        sample.setTimestamp(timestamp);
        if(msg[11] == 0) {
            sample.setRawKind(ActivityKind.TYPE_DEEP_SLEEP);
        } else if(msg[11] == 1)
        {
            sample.setRawKind(ActivityKind.TYPE_LIGHT_SLEEP);
        } else
        {
            sample.setRawKind(ActivityKind.TYPE_UNKNOWN);
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            sample.setUserId(DBHelper.getUser(dbHandler.getDaoSession()).getId());
            sample.setDeviceId(DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId());
            ZeTimeSampleProvider provider = new ZeTimeSampleProvider(getDevice(), dbHandler.getDaoSession());
            provider.addGBActivitySample(sample);
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
        }

        progressSleep = (msg[5]&0xff) | (msg[6] << 8)&0xff00;
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, (int) (progressSleep *100 / availableSleepData), getContext());
        if (progressSleep == availableSleepData) {
            Prefs prefs = GBApplication.getPrefs();
            progressSleep = 0;
            availableSleepData = 0;
            GB.updateTransferNotification(null,"", false, 100, getContext());
            if (getDevice().isBusy()) {
                getDevice().unsetBusyTask();
                getDevice().sendDeviceUpdateIntent(getContext());
            }
            if (!prefs.getBoolean(ZeTimeConstants.PREF_ZETIME_DONT_DEL_ACTDATA, false)) {
                deleteSleepData();
            }
        }
    }

    private void handleHeartRateData(byte[] msg)
    {
        ZeTimeActivitySample sample = new ZeTimeActivitySample();
        int timestamp = (msg[10] << 24)&0xff000000 | (msg[9] << 16)&0xff0000 | (msg[8] << 8)&0xff00 | (msg[7]&0xff);
        timestamp += sevenHourOffset; // the timestamp from the watch has an offset of seven hours, do not know why...
        sample.setHeartRate(msg[11]);
        sample.setTimestamp(timestamp);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            sample.setUserId(DBHelper.getUser(dbHandler.getDaoSession()).getId());
            sample.setDeviceId(DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId());
            ZeTimeSampleProvider provider = new ZeTimeSampleProvider(getDevice(), dbHandler.getDaoSession());
            provider.addGBActivitySample(sample);
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
        }

        progressHeartRate = (msg[5]&0xff) | ((msg[6] << 8)&0xff00);
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, (int) (progressHeartRate *100 / availableHeartRateData), getContext());

        if(((msg[4] << 8)&0xff00 | (msg[3]&0xff)) == 0xe) // if the message is longer than 0x7, than it has to measurements (payload = 0xe)
        {
            timestamp = (msg[17] << 24)&0xff000000 | (msg[16] << 16)&0xff0000 | (msg[15] << 8)&0xff00 | (msg[14]&0xff);
            timestamp += sevenHourOffset; // the timestamp from the watch has an offset of seven hours, do not know why...
            sample.setHeartRate(msg[18]);
            sample.setTimestamp(timestamp);

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                sample.setUserId(DBHelper.getUser(dbHandler.getDaoSession()).getId());
                sample.setDeviceId(DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId());
                ZeTimeSampleProvider provider = new ZeTimeSampleProvider(getDevice(), dbHandler.getDaoSession());
                provider.addGBActivitySample(sample);
            } catch (Exception ex) {
                GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
            }

            progressHeartRate = (msg[12]&0xff) | ((msg[13] << 8)&0xff00);
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, (int) (progressHeartRate *100 / availableHeartRateData), getContext());
        }

        if (progressHeartRate == availableHeartRateData) {
            Prefs prefs = GBApplication.getPrefs();
            progressHeartRate = 0;
            availableHeartRateData = 0;
            GB.updateTransferNotification(null,"", false, 100, getContext());
            if (getDevice().isBusy()) {
                getDevice().unsetBusyTask();
                getDevice().sendDeviceUpdateIntent(getContext());
            }
            if (!prefs.getBoolean(ZeTimeConstants.PREF_ZETIME_DONT_DEL_ACTDATA, false)) {
                deleteHeartRateData();
            }
            if(availableSleepData > 0)
            {
                getSleepData();
            }
        }
    }

    private void sendMsgToWatch(TransactionBuilder builder, byte[] msg)
    {
        if(msg.length > maxMsgLength)
        {
            int msgpartlength = 0;
            byte[] msgpart = null;

            do {
                if((msg.length - msgpartlength) < maxMsgLength)
                {
                    msgpart = new byte[msg.length - msgpartlength];
                    System.arraycopy(msg, msgpartlength, msgpart, 0, msg.length - msgpartlength);
                    msgpartlength += (msg.length - msgpartlength);
                } else {
                    msgpart = new byte[maxMsgLength];
                    System.arraycopy(msg, msgpartlength, msgpart, 0, maxMsgLength);
                    msgpartlength += maxMsgLength;
                }
                builder.write(writeCharacteristic, msgpart);
            }while(msgpartlength < msg.length);
        } else
        {
            builder.write(writeCharacteristic, msg);
        }
        builder.write(ackCharacteristic, new byte[]{ZeTimeConstants.CMD_ACK_WRITE});
    }

    private void handleMusicControl(byte[] musicControlMsg)
    {
        if(musicControlMsg[2] == ZeTimeConstants.CMD_SEND) {
            switch (musicControlMsg[5]) {
                case 0: // play current song
                    musicCmd.event = GBDeviceEventMusicControl.Event.PLAY;
                    break;
                case 1: // pause current song
                    musicCmd.event = GBDeviceEventMusicControl.Event.PAUSE;
                    break;
                case 2: // skip to previous song
                    musicCmd.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                    break;
                case 3: // skip to next song
                    musicCmd.event = GBDeviceEventMusicControl.Event.NEXT;
                    break;
                case 4: // change volume
                    if (musicControlMsg[6] > volume) {
                        musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        if(volume < 90) {
                            volume += (byte) 10;
                        }
                    } else {
                        musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        if(volume > 10) {
                            volume -= (byte) 10;
                        }
                    }
                    try {
                        TransactionBuilder builder = performInitialized("replyMusicVolume");
                        replyMsgToWatch(builder, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                                ZeTimeConstants.CMD_MUSIC_CONTROL,
                                ZeTimeConstants.CMD_REQUEST_RESPOND,
                                0x02,
                                0x00,
                                0x02,
                                volume,
                                ZeTimeConstants.CMD_END});
                        builder.queue(getQueue());
                    } catch (IOException e) {
                        GB.toast(getContext(), "Error reply the music volume: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                    }
                    break;
            }
            evaluateGBDeviceEvent(musicCmd);
        } else {
            if (music != null) {
                music[2] = ZeTimeConstants.CMD_REQUEST_RESPOND;
                try {
                    TransactionBuilder builder = performInitialized("replyMusicState");
                    replyMsgToWatch(builder, music);
                    builder.queue(getQueue());
                } catch (IOException e) {
                    GB.toast(getContext(), "Error reply the music state: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                }
            }
        }
    }

    private void replyMsgToWatch(TransactionBuilder builder, byte[] msg)
    {
        if(msg.length > maxMsgLength)
        {
            int msgpartlength = 0;
            byte[] msgpart = null;

            do {
                if((msg.length - msgpartlength) < maxMsgLength)
                {
                    msgpart = new byte[msg.length - msgpartlength];
                    System.arraycopy(msg, msgpartlength, msgpart, 0, msg.length - msgpartlength);
                    msgpartlength += (msg.length - msgpartlength);
                } else {
                    msgpart = new byte[maxMsgLength];
                    System.arraycopy(msg, msgpartlength, msgpart, 0, maxMsgLength);
                    msgpartlength += maxMsgLength;
                }
                builder.write(replyCharacteristic, msgpart);
            }while(msgpartlength < msg.length);
        } else
        {
            builder.write(replyCharacteristic, msg);
        }
    }

    private void synchronizeTime(TransactionBuilder builder)
    {
        Calendar now = GregorianCalendar.getInstance();
        byte[] timeSync = new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_DATE_TIME,
                ZeTimeConstants.CMD_SEND,
                0x0c,
                0x00,
                (byte)(now.get(Calendar.YEAR) & 0xff),
                (byte)(now.get(Calendar.YEAR) >> 8),
                (byte)(now.get(Calendar.MONTH) + 1),
                (byte)now.get(Calendar.DAY_OF_MONTH),
                (byte)now.get(Calendar.HOUR_OF_DAY),
                (byte)now.get(Calendar.MINUTE),
                (byte)now.get(Calendar.SECOND),
                0x00, // is 24h
                0x00, // SetTime after calibration
                0x01, // Unit
                (byte)((now.get(Calendar.ZONE_OFFSET)/3600000) + (now.get(Calendar.DST_OFFSET)/3600000)), // TimeZone hour + daylight saving
                0x00, // TimeZone minute
                ZeTimeConstants.CMD_END};
        sendMsgToWatch(builder, timeSync);
    }

    // function serving the settings
    private void setWrist(TransactionBuilder builder)
    {
        String value = GBApplication.getPrefs().getString(ZeTimeConstants.PREF_WRIST,"left");

        byte[] wrist = {ZeTimeConstants.CMD_PREAMBLE,
                        ZeTimeConstants.CMD_USAGE_HABITS,
                        ZeTimeConstants.CMD_SEND,
                        (byte)0x1,
                        (byte)0x0,
                        ZeTimeConstants.WEAR_ON_LEFT_WRIST,
                        ZeTimeConstants.CMD_END};
        if (value.equals("right")) {
            wrist[5] = ZeTimeConstants.WEAR_ON_RIGHT_WRIST;
        }

        LOG.warn("Wrist: " + wrist[5]);
        sendMsgToWatch(builder, wrist);
    }

    private void setScreenTime(TransactionBuilder builder)
    {
        int value = GBApplication.getPrefs().getInt(ZeTimeConstants.PREF_SCREENTIME, 30);
        if(value > ZeTimeConstants.MAX_SCREEN_ON_TIME)
        {
            GB.toast(getContext(), "Value for screen on time is greater than 18h! ", Toast.LENGTH_LONG, GB.ERROR);
            value = ZeTimeConstants.MAX_SCREEN_ON_TIME;
        } else if(value < ZeTimeConstants.MIN_SCREEN_ON_TIME)
        {
            GB.toast(getContext(), "Value for screen on time is lesser than 10s! ", Toast.LENGTH_LONG, GB.ERROR);
            value = ZeTimeConstants.MIN_SCREEN_ON_TIME;
        }

        byte[] screentime = {ZeTimeConstants.CMD_PREAMBLE,
                            ZeTimeConstants.CMD_DISPLAY_TIMEOUT,
                            ZeTimeConstants.CMD_SEND,
                            (byte)0x2,
                            (byte)0x0,
                            (byte)(value & 0xff),
                            (byte)(value >> 8),
                            ZeTimeConstants.CMD_END};

        sendMsgToWatch(builder, screentime);
    }

    private void setUserInfo(TransactionBuilder builder)
    {
        ActivityUser activityUser = new ActivityUser();
        byte gender = (byte)activityUser.getGender();
        int age = activityUser.getAge();
        int height = activityUser.getHeightCm();
        int weight = activityUser.getWeightKg()*10; // weight is set and get in 100g granularity

        if(gender == ActivityUser.GENDER_MALE) // translate gender for zetime
        {
            gender = 0;
        } else if(gender == ActivityUser.GENDER_FEMALE)
        {
            gender = 1;
        } else
        {
            gender = 2;
        }

        byte[] userinfo = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_USER_INFO,
                ZeTimeConstants.CMD_SEND,
                (byte)0x5,
                (byte)0x0,
                gender,
                (byte)age,
                (byte)height,
                (byte)(weight & 0xff),
                (byte)(weight >> 8),
                ZeTimeConstants.CMD_END};
        sendMsgToWatch(builder, userinfo);
    }

    private void setUserGoals(TransactionBuilder builder)
    {
        ActivityUser activityUser = new ActivityUser();
        int steps = activityUser.getStepsGoal() / 100; // ZeTime expect the steps in 100 increment
        int calories = activityUser.getCaloriesBurnt();
        int distance = activityUser.getDistanceMeters() / 1000;  // ZeTime only accepts km goals
        int sleep = activityUser.getSleepDuration();
        int activeTime = activityUser.getActiveTimeMinutes();

        // set steps goal
        byte[] goal_steps = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_GOALS,
                ZeTimeConstants.CMD_SEND,
                (byte)0x4,
                (byte)0x0,
                (byte)0x0,
                (byte)(steps & 0xff),
                (byte)(steps >> 8),
                (byte)0x1,
                ZeTimeConstants.CMD_END};
        sendMsgToWatch(builder, goal_steps);

        byte[] goal_calories = new byte[goal_steps.length];
        System.arraycopy(goal_steps, 0, goal_calories, 0, goal_steps.length);
        // set calories goal
        goal_calories[5] = (byte)0x1;
        goal_calories[6] = (byte)(calories & 0xff);
        goal_calories[7] = (byte)(calories >> 8);
        sendMsgToWatch(builder, goal_calories);

        byte[] goal_distance = new byte[goal_steps.length];
        System.arraycopy(goal_steps, 0, goal_distance, 0, goal_steps.length);
        // set distance goal
        goal_distance[5] = (byte)0x2;
        goal_distance[6] = (byte)(distance & 0xff);
        goal_distance[7] = (byte)(distance >> 8);
        sendMsgToWatch(builder, goal_distance);

        byte[] goal_sleep = new byte[goal_steps.length];
        System.arraycopy(goal_steps, 0, goal_sleep, 0, goal_steps.length);
        // set sleep goal
        goal_sleep[5] = (byte)0x3;
        goal_sleep[6] = (byte)(sleep & 0xff);
        goal_sleep[7] = (byte)(sleep >> 8);
        sendMsgToWatch(builder, goal_sleep);

        byte[] goal_activeTime = new byte[goal_steps.length];
        System.arraycopy(goal_steps, 0, goal_activeTime, 0, goal_steps.length);
        // set active time goal
        goal_activeTime[5] = (byte)0x4;
        goal_activeTime[6] = (byte)(activeTime & 0xff);
        goal_activeTime[7] = (byte)(activeTime >> 8);
        sendMsgToWatch(builder, goal_activeTime);
    }

    private void setHeartRateLimits(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();

        boolean alarmEnabled = prefs.getBoolean(ZeTimeConstants.PREF_ZETIME_HEARTRATE_ALARM, false);
        int maxHR = prefs.getInt(ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE, 180);
        int minHR = prefs.getInt(ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE, 60);

        byte[] heartrateAlarm = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_HEARTRATE_ALARM_LIMITS,
                ZeTimeConstants.CMD_SEND,
                (byte)0x3,
                (byte)0x0,
                (byte)(maxHR & 0xff),
                (byte)(minHR & 0xff),
                (byte)(alarmEnabled ? 1 : 0),  // activate alarm
                ZeTimeConstants.CMD_END};
        sendMsgToWatch(builder, heartrateAlarm);
    }

    private void initMusicVolume(TransactionBuilder builder)
    {
        replyMsgToWatch(builder, new byte[]{ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_MUSIC_CONTROL,
                ZeTimeConstants.CMD_REQUEST_RESPOND,
                0x02,
                0x00,
                0x02,
                volume,
                ZeTimeConstants.CMD_END});
    }

    private void setAnalogMode(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        int mode = prefs.getInt(ZeTimeConstants.PREF_ANALOG_MODE, 0);

        byte[] analog = {ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_ANALOG_MODE,
                    ZeTimeConstants.CMD_SEND,
                    (byte)0x1,
                    (byte)0x0,
                    (byte)mode,
                    ZeTimeConstants.CMD_END};

        sendMsgToWatch(builder, analog);
    }

    private void setActivityTracking(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        boolean tracking = prefs.getBoolean(ZeTimeConstants.PREF_ACTIVITY_TRACKING, false);

        byte[] activity = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_CONTROL_DEVICE,
                ZeTimeConstants.CMD_SEND,
                (byte)0x1,
                (byte)0x0,
                (byte)0x9,
                ZeTimeConstants.CMD_END};
        if(tracking)
        {
            activity[5] = (byte)0xa;
        }
        sendMsgToWatch(builder, activity);
    }

    private void setDisplayOnMovement(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        boolean movement = prefs.getBoolean(ZeTimeConstants.PREF_HANDMOVE_DISPLAY, false);

        byte[] handmove = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_SWITCH_SETTINGS,
                ZeTimeConstants.CMD_SEND,
                (byte)0x3,
                (byte)0x0,
                (byte)0x1,
                (byte)0xe,
                (byte)0x0,
                ZeTimeConstants.CMD_END};
        if(movement)
        {
            handmove[7] = (byte)0x1;
        }
        sendMsgToWatch(builder, handmove);
    }

    private void setDoNotDisturb(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        String scheduled = prefs.getString(ZeTimeConstants.PREF_DO_NOT_DISTURB, "off");
        String dndScheduled = getContext().getString(R.string.p_scheduled);
        String start = prefs.getString(ZeTimeConstants.PREF_DO_NOT_DISTURB_START, "22:00");
        String end = prefs.getString(ZeTimeConstants.PREF_DO_NOT_DISTURB_END, "07:00");
        DateFormat df_start = new SimpleDateFormat("HH:mm");
        DateFormat df_end = new SimpleDateFormat("HH:mm");
        Calendar calendar = GregorianCalendar.getInstance();
        Calendar calendar_end = GregorianCalendar.getInstance();

        try {
            calendar.setTime(df_start.parse(start));
            try {
                calendar_end.setTime(df_end.parse(end));

                byte[] doNotDisturb = {ZeTimeConstants.CMD_PREAMBLE,
                        ZeTimeConstants.CMD_DO_NOT_DISTURB,
                        ZeTimeConstants.CMD_SEND,
                        (byte)0x5,
                        (byte)0x0,
                        (byte)0x0,
                        (byte)calendar.get(Calendar.HOUR_OF_DAY),
                        (byte)calendar.get(Calendar.MINUTE),
                        (byte)calendar_end.get(Calendar.HOUR_OF_DAY),
                        (byte)calendar_end.get(Calendar.MINUTE),
                        ZeTimeConstants.CMD_END};

                if(scheduled.equals(dndScheduled))
                {
                    doNotDisturb[5] = (byte)0x1;
                }
                sendMsgToWatch(builder, doNotDisturb);
            } catch(Exception e) {
                LOG.error("Unexpected exception in ZeTimeDeviceSupport.setDoNotDisturb: " + e.getMessage());
            }
        } catch(Exception e) {
            LOG.error("Unexpected exception in ZeTimeDeviceSupport.setDoNotDisturb: " + e.getMessage());
        }
    }

    private void setCaloriesType(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        int type = prefs.getInt(ZeTimeConstants.PREF_CALORIES_TYPE, 0);

        byte[] calories = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_CALORIES_TYPE,
                ZeTimeConstants.CMD_SEND,
                (byte)0x1,
                (byte)0x0,
                (byte)type,
                ZeTimeConstants.CMD_END};

        sendMsgToWatch(builder, calories);
    }

    private void setTimeFormate(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        int type = prefs.getInt(ZeTimeConstants.PREF_TIME_FORMAT, 0);

        byte[] timeformat = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_TIME_SURFACE_SETTINGS,
                ZeTimeConstants.CMD_SEND,
                (byte)0x8,
                (byte)0x0,
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)type,
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                ZeTimeConstants.CMD_END};

        sendMsgToWatch(builder, timeformat);
    }

    private void setDateFormate(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        int type = prefs.getInt(ZeTimeConstants.PREF_DATE_FORMAT, 0);

        byte[] dateformat = {ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_TIME_SURFACE_SETTINGS,
                ZeTimeConstants.CMD_SEND,
                (byte)0x8,
                (byte)0x0,
                (byte)type,
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                (byte)0xff, // set to ff to not change anything on the watch
                ZeTimeConstants.CMD_END};

        sendMsgToWatch(builder, dateformat);
    }

    private void setInactivityAlert(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        boolean enabled = prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_ENABLE, false);
        int threshold = prefs.getInt(ZeTimeConstants.PREF_INACTIVITY_THRESHOLD, 60);

        if(threshold > 0xff)
        {
            threshold = 0xff;
            GB.toast(getContext(), "Value for inactivity threshold is greater than 255min! ", Toast.LENGTH_LONG, GB.ERROR);
        }

        byte[] inactivity = {
                ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_INACTIVITY_ALERT,
                ZeTimeConstants.CMD_SEND,
                (byte)0x8,
                (byte)0x0,
                (byte)0x0,
                (byte)threshold,
                (byte)0x0,
                (byte)0x0,
                (byte)0x0,
                (byte)0x0,
                (byte)0x64,
                (byte)0x0,
                ZeTimeConstants.CMD_END
        };

        if(enabled)
        {
            String start = prefs.getString(ZeTimeConstants.PREF_INACTIVITY_START, "06:00");
            String end = prefs.getString(ZeTimeConstants.PREF_INACTIVITY_END, "22:00");
            DateFormat df_start = new SimpleDateFormat("HH:mm");
            DateFormat df_end = new SimpleDateFormat("HH:mm");
            Calendar calendar = GregorianCalendar.getInstance();
            Calendar calendar_end = GregorianCalendar.getInstance();

            int reps = (1 << 7); // set inactivity active: set bit 7
            reps |= (prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_MO, false) ? 1 : 0);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_TU, false) ? 1 : 0) << 1);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_WE, false) ? 1 : 0) << 2);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_TH, false) ? 1 : 0) << 3);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_FR, false) ? 1 : 0) << 4);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_SA, false) ? 1 : 0) << 5);
            reps |= ((prefs.getBoolean(ZeTimeConstants.PREF_INACTIVITY_SU, false) ? 1 : 0) << 6);

            inactivity[5] = (byte)reps;

            try {
                calendar.setTime(df_start.parse(start));
                try {
                    calendar_end.setTime(df_end.parse(end));

                    inactivity[7] = (byte)calendar.get(Calendar.HOUR_OF_DAY);
                    inactivity[8] = (byte)calendar.get(Calendar.MINUTE);
                    inactivity[9] = (byte)calendar_end.get(Calendar.HOUR_OF_DAY);
                    inactivity[10] = (byte)calendar_end.get(Calendar.MINUTE);
                } catch(Exception e) {
                    LOG.error("Unexpected exception in ZeTimeDeviceSupport.setInactivityAlert: " + e.getMessage());
                }
            } catch(Exception e) {
                LOG.error("Unexpected exception in ZeTimeDeviceSupport.setInactivityAlert: " + e.getMessage());
            }
        }

        sendMsgToWatch(builder, inactivity);
    }

    private void setShockStrength(TransactionBuilder builder)
    {
        Prefs prefs = GBApplication.getPrefs();
        int shockStrength = prefs.getInt(ZeTimeConstants.PREF_SHOCK_STRENGTH, 255);

        byte[] strength = {
                ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_SHOCK_STRENGTH,
                ZeTimeConstants.CMD_SEND,
                (byte)0x1,
                (byte)0x0,
                (byte)shockStrength,
                ZeTimeConstants.CMD_END
        };

        sendMsgToWatch(builder, strength);
    }

    private void setSignaling(TransactionBuilder builder, String signalingType)
    {
        Prefs prefs = GBApplication.getPrefs();
        int signalType = prefs.getInt(signalingType, 0);

        byte[] signaling = {
                ZeTimeConstants.CMD_PREAMBLE,
                ZeTimeConstants.CMD_SHOCK_MODE,
                ZeTimeConstants.CMD_SEND,
                (byte)0x2,
                (byte)0x0,
                (byte)0x0,
                (byte)signalType,
                ZeTimeConstants.CMD_END
        };

        switch(signalingType)
        {
            case ZeTimeConstants.PREF_SMS_SIGNALING:
                signaling[5] = ZeTimeConstants.SMS_TYPE;
                break;
            case ZeTimeConstants.PREF_CALL_SIGNALING:
                signaling[5] = ZeTimeConstants.CALL_TYPE;
                break;
            case ZeTimeConstants.PREF_MISSED_CALL_SIGNALING:
                signaling[5] = ZeTimeConstants.MISSED_CALL_TYPE;
                break;
            case ZeTimeConstants.PREF_EMAIL_SIGNALING:
                signaling[5] = ZeTimeConstants.EMAIL_TYPE;
                break;
            case ZeTimeConstants.PREF_SOCIAL_SIGNALING:
                signaling[5] = ZeTimeConstants.SOCIAL_TYPE;
                break;
            case ZeTimeConstants.PREF_CALENDAR_SIGNALING:
                signaling[5] = ZeTimeConstants.CALENDAR_TYPE;
                break;
            case ZeTimeConstants.PREF_INACTIVITY_SIGNALING:
                signaling[5] = ZeTimeConstants.INACTIVITY_TYPE;
                break;
            case ZeTimeConstants.PREF_LOW_POWER_SIGNALING:
                signaling[5] = ZeTimeConstants.LOW_POWER_TYPE;
                break;
            case ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING:
                signaling[5] = ZeTimeConstants.ANTI_LOSS_TYPE;
                break;
        }

        sendMsgToWatch(builder, signaling);
    }

    @Override
    public void onReadConfiguration(String config) {
        try {
            TransactionBuilder builder = performInitialized("readConfiguration");

            if (!getDevice().isBusy()) {
                getDevice().setBusyTask("readConfiguration");
            }

            byte[] configRead1 = {
                    ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_TIME_SURFACE_SETTINGS,
                    ZeTimeConstants.CMD_REQUEST,
                    (byte)0x1,
                    (byte)0x0,
                    (byte)0x0,
                    ZeTimeConstants.CMD_END
            };
            sendMsgToWatch(builder, configRead1);

            byte[] configRead2 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead2, 0, configRead1.length);
            configRead2[1] = ZeTimeConstants.CMD_SHOCK_MODE;
            sendMsgToWatch(builder, configRead2);

            byte[] configRead3 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead3, 0, configRead1.length);
            configRead3[1] = ZeTimeConstants.CMD_DO_NOT_DISTURB;
            sendMsgToWatch(builder, configRead3);

            byte[] configRead4 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead4, 0, configRead1.length);
            configRead4[1] = ZeTimeConstants.CMD_ANALOG_MODE;
            sendMsgToWatch(builder, configRead4);

            byte[] configRead5 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead5, 0, configRead1.length);
            configRead5[1] = ZeTimeConstants.CMD_CONTROL_DEVICE;
            sendMsgToWatch(builder, configRead5);

            byte[] configRead6 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead6, 0, configRead1.length);
            configRead6[1] = ZeTimeConstants.CMD_DISPLAY_TIMEOUT;
            sendMsgToWatch(builder, configRead6);

            byte[] configRead7 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead7, 0, configRead1.length);
            configRead7[1] = ZeTimeConstants.CMD_USAGE_HABITS;
            sendMsgToWatch(builder, configRead7);

            byte[] configRead8 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead8, 0, configRead1.length);
            configRead8[1] = ZeTimeConstants.CMD_AUTO_HEARTRATE;
            sendMsgToWatch(builder, configRead8);

            byte[] configRead9 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead9, 0, configRead1.length);
            configRead9[1] = ZeTimeConstants.CMD_HEARTRATE_ALARM_LIMITS;
            sendMsgToWatch(builder, configRead9);

            byte[] configRead10 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead10, 0, configRead1.length);
            configRead10[1] = ZeTimeConstants.CMD_INACTIVITY_ALERT;
            sendMsgToWatch(builder, configRead10);

            byte[] configRead11 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead11, 0, configRead1.length);
            configRead11[1] = ZeTimeConstants.CMD_CALORIES_TYPE;
            sendMsgToWatch(builder, configRead11);

            byte[] configRead12 = new byte[configRead1.length];
            System.arraycopy(configRead1, 0, configRead12, 0, configRead1.length);
            configRead12[1] = ZeTimeConstants.CMD_SWITCH_SETTINGS;
            sendMsgToWatch(builder, configRead12);

            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error reading configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void onReadReminders(TransactionBuilder builder) {
//        try {
//            TransactionBuilder builder = performInitialized("readReminders");

            byte[] reminders = {
                    ZeTimeConstants.CMD_PREAMBLE,
                    ZeTimeConstants.CMD_REMINDERS,
                    ZeTimeConstants.CMD_REQUEST,
                    (byte)0x1,
                    (byte)0x0,
                    (byte)0x0,
                    ZeTimeConstants.CMD_END
            };
            sendMsgToWatch(builder, reminders);

            builder.queue(getQueue());
//        } catch (IOException e) {
//            GB.toast(getContext(), "Error reading reminders: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
//        }
    }

    private void getDateTimeFormat(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        prefs.putString(ZeTimeConstants.PREF_DATE_FORMAT, Integer.toString(msg[5]));
        prefs.putString(ZeTimeConstants.PREF_TIME_FORMAT, Integer.toString(msg[6]));
        prefs.apply();
    }

    private void getSignaling(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        prefs.putString(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING, Integer.toString(msg[5]));
        prefs.putString(ZeTimeConstants.PREF_CALL_SIGNALING, Integer.toString(msg[7]));
        prefs.putString(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING, Integer.toString(msg[8]));
        prefs.putString(ZeTimeConstants.PREF_SMS_SIGNALING, Integer.toString(msg[9]));
        prefs.putString(ZeTimeConstants.PREF_SOCIAL_SIGNALING, Integer.toString(msg[10]));
        prefs.putString(ZeTimeConstants.PREF_EMAIL_SIGNALING, Integer.toString(msg[11]));
        prefs.putString(ZeTimeConstants.PREF_CALENDAR_SIGNALING, Integer.toString(msg[12]));
        prefs.putString(ZeTimeConstants.PREF_INACTIVITY_SIGNALING, Integer.toString(msg[13]));
        prefs.putString(ZeTimeConstants.PREF_LOW_POWER_SIGNALING, Integer.toString(msg[14]));
        prefs.apply();
    }

    private void getDoNotDisturb(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();
        String starttime = String.format("%02d:%02d", msg[6], msg[7]);
        String endtime = String.format("%02d:%02d", msg[8], msg[9]);

        if(0x1 == msg[5]) {
            prefs.putString(ZeTimeConstants.PREF_DO_NOT_DISTURB, "scheduled");
        } else {
            prefs.putString(ZeTimeConstants.PREF_DO_NOT_DISTURB, "off");
        }
        prefs.putString(ZeTimeConstants.PREF_DO_NOT_DISTURB_START, starttime);
        prefs.putString(ZeTimeConstants.PREF_DO_NOT_DISTURB_END, endtime);
        prefs.apply();
    }

    private void getAnalogMode(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        prefs.putString(ZeTimeConstants.PREF_ANALOG_MODE, Integer.toString(msg[5]));
        prefs.apply();
    }

    private void getActivityTracking(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        if(0x1 == msg[6])
        {
            prefs.putBoolean(ZeTimeConstants.PREF_ACTIVITY_TRACKING, false);
        } else
        {
            prefs.putBoolean(ZeTimeConstants.PREF_ACTIVITY_TRACKING, true);
        }
        prefs.apply();
    }

    private void getScreenTime(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        prefs.putString(ZeTimeConstants.PREF_SCREENTIME, Integer.toString((msg[5] | (msg[6] << 8))));
        prefs.apply();
    }

    private void getWrist(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();

        if(ZeTimeConstants.WEAR_ON_LEFT_WRIST == msg[5]) {
            prefs.putString(ZeTimeConstants.PREF_WRIST, "left");
        } else if(ZeTimeConstants.WEAR_ON_RIGHT_WRIST == msg[5]) {
            prefs.putString(ZeTimeConstants.PREF_WRIST, "right");
        }
        prefs.apply();
    }

    private void getHeartRateMeasurement(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();
        prefs.putString(ZeTimeConstants.PREF_ZETIME_HEARTRATE_INTERVAL, Integer.toString((msg[5]*60))); // multiply with 60 because of the conversion from minutes to seconds
        prefs.apply();
    }

    private void getHeartRateLimits(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();
        prefs.apply();
    }

    private void getInactivityAlert(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();
        String starttime = String.format("%02d:%02d", msg[7], msg[8]);
        String endtime = String.format("%02d:%02d", msg[9], msg[10]);

        prefs.putString(ZeTimeConstants.PREF_INACTIVITY_THRESHOLD, Integer.toString(msg[6]));
        if(0 != msg[5])
        {
            prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_ENABLE, true);
            prefs.putString(ZeTimeConstants.PREF_INACTIVITY_START, starttime);
            prefs.putString(ZeTimeConstants.PREF_INACTIVITY_END, endtime);
            if(0 != (msg[5] & (1 << 0))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_MO, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_MO, false);
            }
            if(0 != (msg[5] & (1 << 1))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_TU, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_TU, false);
            }
            if(0 != (msg[5] & (1 << 2))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_WE, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_WE, false);
            }
            if(0 != (msg[5] & (1 << 3))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_TH, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_TH, false);
            }
            if(0 != (msg[5] & (1 << 4))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_FR, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_FR, false);
            }
            if(0 != (msg[5] & (1 << 5))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_SA, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_SA, false);
            }
            if(0 != (msg[5] & (1 << 6))) {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_SU, true);
            } else
            {
                prefs.putBoolean(ZeTimeConstants.PREF_INACTIVITY_SU, false);
            }
        }
        prefs.apply();
    }

    private void getCaloriesType(byte[] msg)
    {
        SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor myedit = prefs.edit();

        myedit.putString(ZeTimeConstants.PREF_CALORIES_TYPE, Integer.toString(msg[5]));
        myedit.apply();
    }

    private void getDisplayOnMovement(byte[] msg)
    {
        SharedPreferences.Editor prefs = GBApplication.getPrefs().getPreferences().edit();
        if(0 != (msg[6] & (1 << 6))) {
            prefs.putBoolean(ZeTimeConstants.PREF_HANDMOVE_DISPLAY, true);
        } else {
            prefs.putBoolean(ZeTimeConstants.PREF_HANDMOVE_DISPLAY, false);
        }
        prefs.apply();
        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());

        }
    }

    private void storeActualReminders(byte[] msg)
    {
        if(msg[3] == 0xb) // there is a reminder on the watch
        {
            System.arraycopy(msg, 6, remindersOnWatch[msg[5]-1], 0, 10);
        }
    }
}
