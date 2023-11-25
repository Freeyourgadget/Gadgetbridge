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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;

public class XiaomiCalendarService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiCalendarService.class);

    public static final int COMMAND_TYPE = 12;

    private static final int CMD_CALENDAR_SET = 1;

    private static final int MAX_EVENTS = 50; // TODO confirm actual limit

    private final Set<CalendarEvent> lastSync = new HashSet<>();

    public XiaomiCalendarService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        LOG.warn("Unknown calendar command {}", cmd.getSubtype());
    }

    @Override
    public void initialize() {
        syncCalendar();
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR:
                syncCalendar();
                return true;
        }

        return false;
    }

    public void onAddCalendarEvent(final CalendarEventSpec ignoredCalendarEventSpec) {
        // we must sync everything
        syncCalendar();
    }

    public void onDeleteCalendarEvent(final byte ignoredType, final long ignoredId) {
        // we must sync everything
        syncCalendar();
    }

    public void syncCalendar() {
        final boolean syncEnabled = GBApplication.getDeviceSpecificSharedPrefs(getSupport().getDevice().getAddress())
                .getBoolean(PREF_SYNC_CALENDAR, false);

        final XiaomiProto.CalendarSync.Builder calendarSync = XiaomiProto.CalendarSync.newBuilder();

        if (!syncEnabled) {
            LOG.debug("Calendar sync is disabled");
            lastSync.clear();
            calendarSync.setDisabled(true);
        } else {
            final CalendarManager upcomingEvents = new CalendarManager(getSupport().getContext(), getSupport().getDevice().getAddress());
            final List<CalendarEvent> calendarEvents = upcomingEvents.getCalendarEventList();

            final Set<CalendarEvent> thisSync = new HashSet<>();
            int nEvents = 0;

            for (final CalendarEvent calendarEvent : calendarEvents) {
                if (nEvents++ > MAX_EVENTS) {
                    LOG.warn("Syncing only first {} events of {}", MAX_EVENTS, calendarEvents.size());
                    break;
                }

                thisSync.add(calendarEvent);

                final XiaomiProto.CalendarEvent xiaomiCalendarEvent = XiaomiProto.CalendarEvent.newBuilder()
                        .setTitle(calendarEvent.getTitle())
                        .setDescription(StringUtils.ensureNotNull(calendarEvent.getDescription()))
                        .setLocation(StringUtils.ensureNotNull(calendarEvent.getLocation()))
                        .setStart(calendarEvent.getBeginSeconds())
                        .setEnd((int) (calendarEvent.getEnd() / 1000))
                        .setAllDay(calendarEvent.isAllDay())
                        .setNotifyMinutesBefore(0) // TODO fetch from event
                        .build();

                calendarSync.addEvent(xiaomiCalendarEvent);
            }

            if (thisSync.equals(lastSync)) {
                LOG.debug("Already synced this set of events, won't send to device");
                return;
            }

            lastSync.clear();
            lastSync.addAll(thisSync);
        }

        LOG.debug("Syncing {} calendar events", lastSync.size());

        getSupport().sendCommand(
                "sync calendar",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CALENDAR_SET)
                        .setCalendar(XiaomiProto.Calendar.newBuilder().setCalendarSync(calendarSync))
                        .build()
        );
    }
}
