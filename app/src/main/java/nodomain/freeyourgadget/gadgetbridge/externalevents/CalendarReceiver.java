/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Daniel Hauck, Gabriele Monaco, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;


import static androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.Hashtable;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncState;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CalendarReceiver extends ContentObserver {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarReceiver.class);

    private static final String ACTION_FORCE_SYNC = "FORCE_CALENDAR_SYNC";

    private final Hashtable<Long, EventSyncState> eventState = new Hashtable<>();

    private final Context mContext;
    private final GBDevice mGBDevice;
    private final Handler mSyncHandler;
    private final BroadcastReceiver mForceSyncReceiver;

    private static class EventSyncState {
        private int state;
        private final CalendarEvent event;

        EventSyncState(CalendarEvent event, int state) {
            this.state = state;
            this.event = event;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public CalendarEvent getEvent() {
            return event;
        }
    }

    private static class EventState {
        private static final int NOT_SYNCED = 0;
        private static final int SYNCED = 1;
        private static final int NEEDS_UPDATE = 2;
        private static final int NEEDS_DELETE = 3;
    }

    public CalendarReceiver(final Context context, final GBDevice gbDevice) {
        super(new Handler());
        LOG.info("Created calendar receiver");
        mContext = context;
        mGBDevice = gbDevice;
        mSyncHandler = new Handler();
        mForceSyncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                LOG.info("Got force sync: {}", intent.getAction());
                scheduleSync();
            }
        };
    }

    public GBDevice getGBDevice() {
        return mGBDevice;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        LOG.info("Got calendar change: {}", uri);
        scheduleSync();
    }

    public void scheduleSync() {
        LOG.debug("Scheduling calendar sync");
        mSyncHandler.removeCallbacksAndMessages(null);
        mSyncHandler.postDelayed(this::syncCalendar, 2500L);
    }

    public void syncCalendar() {
        List<CalendarEvent> eventList = (new CalendarManager(mContext, mGBDevice.getAddress())).getCalendarEventList();
        LOG.debug("Syncing {} calendar events", eventList.size());
        syncCalendar(eventList);
    }

    public void syncCalendar(List<CalendarEvent> eventList) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            syncCalendar(eventList, session);
        } catch (Exception e1) {
            GB.toast("Database Error while syncing Calendar", Toast.LENGTH_SHORT, GB.ERROR, e1);
        }
    }

    public void syncCalendar(List<CalendarEvent> eventList, DaoSession session) {
        LOG.info("Syncing with calendar.");
        Hashtable<Long, CalendarEvent> eventTable = new Hashtable<>();
        Long deviceId = DBHelper.getDevice(mGBDevice, session).getId();
        QueryBuilder<CalendarSyncState> qb = session.getCalendarSyncStateDao().queryBuilder();

        for (CalendarEvent e : eventList) {
            long id = e.getId();
            eventTable.put(id, e);
            if (!eventState.containsKey(e.getId())) {
                qb = session.getCalendarSyncStateDao().queryBuilder();

                CalendarSyncState calendarSyncState = qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId), CalendarSyncStateDao.Properties.CalendarEntryId.eq(id)))
                        .build().unique();
                if (calendarSyncState == null) {
                    eventState.put(id, new EventSyncState(e, EventState.NOT_SYNCED));
                    LOG.info("event id={} is yet unknown to device id={}", id, deviceId);
                } else if (calendarSyncState.getHash() == e.hashCode()) {
                    eventState.put(id, new EventSyncState(e, EventState.SYNCED));
                    LOG.info("event id={} is up to date on device id={}", id, deviceId);
                } else {
                    eventState.put(id, new EventSyncState(e, EventState.NEEDS_UPDATE));
                    LOG.info("event id={} is not up to date on device id={}", id, deviceId);
                }
            }
        }

        // add all missing calendar ids on the device to sync status (so that they are deleted later)
        List<CalendarSyncState> CalendarSyncStateList = qb.where(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (CalendarSyncState CalendarSyncState : CalendarSyncStateList) {
            if (!eventState.containsKey(CalendarSyncState.getCalendarEntryId())) {
                eventState.put(CalendarSyncState.getCalendarEntryId(), new EventSyncState(null, EventState.NEEDS_DELETE));
                LOG.info("insert null event for orphaned calendar id={} for device={}", CalendarSyncState.getCalendarEntryId(), mGBDevice.getName());
            }
        }

        Enumeration<Long> ids = eventState.keys();
        while (ids.hasMoreElements()) {
            qb = session.getCalendarSyncStateDao().queryBuilder();
            Long i = ids.nextElement();
            EventSyncState es = eventState.get(i);
            if (es == null) {
                LOG.error("Failed to get event state for {}", i);
                continue;
            }
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
            if (es == null) {
                LOG.error("Failed to get event state {} for sync", i);
                continue;
            }
            int syncState = es.getState();
            if (syncState == EventState.NOT_SYNCED || syncState == EventState.NEEDS_UPDATE) {
                CalendarEvent calendarEvent = es.getEvent();
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = i;
                calendarEventSpec.title = calendarEvent.getTitle();
                calendarEventSpec.allDay = calendarEvent.isAllDay();
                calendarEventSpec.reminders = new ArrayList<>(calendarEvent.getRemindersAbsoluteTs());
                calendarEventSpec.timestamp = calendarEvent.getBeginSeconds();
                calendarEventSpec.durationInSeconds = calendarEvent.getDurationSeconds(); //FIXME: leads to problems right now
                if (calendarEvent.isAllDay()) {
                    // As per the CalendarContract, for all-day events, the start timestamp is always in UTC
                    // and corresponds to the midnight boundary
                    final int numDays = (int) TimeUnit.DAYS.convert(
                            calendarEvent.getEnd() - calendarEvent.getBegin(),
                            TimeUnit.MILLISECONDS
                    );
                    calendarEventSpec.durationInSeconds = 24 * 60 * 60 * numDays;
                }
                calendarEventSpec.description = calendarEvent.getDescription();
                calendarEventSpec.location = calendarEvent.getLocation();
                calendarEventSpec.type = CalendarEventSpec.TYPE_UNKNOWN;
                calendarEventSpec.calName = calendarEvent.getUniqueCalName();
                calendarEventSpec.color = calendarEvent.getColor();
                if (syncState == EventState.NEEDS_UPDATE) {
                    GBApplication.deviceService(mGBDevice).onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                }
                GBApplication.deviceService(mGBDevice).onAddCalendarEvent(calendarEventSpec);
                es.setState(EventState.SYNCED);
                eventState.put(i, es);
                // update db
                session.insertOrReplace(new CalendarSyncState(null, deviceId, i, es.event.hashCode()));
            } else if (syncState == EventState.NEEDS_DELETE) {
                GBApplication.deviceService(mGBDevice).onDeleteCalendarEvent(CalendarEventSpec.TYPE_UNKNOWN, i);
                eventState.remove(i);
                // delete from db for current device only
                QueryBuilder<CalendarSyncState> qb = session.getCalendarSyncStateDao().queryBuilder();
                qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId), CalendarSyncStateDao.Properties.CalendarEntryId.eq(i)))
                        .buildDelete().executeDeleteWithoutDetachingEntities();
            }
        }
    }

    public void registerBroadcastReceivers() {
        mContext.getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this);
        // Add a receiver to allow us to quickly force as calendar sync (without having to provide data)
        ContextCompat.registerReceiver(mContext, mForceSyncReceiver, new IntentFilter(ACTION_FORCE_SYNC), RECEIVER_NOT_EXPORTED);
    }

    public void dispose() {
        mContext.getContentResolver().unregisterContentObserver(this);
        mContext.unregisterReceiver(mForceSyncReceiver);
        mSyncHandler.removeCallbacksAndMessages(null);
    }

    public static void forceSync() {
        final Intent intent = new Intent(ACTION_FORCE_SYNC);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        GBApplication.getContext().sendBroadcast(intent);
    }
}
