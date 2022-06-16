/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
package nodomain.freeyourgadget.gadgetbridge.util.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.text.format.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CalendarManager {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarManager.class);

    // needed for pebble: time, duration, layout, reminders, actions
    // layout: type, title, subtitle, body (max 512), tinyIcon, smallIcon, largeIcon
    //further: primaryColor, secondaryColor, backgroundColor, headings, paragraphs, lastUpdated
    // taken from: https://developer.getpebble.com/guides/timeline/pin-structure/

    // needed for MiBand:
    // time

    private static final String[] EVENT_INSTANCE_PROJECTION = new String[]{
            Instances._ID,

            Instances.BEGIN,
            Instances.END,
            Instances.DURATION,
            Instances.TITLE,
            Instances.DESCRIPTION,
            Instances.EVENT_LOCATION,
            Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            Instances.CALENDAR_COLOR,
            Instances.ALL_DAY
    };

    private static final int lookahead_days = 7;

    private final String deviceAddress;
    private final Context mContext;

    public CalendarManager(final Context context, final String deviceAddress) {
        this.mContext = context;
        this.deviceAddress = deviceAddress;

        loadCalendarsBlackList();
    }

    public List<CalendarEvent> getCalendarEventList() {
        loadCalendarsBlackList();

        final List<CalendarEvent> calendarEventList = new ArrayList<CalendarEvent>();

        Calendar cal = GregorianCalendar.getInstance();
        long dtStart = cal.getTimeInMillis();
        cal.add(Calendar.DATE, lookahead_days);
        long dtEnd = cal.getTimeInMillis();

        Uri.Builder eventsUriBuilder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, dtStart);
        ContentUris.appendId(eventsUriBuilder, dtEnd);
        Uri eventsUri = eventsUriBuilder.build();

        try (Cursor evtCursor = mContext.getContentResolver().query(eventsUri, EVENT_INSTANCE_PROJECTION, null, null, Instances.BEGIN + " ASC")) {
            if (evtCursor == null || evtCursor.getCount() == 0) {
                return calendarEventList;
            }
            while (evtCursor.moveToNext()) {
                long start = evtCursor.getLong(1);
                long end = evtCursor.getLong(2);
                if (end == 0) {
                    LOG.info("no end time, will parse duration string");
                    Time time = new Time(); //FIXME: deprecated FTW
                    time.parse(evtCursor.getString(3));
                    end = start + time.toMillis(false);
                }
                CalendarEvent calEvent = new CalendarEvent(
                        start,
                        end,
                        evtCursor.getLong(0),
                        evtCursor.getString(4),
                        evtCursor.getString(5),
                        evtCursor.getString(6),
                        evtCursor.getString(7),
                        evtCursor.getString(8),
                        evtCursor.getInt(9),
                        !evtCursor.getString(10).equals("0")
                );
                if (!calendarIsBlacklisted(calEvent.getUniqueCalName())) {
                    calendarEventList.add(calEvent);
                } else {
                    LOG.debug("calendar {} skipped because it's blacklisted", calEvent.getUniqueCalName());
                }
            }
            return calendarEventList;
        } catch (final Exception e) {
            LOG.error("could not query calendar, permission denied?", e);
            return calendarEventList;
        }
    }

    private static HashSet<String> calendars_blacklist = null;

    public boolean calendarIsBlacklisted(String calendarUniqueName) {
        if (calendars_blacklist == null) {
            LOG.warn("calendarIsBlacklisted: calendars_blacklist is null!");
        }
        return calendars_blacklist != null && calendars_blacklist.contains(calendarUniqueName);
    }

    public void setCalendarsBlackList(Set<String> calendarNames) {
        if (calendarNames == null) {
            LOG.info("Set null apps_notification_blacklist");
            calendars_blacklist = new HashSet<>();
        } else {
            calendars_blacklist = new HashSet<>(calendarNames);
        }
        LOG.info("New calendars_blacklist has {} entries", calendars_blacklist.size());
        saveCalendarsBlackList();
    }

    public void addCalendarToBlacklist(String calendarUniqueName) {
        if (calendars_blacklist.add(calendarUniqueName)) {
            LOG.info("Blacklisted calendar " + calendarUniqueName);
            saveCalendarsBlackList();
        } else {
            LOG.warn("Calendar {} already blacklisted!", calendarUniqueName);
        }
    }

    public void removeFromCalendarBlacklist(String calendarUniqueName) {
        calendars_blacklist.remove(calendarUniqueName);
        LOG.info("Unblacklisted calendar " + calendarUniqueName);
        saveCalendarsBlackList();
    }

    private void loadCalendarsBlackList() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);

        LOG.info("Loading calendars_blacklist");
        calendars_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.CALENDAR_BLACKLIST, null);
        if (calendars_blacklist == null) {
            calendars_blacklist = new HashSet<>();
        }
        LOG.info("Loaded calendars_blacklist has {} entries", calendars_blacklist.size());
    }

    private void saveCalendarsBlackList() {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);

        LOG.info("Saving calendars_blacklist with {} entries", calendars_blacklist.size());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (calendars_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.CALENDAR_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.CALENDAR_BLACKLIST, calendars_blacklist);
        }
        editor.apply();
    }
}
