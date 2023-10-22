/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.location.Location;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.AbstractXiaomiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiScheduleService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiSystemService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWatchfaceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWeatherService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class XiaomiSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSupport.class);

    protected XiaomiCharacteristic characteristicCommandRead;
    protected XiaomiCharacteristic characteristicCommandWrite;
    protected XiaomiCharacteristic characteristicActivityData;
    protected XiaomiCharacteristic characteristicDataUpload;

    protected final XiaomiAuthService authService = new XiaomiAuthService(this);
    protected final XiaomiMusicService musicService = new XiaomiMusicService(this);
    protected final XiaomiHealthService healthService = new XiaomiHealthService(this);
    protected final XiaomiNotificationService notificationService = new XiaomiNotificationService(this);
    protected final XiaomiScheduleService scheduleService = new XiaomiScheduleService(this);
    protected final XiaomiWeatherService weatherService = new XiaomiWeatherService(this);
    protected final XiaomiSystemService systemService = new XiaomiSystemService(this);
    protected final XiaomiCalendarService calendarService = new XiaomiCalendarService(this);
    protected final XiaomiWatchfaceService watchfaceService = new XiaomiWatchfaceService(this);

    private String mFirmwareVersion = null;

    private final Map<Integer, AbstractXiaomiService> mServiceMap = new LinkedHashMap<Integer, AbstractXiaomiService>() {{
        put(XiaomiAuthService.COMMAND_TYPE, authService);
        put(XiaomiMusicService.COMMAND_TYPE, musicService);
        put(XiaomiHealthService.COMMAND_TYPE, healthService);
        put(XiaomiNotificationService.COMMAND_TYPE, notificationService);
        put(XiaomiScheduleService.COMMAND_TYPE, scheduleService);
        put(XiaomiWeatherService.COMMAND_TYPE, weatherService);
        put(XiaomiSystemService.COMMAND_TYPE, systemService);
        put(XiaomiCalendarService.COMMAND_TYPE, calendarService);
        put(XiaomiWatchfaceService.COMMAND_TYPE, watchfaceService);
    }};

    public XiaomiSupport() {
        super(LOG);
    }

    protected abstract boolean isEncrypted();
    protected abstract UUID getCharacteristicCommandRead();
    protected abstract UUID getCharacteristicCommandWrite();
    protected abstract UUID getCharacteristicActivityData();
    protected abstract UUID getCharacteristicDataUpload();
    protected abstract void startAuthentication(final TransactionBuilder builder);

    @Override
    protected final TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic btCharacteristicCommandRead = getCharacteristic(getCharacteristicCommandRead());
        final BluetoothGattCharacteristic btCharacteristicCommandWrite = getCharacteristic(getCharacteristicCommandWrite());
        final BluetoothGattCharacteristic btCharacteristicActivityData = getCharacteristic(getCharacteristicActivityData());
        final BluetoothGattCharacteristic btCharacteristicDataUpload = getCharacteristic(getCharacteristicDataUpload());

        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (getDevice().getFirmwareVersion() == null && mFirmwareVersion != null) {
            getDevice().setFirmwareVersion(mFirmwareVersion);
        }

        if (btCharacteristicCommandRead == null || btCharacteristicCommandWrite == null) {
            LOG.warn("Characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        this.characteristicCommandRead = new XiaomiCharacteristic(this, btCharacteristicCommandRead, authService);
        this.characteristicCommandRead.setEncrypted(isEncrypted());
        this.characteristicCommandRead.setHandler(this::handleCommandBytes);
        this.characteristicCommandWrite = new XiaomiCharacteristic(this, btCharacteristicCommandWrite, authService);
        this.characteristicCommandWrite.setEncrypted(isEncrypted());
        this.characteristicActivityData = new XiaomiCharacteristic(this, btCharacteristicActivityData, authService);
        this.characteristicActivityData.setHandler(healthService.getActivityFetcher()::addChunk);
        this.characteristicActivityData.setEncrypted(isEncrypted());
        this.characteristicDataUpload = new XiaomiCharacteristic(this, btCharacteristicDataUpload, authService);
        this.characteristicDataUpload.setEncrypted(isEncrypted());

        builder.requestMtu(247);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        builder.notify(btCharacteristicCommandWrite, true);
        builder.notify(btCharacteristicCommandRead, true);
        builder.notify(btCharacteristicActivityData, true);
        builder.notify(btCharacteristicDataUpload, true);

        startAuthentication(builder);

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return false;
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (mFirmwareVersion == null && gbDevice.getFirmwareVersion() != null) {
            mFirmwareVersion = gbDevice.getFirmwareVersion();
        }

        super.setContext(gbDevice, btAdapter, context);
        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.setContext(context);
        }
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();
        final byte[] value = characteristic.getValue();

        if (characteristicCommandRead.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicCommandRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicCommandWrite.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicCommandWrite.onCharacteristicChanged(value);
            return true;
        } else if (characteristicActivityData.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicActivityData.onCharacteristicChanged(value);
            return true;
        } else if (characteristicDataUpload.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicDataUpload.onCharacteristicChanged(value);
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristicUUID, GB.hexdump(value));
        return false;
    }

    public void handleCommandBytes(final byte[] plainValue) {
        LOG.debug("Got command: {}", GB.hexdump(plainValue));

        final XiaomiProto.Command cmd;
        try {
            cmd = XiaomiProto.Command.parseFrom(plainValue);
        } catch (final Exception e) {
            LOG.error("Failed to parse bytes as protobuf command payload", e);
            return;
        }

        final AbstractXiaomiService service = mServiceMap.get(cmd.getType());
        if (service != null) {
            service.handleCommand(cmd);
            return;
        }

        LOG.warn("Unexpected watch command type {}", cmd.getType());
    }

    @Override
    public void onSendConfiguration(final String config) {
        final Prefs prefs = getDevicePrefs();

        // Check if any of the services handles this config
        for (final AbstractXiaomiService service : mServiceMap.values()) {
            if (service.onSendConfiguration(config, prefs)) {
                return;
            }
        }

        LOG.warn("Unhandled config changed: {}", config);
    }

    @Override
    public void onSetTime() {
        systemService.setCurrentTime();

        if (getCoordinator().supportsCalendarEvents()) {
            // TODO this should not be done here
            calendarService.syncCalendar();
        }
    }

    @Override
    public void onTestNewFunction() {
        sendCommand("test new function", 2, 29);
    }

    @Override
    public void onFindPhone(final boolean start) {
        systemService.onFindPhone(start);
    }

    @Override
    public void onFindDevice(final boolean start) {
        systemService.onFindWatch(start);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        musicService.onSetPhoneVolume(volume);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        healthService.onSetGpsLocation(location);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        scheduleService.onSetReminders(reminders);
    }

    @Override
    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        scheduleService.onSetWorldClocks(clocks);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        notificationService.onNotification(notificationSpec);
    }

    @Override
    public void onDeleteNotification(final int id) {
        notificationService.onDeleteNotification(id);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        scheduleService.onSetAlarms(alarms);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        notificationService.onSetCallState(callSpec);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        notificationService.onSetCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        musicService.onSetMusicState(stateSpec);
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        musicService.onSetMusicInfo(musicSpec);
    }

    @Override
    public void onInstallApp(final Uri uri) {
        watchfaceService.installWatchface(uri);
    }

    @Override
    public void onAppInfoReq() {
        watchfaceService.requestWatchfaceList();
    }

    @Override
    public void onAppStart(final UUID uuid, boolean start) {
        if (start) {
            watchfaceService.setWatchface(uuid);
        }
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        watchfaceService.deleteWatchface(uuid);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        healthService.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onHeartRateTest() {
        healthService.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        healthService.enableRealtimeStats(enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        healthService.enableRealtimeStats(enable);
    }

    @Override
    public void onScreenshotReq() {
        // TODO
        super.onScreenshotReq();
    }

    @Override
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        // TODO
        super.onEnableHeartRateSleepSupport(enable);
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        // TODO
        super.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        calendarService.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, long id) {
        calendarService.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onSendWeather(final WeatherSpec weatherSpec) {
        weatherService.onSendWeather(weatherSpec);
    }

    public XiaomiCoordinator getCoordinator() {
        return (XiaomiCoordinator) gbDevice.getDeviceCoordinator();
    }

    protected void phase2Initialize() {
        LOG.info("phase2Initialize");

        characteristicCommandRead.reset();
        characteristicCommandWrite.reset();
        characteristicActivityData.reset();
        characteristicDataUpload.reset();

        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            systemService.setCurrentTime();
        }

        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.initialize();
        }
    }

    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("characteristicCommandWrite is null!");
            return;
        }

        this.characteristicCommandWrite.write(taskName, command.toByteArray());
    }

    /**
     * Realistically, this function should only be used during auth, as we must schedule the command after
     * notifications were enabled on the characteristics, and for that we need the builder to guarantee the
     * order.
     */
    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("characteristicCommandWrite is null!");
            return;
        }

        this.characteristicCommandWrite.write(builder, command.toByteArray());
    }

    public void sendCommand(final String taskName, final int type, final int subtype) {
        sendCommand(
                taskName,
                XiaomiProto.Command.newBuilder()
                        .setType(type)
                        .setSubtype(subtype)
                        .build()
        );
    }
}
