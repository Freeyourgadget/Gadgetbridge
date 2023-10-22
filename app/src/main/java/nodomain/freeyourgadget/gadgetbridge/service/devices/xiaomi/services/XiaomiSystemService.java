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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiSystemService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSystemService.class);

    public static final int COMMAND_TYPE = 2;

    public static final int CMD_BATTERY = 1;
    public static final int CMD_DEVICE_INFO = 2;
    public static final int CMD_CLOCK = 3;
    public static final int CMD_LANGUAGE = 6;
    public static final int CMD_PASSWORD_GET = 9;
    public static final int CMD_FIND_PHONE = 17;
    public static final int CMD_FIND_WATCH = 18;
    public static final int CMD_PASSWORD_SET = 21;
    public static final int CMD_DISPLAY_ITEMS_GET = 29;
    public static final int CMD_DISPLAY_ITEMS_SET = 30;
    public static final int CMD_CHARGER = 79;

    public XiaomiSystemService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize() {
        // Request device info and configs
        getSupport().sendCommand("get device info", COMMAND_TYPE, CMD_DEVICE_INFO);
        getSupport().sendCommand("get battery", COMMAND_TYPE, CMD_BATTERY);
        getSupport().sendCommand("get password", COMMAND_TYPE, CMD_PASSWORD_GET);
        getSupport().sendCommand("get display items", COMMAND_TYPE, CMD_DISPLAY_ITEMS_GET);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        // TODO
        switch (cmd.getSubtype()) {
            case CMD_DEVICE_INFO:
                handleDeviceInfo(cmd.getSystem().getDeviceInfo());
                return;
            case CMD_BATTERY:
                handleBattery(cmd.getSystem().getPower().getBattery());
                return;
            case CMD_PASSWORD_GET:
                handlePassword(cmd.getSystem().getPassword());
                return;
            case CMD_FIND_PHONE:
                LOG.debug("Got find phone: {}", cmd.getSystem().getFindDevice());
                final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                if (cmd.getSystem().getFindDevice() == 0) {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                } else {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                }
                getSupport().evaluateGBDeviceEvent(findPhoneEvent);
                return;
            case CMD_DISPLAY_ITEMS_GET:
                handleDisplayItems(cmd.getSystem().getDisplayItems());
                return;
            case CMD_CHARGER:
                // charger event, request battery state
                getSupport().sendCommand("request battery state", COMMAND_TYPE, CMD_BATTERY);
                return;
        }

        LOG.warn("Unknown system command {}", cmd.getSubtype());
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                setLanguage();
                return true;
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                setCurrentTime();
                return true;
            case PasswordCapabilityImpl.PREF_PASSWORD_ENABLED:
            case PasswordCapabilityImpl.PREF_PASSWORD:
                setPassword();
            case HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE:
                setDisplayItems();
                return true;
        }

        return super.onSendConfiguration(config, prefs);
    }

    public void setLanguage() {
        String localeString = GBApplication.getDeviceSpecificSharedPrefs(getSupport().getDevice().getAddress()).getString(
                DeviceSettingsPreferenceConst.PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO
        );
        if (DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO.equals(localeString)) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (StringUtils.isNullOrEmpty(country)) {
                // sometimes country is null, no idea why, guess it.
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }

        LOG.info("Set language: {}", localeString);

        getSupport().sendCommand(
                "set language",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_LANGUAGE)
                        .setSystem(XiaomiProto.System.newBuilder().setLanguage(
                                XiaomiProto.Language.newBuilder().setCode(localeString.toLowerCase(Locale.ROOT))
                        ))
                        .build()
        );
    }

    public void setCurrentTime() {
        LOG.debug("Setting current time");

        final Calendar now = GregorianCalendar.getInstance();
        final TimeZone tz = TimeZone.getDefault();

        final GBPrefs gbPrefs = new GBPrefs(new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getSupport().getDevice().getAddress())));
        final String timeFormat = gbPrefs.getTimeFormat();
        final boolean is24hour = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H.equals(timeFormat);

        final XiaomiProto.Clock clock = XiaomiProto.Clock.newBuilder()
                .setTime(XiaomiProto.Time.newBuilder()
                        .setHour(now.get(Calendar.HOUR_OF_DAY))
                        .setMinute(now.get(Calendar.MINUTE))
                        .setSecond(now.get(Calendar.SECOND))
                        .setMillisecond(now.get(Calendar.MILLISECOND))
                        .build())
                .setDate(XiaomiProto.Date.newBuilder()
                        .setYear(now.get(Calendar.YEAR))
                        .setMonth(now.get(Calendar.MONTH) + 1)
                        .setDay(now.get(Calendar.DATE))
                        .build())
                .setTimezone(XiaomiProto.TimeZone.newBuilder()
                        .setZoneOffset(((now.get(Calendar.ZONE_OFFSET) / 1000) / 60) / 15)
                        .setDstOffset(((now.get(Calendar.DST_OFFSET) / 1000) / 60) / 15)
                        .setName(tz.getID())
                        .build())
                .setIsNot24Hour(!is24hour)
                .build();

        getSupport().sendCommand(
                "set time",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CLOCK)
                        .setSystem(XiaomiProto.System.newBuilder().setClock(clock).build())
                        .build()
        );
    }

    private void handleDeviceInfo(final XiaomiProto.DeviceInfo deviceInfo) {
        LOG.debug("Got device info: fw={} hw={} sn={}", deviceInfo.getFirmware(), deviceInfo.getModel(), deviceInfo.getSerialNumber());

        final GBDeviceEventVersionInfo gbDeviceEventVersionInfo = new GBDeviceEventVersionInfo();
        gbDeviceEventVersionInfo.fwVersion = deviceInfo.getFirmware();
        //gbDeviceEventVersionInfo.fwVersion2 = "N/A";
        gbDeviceEventVersionInfo.hwVersion = deviceInfo.getModel();
        final GBDeviceEventUpdateDeviceInfo gbDeviceEventUpdateDeviceInfo = new GBDeviceEventUpdateDeviceInfo("SERIAL: ", deviceInfo.getSerialNumber());

        getSupport().evaluateGBDeviceEvent(gbDeviceEventVersionInfo);
        getSupport().evaluateGBDeviceEvent(gbDeviceEventUpdateDeviceInfo);
    }

    private void handleBattery(final XiaomiProto.Battery battery) {
        LOG.debug("Got battery: {}", battery.getLevel());

        final GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.batteryIndex = 0;
        batteryInfo.level = battery.getLevel();
        switch (battery.getState()) {
            case 1:
                batteryInfo.state = BatteryState.BATTERY_CHARGING;
                break;
            case 2:
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                break;
            default:
                batteryInfo.state = BatteryState.UNKNOWN;
                LOG.warn("Unknown battery state {}", battery.getState());
        }
        getSupport().evaluateGBDeviceEvent(batteryInfo);
    }

    private void setPassword() {
        final Prefs prefs = getDevicePrefs();

        final boolean passwordEnabled = prefs.getBoolean(PasswordCapabilityImpl.PREF_PASSWORD_ENABLED, false);
        final String password = prefs.getString(PasswordCapabilityImpl.PREF_PASSWORD, null);

        LOG.info("Setting password: {}, {}", passwordEnabled, password);

        if (password == null || password.isEmpty()) {
            LOG.warn("Invalid password: {}", password);
            return;
        }

        final XiaomiProto.Password.Builder passwordBuilder = XiaomiProto.Password.newBuilder()
                .setState(passwordEnabled ? 2 : 1)
                .setPassword(password);

        getSupport().sendCommand(
                "set password",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_PASSWORD_SET)
                        .setSystem(XiaomiProto.System.newBuilder().setPassword(passwordBuilder).build())
                        .build()
        );
    }

    private void handlePassword(final XiaomiProto.Password password) {
        LOG.debug("Got device password");
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(
                PasswordCapabilityImpl.PREF_PASSWORD_ENABLED,
                password.getState() == 2
        );
        if (password.hasPassword()) {
            eventUpdatePreferences.withPreference(
                    PasswordCapabilityImpl.PREF_PASSWORD,
                    password.getPassword()
            );
        }
        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setDisplayItems() {
        final Prefs prefs = getDevicePrefs();
        final List<String> allScreens = new ArrayList<>(prefs.getList(XiaomiPreferences.getPrefPossibleValuesKey(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE), Collections.emptyList()));
        final List<String> enabledScreens = new ArrayList<>(prefs.getList(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, Collections.emptyList()));
        if (allScreens.isEmpty()) {
            LOG.warn("No list of all screens");
            return;
        }

        LOG.debug("Setting display items: {}", enabledScreens);

        if (!enabledScreens.contains("setting")) {
            enabledScreens.add("setting");
        }

        boolean inMoreSection = false;
        final XiaomiProto.DisplayItems.Builder displayItems = XiaomiProto.DisplayItems.newBuilder();
        for (final String enabledScreen : enabledScreens) {
            if (enabledScreen.equals("more")) {
                inMoreSection = true;
                continue;
            }

            final XiaomiProto.DisplayItem.Builder displayItem = XiaomiProto.DisplayItem.newBuilder()
                    .setCode(enabledScreen)
                    .setName(DISPLAY_ITEM_NAMES.get(enabledScreen))
                    .setUnknown5(1);

            if (inMoreSection) {
                displayItem.setInMoreSection(true);
            }

            if ("setting".equals(enabledScreen)) {
                displayItem.setIsSettings(1);
            }

            displayItems.addDisplayItem(displayItem);
        }

        for (final String screen : allScreens) {
            if (enabledScreens.contains(screen)) {
                continue;
            }

            final XiaomiProto.DisplayItem.Builder displayItem = XiaomiProto.DisplayItem.newBuilder()
                    .setCode(screen)
                    .setName(DISPLAY_ITEM_NAMES.get(screen))
                    .setDisabled(true)
                    .setUnknown5(1);

            displayItems.addDisplayItem(displayItem);
        }

        getSupport().sendCommand(
                "set display items",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_DISPLAY_ITEMS_SET)
                        .setSystem(XiaomiProto.System.newBuilder().setDisplayItems(displayItems))
                        .build()
        );
    }

    private void handleDisplayItems(final XiaomiProto.DisplayItems displayItems) {
        LOG.debug("Got {} display items", displayItems.getDisplayItemCount());

        final List<String> allScreens = new ArrayList<>();
        final List<String> mainScreens = new ArrayList<>();
        final List<String> moreScreens = new ArrayList<>();
        for (final XiaomiProto.DisplayItem displayItem : displayItems.getDisplayItemList()) {
            allScreens.add(displayItem.getCode());
            if (!displayItem.getDisabled()) {
                if (displayItem.getInMoreSection()) {
                    moreScreens.add(displayItem.getCode());
                } else {
                    mainScreens.add(displayItem.getCode());
                }
            }
        }

        final List<String> enabledScreens = new ArrayList<>(mainScreens);
        if (!moreScreens.isEmpty()) {
            enabledScreens.add("more");
            enabledScreens.addAll(moreScreens);
        }

        final String allScreensPrefValue = StringUtils.join(",", allScreens.toArray(new String[0])).toString();
        final String prefValue = StringUtils.join(",", enabledScreens.toArray(new String[0])).toString();

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(XiaomiPreferences.getPrefPossibleValuesKey(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE), allScreensPrefValue)
                .withPreference(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, prefValue);

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public void onFindPhone(final boolean start) {
        LOG.debug("Find phone: {}", start);

        if (!start) {
            // Stop on watch
            getSupport().sendCommand(
                    "find phone stop",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_FIND_PHONE)
                            .setSystem(XiaomiProto.System.newBuilder().setFindDevice(1).build())
                            .build()
            );
        }
    }

    public void onFindWatch(final boolean start) {
        LOG.debug("Find watch: {}", start);

        getSupport().sendCommand(
                "find watch " + start,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_FIND_WATCH)
                        .setSystem(XiaomiProto.System.newBuilder().setFindDevice(start ? 0 : 1).build())
                        .build()
        );
    }

    private static final Map<String, String> DISPLAY_ITEM_NAMES = new HashMap<String, String>() {{
        put("today_act", "Stats");
        put("sport", "Workout");
        put("sport_record", "Activity");
        put("sport_course", "Running");
        put("sport_state", "Status");
        put("heart", "Heart rate");
        put("pai", "Vitality");
        put("blood_ox", "SpO₂");
        put("sleep", "Sleep");
        put("press", "Stress");
        put("weather", "Weather");
        put("alarm", "Alarm");
        put("setting", "Settings");
        put("event_reminder", "Alerts");
        put("schedule", "Events");
        put("breath", "Breathing");
        put("stopwatch", "Stopwatch");
        put("music", "Music");
        put("find_phone", "Find phone");
        put("world_clock", "World clock");
        put("phone_mute", "Silence phone");
        put("phone_remote", "Camera");
        put("count_down", "Timer");
        put("focus", "Focus");
        put("flash_light", "Flashlight");
        put("fm_health", "Cycles");
    }};
}
