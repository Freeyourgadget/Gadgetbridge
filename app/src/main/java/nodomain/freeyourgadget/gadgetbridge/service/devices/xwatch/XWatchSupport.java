/*  Copyright (C) 2018 Andreas Shimokawa, ladbsoft

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xwatch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchService;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.XWatchActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XWatchSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XWatchSupport.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    TransactionBuilder builder = null;
    private DeviceInfo mDeviceInfo;
    private byte dayToFetch; //0 = Today; 1 = Yesterday ...
    private byte maxDayToFetch;
    long lastButtonTimestamp;

    public XWatchSupport() {
        super(LOG);

        addSupportedService(XWatchService.UUID_SERVICE);
        addSupportedService(XWatchService.UUID_WRITE);
        addSupportedService(XWatchService.UUID_NOTIFY);
    }

    public static byte[] crcChecksum(byte[] data) {
        byte[] return_data = new byte[(data.length + 1)];
        byte checksum = 0;

        for (int i = 0; i < data.length; i++) {
            return_data[i] = data[i];
            checksum += data[i];
        }
        return_data[return_data.length - 1] = checksum;

        return return_data;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZING, getContext()));

        enableNotifications(builder)
                .setDateTime(builder)
                .setInitialized(builder);

        return builder;
    }

    /**
     * Last action of initialization sequence. Sets the device to initialized.
     * It is only invoked if all other actions were successfully run, so the device
     * must be initialized, then.
     *
     * @param builder
     */
    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZED, getContext()));
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public boolean connectFirstTime() {
        for (int i = 0; i < 5; i++) {
            if (connect()) {
                return true;
            }
        }
        return false;
    }

    private XWatchSupport setDateTime(TransactionBuilder builder) {
        byte[] data;

        LOG.debug("Sending current date to the XWatch");
        BluetoothGattCharacteristic deviceData = getCharacteristic(XWatchService.UUID_WRITE);

        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String y = time.substring(2, 4);
        String M = time.substring(4, 6);
        String d = time.substring(6, 8);
        String H = time.substring(8, 10);
        String m = time.substring(10, 12);
        String s = time.substring(12, 14);
        System.out.println(y + ":" + M + ":" + d + ":" + H + ":" + m + ":" + time.substring(12, 14));

        data = new byte[]{(byte) 1,
                (byte) Integer.parseInt(y, 16),
                (byte) Integer.parseInt(M, 16),
                (byte) Integer.parseInt(d, 16),
                (byte) Integer.parseInt(H, 16),
                (byte) Integer.parseInt(m, 16),
                (byte) Integer.parseInt(s, 16),
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0};

        data = crcChecksum(data);

        builder.write(deviceData, data);

        return this;
    }

    private XWatchSupport enableNotifications(TransactionBuilder builder) {
        LOG.debug("Enabling action button");
        BluetoothGattCharacteristic deviceInfo = getCharacteristic(XWatchService.UUID_NOTIFY);
        builder.notify(deviceInfo, true);
        return this;
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        //Not supported
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        // not supported
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        // not supported
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        //TODO: Implement
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        //TODO: Implement
    }

    @Override
    public void onDeleteNotification(int id) {
        //TODO: Implement
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("Set date and time");
            setDateTime(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time and date on XWatch device", ex);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        //TODO: Implement (if necessary)
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        // not supported
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        // not supported
    }

    @Override
    public void onReboot() {
        //Not supported
    }

    @Override
    public void onHeartRateTest() {
        //Not supported
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        //Not supported
    }

    @Override
    public void onFindDevice(boolean start) {
        //TODO: Implement
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        //TODO: Implement
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            if(builder == null) {
                builder = performInitialized("fetchActivityData");
            }
            requestSummarizedData(builder);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        //Not supported
    }

    @Override
    public void onInstallApp(Uri uri) {
        //Not supported
    }

    @Override
    public void onAppInfoReq() {
        // not supported
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        // not supported
    }

    @Override
    public void onAppDelete(UUID uuid) {
        // not supported
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        // not supported
    }

    @Override
    public void onScreenshotReq() {
        // not supported
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (XWatchService.UUID_NOTIFY.equals(characteristicUUID)) {
            byte[] data = characteristic.getValue();
            if (data[0] == XWatchService.COMMAND_ACTIVITY_TOTALS) {
                handleSummarizedData(characteristic.getValue());
            } else if (data[0] == XWatchService.COMMAND_ACTIVITY_DATA) {
                handleDetailedData(characteristic.getValue());
            } else if (data[0] == XWatchService.COMMAND_ACTION_BUTTON) {
                handleButtonPressed(characteristic.getValue());
            } else if (data[0] == XWatchService.COMMAND_CONNECTED) {
                handleDeviceInfo(data, BluetoothGatt.GATT_SUCCESS);
            } else {
                LOG.info("Handled characteristic with unknown data: " + characteristicUUID);
                logMessageContent(characteristic.getValue());
            }
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            logMessageContent(characteristic.getValue());
        }
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        return super.onCharacteristicChanged(gatt, characteristic);
        //TODO: Implement (if necessary)
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
        return super.onCharacteristicWrite(gatt, characteristic, status);
        //TODO: Implement (if necessary)
    }

    public XWatchActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        XWatchActivitySample sample = new XWatchActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }

    private void handleDeviceInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mDeviceInfo = new DeviceInfo(value);
            LOG.warn("Device info: " + mDeviceInfo);
            versionCmd.hwVersion = "1.0";
            versionCmd.fwVersion = "1.0";
            handleGBDeviceEvent(versionCmd);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        // nothing yet
    }

    @Override
    public void onTestNewFunction() {
        //Not supported
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        //Not supported
    }

    private void handleSummarizedData(byte[] value) {
        int daysIntTotal;
        int daysIntPart;

        if (value.length != 16) {
            LOG.warn("GOT UNEXPECTED SENSOR DATA WITH LENGTH: " + value.length);
            for (byte b : value) {
                LOG.warn("DATA: " + String.format("0x%4x", b));
            }
        } else {
            daysIntPart = (value[1] & 255) << 24;
            daysIntTotal = daysIntPart;
            daysIntPart = (value[2] & 255) << 16;
            daysIntTotal += daysIntPart;
            daysIntPart = (value[3] & 255) << 8;
            daysIntTotal += daysIntPart;
            daysIntPart = (value[4] & 255);
            daysIntTotal += daysIntPart;

            dayToFetch = 0;
            maxDayToFetch = (byte) Integer.bitCount(daysIntTotal);

            try {
                requestDetailedData(builder);
                performConnected(builder.getTransaction());
            } catch (IOException e) {
                GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    private void handleDetailedData(byte[] value) {
        int category, intensity, steps = 0;

        if (value.length != 16) {
            LOG.warn("GOT UNEXPECTED SENSOR DATA WITH LENGTH: " + value.length);
            for (byte b : value) {
                LOG.warn("DATA: " + String.format("0x%4x", b));
            }
        } else {
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                XWatchSampleProvider provider = new XWatchSampleProvider(getDevice(), dbHandler.getDaoSession());
                User user = DBHelper.getUser(dbHandler.getDaoSession());
                Device device = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession());
                int timestampInSeconds = 0;

                timestampInSeconds = getTimestampFromData(
                        value[2],
                        value[3],
                        value[4],
                        value[5]
                );

                category = ActivityKind.TYPE_ACTIVITY;
                intensity = (value[7] & 255) + ((value[8] & 255) << 8);
                steps = (value[9] & 255) + ((value[10] & 255) << 8);

                XWatchActivitySample sample = createActivitySample(device, user, timestampInSeconds, provider);
                sample.setRawIntensity(intensity);
                sample.setSteps(steps);
                sample.setRawKind(category);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("sample: " + sample);
                }

                provider.addGBActivitySample(sample);

                if (value[5] == 95) {
                    dayToFetch++;
                    if(dayToFetch <= maxDayToFetch) {
                        try {
                            builder = performInitialized("fetchActivityData");
                            requestDetailedData(builder);
                            performConnected(builder.getTransaction());
                        } catch (IOException e) {
                            GB.toast(getContext(), "Error fetching activity data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                        }
                    }
                }
            } catch (Exception ex) {
                GB.toast(getContext(), ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }
    }

    private void handleButtonPressed(byte[] value) {
        long currentTimestamp = System.currentTimeMillis();

        AudioManager audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        if(audioManager.isWiredHeadsetOn()) {
            if (currentTimestamp - lastButtonTimestamp < 1000) {
                if (audioManager.isMusicActive()) {
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
                } else {
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
                    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
                }
            } else {
                audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            }
        }

        lastButtonTimestamp = currentTimestamp;
    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {
        //Not supported
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        //Not supported
    }

    private void requestSummarizedData(TransactionBuilder builder) {
        byte[] fetch = new byte[]{(byte) XWatchService.COMMAND_ACTIVITY_TOTALS,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0};

        fetch = XWatchSupport.crcChecksum(fetch);
        builder.write(getCharacteristic(XWatchService.UUID_WRITE), fetch);
    }

    private void requestDetailedData(TransactionBuilder builder) {
        byte[] fetch = new byte[]{(byte) XWatchService.COMMAND_ACTIVITY_DATA,
                (byte) dayToFetch,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0};

        fetch = XWatchSupport.crcChecksum(fetch);
        builder.write(getCharacteristic(XWatchService.UUID_WRITE), fetch);
    }

    private int getTimestampFromData(byte year, byte month, byte day, byte hoursminutes) {
        int timestamp = 0;
        int yearInt, monthInt, dayInt, hoursMinutesInt = 0;
        int hours, minutes = 0;

        yearInt = Integer.valueOf(String.format("%02x", year, 16));
        monthInt = Integer.valueOf(String.format("%02x", month, 16));
        dayInt = Integer.valueOf(String.format("%02x", day, 16));
        hoursMinutesInt = Integer.valueOf(String.format("%02x", hoursminutes), 16);

        minutes = hoursMinutesInt % 4;
        hours = (hoursMinutesInt - minutes) / 4;
        minutes = minutes * 15;

        GregorianCalendar cal = new GregorianCalendar(
                2000 + yearInt,
                monthInt - 1,
                dayInt,
                hours,
                minutes
        );

        timestamp = (int)(cal.getTimeInMillis() / 1000);

        return timestamp;
    }
}
