/*  Copyright (C) 2017-2018 Andreas Shimokawa, Daniele Gobbetti, protomors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.no1f1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.no1f1.No1F1Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.no1f1.No1F1SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.No1F1ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static org.apache.commons.lang3.math.NumberUtils.min;

public class No1F1Support extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(No1F1Support.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;
    private List<No1F1ActivitySample> samples = new ArrayList<>();
    private byte crc = 0;
    private int firstTimestamp = 0;

    public No1F1Support() {
        super(LOG);
        addSupportedService(No1F1Constants.UUID_SERVICE_NO1);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(No1F1Constants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(No1F1Constants.UUID_CHARACTERISTIC_CONTROL);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        sendSettings(builder);

        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_FIRMWARE_VERSION});
        builder.write(ctrlCharacteristic, new byte[]{No1F1Constants.CMD_BATTERY});

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case No1F1Constants.CMD_FIRMWARE_VERSION:
                versionCmd.fwVersion = new String(Arrays.copyOfRange(data, 1, data.length));
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case No1F1Constants.CMD_BATTERY:
                batteryCmd.level = data[1];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + data[1]);
                return true;
            case No1F1Constants.CMD_DATETIME:
                LOG.info("Time is set to: " + (data[1] * 256 + ((int) data[2] & 0xff)) + "-" + data[3] + "-" + data[4] + " " + data[5] + ":" + data[6] + ":" + data[7]);
                return true;
            case No1F1Constants.CMD_USER_DATA:
                LOG.info("User data updated");
                return true;
            case No1F1Constants.CMD_FETCH_STEPS:
            case No1F1Constants.CMD_FETCH_SLEEP:
            case No1F1Constants.CMD_FETCH_HEARTRATE:
                handleActivityData(data);
                return true;
            case No1F1Constants.CMD_REALTIME_HEARTRATE:
                handleRealtimeHeartRateData(data);
                return true;
            case No1F1Constants.CMD_NOTIFICATION:
            case No1F1Constants.CMD_ICON:
            case No1F1Constants.CMD_DEVICE_SETTINGS:
            case No1F1Constants.CMD_DISPLAY_SETTINGS:
                return true;
            default:
                LOG.warn("Unhandled characteristic change: " + characteristicUUID + " code: " + Arrays.toString(data));
                return true;
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                showNotification(No1F1Constants.NOTIFICATION_SMS, notificationSpec.phoneNumber, notificationSpec.body);
                setVibration(1, 3);
                break;
            default:
                showIcon(No1F1Constants.ICON_WECHAT);
                setVibration(1, 2);
                break;
        }
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            Calendar c = GregorianCalendar.getInstance();
            byte[] datetimeBytes = new byte[]{
                    No1F1Constants.CMD_DATETIME,
                    (byte) ((c.get(Calendar.YEAR) / 256) & 0xff),
                    (byte) (c.get(Calendar.YEAR) % 256),
                    (byte) (c.get(Calendar.MONTH) + 1),
                    (byte) c.get(Calendar.DAY_OF_MONTH),
                    (byte) c.get(Calendar.HOUR_OF_DAY),
                    (byte) c.get(Calendar.MINUTE),
                    (byte) c.get(Calendar.SECOND)
            };
            builder.write(ctrlCharacteristic, datetimeBytes);
        } catch (IOException e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("Set alarm");
            boolean anyAlarmEnabled = false;
            for (Alarm alarm : alarms) {
                anyAlarmEnabled |= alarm.isEnabled();
                Calendar calendar = alarm.getAlarmCal();

                int maxAlarms = 3;
                if (alarm.getIndex() >= maxAlarms) {
                    if (alarm.isEnabled()) {
                        GB.toast(getContext(), "Only 3 alarms are supported.", Toast.LENGTH_LONG, GB.WARN);
                    }
                    return;
                }

                int daysMask = 0;
                if (alarm.isEnabled()) {
                    daysMask = alarm.getRepetitionMask();
                    // Mask for this device starts from sunday and not from monday.
                    daysMask = (daysMask / 64) + (daysMask >> 1);
                }
                byte[] alarmMessage = new byte[]{
                        No1F1Constants.CMD_ALARM,
                        (byte) daysMask,
                        (byte) calendar.get(Calendar.HOUR_OF_DAY),
                        (byte) calendar.get(Calendar.MINUTE),
                        (byte) (alarm.isEnabled() ? 2 : 0), // vibration duration
                        (byte) (alarm.isEnabled() ? 10 : 0), // vibration count
                        (byte) (alarm.isEnabled() ? 2 : 0), // unknown
                        (byte) 0,
                        (byte) (alarm.getIndex() + 1)
                };
                builder.write(ctrlCharacteristic, alarmMessage);
            }
            builder.queue(getQueue());
            if (anyAlarmEnabled) {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);
            } else {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException ex) {
            GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            showNotification(No1F1Constants.NOTIFICATION_CALL, callSpec.name, callSpec.number);
            setVibration(3, 5);
        } else {
            stopNotification();
            setVibration(0, 0);
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
        sendFetchCommand(No1F1Constants.CMD_FETCH_STEPS);
    }

    @Override
    public void onReboot() {
    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("heartRateTest");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_REALTIME_HEARTRATE,
                    (byte) 0x11
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error starting heart rate measurement: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        if (start)
            setVibration(3, 10);
        else
            setVibration(0, 0);
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
        TransactionBuilder builder;
        try {
            builder = performInitialized("Sending configuration for option: " + config);
            switch (config) {
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                    setDisplaySettings(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    /**
     * Set display settings (time format and measurement system)
     *
     * @param transaction
     * @return
     */
    private No1F1Support setDisplaySettings(TransactionBuilder transaction) {
        byte[] displayBytes = new byte[]{
                No1F1Constants.CMD_DISPLAY_SETTINGS,
                0x00, // 1 - display distance in kilometers, 2 - in miles
                0x00 // 1 - display 24-hour clock, 2 - for 12-hour with AM/PM
        };
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));
        if (units.equals(getContext().getString(R.string.p_unit_metric))) {
            displayBytes[1] = 1;
        } else {
            displayBytes[1] = 2;
        }
        if (DateFormat.is24HourFormat(getContext())) {
            displayBytes[2] = 1;
        } else {
            displayBytes[2] = 2;
        }
        transaction.write(ctrlCharacteristic, displayBytes);
        return this;
    }

    private void sendSettings(TransactionBuilder builder) {
        // TODO Create custom settings page for changing hardcoded values

        // set user data
        ActivityUser activityUser = new ActivityUser();
        byte[] userBytes = new byte[]{
                No1F1Constants.CMD_USER_DATA,
                0,
                (byte) Math.round(activityUser.getHeightCm() * 0.43), // step length in cm
                0,
                (byte) activityUser.getWeightKg(),
                5, // screen on time
                0,
                0,
                (byte) (activityUser.getStepsGoal() / 256),
                (byte) (activityUser.getStepsGoal() % 256),
                1, // unknown
                (byte) 0xff, // unknown
                0,
                (byte) activityUser.getAge(),
                0
        };
        if (activityUser.getGender() == ActivityUser.GENDER_FEMALE)
            userBytes[14] = 2; // female
        else
            userBytes[14] = 1; // male

        builder.write(ctrlCharacteristic, userBytes);

        // more settings
        builder.write(ctrlCharacteristic, new byte[]{
                No1F1Constants.CMD_DEVICE_SETTINGS,
                0x00, // 1 - turns on inactivity alarm
                0x3c,
                0x02,
                0x03,
                0x01,
                0x00
        });

        setDisplaySettings(builder);

        // heart rate measurement mode
        builder.write(ctrlCharacteristic, new byte[]{
                No1F1Constants.CMD_HEARTRATE_SETTINGS,
                0x02, // 1 - static (measure for 15 seconds), 2 - realtime
        });

        // periodic heart rate measurement
        builder.write(ctrlCharacteristic, new byte[]{
                No1F1Constants.CMD_HEARTRATE_SETTINGS,
                0x01,
                0x02 // measure heart rate every 2 hours (0 to turn off)
        });
    }

    private void setVibration(int duration, int count) {
        try {
            TransactionBuilder builder = performInitialized("vibrate");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_ALARM,
                    0,
                    0,
                    0,
                    (byte) duration,
                    (byte) count,
                    2,
                    1
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn("Unable to set vibration", e);
        }
    }

    private void showIcon(int iconId) {
        try {
            TransactionBuilder builder = performInitialized("showIcon");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_ICON,
                    (byte) iconId
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing icon: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void showNotification(int type, String header, String body) {
        try {
            // TODO Add transliteration.
            TransactionBuilder builder = performInitialized("showNotification");
            int length;
            byte[] bytes;
            byte[] msg;

            // send header
            bytes = header.toString().getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = No1F1Constants.CMD_NOTIFICATION;
            msg[1] = No1F1Constants.NOTIFICATION_HEADER;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            // send body
            bytes = header.toString().getBytes("EUC-JP");
            length = min(bytes.length, 18);
            msg = new byte[length + 2];
            msg[0] = No1F1Constants.CMD_NOTIFICATION;
            msg[1] = (byte) type;
            System.arraycopy(bytes, 0, msg, 2, length);
            builder.write(ctrlCharacteristic, msg);

            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing notificaton: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void stopNotification() {
        try {
            TransactionBuilder builder = performInitialized("clearNotification");
            byte[] msg = new byte[]{
                    No1F1Constants.CMD_NOTIFICATION,
                    No1F1Constants.NOTIFICATION_STOP
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn("Unable to stop notification", e);
        }
    }

    private void sendFetchCommand(byte type) {
        samples.clear();
        crc = 0;
        firstTimestamp = 0;
        try {
            TransactionBuilder builder = performInitialized("fetchActivityData");
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
            byte[] msg = new byte[]{
                    type,
                    (byte) 0xfa
            };
            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void handleActivityData(byte[] data) {
        if (data[1] == (byte) 0xfd) {
            LOG.info("CRC received: " + (data[2] & 0xff) + ", calculated: " + (crc & 0xff));
            if (data[2] != crc) {
                GB.toast(getContext(), "Incorrect CRC. Try fetching data again.", Toast.LENGTH_LONG, GB.ERROR);
                GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
                if (getDevice().isBusy()) {
                    getDevice().unsetBusyTask();
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
            } else if (samples.size() > 0) {
                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                    Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
                    No1F1SampleProvider provider = new No1F1SampleProvider(getDevice(), dbHandler.getDaoSession());
                    for (int i = 0; i < samples.size(); i++) {
                        samples.get(i).setDeviceId(deviceId);
                        samples.get(i).setUserId(userId);
                        if (data[0] == No1F1Constants.CMD_FETCH_STEPS) {
                            samples.get(i).setRawKind(ActivityKind.TYPE_ACTIVITY);
                            samples.get(i).setRawIntensity(samples.get(i).getSteps());
                        } else if (data[0] == No1F1Constants.CMD_FETCH_SLEEP) {
                            if (samples.get(i).getRawIntensity() < 7)
                                samples.get(i).setRawKind(ActivityKind.TYPE_DEEP_SLEEP);
                            else
                                samples.get(i).setRawKind(ActivityKind.TYPE_LIGHT_SLEEP);
                        }
                        provider.addGBActivitySample(samples.get(i));
                    }
                    LOG.info("Activity data saved");
                    if (data[0] == No1F1Constants.CMD_FETCH_STEPS) {
                        sendFetchCommand(No1F1Constants.CMD_FETCH_SLEEP);
                    } else if (data[0] == No1F1Constants.CMD_FETCH_SLEEP) {
                        sendFetchCommand(No1F1Constants.CMD_FETCH_HEARTRATE);
                    } else {
                        GB.updateTransferNotification(null,"", false, 100, getContext());
                        if (getDevice().isBusy()) {
                            getDevice().unsetBusyTask();
                            getDevice().sendDeviceUpdateIntent(getContext());
                        }
                    }
                } catch (Exception ex) {
                    GB.toast(getContext(), "Error saving activity data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                    GB.updateTransferNotification(null,"Data transfer failed", false, 0, getContext());
                }
            }
        } else {
            No1F1ActivitySample sample = new No1F1ActivitySample();

            Calendar timestamp = GregorianCalendar.getInstance();
            timestamp.set(Calendar.YEAR, data[1] * 256 + (data[2] & 0xff));
            timestamp.set(Calendar.MONTH, (data[3] - 1) & 0xff);
            timestamp.set(Calendar.DAY_OF_MONTH, data[4] & 0xff);
            timestamp.set(Calendar.HOUR_OF_DAY, data[5] & 0xff);
            timestamp.set(Calendar.SECOND, 0);

            int startProgress = 0;
            if (data[0] == No1F1Constants.CMD_FETCH_STEPS) {
                timestamp.set(Calendar.MINUTE, 0);
                sample.setSteps(data[6] * 256 + (data[7] & 0xff));
                crc ^= (data[6] ^ data[7]);
            } else if (data[0] == No1F1Constants.CMD_FETCH_SLEEP) {
                timestamp.set(Calendar.MINUTE, data[6] & 0xff);
                sample.setRawIntensity(data[7] * 256 + (data[8] & 0xff));
                crc ^= (data[7] ^ data[8]);
                startProgress = 33;
            } else if (data[0] == No1F1Constants.CMD_FETCH_HEARTRATE) {
                timestamp.set(Calendar.MINUTE, data[6] & 0xff);
                sample.setHeartRate(data[7] & 0xff);
                crc ^= (data[6] ^ data[7]);
                startProgress = 66;
            }

            sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000L));
            samples.add(sample);

            if (firstTimestamp == 0)
                firstTimestamp = sample.getTimestamp();
            int progress = startProgress + 33 * (sample.getTimestamp() - firstTimestamp) /
                    ((int) (Calendar.getInstance().getTimeInMillis() / 1000L) - firstTimestamp);
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, progress, getContext());
        }
    }

    private void handleRealtimeHeartRateData(byte[] data) {
        if (data.length == 2) {
            if (data[1] == (byte) 0x11)
                LOG.info("Heart rate measurement started.");
            else
                LOG.info("Heart rate measurement stopped.");
            return;
        }
        // Check if data is valid. Otherwise ignore sample.
        if (data[2] == 0) {
            No1F1ActivitySample sample = new No1F1ActivitySample();
            sample.setTimestamp((int) (GregorianCalendar.getInstance().getTimeInMillis() / 1000L));
            sample.setHeartRate(data[3] & 0xff);
            LOG.info("Current heart rate is: " + sample.getHeartRate() + " BPM");
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
                No1F1SampleProvider provider = new No1F1SampleProvider(getDevice(), dbHandler.getDaoSession());
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
                provider.addGBActivitySample(sample);
            } catch (Exception ex) {
                LOG.warn("Error saving current heart rate: " + ex.getLocalizedMessage());
            }
        }
    }
}
