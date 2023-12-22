/*  Copyright (C) 2023 José Rebelo, Andreas Shimokawa

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
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.AbstractXiaomiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiDataUploadService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiPhonebookService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiScheduleService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiSystemService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWatchfaceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWeatherService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiSupport extends AbstractBTLEDeviceSupport {
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
    protected final XiaomiDataUploadService dataUploadService = new XiaomiDataUploadService(this);
    protected final XiaomiPhonebookService phonebookService = new XiaomiPhonebookService(this);

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
        put(XiaomiDataUploadService.COMMAND_TYPE, dataUploadService);
        put(XiaomiPhonebookService.COMMAND_TYPE, phonebookService);
    }};

    public XiaomiSupport() {
        super(LOG);
        for (final UUID uuid : XiaomiBleUuids.UUIDS.keySet()) {
            addSupportedService(uuid);
        }
    }

    @Override
    protected final TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        XiaomiBleUuids.XiaomiBleUuidSet uuidSet = null;
        BluetoothGattCharacteristic btCharacteristicCommandRead = null;
        BluetoothGattCharacteristic btCharacteristicCommandWrite = null;
        BluetoothGattCharacteristic btCharacteristicActivityData = null;
        BluetoothGattCharacteristic btCharacteristicDataUpload = null;

        // Attempt to find a known xiaomi service
        for (Map.Entry<UUID, XiaomiBleUuids.XiaomiBleUuidSet> xiaomiUuid : XiaomiBleUuids.UUIDS.entrySet()) {
            if (getSupportedServices().contains(xiaomiUuid.getKey())) {
                LOG.debug("Found Xiaomi service: {}", xiaomiUuid.getKey());
                uuidSet = xiaomiUuid.getValue();

                btCharacteristicCommandRead = getCharacteristic(uuidSet.getCharacteristicCommandRead());
                btCharacteristicCommandWrite = getCharacteristic(uuidSet.getCharacteristicCommandWrite());
                btCharacteristicActivityData = getCharacteristic(uuidSet.getCharacteristicActivityData());
                btCharacteristicDataUpload = getCharacteristic(uuidSet.getCharacteristicDataUpload());
                if (btCharacteristicCommandRead == null) {
                    LOG.warn("btCharacteristicCommandRead characteristicc is null");
                    continue;
                } else if (btCharacteristicCommandWrite == null) {
                    LOG.warn("btCharacteristicCommandWrite characteristicc is null");
                    continue;
                } else if (btCharacteristicActivityData == null) {
                    LOG.warn("btCharacteristicActivityData characteristicc is null");
                    continue;
                } else if (btCharacteristicDataUpload == null) {
                    LOG.warn("btCharacteristicDataUpload characteristicc is null");
                    continue;
                }

                break;
            }
        }

        if (uuidSet == null) {
            GB.toast(getContext(), "Failed to find known Xiaomi service", Toast.LENGTH_LONG, GB.ERROR);
            LOG.warn("Failed to find known Xiaomi service");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.NOT_CONNECTED, getContext()));
            return builder;
        }

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
        this.characteristicCommandRead.setEncrypted(uuidSet.isEncrypted());
        this.characteristicCommandRead.setHandler(this::handleCommandBytes);
        this.characteristicCommandWrite = new XiaomiCharacteristic(this, btCharacteristicCommandWrite, authService);
        this.characteristicCommandWrite.setEncrypted(uuidSet.isEncrypted());
        this.characteristicActivityData = new XiaomiCharacteristic(this, btCharacteristicActivityData, authService);
        this.characteristicActivityData.setHandler(healthService.getActivityFetcher()::addChunk);
        this.characteristicActivityData.setEncrypted(uuidSet.isEncrypted());
        this.characteristicDataUpload = new XiaomiCharacteristic(this, btCharacteristicDataUpload, authService);
        this.characteristicDataUpload.setEncrypted(uuidSet.isEncrypted());
        this.characteristicDataUpload.setIncrementNonce(false);
        this.dataUploadService.setDataUploadCharacteristic(this.characteristicDataUpload);

        builder.requestMtu(247);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        builder.notify(btCharacteristicCommandWrite, true);
        builder.notify(btCharacteristicCommandRead, true);
        builder.notify(btCharacteristicActivityData, true);
        builder.notify(btCharacteristicDataUpload, true);

        if (uuidSet.isEncrypted()) {
            authService.startEncryptedHandshake(builder);
        } else {
            authService.startClearTextHandshake(builder);
        }

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
        //sendCommand("test new function", 2, 29);
        parseAllActivityFilesFromStorage();
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
        final XiaomiFWHelper fwHelper = new XiaomiFWHelper(uri, getContext());

        if (!fwHelper.isValid()) {
            LOG.warn("Uri {} is not valid", uri);
            return;
        }

        if (fwHelper.isFirmware()) {
            systemService.installFirmware(fwHelper);
        } else if (fwHelper.isWatchface()) {
            watchfaceService.installWatchface(fwHelper);
        } else {
            LOG.warn("Unknown fwhelper for {}", uri);
        }
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
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        healthService.setHeartRateConfig();
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        healthService.setHeartRateConfig();
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

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        phonebookService.setContacts((List<Contact>) contacts);
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

    public XiaomiDataUploadService getDataUploader() {
        return this.dataUploadService;
    }

    @Override
    public String customStringFilter(final String inputString) {
        return StringUtils.replaceEach(inputString, EMOJI_SOURCE, EMOJI_TARGET);
    }

    private void parseAllActivityFilesFromStorage() {
        // This function as-is should only be used for debug purposes
        if (!BuildConfig.DEBUG) {
            LOG.error("This should never be used in release builds");
            return;
        }

        LOG.info("Parsing all activity files from storage");

        try {
            final File externalFilesDir = FileUtils.getExternalFilesDir();
            final File targetDir = new File(externalFilesDir, "rawFetchOperations");

            if (!targetDir.exists()) {
                LOG.warn("rawFetchOperations not found");
                return;
            }

            final File[] activityFiles = targetDir.listFiles((dir, name) -> name.startsWith("xiaomi_"));

            if (activityFiles == null) {
                LOG.warn("activityFiles is null");
                return;
            }

            for (final File activityFile : activityFiles) {
                LOG.debug("Parsing {}", activityFile);

                // The logic below just replicates XiaomiActivityFileFetcher

                final byte[] data;
                try (InputStream in = new FileInputStream(activityFile)) {
                    data = FileUtils.readAll(in, 999999);
                } catch (final IOException ioe) {
                    LOG.error("Failed to read " + activityFile, ioe);
                    continue;
                }

                final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
                final byte[] activityData = Arrays.copyOfRange(data, 8, data.length - 4);
                final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

                final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
                if (activityParser == null) {
                    LOG.warn("Failed to find parser for {}", fileId);
                    continue;
                }

                try {
                    if (activityParser.parse(this, fileId, activityData)) {
                        LOG.info("Successfully parsed {}", fileId);
                    } else {
                        LOG.warn("Failed to parse {}", fileId);
                    }
                } catch (final Exception ex) {
                    LOG.error("Exception while parsing " + fileId, ex);
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
        }
    }

    private static final String[] EMOJI_SOURCE = new String[]{
            "\uD83D\uDE0D", // 😍
            "\uD83D\uDE18", // 😘
            "\uD83D\uDE02", // 😂
            "\uD83D\uDE0A", // 😊
            "\uD83D\uDE0E", // 😎
            "\uD83D\uDE09", // 😉
            "\uD83D\uDC8B", // 💋
            "\uD83D\uDC4D", // 👍
            "\uD83E\uDD23", // 🤣
            "\uD83D\uDC95", // 💕
            "\uD83D\uDE00", // 😀
            "\uD83D\uDE04", // 😄
            "\uD83D\uDE2D", // 😭
            "\uD83E\uDD7A", // 🥺
            "\uD83D\uDE4F", // 🙏
            "\uD83E\uDD70", // 🥰
            "\uD83E\uDD14", // 🤔
            "\uD83D\uDD25", // 🔥
            "\uD83D\uDE29", // 😩
            "\uD83D\uDE14", // 😔
            "\uD83D\uDE01", // 😁
            "\uD83D\uDC4C", // 👌
            "\uD83D\uDE0F", // 😏
            "\uD83D\uDE05", // 😅
            "\uD83E\uDD0D", // 🤍
            "\uD83D\uDC94", // 💔
            "\uD83D\uDE0C", // 😌
            "\uD83D\uDE22", // 😢
            "\uD83D\uDC99", // 💙
            "\uD83D\uDC9C", // 💜
            "\uD83C\uDFB6", // 🎶
            "\uD83D\uDE33", // 😳
            "\uD83D\uDC96", // 💖
            "\uD83D\uDE4C", // 🙌
            "\uD83D\uDCAF", // 💯
            "\uD83D\uDE48", // 🙈
            "\uD83D\uDE0B", // 😋
            "\uD83D\uDE11", // 😑
            "\uD83D\uDE34", // 😴
            "\uD83D\uDE2A", // 😪
            "\uD83D\uDE1C", // 😜
            "\uD83D\uDE1B", // 😛
            "\uD83D\uDE1D", // 😝
            "\uD83D\uDE1E", // 😞
            "\uD83D\uDE15", // 😕
            "\uD83D\uDC97", // 💗
            "\uD83D\uDC4F", // 👏
            "\uD83D\uDE10", // 😐
            "\uD83D\uDC49", // 👉
            "\uD83D\uDC9B", // 💛
            "\uD83D\uDC9E", // 💞
            "\uD83D\uDCAA", // 💪
            "\uD83C\uDF39", // 🌹
            "\uD83D\uDC80", // 💀
            "\uD83D\uDE31", // 😱
            "\uD83D\uDC98", // 💘
            "\uD83E\uDD1F", // 🤟
            "\uD83D\uDE21", // 😡
            "\uD83D\uDCF7", // 📷
            "\uD83C\uDF38", // 🌸
            "\uD83D\uDE08", // 😈
            "\uD83D\uDC48", // 👈
            "\uD83C\uDF89", // 🎉
            "\uD83D\uDC81", // 💁
            "\uD83D\uDE4A", // 🙊
            "\uD83D\uDC9A", // 💚
            "\uD83D\uDE2B", // 😫
            "\uD83D\uDE24", // 😤
            "\uD83D\uDC93", // 💓
            "\uD83C\uDF1A", // 🌚
            "\uD83D\uDC47", // 👇
            "\uD83D\uDE07", // 😇
            "\uD83D\uDC4A", // 👊
            "\uD83D\uDC51", // 👑
            "\uD83D\uDE13", // 😓
            "\uD83D\uDE3B", // 😻
            "\uD83D\uDD34", // 🔴
            "\uD83D\uDE25", // 😥
            "\uD83E\uDD29", // 🤩
            "\uD83D\uDE1A", // 😚
            "\uD83D\uDE37", // 😷
            "\uD83D\uDC4B", // 👋
            "\uD83D\uDCA5", // 💥
            "\uD83E\uDD2D", // 🤭
            "\uD83C\uDF1F", // 🌟
            "\uD83E\uDD71", // 🥱
            "\uD83D\uDCA9", // 💩
            "\uD83D\uDE80", // 🚀
    };

    private static final String[] EMOJI_TARGET = new String[]{
            "ꀂ", // 😍
            "ꀃ", // 😘
            "ꀄ", // 😂
            "ꀅ", // 😊
            "ꀆ", // 😎
            "ꀇ", // 😉
            "ꀈ", // 💋
            "ꀉ", // 👍
            "ꀊ", // 🤣
            "ꀋ", // 💕
            "ꀌ", // 😀
            "ꀍ", // 😄
            "ꀎ", // 😭
            "ꀏ", // 🥺
            "ꀑ", // 🙏
            "ꀒ", // 🥰
            "ꀓ", // 🤔
            "ꀔ", // 🔥
            "ꀗ", // 😩
            "ꀘ", // 😔
            "ꀙ", // 😁
            "ꀚ", // 👌
            "ꀛ", // 😏
            "ꀜ", // 😅
            "ꀝ", // 🤍
            "ꀞ", // 💔
            "ꀟ", // 😌
            "ꀠ", // 😢
            "ꀡ", // 💙
            "ꀢ", // 💜
            "ꀤ", // 🎶
            "ꀥ", // 😳
            "ꀦ", // 💖
            "ꀧ", // 🙌
            "ꀨ", // 💯
            "ꀩ", // 🙈
            "ꀫ", // 😋
            "ꀬ", // 😑
            "ꀭ", // 😴
            "ꀮ", // 😪
            "ꀯ", // 😜
            "ꀰ", // 😛
            "ꀱ", // 😝
            "ꀲ", // 😞
            "ꀳ", // 😕
            "ꀴ", // 💗
            "ꀵ", // 👏
            "ꀶ", // 😐
            "ꀷ", // 👉
            "ꀸ", // 💛
            "ꀹ", // 💞
            "ꀺ", // 💪
            "ꀻ", // 🌹
            "ꀼ", // 💀
            "ꀽ", // 😱
            "ꀾ", // 💘
            "ꀿ", // 🤟
            "ꁀ", // 😡
            "ꁁ", // 📷
            "ꁂ", // 🌸
            "ꁃ", // 😈
            "ꁄ", // 👈
            "ꁅ", // 🎉
            "ꁆ", // 💁
            "ꁇ", // 🙊
            "ꁈ", // 💚
            "ꁉ", // 😫
            "ꁊ", // 😤
            "ꁍ", // 💓
            "ꁎ", // 🌚
            "ꁏ", // 👇
            "ꁒ", // 😇
            "ꁓ", // 👊
            "ꁔ", // 👑
            "ꁕ", // 😓
            "ꁖ", // 😻
            "ꁗ", // 🔴
            "ꁘ", // 😥
            "ꁙ", // 🤩
            "ꁚ", // 😚
            "ꁜ", // 😷
            "ꁝ", // 👋
            "ꁞ", // 💥
            "ꁠ", // 🤭
            "ꁡ", // 🌟
            "ꁢ", // 🥱
            "ꁣ", // 💩
            "ꁤ", // 🚀
    };
}
