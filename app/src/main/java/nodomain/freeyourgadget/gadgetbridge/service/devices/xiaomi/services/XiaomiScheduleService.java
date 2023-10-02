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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;

public class XiaomiScheduleService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiScheduleService.class);

    public static final int COMMAND_TYPE = 17;

    private static final int CMD_ALARMS_GET = 0;
    private static final int CMD_ALARMS_CREATE = 1;
    private static final int CMD_ALARMS_EDIT = 3;
    private static final int CMD_ALARMS_DELETE = 4;

    private static final int REPETITION_ONCE = 0;
    private static final int REPETITION_DAILY = 1;
    private static final int REPETITION_WEEKLY = 5;
    private static final int REPETITION_MONTHLY = 7;
    private static final int REPETITION_YEARLY = 8;

    private static final int ALARM_SMART = 1;
    private static final int ALARM_NORMAL = 2;

    // Map of alarm position to Alarm, as returned by the band
    private final Map<Integer, Alarm> watchAlarms = new HashMap<>();

    public XiaomiScheduleService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_ALARMS_GET:
                handleAlarms(cmd.getSchedule().getAlarms());
                break;
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestAlarms(builder);
    }

    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        // TODO
    }

    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        // TODO
    }

    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final List<Integer> alarmsToDelete = new ArrayList<>();

        // TODO this is flaky, since it's the watch that defines the IDs...

        for (final Alarm alarm : alarms) {
            final Alarm watchAlarm = watchAlarms.get(alarm.getPosition());

            if (alarm.getUnused() && watchAlarm == null) {
                // Disabled on both
                continue;
            }

            if (alarm.getUnused() && watchAlarm != null) {
                // Delete from watch
                alarmsToDelete.add(watchAlarm.getPosition() + 1);
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
                schedule.setEditAlarm(
                        XiaomiProto.Alarm.newBuilder()
                                .setId(alarm.getPosition() + 1)
                                .setAlarmDetails(alarmDetails)
                                .build()
                );
            } else {
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
                    "delete unused alarms",
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
        LOG.debug("Got {} alarms", alarms.getAlarmCount());

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
            final boolean alarmNeedsUpdate = updatedAlarm == null ||
                    alarm.getUnused() != updatedAlarm.getUnused() ||
                    alarm.getEnabled() != updatedAlarm.getEnabled() ||
                    alarm.getSmartWakeup() != updatedAlarm.getSmartWakeup() ||
                    alarm.getHour() != updatedAlarm.getHour() ||
                    alarm.getMinute() != updatedAlarm.getMinute() ||
                    alarm.getRepetition() != updatedAlarm.getRepetition();

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

    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        // TODO
    }

    public void onDeleteCalendarEvent(final byte type, long id) {
        // TODO
    }
}
