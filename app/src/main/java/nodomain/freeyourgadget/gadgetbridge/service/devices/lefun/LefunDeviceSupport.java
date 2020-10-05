/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti
    Copyright (C) 2020 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.FindPhoneCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetActivityDataCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetPpgDataCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetSleepDataCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetStepsDataCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.PpgResultCommand;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunBiometricSample;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunSleepSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.FindDeviceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetActivityDataRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetBatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetFirmwareInfoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetPpgDataRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetSleepDataRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.SendCallNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.SendNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.SetAlarmRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.StartPpgRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LefunDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LefunDeviceSupport.class);

    private final List<Request> inProgressRequests = Collections.synchronizedList(new ArrayList<Request>());
    private final Queue<Request> queuedRequests = new ConcurrentLinkedQueue<>();

    private int lastStepsCount = -1;
    private int lastStepsTimestamp;

    public LefunDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(LefunConstants.UUID_SERVICE_LEFUN);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setGattCallback(this);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // Enable notification
        builder.notify(getCharacteristic(LefunConstants.UUID_CHARACTERISTIC_LEFUN_NOTIFY), true);

        // Init device (get version info, battery level, and set time)
        try {
            GetFirmwareInfoRequest firmwareReq = new GetFirmwareInfoRequest(this, builder);
            firmwareReq.perform();
            inProgressRequests.add(firmwareReq);

            SetTimeRequest timeReq = new SetTimeRequest(this, builder);
            timeReq.perform();
            inProgressRequests.add(timeReq);

            GetBatteryLevelRequest batReq = new GetBatteryLevelRequest(this, builder);
            batReq.perform();
            inProgressRequests.add(batReq);
        } catch (IOException e) {
            GB.toast(getContext(), "Failed to initialize Lefun device", Toast.LENGTH_SHORT,
                    GB.ERROR, e);
        }

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        try {
            TransactionBuilder builder = performInitialized(SetTimeRequest.class.getSimpleName());
            SendNotificationRequest request = new SendNotificationRequest(this, builder);
            request.setNotification(notificationSpec);
            request.perform();
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Failed to send notification", Toast.LENGTH_SHORT,
                    GB.ERROR, e);
        }
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized(SetTimeRequest.class.getSimpleName());
            SetTimeRequest request = new SetTimeRequest(this, builder);
            request.perform();
            inProgressRequests.add(request);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Failed to set time", Toast.LENGTH_SHORT,
                    GB.ERROR, e);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        int i = 0;
        for (Alarm alarm : alarms) {
            try {
                TransactionBuilder builder = performInitialized(SetAlarmRequest.class.getSimpleName());
                SetAlarmRequest request = new SetAlarmRequest(this, builder);
                request.setIndex(i);
                request.setEnabled(alarm.getEnabled());
                request.setDayOfWeek(alarm.getRepetition());
                request.setHour(alarm.getHour());
                request.setMinute(alarm.getMinute());
                request.perform();
                inProgressRequests.add(request);
                performConnected(builder.getTransaction());
            } catch (IOException e) {
                GB.toast(getContext(), "Failed to set alarm", Toast.LENGTH_SHORT,
                        GB.ERROR, e);
            }
            ++i;
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                try {
                    TransactionBuilder builder = performInitialized(SetTimeRequest.class.getSimpleName());
                    SendCallNotificationRequest request = new SendCallNotificationRequest(this, builder);
                    request.setCallNotification(callSpec);
                    request.perform();
                    performConnected(builder.getTransaction());
                } catch (IOException e) {
                    GB.toast(getContext(), "Failed to send call notification", Toast.LENGTH_SHORT,
                            GB.ERROR, e);
                }
                break;
        }
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
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            for (int i = 0; i < 7; ++i) {
                GetActivityDataRequest req = new GetActivityDataRequest(this);
                req.setDaysAgo(i);
                queuedRequests.add(req);
            }

            for (int i = 0; i < LefunConstants.PPG_TYPE_COUNT; ++i) {
                GetPpgDataRequest req = new GetPpgDataRequest(this);
                req.setPpgType(i);
                queuedRequests.add(req);
            }

            for (int i = 0; i < 7; ++i) {
                GetSleepDataRequest req = new GetSleepDataRequest(this);
                req.setDaysAgo(i);
                queuedRequests.add(req);
            }

            runNextQueuedRequest();
        }
    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized(StartPpgRequest.class.getSimpleName());
            StartPpgRequest request = new StartPpgRequest(this, builder);
            request.setPpgType(LefunConstants.PPG_TYPE_HEART_RATE);
            request.perform();
            inProgressRequests.add(request);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Failed to start heart rate test", Toast.LENGTH_SHORT,
                    GB.ERROR, e);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            try {
                TransactionBuilder builder = performInitialized(FindDeviceRequest.class.getSimpleName());
                FindDeviceRequest request = new FindDeviceRequest(this, builder);
                request.perform();
                inProgressRequests.add(request);
                performConnected(builder.getTransaction());
            } catch (IOException e) {
                GB.toast(getContext(), "Failed to initiate find device", Toast.LENGTH_SHORT,
                        GB.ERROR, e);
            }
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {

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

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(LefunConstants.UUID_CHARACTERISTIC_LEFUN_NOTIFY)) {
            byte[] data = characteristic.getValue();
            // Parse response
            if (data.length >= LefunConstants.CMD_HEADER_LENGTH && data[0] == LefunConstants.CMD_RESPONSE_ID) {
                // Note: full validation is done within the request
                byte commandId = data[2];
                synchronized (inProgressRequests) {
                    for (Request req : inProgressRequests) {
                        if (req.expectsResponse() && req.getCommandId() == commandId) {
                            try {
                                req.handleResponse(data);
                                if (req.shouldRemoveAfterHandling())
                                    inProgressRequests.remove(req);
                                return true;
                            } catch (IllegalArgumentException e) {
                                LOG.error("Failed to handle response", e);
                            }
                        }
                    }
                }

                if (handleAsynchronousResponse(commandId, data))
                    return true;

                logMessageContent(data);
                LOG.error(String.format("No handler for response 0x%02x", commandId));
                return false;
            }

            logMessageContent(data);
            LOG.error("Invalid response received");
            return false;
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handleAsynchronousResponse(byte commandId, byte[] data) {
        // Assume data already checked for correct response code and length
        switch (commandId) {
            case LefunConstants.CMD_PPG_RESULT:
                return handleAsynchronousPpgResult(data);
            case LefunConstants.CMD_FIND_PHONE:
                return handleAntiLoss(data);
            case LefunConstants.CMD_STEPS_DATA:
                return handleAsynchronousActivity(data);
        }
        return false;
    }

    private boolean handleAsynchronousActivity(byte[] data) {
        try {
            GetStepsDataCommand cmd = new GetStepsDataCommand();
            cmd.deserialize(data);
            broadcastSample(cmd);
            return true;
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to handle live activity update", e);
            return false;
        }
    }

    // Adapted from nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3.MakibesHR3DeviceSupport.broadcastSample
    private void broadcastSample(GetStepsDataCommand command) {
        Calendar now = Calendar.getInstance();
        int timestamp = (int) (now.getTimeInMillis() / 1000);
        // Workaround for a world where sub-second time resolution is not a thing
        if (lastStepsTimestamp == timestamp) return;
        lastStepsTimestamp = timestamp;
        LefunActivitySample sample = new LefunActivitySample();
        sample.setTimestamp(timestamp);
        if (lastStepsCount == -1 || command.getSteps() < lastStepsCount) {
            lastStepsCount = command.getSteps();
        }
        int diff = command.getSteps() - lastStepsCount;
        sample.setSteps(diff);
        lastStepsCount = command.getSteps();
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private boolean handleAsynchronousPpgResult(byte[] data) {
        try {
            PpgResultCommand cmd = new PpgResultCommand();
            cmd.deserialize(data);
            handlePpgData(cmd);
            return true;
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to PPG result", e);
            return false;
        }
    }

    private boolean handleAntiLoss(byte[] data) {
        try {
            FindPhoneCommand cmd = new FindPhoneCommand();
            cmd.deserialize(data);
            GBDeviceEventFindPhone event = new GBDeviceEventFindPhone();
            event.event = GBDeviceEventFindPhone.Event.START;
            evaluateGBDeviceEvent(event);
            return true;
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to handle anti-loss", e);
            return false;
        }
    }

    public void completeInitialization() {
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    private int dateToTimestamp(byte year, byte month, byte day, byte hour, byte minute, byte second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(
                ((int) year & 0xff) + 2000,
                ((int) month & 0xff) - 1,
                (int) day,
                (int) hour,
                (int) minute,
                (int) second
        );
        return (int) (calendar.getTimeInMillis() / 1000);
    }

    private LefunActivitySample getActivitySample(DaoSession session, int timestamp) {
        LefunActivitySampleDao dao = session.getLefunActivitySampleDao();
        Long userId = DBHelper.getUser(session).getId();
        Long deviceId = DBHelper.getDevice(getDevice(), session).getId();
        Query<LefunActivitySample> q = dao.queryBuilder()
                .where(LefunActivitySampleDao.Properties.Timestamp.eq(timestamp))
                .where(LefunActivitySampleDao.Properties.DeviceId.eq(deviceId))
                .where(LefunActivitySampleDao.Properties.UserId.eq(userId))
                .build();
        return q.unique();
    }

    public void handleActivityData(GetActivityDataCommand command) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();
            int timestamp = dateToTimestamp(command.getYear(), command.getMonth(), command.getDay(),
                    command.getHour(), command.getMinute(), (byte) 0);
            // For the most part I'm ignoring the sample provider, because it doesn't really help
            // when I need to combine sample data instead of replacing
            LefunActivitySample sample = getActivitySample(session, timestamp);
            if (sample == null) {
                sample = new LefunActivitySample(timestamp,
                        DBHelper.getDevice(getDevice(), session).getId());
                sample.setUserId(DBHelper.getUser(session).getId());
                sample.setRawKind(LefunConstants.DB_ACTIVITY_KIND_ACTIVITY);
            }

            sample.setSteps(command.getSteps());
            sample.setDistance(command.getDistance());
            sample.setCalories(command.getCalories());
            sample.setRawIntensity(LefunConstants.INTENSITY_AWAKE);

            session.getLefunActivitySampleDao().insertOrReplace(sample);
        } catch (Exception e) {
            LOG.error("Error handling activity data", e);
        }
    }

    private void handlePpgData(int timestamp, int ppgType, byte[] ppgData) {
        int ppgData0 = ppgData[0] & 0xff;
        int ppgData1 = ppgData.length > 1 ? ppgData[1] & 0xff : 0;

        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            if (ppgType == LefunConstants.PPG_TYPE_HEART_RATE) {
                LefunActivitySample sample = getActivitySample(session, timestamp);
                if (sample == null) {
                    sample = new LefunActivitySample(timestamp,
                            DBHelper.getDevice(getDevice(), session).getId());
                    sample.setUserId(DBHelper.getUser(session).getId());
                    sample.setRawKind(LefunConstants.DB_ACTIVITY_KIND_HEART_RATE);
                }

                sample.setHeartRate(ppgData0);

                session.getLefunActivitySampleDao().insertOrReplace(sample);
            }

            LefunBiometricSample bioSample = new LefunBiometricSample(timestamp,
                    DBHelper.getDevice(getDevice(), session).getId());
            bioSample.setUserId(DBHelper.getUser(session).getId());
            bioSample.setType(ppgType);
            bioSample.setValue1(ppgData0);
            bioSample.setValue2(ppgData1);
            session.getLefunBiometricSampleDao().insertOrReplace(bioSample);
        } catch (Exception e) {
            LOG.error("Error handling PPG data", e);
        }
    }

    public void handlePpgData(GetPpgDataCommand command) {
        int timestamp = dateToTimestamp(command.getYear(), command.getMonth(), command.getDay(),
                command.getHour(), command.getMinute(), command.getSecond());
        int ppgType = command.getPpgType();
        byte[] ppgData = command.getPpgData();
        handlePpgData(timestamp, ppgType, ppgData);
    }

    public void handlePpgData(PpgResultCommand command) {
        int timestamp = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        int ppgType = command.getPpgType();
        byte[] ppgData = command.getPpgData();
        handlePpgData(timestamp, ppgType, ppgData);
    }

    public void handleSleepData(GetSleepDataCommand command) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();
            int timestamp = dateToTimestamp(command.getYear(), command.getMonth(), command.getDay(),
                    command.getHour(), command.getMinute(), (byte) 0);

            LefunActivitySample sample = getActivitySample(session, timestamp);
            if (sample == null) {
                sample = new LefunActivitySample(timestamp,
                        DBHelper.getDevice(getDevice(), session).getId());
                sample.setUserId(DBHelper.getUser(session).getId());
            }

            int rawKind;
            int intensity;
            switch (command.getSleepType()) {
                case GetSleepDataCommand.SLEEP_TYPE_AWAKE:
                    rawKind = LefunConstants.DB_ACTIVITY_KIND_ACTIVITY;
                    intensity = LefunConstants.INTENSITY_AWAKE;
                    break;
                case GetSleepDataCommand.SLEEP_TYPE_LIGHT_SLEEP:
                    rawKind = LefunConstants.DB_ACTIVITY_KIND_LIGHT_SLEEP;
                    intensity = LefunConstants.INTENSITY_LIGHT_SLEEP;
                    break;
                case GetSleepDataCommand.SLEEP_TYPE_DEEP_SLEEP:
                    rawKind = LefunConstants.DB_ACTIVITY_KIND_DEEP_SLEEP;
                    intensity = LefunConstants.INTENSITY_DEEP_SLEEP;
                    break;
                default:
                    rawKind = LefunConstants.DB_ACTIVITY_KIND_UNKNOWN;
                    intensity = LefunConstants.INTENSITY_AWAKE;
                    break;
            }

            sample.setRawKind(rawKind);
            sample.setRawIntensity(intensity);

            session.getLefunActivitySampleDao().insertOrReplace(sample);

            LefunSleepSample sleepSample = new LefunSleepSample(timestamp,
                    DBHelper.getDevice(getDevice(), session).getId());
            sleepSample.setUserId(DBHelper.getUser(session).getId());
            sleepSample.setType(command.getSleepType());
            session.getLefunSleepSampleDao().insertOrReplace(sleepSample);
        } catch (Exception e) {
            LOG.error("Error handling sleep data", e);
        }
    }

    public void runNextQueuedRequest() {
        Request request = queuedRequests.poll();
        if (request != null) {
            try {
                request.perform();
                if (!request.isSelfQueue())
                    performConnected(request.getTransactionBuilder().getTransaction());
            } catch (IOException e) {
                GB.toast(getContext(), "Failed to run next queued request", Toast.LENGTH_SHORT,
                        GB.ERROR, e);
            }
        }
    }
}
