/*  Copyright (C) 2017-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Daniel Hauck

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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncState;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEvents;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CalendarReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarReceiver.class);
    private Hashtable<Long, EventSyncState> eventState = new Hashtable<>();

    private GBDevice mGBDevice;

    private class EventSyncState {
        private int state;
        private CalendarEvents.CalendarEvent event;

        EventSyncState(CalendarEvents.CalendarEvent event, int state) {
            this.state = state;
            this.event = event;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public CalendarEvents.CalendarEvent getEvent() {
            return event;
        }

        public void setEvent(CalendarEvents.CalendarEvent event) {
            this.event = event;
        }
    }

    private static class EventState {
        private static final int NOT_SYNCED = 0;
        private static final int SYNCED = 1;
        private static final int NEEDS_UPDATE = 2;
        private static final int NEEDS_DELETE = 3;
    }

    public CalendarReceiver(GBDevice gbDevice) {
        LOG.info("Created calendar receiver.");
        mGBDevice = gbDevice;
        onReceive(GBApplication.getContext(), new Intent());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("got calendar changed broadcast");
        List<CalendarEvents.CalendarEvent> eventList = (new CalendarEvents()).getCalendarEventList(GBApplication.getContext());
        syncCalendar(eventList);
    }

    public void syncCalendar(List<CalendarEvents.CalendarEvent> eventList) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            syncCalendar(eventList, session);
        } catch (Exception e1) {
            GB.toast("Database Error while syncing Calendar", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    public void syncCalendar(List<CalendarEvents.CalendarEvent> eventList, DaoSession session) {
        LOG.info("Syncing with calendar.");
        Hashtable<Long, CalendarEvents.CalendarEvent> eventTable = new Hashtable<>();
        Long deviceId = DBHelper.getDevice(mGBDevice, session).getId();
        QueryBuilder<CalendarSyncState> qb = session.getCalendarSyncStateDao().queryBuilder();


        for (CalendarEvents.CalendarEvent e : eventList) {
            long id = e.getId();
            eventTable.put(id, e);
            if (!eventState.containsKey(e.getId())) {
                qb = session.getCalendarSyncStateDao().queryBuilder();

                CalendarSyncState calendarSyncState = qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId), CalendarSyncStateDao.Properties.CalendarEntryId.eq(id)))
                        .build().unique();
                if (calendarSyncState == null) {
                    eventState.put(id, new EventSyncState(e, EventState.NOT_SYNCED));
                    LOG.info("event id=" + id + " is yet unknown to device id=" + deviceId);
                } else if (calendarSyncState.getHash() == e.hashCode()) {
                    eventState.put(id, new EventSyncState(e, EventState.SYNCED));
                    LOG.info("event id=" + id + " is up to date on device id=" + deviceId);
                }
                else {
                    eventState.put(id, new EventSyncState(e, EventState.NEEDS_UPDATE));
                    LOG.info("event id=" + id + " is not up to date on device id=" + deviceId);
                }
            }
        }

        // add all missing calendar ids on the device to sync status (so that they are deleted later)
        List<CalendarSyncState> CalendarSyncStateList = qb.where(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (CalendarSyncState CalendarSyncState : CalendarSyncStateList) {
            if (!eventState.containsKey(CalendarSyncState.getCalendarEntryId())) {
                eventState.put(CalendarSyncState.getCalendarEntryId(), new EventSyncState(null, EventState.NEEDS_DELETE));
                LOG.info("insert null event for orphanded calendar id=" + CalendarSyncState.getCalendarEntryId() + " for device=" + mGBDevice.getName());
            }
        }

        Enumeration<Long> ids = eventState.keys();
        while (ids.hasMoreElements()) {
            qb = session.getCalendarSyncStateDao().queryBuilder();
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
                    // delete for current device only
                    qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId), CalendarSyncStateDao.Properties.CalendarEntryId.eq(i)))
                            .buildDelete().executeDeleteWithoutDetachingEntities();
                    eventState.remove(i);
                } else {
                    es.setState(EventState.NEEDS_DELETE);
                    eventState.put(i, es);
                }
            }
            updateEvents(deviceId, session);
        }
    }

    private void updateEvents(Long deviceId, DaoSession session) {
        Enumeration<Long> ids = eventState.keys();
        while (ids.hasMoreElements()) {
            Long i = ids.nextElement();
            EventSyncState es = eventState.get(i);
            int syncState = es.getState();
            if (syncState == EventState.NOT_SYNCED || syncState == EventState.NEEDS_UPDATE) {
                CalendarEvents.CalendarEvent calendarEvent = es.getEvent();
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = i;
                calendarEventSpec.title = calendarEvent.getTitle();
                calendarEventSpec.allDay = calendarEvent.isAllDay();
                calendarEventSpec.timestamp = calendarEvent.getBeginSeconds();
                calendarEventSpec.durationInSeconds = calendarEvent.getDurationSeconds(); //FIXME: leads to problems right now
                if (calendarEvent.isAllDay()) {
                    //force the all day events to begin at midnight and last a whole day
                    Calendar c = GregorianCalendar.getInstance();
                    c.setTimeInMillis(calendarEvent.getBegin());
                    c.set(Calendar.HOUR, 0);
                    calendarEventSpec.timestamp = (int) (c.getTimeInMillis() / 1000);
                    calendarEventSpec.durationInSeconds = 24 * 60 * 60;
                }
                calendarEventSpec.description = calendarEvent.getDescription();
                calendarEventSpec.location = calendarEvent.getLocation();
                calendarEventSpec.type = CalendarEventSpec.TYPE_UNKNOWN;
                if (syncState == EventState.NEEDS_UPDATE) {
                    GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                }
                GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
                es.setState(EventState.SYNCED);
                eventState.put(i, es);
                // update db
                session.insertOrReplace(new CalendarSyncState(null, deviceId, i, es.event.hashCode()));
            } else if (syncState == EventState.NEEDS_DELETE) {
                GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                eventState.remove(i);
                // delete from db for current device only
                QueryBuilder<CalendarSyncState> qb = session.getCalendarSyncStateDao().queryBuilder();
                qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId), CalendarSyncStateDao.Properties.CalendarEntryId.eq(i)))
                        .buildDelete().executeDeleteWithoutDetachingEntities();
            }
        }
    }
}
