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

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiConstants.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.AbstractXiaomiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiScheduleService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiSystemService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWeatherService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSupport.class);

    private final XiaomiCipher cipher = new XiaomiCipher(this);

    private final XiaomiMusicService musicService = new XiaomiMusicService(this);
    private final XiaomiHealthService healthService = new XiaomiHealthService(this);
    private final XiaomiNotificationService notificationService = new XiaomiNotificationService(this);
    private final XiaomiScheduleService scheduleService = new XiaomiScheduleService(this);
    private final XiaomiWeatherService weatherService = new XiaomiWeatherService(this);
    private final XiaomiSystemService systemService = new XiaomiSystemService(this);

    private final Map<Integer, AbstractXiaomiService> mServiceMap = new LinkedHashMap<Integer, AbstractXiaomiService>() {{
        put(XiaomiMusicService.COMMAND_TYPE, musicService);
        put(XiaomiHealthService.COMMAND_TYPE, healthService);
        put(XiaomiNotificationService.COMMAND_TYPE, notificationService);
        put(XiaomiScheduleService.COMMAND_TYPE, scheduleService);
        put(XiaomiWeatherService.COMMAND_TYPE, weatherService);
        put(XiaomiSystemService.COMMAND_TYPE, systemService);
    }};

    public XiaomiSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_HUMAN_INTERFACE_DEVICE);
        addSupportedService(UUID_SERVICE_XIAOMI_FE95);
        addSupportedService(UUID_SERVICE_XIAOMI_FDAB);
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
        super.setContext(gbDevice, btAdapter, context);
        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.setContext(context);
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic characteristicCommandWrite = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE);
        final BluetoothGattCharacteristic characteristicCommandRead = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ);

        if (characteristicCommandWrite == null || characteristicCommandRead == null) {
            LOG.warn("Command characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        // FIXME why is this needed?
        getDevice().setFirmwareVersion("...");
        //getDevice().setFirmwareVersion2("...");

        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ), true);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE), true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        cipher.startAuthentication(builder);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();
        final byte[] value = characteristic.getValue();

        if (UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE.equals(characteristicUUID)) {
            if (Arrays.equals(value, PAYLOAD_ACK)) {
                LOG.debug("Got command write ack");
            } else {
                LOG.warn("Unexpected notification from command write: {}", GB.hexdump(value));
            }

            return true;
        }

        if (UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ.equals(characteristicUUID)) {
            sendAck(characteristic);

            final int header = BLETypeConversions.toUint16(value, 0);
            final int type = BLETypeConversions.toUnsigned(value, 2);
            final int encryption = BLETypeConversions.toUnsigned(value, 3);

            if (header != 0) {
                LOG.warn("Non-zero header not supported");
                return true;
            }
            if (type != 2) {
                LOG.warn("Unsupported type {}", type);
                return true;
            }

            final byte[] plainValue;
            if (encryption == 1) {
                plainValue = cipher.decrypt(ArrayUtils.subarray(value, 4, value.length));
            } else {
                plainValue = ArrayUtils.subarray(value, 4, value.length);
            }

            LOG.debug("Got command: {}", GB.hexdump(plainValue));

            final XiaomiProto.Command cmd;
            try {
                cmd = XiaomiProto.Command.parseFrom(plainValue);
            } catch (final Exception e) {
                LOG.error("Failed to parse bytes as protobuf command payload", e);
                return true;
            }

            final AbstractXiaomiService service = mServiceMap.get(cmd.getType());
            if (service != null) {
                service.handleCommand(cmd);
                return true;
            }

            if (cmd.getType() == CMD_TYPE_AUTH) {
                cipher.handleAuthCommand(cmd);
                return true;
            }

            LOG.warn("Unexpected watch command type {}", cmd.getType());
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristicUUID, GB.hexdump(value));
        return false;
    }

    @Override
    public void onSendConfiguration(final String config) {
        // TODO
        // TODO user info
        // TODO 24h
        GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR);
    }

    @Override
    public void onSetTime() {
        final TransactionBuilder builder;
        try {
            builder = performInitialized("set time");
        } catch (final IOException e) {
            LOG.error("Failed to initialize transaction builder", e);
            return;
        }
        systemService.setCurrentTime(builder);
        builder.queue(getQueue());
    }

    @Override
    public void onTestNewFunction() {
        final TransactionBuilder builder = createTransactionBuilder("test new function");

        builder.queue(getQueue());
    }

    @Override
    public void onFindPhone(final boolean start) {
        // TODO possible to notify watch?
        super.onFindPhone(start);
    }

    @Override
    public void onFindDevice(final boolean start) {
        // TODO onFindDevice
        super.onFindDevice(start);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        musicService.onSetPhoneVolume(volume);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        // TODO onSetGpsLocation
        super.onSetGpsLocation(location);
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
        // TODO
        super.onInstallApp(uri);
    }

    @Override
    public void onAppInfoReq() {
        // TODO
        super.onAppInfoReq();
    }

    @Override
    public void onAppStart(final UUID uuid, boolean start) {
        // TODO
        super.onAppStart(uuid, start);
    }

    @Override
    public void onAppDownload(final UUID uuid) {
        // TODO
        super.onAppDownload(uuid);
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        // TODO
        super.onAppDelete(uuid);
    }

    @Override
    public void onAppConfiguration(final UUID appUuid, String config, Integer id) {
        // TODO
        super.onAppConfiguration(appUuid, config, id);
    }

    @Override
    public void onAppReorder(final UUID[] uuids) {
        // TODO
        super.onAppReorder(uuids);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        // TODO
        super.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onReset(final int flags) {
        // TODO
        super.onReset(flags);
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
        scheduleService.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, long id) {
        scheduleService.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onSendWeather(final WeatherSpec weatherSpec) {
        weatherService.onSendWeather(weatherSpec);
    }

    protected void phase2Initialize(final TransactionBuilder builder) {
        LOG.info("phase2Initialize");
        encryptedIndex = 1; // TODO not here

        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            systemService.setCurrentTime(builder);
        }

        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.initialize(builder);
        }
    }

    private void sendAck(final BluetoothGattCharacteristic characteristic) {
        final TransactionBuilder builder = createTransactionBuilder("send ack");
        builder.write(characteristic, PAYLOAD_ACK);
        builder.queue(getQueue());
    }

    private short encryptedIndex = 0;

    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendCommand(builder, command);
        builder.queue(getQueue());
    }

    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        final byte[] commandBytes = command.toByteArray();
        final byte[] encryptedCommandBytes = cipher.encrypt(commandBytes, encryptedIndex);
        final ByteBuffer buf = ByteBuffer.allocate(6 + encryptedCommandBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0);
        buf.put((byte) 2); // 2 for command
        buf.put((byte) 1); // 1 for encrypted
        buf.putShort(encryptedIndex++);
        buf.put(encryptedCommandBytes);
        LOG.debug("Sending command {} as {}", GB.hexdump(commandBytes), GB.hexdump(buf.array()));
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE), buf.array());
    }

    public void sendCommand(final TransactionBuilder builder, final int type, final int subtype) {
        sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(type)
                        .setSubtype(subtype)
                        .build()
        );
    }
}
