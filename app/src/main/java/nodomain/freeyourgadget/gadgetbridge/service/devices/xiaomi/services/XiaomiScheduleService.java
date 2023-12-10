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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
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
    private static final int CMD_REMINDERS_GET = 14;
    private static final int CMD_REMINDERS_CREATE = 15;
    private static final int CMD_REMINDERS_EDIT = 17;
    private static final int CMD_REMINDERS_DELETE = 18;

    private static final int REPETITION_ONCE = 0;
    private static final int REPETITION_DAILY = 1;
    private static final int REPETITION_WEEKLY = 5;
    private static final int REPETITION_MONTHLY = 7;
    private static final int REPETITION_YEARLY = 8;

    private static final int ALARM_SMART = 1;
    private static final int ALARM_NORMAL = 2;

    // Reminders created by this service will have this prefix
    private static final String REMINDER_DB_PREFIX = "xiaomi_";

    private static final Map<String, String> WORLD_CLOCK_CODES = new HashMap<String, String>() {{
        put("Europe/Lisbon", "C173");
        put("Australia/Sydney", "C151");
        // TODO map everything
    }};

    // Map of alarm position to Alarm/Reminder, as returned by the watch, indexed by GB position (0-indexed),
    // does NOT match the ID returned by the watch, but should be offset by 1
    private final Map<Integer, Alarm> watchAlarms = new HashMap<>();
    private final Map<String, Reminder> watchReminders = new HashMap<>();

    private int pendingAlarmAcks = 0;
    private int pendingReminderAcks = 0;

    public XiaomiScheduleService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_ALARMS_GET:
                handleAlarms(cmd.getSchedule().getAlarms());
                return;
            case CMD_ALARMS_CREATE:
                pendingAlarmAcks--;
                LOG.debug("Got alarms create ack, remaining {}", pendingAlarmAcks);
                if (pendingAlarmAcks <= 0) {
                    LOG.debug("Requesting alarms after all acks");
                    requestAlarms();
                }
                return;
            case CMD_SLEEP_MODE_SET:
                LOG.debug("Got sleep mode set ack, status={}", cmd.getStatus());
                return;
            case CMD_WORLD_CLOCKS_GET:
                handleWorldClocks(cmd.getSchedule().getWorldClocks());
                return;
            case CMD_SLEEP_MODE_GET:
                handleSleepModeConfig(cmd.getSchedule().getSleepMode());
                return;
            case CMD_REMINDERS_GET:
                handleReminders(cmd.getSchedule().getReminders());
                return;
            case CMD_REMINDERS_CREATE:
                pendingReminderAcks--;
                LOG.debug("Got alarms create ack, remaining {}", pendingReminderAcks);
                if (pendingReminderAcks <= 0) {
                    LOG.debug("Requesting reminders after all acks");
                    requestReminders();
                }
                return;
        }

        LOG.warn("Unknown schedule command {}", cmd.getSubtype());
    }

    @Override
    public void initialize() {
        requestAlarms();
        requestReminders();
        requestWorldClocks();
        getSupport().sendCommand("get sleep mode", COMMAND_TYPE, CMD_SLEEP_MODE_GET);
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_START:
            case DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_END:
                setSleepModeConfig();
                return true;
        }

        return false;
    }

    public void requestReminders() {
        getSupport().sendCommand("request reminders", COMMAND_TYPE, CMD_REMINDERS_GET);
    }

    public void handleReminders(final XiaomiProto.Reminders reminders) {
        LOG.debug("Got {} reminders from the watch", reminders.getReminderCount());

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(XiaomiPreferences.PREF_REMINDER_SLOTS, reminders.getMaxReminders());

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);

        watchReminders.clear();
        for (final XiaomiProto.Reminder reminder : reminders.getReminderList()) {
            final nodomain.freeyourgadget.gadgetbridge.entities.Reminder gbReminder = new nodomain.freeyourgadget.gadgetbridge.entities.Reminder();
            gbReminder.setReminderId(REMINDER_DB_PREFIX + reminder.getId());
            gbReminder.setMessage(reminder.getReminderDetails().getTitle());
            gbReminder.setDate(XiaomiPreferences.toDate(reminder.getReminderDetails().getDate(), reminder.getReminderDetails().getTime()));

            switch (reminder.getReminderDetails().getRepeatMode()) {
                case REPETITION_ONCE:
                    gbReminder.setRepetition(Alarm.ALARM_ONCE);
                    break;
                case REPETITION_DAILY:
                    gbReminder.setRepetition(Alarm.ALARM_DAILY);
                    break;
                case REPETITION_WEEKLY:
                    gbReminder.setRepetition(reminder.getReminderDetails().getRepeatFlags());
                    break;
            }

            watchReminders.put(gbReminder.getReminderId(), gbReminder);
        }

        final List<nodomain.freeyourgadget.gadgetbridge.entities.Reminder> dbReminders = DBHelper.getReminders(getSupport().getDevice());

        final Set<String> dbReminderIds = new HashSet<>();

        int numUpdatedReminders = 0;

        // Delete reminders that do not exist on the watch anymore
        for (nodomain.freeyourgadget.gadgetbridge.entities.Reminder reminder : dbReminders) {
            if (!reminder.getReminderId().startsWith(REMINDER_DB_PREFIX)) {
                LOG.debug("Deleting reminder {}", reminder.getReminderId());
                DBHelper.delete(reminder);
                numUpdatedReminders++;
                continue;
            }

            dbReminderIds.add(reminder.getReminderId());
        }

        // Persist unknown reminders
        // We assume that reminders are not modifiable from the watch, unlike alarms
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession daoSession = db.getDaoSession();
            final Device device = DBHelper.getDevice(getSupport().getDevice(), daoSession);
            final User user = DBHelper.getUser(daoSession);

            for (final Reminder watchReminder : watchReminders.values()) {
                final String reminderId = watchReminder.getReminderId();
                if (dbReminderIds.contains(reminderId)) {
                    continue;
                }

                // Reminder not known - persist it to database
                LOG.info("Persisting reminder {}", reminderId);

                final nodomain.freeyourgadget.gadgetbridge.entities.Reminder reminder = new nodomain.freeyourgadget.gadgetbridge.entities.Reminder();
                reminder.setReminderId(watchReminder.getReminderId());
                reminder.setDate(watchReminder.getDate());
                reminder.setMessage(watchReminder.getMessage());
                reminder.setRepetition(watchReminder.getRepetition());
                reminder.setDeviceId(device.getId());
                reminder.setUserId(user.getId());

                DBHelper.store(reminder);

                numUpdatedReminders++;
            }
        } catch (final Exception e) {
            LOG.error("Error accessing database", e);
        }

        if (numUpdatedReminders > 0) {
            final Intent intent = new Intent(DeviceService.ACTION_SAVE_REMINDERS);
            LocalBroadcastManager.getInstance(getSupport().getContext()).sendBroadcast(intent);
        }
    }

    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        final List<Integer> remindersToDelete = new ArrayList<>();

        pendingReminderAcks = 0;

        final Set<String> newReminderIds = new HashSet<>();
        for (final Reminder reminder : reminders) {
            newReminderIds.add(reminder.getReminderId());
        }

        for (final Reminder watchReminder : watchReminders.values()) {
            if (!newReminderIds.contains(watchReminder.getReminderId())) {
                final Integer watchId = Integer.parseInt(watchReminder.getReminderId().replace(REMINDER_DB_PREFIX, ""));
                remindersToDelete.add(watchId);
            }
        }

        for (final Integer id : remindersToDelete) {
            watchReminders.remove(REMINDER_DB_PREFIX + id);
        }

        for (final Reminder reminder : reminders) {
            final boolean isCreateReminder;
            if (reminder.getReminderId().startsWith(REMINDER_DB_PREFIX) && watchReminders.containsKey(reminder.getReminderId())) {
                // Update reminder on the watch if needed
                final Reminder watchReminder = watchReminders.get(reminder.getReminderId());
                if (watchReminder != null && remindersEqual(reminder, watchReminder)) {
                    LOG.debug("Reminder {} is already up-to-date on watch", watchReminder.getReminderId());
                    continue;
                }

                isCreateReminder = (watchReminder == null);
            } else {
                isCreateReminder = true;
            }

            final Calendar reminderTime = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            reminderTime.setTimeInMillis(reminder.getDate().getTime());

            final XiaomiProto.ReminderDetails.Builder reminderDetails = XiaomiProto.ReminderDetails.newBuilder()
                    .setTime(XiaomiProto.Time.newBuilder()
                            .setHour(reminderTime.get(Calendar.HOUR_OF_DAY))
                            .setMinute(reminderTime.get(Calendar.MINUTE))
                            .setSecond(reminderTime.get(Calendar.SECOND))
                            .setMillisecond(reminderTime.get(Calendar.MILLISECOND))
                            .build())
                    .setDate(XiaomiProto.Date.newBuilder()
                            .setYear(reminderTime.get(Calendar.YEAR))
                            .setMonth(reminderTime.get(Calendar.MONTH) + 1)
                            .setDay(reminderTime.get(Calendar.DATE))
                            .build())
                    .setTitle(reminder.getMessage());

            switch (reminder.getRepetition()) {
                case Alarm.ALARM_ONCE:
                    reminderDetails.setRepeatMode(REPETITION_ONCE);
                    break;
                case Alarm.ALARM_DAILY:
                    reminderDetails.setRepeatMode(REPETITION_DAILY);
                    break;
                default:
                    reminderDetails.setRepeatMode(REPETITION_WEEKLY);
                    reminderDetails.setRepeatFlags(reminder.getRepetition());
                    break;
            }

            final XiaomiProto.Schedule.Builder schedule = XiaomiProto.Schedule.newBuilder();

            if (!isCreateReminder) {
                // update existing alarm
                LOG.debug("Update reminder {}", reminder.getReminderId());
                watchReminders.put(reminder.getReminderId(), reminder);
                schedule.setEditReminder(
                        XiaomiProto.Reminder.newBuilder()
                                .setId(Integer.parseInt(reminder.getReminderId().replace(REMINDER_DB_PREFIX, "")))
                                .setReminderDetails(reminderDetails)
                                .build()
                );
            } else {
                LOG.debug("Create reminder {}", reminder.getReminderId());
                // watchReminders will be updated later, since we don't know the correct ID here
                pendingReminderAcks++;
                schedule.setCreateReminder(reminderDetails);
            }

            getSupport().sendCommand(
                    (isCreateReminder ? "create" : "update") + " reminder " + reminder.getReminderId(),
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(isCreateReminder ? CMD_REMINDERS_CREATE : CMD_REMINDERS_EDIT)
                            .setSchedule(schedule)
                            .build()
            );
        }

        if (!remindersToDelete.isEmpty()) {
            final XiaomiProto.ReminderDelete reminderDelete = XiaomiProto.ReminderDelete.newBuilder()
                    .addAllId(remindersToDelete)
                    .build();

            final XiaomiProto.Schedule schedule = XiaomiProto.Schedule.newBuilder()
                    .setDeleteReminder(reminderDelete)
                    .build();

            getSupport().sendCommand(
                    "delete " + remindersToDelete.size() + " reminders",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_REMINDERS_DELETE)
                            .setSchedule(schedule)
                            .build()
            );
        }
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

    public void requestWorldClocks() {
        getSupport().sendCommand("get world clocks", COMMAND_TYPE, CMD_WORLD_CLOCKS_GET);
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

    public void requestAlarms() {
        getSupport().sendCommand("get alarms", COMMAND_TYPE, CMD_ALARMS_GET);
    }

    public void handleAlarms(final XiaomiProto.Alarms alarms) {
        LOG.debug("Got {} alarms from the watch", alarms.getAlarmCount());

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(XiaomiPreferences.PREF_ALARM_SLOTS, alarms.getMaxAlarms());

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);

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

    private boolean remindersEqual(final Reminder reminder1, final Reminder reminder2) {
        return Objects.equals(reminder1.getMessage(), reminder2.getMessage()) &&
                Objects.equals(reminder1.getDate(), reminder2.getDate()) &&
                reminder1.getRepetition() == reminder2.getRepetition();
    }

    private void handleSleepModeConfig(final XiaomiProto.SleepMode sleepMode) {
        LOG.debug("Got sleep mode config");

        final String start = XiaomiPreferences.prefFromHourMin(sleepMode.getSchedule().getStart());
        final String end = XiaomiPreferences.prefFromHourMin(sleepMode.getSchedule().getEnd());

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(XiaomiPreferences.FEAT_SLEEP_MODE_SCHEDULE, true)
                .withPreference("prefs_enable_sleep_time", null)
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_ENABLED, sleepMode.getEnabled())
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_START, start)
                .withPreference(DeviceSettingsPreferenceConst.PREF_SLEEP_MODE_SCHEDULE_END, end);

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setSleepModeConfig() {
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
                "set sleep mode",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SLEEP_MODE_SET)
                        .setSchedule(XiaomiProto.Schedule.newBuilder().setSleepMode(sleepMode))
                        .build()
        );
    }
}
