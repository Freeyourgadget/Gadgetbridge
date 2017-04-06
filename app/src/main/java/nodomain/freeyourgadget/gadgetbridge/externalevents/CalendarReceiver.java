/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEvents;

public class CalendarReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarReceiver.class);
    private static Hashtable<Long,EventSyncState> eventState = new Hashtable<>();

    private class EventSyncState {
        private EventState state;
        private CalendarEvents.CalendarEvent event;

        public EventSyncState(CalendarEvents.CalendarEvent event, EventState state) {
            this.state = state;
            this.event = event;
        }

        public EventState getState() {
            return state;
        }

        public void setState(EventState state) {
            this.state = state;
        }

        public CalendarEvents.CalendarEvent getEvent() {
            return event;
        }

        public void setEvent(CalendarEvents.CalendarEvent event) {
            this.event = event;
        }
    }

    private enum EventState {
        NOT_SYNCED, SYNCED, NEEDS_UPDATE, NEEDS_DELETE
    }

    public CalendarReceiver() {
        LOG.info("Created calendar receiver.");
        Context context = GBApplication.getContext();
        Intent intent = new Intent("CALENDAR_SYNC");
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("CALENDAR_SYNC"), 0);
        AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 10000, AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("Syncing with calendar.");
        List<CalendarEvents.CalendarEvent> eventList = (new CalendarEvents()).getCalendarEventList(GBApplication.getContext());
        Hashtable<Long,CalendarEvents.CalendarEvent> eventTable = new Hashtable<>();

        for (CalendarEvents.CalendarEvent e: eventList) {
            eventTable.put(e.getId(), e);
            if (!eventState.containsKey(e.getId())) {
                eventState.put(e.getId(), new EventSyncState(e, EventState.NOT_SYNCED));
            }
        }

        Enumeration<Long> ids = eventState.keys();
        while (ids.hasMoreElements()) {
            Long i = ids.nextElement();
            EventSyncState es = eventState.get(i);
            if (eventTable.containsKey(i)) {
                if (es.getState() == EventState.SYNCED) {
                    if (!es.getEvent().equals(eventTable.get(i))) {
                        eventState.put(i, new EventSyncState(eventTable.get(i), EventState.NEEDS_UPDATE));
                    }
                }
            } else {
                if (es.getState() == EventState.NOT_SYNCED) {
                    eventState.remove(i);
                } else {
                    es.setState(EventState.NEEDS_DELETE);
                    eventState.put(i, es);
                }
            }
        }

        updateEvents();
    }

    private void updateEvents() {
        Enumeration<Long> ids = eventState.keys();
        while (ids.hasMoreElements()) {
            Long i = ids.nextElement();
            EventSyncState es = eventState.get(i);
            if ((es.getState() == EventState.NOT_SYNCED) || (es.getState() == EventState.NEEDS_UPDATE)) {
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.title = es.getEvent().getTitle();
                calendarEventSpec.id = i;
                calendarEventSpec.timestamp = es.getEvent().getBeginSeconds();
                calendarEventSpec.description = es.getEvent().getDescription();
                calendarEventSpec.type = CalendarEventSpec.TYPE_UNKNOWN;
                GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
                es.setState(EventState.SYNCED);
                eventState.put(i, es);
            } else if (es.getState() == EventState.NEEDS_DELETE) {
                GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                eventState.remove(i);
            }
        }
    }
}
