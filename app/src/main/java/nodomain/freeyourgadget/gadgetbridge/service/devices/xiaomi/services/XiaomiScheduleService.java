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

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiScheduleService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiScheduleService.class);

    public static final int COMMAND_TYPE = 17;

    private static final int CMD_ALARMS_GET = 0;
    private static final int CMD_ALARMS_CREATE = 1;
    private static final int CMD_ALARMS_EDIT = 2;
    private static final int CMD_ALARMS_DELETE = 4;
    private static final int CMD_SLEEP_MODE_GET = 8;
    private static final int CMD_SLEEP_MODE_SET = 9;
    private static final int CMD_WORLD_CLOCKS_GET = 10;
    private static final int CMD_WORLD_CLOCKS_SET = 11;

    private static final int REPETITION_ONCE = 0;
    private static final int REPETITION_DAILY = 1;
    private static final int REPETITION_WEEKLY = 5;
    private static final int REPETITION_MONTHLY = 7;
    private static final int REPETITION_YEARLY = 8;

    private static final int ALARM_SMART = 1;
    private static final int ALARM_NORMAL = 2;

    private static final Map<String, String> WORLD_CLOCK_CODES = new HashMap<String, String>() {{
        put("Europe/Lisbon", "C173");
        put("Australia/Sydney", "C151");
        // TODO map everything
    }};

    // Map of alarm position to Alarm, as returned by the band, indexed by GB watch position (0-indexed),
    // does NOT match watch ID
    private final Map<Integer, Alarm> watchAlarms = new HashMap<>();

    private int pendingAlarmAcks = 0;

    public XiaomiScheduleService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_ALARMS_GET:
                handleAlarms(cmd.getSchedule().getAlarms());
                break;
            case CMD_ALARMS_CREATE:
                pendingAlarmAcks--;
                if (pendingAlarmAcks <= 0) {
                    final TransactionBuilder builder = getSupport().createTransactionBuilder("request alarms after all acks");
                    requestAlarms(builder);
                    builder.queue(getSupport().getQueue());
                }
                break;
            case CMD_WORLD_CLOCKS_GET:
                handleWorldClocks(cmd.getSchedule().getWorldClocks());
                break;
            case CMD_SLEEP_MODE_GET:
                handleSleepModeConfig(cmd.getSchedule().getSleepMode());
                break;
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestAlarms(builder);
        requestWorldClocks(builder);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_SLEEP_MODE_GET);
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        final TransactionBuilder builder = getSupport().createTransactionBuilder("set " + config);

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME:
            case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_START:
            case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_END:
                setSleepModeConfig(builder);
                return true;
        }

        return false;
    }

    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        // TODO
    }

    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        final XiaomiProto.WorldClocks.Builder worldClocksBuilder = XiaomiProto.WorldClocks.newBuilder();

        for (final WorldClock clock : clocks) {
            final String clockCode = WORLD_CLOCK_CODES.get(clock.getTimeZoneId());
            if (clockCode != null) {
                worldClocksBuilder.addWorldClock(clockCode);
            } else {
                LOG.warn("Unknown timezone code for {}", clock.getTimeZoneId());
            }
        }

        final XiaomiProto.Schedule schedule = XiaomiProto.Schedule.newBuilder()
                .setWorldClocks(worldClocksBuilder.build())
                .build();

        getSupport().sendCommand(
                "send world clocks",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_WORLD_CLOCKS_SET)
                        .setSchedule(schedule)
                        .build()
        );
    }

    public void requestWorldClocks(final TransactionBuilder builder) {
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_WORLD_CLOCKS_GET);
    }

    public void handleWorldClocks(final XiaomiProto.WorldClocks worldClocks) {
        LOG.info("Got {} world clocks: {}", worldClocks.getWorldClockCount(), worldClocks.getWorldClockList());
        // TODO map the world clock codes
    }

    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final List<Integer> alarmsToDelete = new ArrayList<>();

        pendingAlarmAcks = 0;

        for (final Alarm alarm : alarms) {
            final Alarm watchAlarm = watchAlarms.get(alarm.getPosition());

            if (alarm.getUnused() && watchAlarm == null) {
                // Disabled on both
                //LOG.debug("Alarm {} is unused on both", alarm.getPosition());
                continue;
            }

            if (alarm.getUnused() && watchAlarm != null) {
                // Delete from watch
                alarmsToDelete.add(watchAlarm.getPosition() + 1); // watch positions, not GB
                watchAlarms.remove(alarm.getPosition());
                LOG.debug("Delete alarm {} from watch", alarm.getPosition());
                continue;
            }

            if (watchAlarm != null && alarmsEqual(alarm, watchAlarm)) {
                //LOG.debug("Alarm {} is already up-to-date on watch", alarm.getPosition());
                continue;
            }

            final XiaomiProto.HourMinute hourMinute = XiaomiProto.HourMinute.newBuilder()
                    .setHour(alarm.getHour())
                    .setMinute(alarm.getMinute())
                    .build();

            final XiaomiProto.AlarmDetails.Builder alarmDetails = XiaomiProto.AlarmDetails.newBuilder()
                    .setTime(hourMinute)
                    .setEnabled(alarm.getEnabled())
                    .setSmart(alarm.getSmartWakeup() ? ALARM_SMART : ALARM_NORMAL);

            switch (alarm.getRepetition()) {
                case Alarm.ALARM_ONCE:
                    alarmDetails.setRepeatMode(REPETITION_ONCE);
                    break;
                case Alarm.ALARM_DAILY:
                    alarmDetails.setRepeatMode(REPETITION_DAILY);
                    break;
                default:
                    alarmDetails.setRepeatMode(REPETITION_WEEKLY);
                    alarmDetails.setRepeatFlags(alarm.getRepetition());
                    break;
            }

            final XiaomiProto.Schedule.Builder schedule = XiaomiProto.Schedule.newBuilder();

            if (watchAlarm != null) {
                // update existing alarm
                LOG.debug("Update alarm {}", alarm.getPosition());
                watchAlarms.put(alarm.getPosition(), alarm);
                schedule.setEditAlarm(
                        XiaomiProto.Alarm.newBuilder()
                                .setId(alarm.getPosition() + 1)
                                .setAlarmDetails(alarmDetails)
                                .build()
                );
            } else {
                LOG.debug("Create alarm {}", alarm.getPosition());
                // watchAlarms will be updated later, since we don't know the correct ID here
                pendingAlarmAcks++;
                schedule.setCreateAlarm(alarmDetails);
            }

            getSupport().sendCommand(
                    (watchAlarm == null ? "create" : "update") + " alarm " + alarm.getPosition(),
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(watchAlarm == null ? CMD_ALARMS_CREATE : CMD_ALARMS_EDIT)
                            .setSchedule(schedule)
                            .build()
            );
        }

        if (!alarmsToDelete.isEmpty()) {
            final XiaomiProto.AlarmDelete alarmDelete = XiaomiProto.AlarmDelete.newBuilder()
                    .addAllId(alarmsToDelete)
                    .build();

            final XiaomiProto.Schedule schedule = XiaomiProto.Schedule.newBuilder()
                    .setDeleteAlarm(alarmDelete)
                    .build();

            getSupport().sendCommand(
                    "delete " + alarmsToDelete.size() + " unused alarms",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_ALARMS_DELETE)
                            .setSchedule(schedule)
                            .build()
            );
        }
    }

    public void requestAlarms(final TransactionBuilder builder) {
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_ALARMS_GET);
    }

    public void handleAlarms(final XiaomiProto.Alarms alarms) {
        LOG.debug("Got {} alarms from the watch", alarms.getAlarmCount());

        watchAlarms.clear();
        for (final XiaomiProto.Alarm alarm : alarms.getAlarmList()) {
            final nodomain.freeyourgadget.gadgetbridge.entities.Alarm gbAlarm = new nodomain.freeyourgadget.gadgetbridge.entities.Alarm();
            gbAlarm.setUnused(false); // If the band sent it, it's not unused
            gbAlarm.setPosition(alarm.getId() - 1); // band id starts at 1
            gbAlarm.setEnabled(alarm.getAlarmDetails().getEnabled());
            gbAlarm.setSmartWakeup(alarm.getAlarmDetails().getSmart() == ALARM_SMART);
            gbAlarm.setHour(alarm.getAlarmDetails().getTime().getHour());
            gbAlarm.setMinute(alarm.getAlarmDetails().getTime().getMinute());
            switch (alarm.getAlarmDetails().getRepeatMode()) {
                case REPETITION_ONCE:
                    gbAlarm.setRepetition(Alarm.ALARM_ONCE);
                    break;
                case REPETITION_DAILY:
                    gbAlarm.setRepetition(Alarm.ALARM_DAILY);
                    break;
                case REPETITION_WEEKLY:
                    gbAlarm.setRepetition(alarm.getAlarmDetails().getRepeatFlags());
                    break;
            }

            watchAlarms.put(gbAlarm.getPosition(), gbAlarm);
        }

        final List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> dbAlarms = DBHelper.getAlarms(getSupport().getDevice());
        int numUpdatedAlarms = 0;

        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : dbAlarms) {
            final int pos = alarm.getPosition();
            final Alarm updatedAlarm = watchAlarms.get(pos);

            final boolean alarmNeedsUpdate;
            if (updatedAlarm == null) {
                alarmNeedsUpdate = !alarm.getUnused();
            } else {
                alarmNeedsUpdate = !alarmsEqual(alarm, updatedAlarm);
            }
            if (alarmNeedsUpdate) {
                numUpdatedAlarms++;
                LOG.info("Updating alarm index={}, unused={}", pos, updatedAlarm == null);
                alarm.setUnused(updatedAlarm == null);
                if (updatedAlarm != null) {
                    alarm.setEnabled(updatedAlarm.getEnabled());
                    alarm.setSmartWakeup(updatedAlarm.getSmartWakeup());
                    alarm.setHour(updatedAlarm.getHour());
                    alarm.setMinute(updatedAlarm.getMinute());
                    alarm.setRepetition(updatedAlarm.getRepetition());
                }
                DBHelper.store(alarm);
            }
        }

        if (numUpdatedAlarms > 0) {
            final Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
            LocalBroadcastManager.getInstance(getSupport().getContext()).sendBroadcast(intent);
        }
    }

    private boolean alarmsEqual(final Alarm alarm1, final Alarm alarm2) {
        return alarm1.getUnused() == alarm2.getUnused() &&
                alarm1.getEnabled() == alarm2.getEnabled() &&
                alarm1.getSmartWakeup() == alarm2.getSmartWakeup() &&
                alarm1.getHour() == alarm2.getHour() &&
                alarm1.getMinute() == alarm2.getMinute() &&
                alarm1.getRepetition() == alarm2.getRepetition();
    }

    private void handleSleepModeConfig(final XiaomiProto.SleepMode sleepMode) {
        LOG.debug("Got sleep mode config");

        final String start = XiaomiPreferences.prefFromHourMin(sleepMode.getSchedule().getStart());
        final String end = XiaomiPreferences.prefFromHourMin(sleepMode.getSchedule().getEnd());

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference("prefs_enable_sleep_time", null)
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_ENABLED, sleepMode.getEnabled())
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_START, start)
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_END, end);

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setSleepModeConfig(final TransactionBuilder builder) {
        LOG.debug("Set sleep mode config");

        final Prefs prefs = getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_ENABLED, false);
        final Date start = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_START, "22:00");
        final Date end = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_END, "06:00");

        final XiaomiProto.SleepMode sleepMode = XiaomiProto.SleepMode.newBuilder()
                .setEnabled(enabled)
                .setSchedule(XiaomiProto.SleepModeSchedule.newBuilder()
                        .setUnknown3(0)
                        .setStart(XiaomiPreferences.prefToHourMin(start))
                        .setEnd(XiaomiPreferences.prefToHourMin(end)))
                .build();

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SLEEP_MODE_SET)
                        .setSchedule(XiaomiProto.Schedule.newBuilder().setSleepMode(sleepMode))
                        .build()
        );
    }
}
