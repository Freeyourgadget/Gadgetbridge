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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiSystemService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSystemService.class);

    public static final int COMMAND_TYPE = 2;

    public static final int CMD_BATTERY = 1;
    public static final int CMD_DEVICE_INFO = 2;
    public static final int CMD_CLOCK = 3;
    public static final int CMD_PASSWORD_GET = 9;
    public static final int CMD_FIND_PHONE = 17;
    public static final int CMD_PASSWORD_SET = 21;
    public static final int CMD_CHARGER = 79;

    public XiaomiSystemService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        // Request device info and configs
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_DEVICE_INFO);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_BATTERY);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_PASSWORD_GET);
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
            case CMD_CHARGER:
                // charger event, request battery state
                getSupport().sendCommand("request battery state", COMMAND_TYPE, CMD_BATTERY);
                return;
        }

        LOG.warn("Unknown config command {}", cmd.getSubtype());
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        final TransactionBuilder builder = getSupport().createTransactionBuilder("set " + config);

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                setCurrentTime(builder);
                builder.queue(getSupport().getQueue());
                return true;
            case PasswordCapabilityImpl.PREF_PASSWORD_ENABLED:
            case PasswordCapabilityImpl.PREF_PASSWORD:
                setPassword(builder);
                return true;
        }

        return super.onSendConfiguration(config, prefs);
    }

    public void setCurrentTime(final TransactionBuilder builder) {
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
                builder,
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

    private void setPassword(final TransactionBuilder builder) {
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
                builder,
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
}
