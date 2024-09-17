/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;


import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FORCE_CONNECTION_TYPE;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
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
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
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

public class XiaomiSupport extends AbstractDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSupport.class);

    private final XiaomiAuthService authService = new XiaomiAuthService(this);
    private final XiaomiMusicService musicService = new XiaomiMusicService(this);
    private final XiaomiHealthService healthService = new XiaomiHealthService(this);
    private final XiaomiNotificationService notificationService = new XiaomiNotificationService(this);
    private final XiaomiScheduleService scheduleService = new XiaomiScheduleService(this);
    private final XiaomiWeatherService weatherService = new XiaomiWeatherService(this);
    private final XiaomiSystemService systemService = new XiaomiSystemService(this);
    private final XiaomiCalendarService calendarService = new XiaomiCalendarService(this);
    private final XiaomiWatchfaceService watchfaceService = new XiaomiWatchfaceService(this);
    private final XiaomiDataUploadService dataUploadService = new XiaomiDataUploadService(this);
    private final XiaomiPhonebookService phonebookService = new XiaomiPhonebookService(this);

    private String cachedFirmwareVersion = null;
    private XiaomiConnectionSupport connectionSupport = null;

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

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        super.setAutoReconnect(enabled);
        if (this.connectionSupport != null) {
            this.connectionSupport.setAutoReconnect(enabled);
        }
    }

    private DeviceCoordinator.ConnectionType getForcedConnectionTypeFromPrefs() {
        final String connTypeAuto = getContext().getString(R.string.pref_force_connection_type_auto_value);
        String connTypePref = getDevicePrefs().getString(PREF_FORCE_CONNECTION_TYPE, connTypeAuto);

        if (getContext().getString(R.string.pref_force_connection_type_ble_value).equals(connTypePref))
            return DeviceCoordinator.ConnectionType.BLE;

        if (getContext().getString(R.string.pref_force_connection_type_bt_classic_value).equals(connTypePref))
            return DeviceCoordinator.ConnectionType.BT_CLASSIC;

        // either set to default, unknown option selected, or has not been set
        return DeviceCoordinator.ConnectionType.BOTH;
    }

    private XiaomiConnectionSupport createConnectionSpecificSupport() {
        DeviceCoordinator.ConnectionType connType = getCoordinator().getConnectionType();

        if (connType == DeviceCoordinator.ConnectionType.BOTH) {
            connType = getForcedConnectionTypeFromPrefs();
        }

        switch (connType) {
            case BLE:
            case BOTH:
                return new XiaomiBleSupport(this);
            case BT_CLASSIC:
                return new XiaomiSppSupport(this);
        }

        LOG.error("Cannot create connection-specific support, unhanded {} connection type", connType);
        return null;
    }

    public XiaomiConnectionSupport getConnectionSpecificSupport() {
        if (connectionSupport == null) {
            connectionSupport = createConnectionSpecificSupport();
        }

        return connectionSupport;
    }

    @Override
    public boolean connect() {
        if (getConnectionSpecificSupport() != null)
            return getConnectionSpecificSupport().connect();

        LOG.error("getConnectionSpecificSupport returned null, could not connect");
        return false;
    }

    @Override
    public void dispose() {
        if (this.connectionSupport != null) {
            XiaomiConnectionSupport connectionSupport = this.connectionSupport;
            this.connectionSupport = null;
            connectionSupport.dispose();
        }
    }

    public void setContext(final GBDevice device, final BluetoothAdapter adapter, final Context context) {
        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (device.getFirmwareVersion() != null) {
            setCachedFirmwareVersion(device.getFirmwareVersion());
        }

        super.setContext(device, adapter, context);

        for (AbstractXiaomiService service : mServiceMap.values()) {
            service.setContext(context);
        }

        if (getConnectionSpecificSupport() != null) {
            getConnectionSpecificSupport().setContext(device, adapter, context);
        }
    }

    public String getCachedFirmwareVersion() {
        return this.cachedFirmwareVersion;
    }

    public void setCachedFirmwareVersion(String version) {
        this.cachedFirmwareVersion = version;
    }

    public void onDisconnect() {
        // propagate disconnection to services
        for (AbstractXiaomiService service : mServiceMap.values()) {
            service.onDisconnect();
        }
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
    public void onSendWeather(final ArrayList<WeatherSpec> weatherSpecs) {
        weatherService.onSendWeather(weatherSpecs);
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        //noinspection unchecked
        phonebookService.setContacts((List<Contact>) contacts);
    }

    public XiaomiCoordinator getCoordinator() {
        return (XiaomiCoordinator) gbDevice.getDeviceCoordinator();
    }

    protected void onAuthSuccess() {
        LOG.info("onAuthSuccess");

        getConnectionSpecificSupport().onAuthSuccess();

        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            systemService.setCurrentTime();
        }

        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.initialize();
        }
    }

    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        getConnectionSpecificSupport().sendCommand(taskName, command);
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

    public XiaomiAuthService getAuthService() {
        return this.authService;
    }

    public XiaomiDataUploadService getDataUploadService() {
        return this.dataUploadService;
    }

    public XiaomiHealthService getHealthService() {
        return this.healthService;
    }

    @Override
    public String customStringFilter(final String inputString) {
        return StringUtils.replaceEach(inputString, EMOJI_SOURCE, EMOJI_TARGET);
    }

    boolean parsingActivityFilesFromStorage = false;

    private void parseAllActivityFilesFromStorage() {
        if (parsingActivityFilesFromStorage) {
            GB.toast(getContext(), "Already parsing!", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        parsingActivityFilesFromStorage = true;

        LOG.info("Parsing all activity files from storage");

        final File[] activityFiles;
        try {
            final File externalFilesDir = getCoordinator().getWritableExportDirectory(getDevice());
            final File exportDir = new File(externalFilesDir, "rawFetchOperations");

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                GB.toast(getContext(), "export directory " + exportDir + " not found", Toast.LENGTH_LONG, GB.ERROR);
                return;
            }

            activityFiles = exportDir.listFiles((dir, name) -> name.startsWith("xiaomi_"));
            if (activityFiles == null) {
                LOG.error("activityFiles is null for {}", exportDir);
                GB.toast(getContext(), "activityFiles is null for " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
            if (activityFiles.length == 0) {
                LOG.error("No activity files found in {}", exportDir);
                GB.toast(getContext(), "No activity files found in " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            GB.toast(getContext(), "Failed to parse from storage", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        GB.toast(getContext(), "Check notification for progress", Toast.LENGTH_LONG, GB.INFO);
        GB.updateTransferNotification("Parsing activity files", "...", true, 0, getContext());
        final long[] lastNotificationUpdateTs = new long[]{System.currentTimeMillis()};

        final Handler handler = new Handler(getContext().getMainLooper());
        new Thread(() -> {
            try {
                int[] i = new int[]{0};
                for (final File activityFile : activityFiles) {
                    i[0]++;

                    LOG.debug("Parsing {}", activityFile);

                    final long now = System.currentTimeMillis();
                    if (now - lastNotificationUpdateTs[0] > 1500L) {
                        lastNotificationUpdateTs[0] = now;
                        handler.post(() -> {
                            GB.updateTransferNotification(
                                    "Parsing activity files", "File " + i[0] + " of " + activityFiles.length,
                                    true,
                                    (i[0] * 100) / activityFiles.length, getContext()
                            );
                            ;
                        });
                    }

                    // The logic below just replicates XiaomiActivityFileFetcher

                    final byte[] data;
                    try (InputStream in = new FileInputStream(activityFile)) {
                        data = FileUtils.readAll(in, 999999);
                    } catch (final IOException ioe) {
                        LOG.error("Failed to read {}", activityFile, ioe);
                        continue;
                    }

                    final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
                    final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

                    final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
                    if (activityParser == null) {
                        LOG.warn("Failed to find parser for {}", fileId);
                        continue;
                    }

                    try {
                        if (activityParser.parse(this, fileId, data)) {
                            LOG.info("Successfully parsed {}", fileId);
                        } else {
                            LOG.warn("Failed to parse {}", fileId);
                        }
                    } catch (final Exception ex) {
                        LOG.error("Exception while parsing {}", fileId, ex);
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to parse from storage", e);
            }

            handler.post(() -> {
                parsingActivityFilesFromStorage = false;
                GB.updateTransferNotification("", "", false, 100, getContext());
                GB.signalActivityDataFinish(getDevice());
            });
        }).start();
    }

    public void setFeatureSupported(final String featureKey, final boolean supported) {
        LOG.debug("Setting feature {} -> {}", featureKey, supported ? "supported" : "not supported");
        evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(featureKey, supported));
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
