/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.provider.ContactsContract;
import android.text.format.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
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
            Instances.ORGANIZER,
            Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            Instances.CALENDAR_COLOR,
            Instances.ALL_DAY,
            Instances.EVENT_ID, //needed for reminders
            CalendarContract.Calendars.ACCOUNT_TYPE,
            Instances.CALENDAR_ID,
            Instances.RRULE
    };

    private final String deviceAddress;
    private final Context mContext;

    public CalendarManager(final Context context, final String deviceAddress) {
        this.mContext = context;
        this.deviceAddress = deviceAddress;

        loadCalendarsBlackList();
    }

    public List<CalendarEvent> getCalendarEventList() {
        loadCalendarsBlackList();

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);
        final Prefs prefs = new Prefs(sharedPrefs);
        int lookaheadDays = Math.max(1, prefs.getInt("calendar_lookahead_days", 7));
        final List<CalendarEvent> calendarEventList = getCalendarEvents(lookaheadDays);
        if (sharedPrefs.getBoolean("sync_birthdays", false)) {
            calendarEventList.addAll(getBirthdays(lookaheadDays));
            calendarEventList.sort(Comparator.comparingInt(CalendarEvent::getBeginSeconds));
        }
        return calendarEventList;
    }

    public List<CalendarEvent> getCalendarEvents(final int lookaheadDays) {
        final List<CalendarEvent> calendarEventList = new ArrayList<>();

        Calendar cal = GregorianCalendar.getInstance();
        long dtStart = cal.getTimeInMillis();
        cal.add(Calendar.DATE, lookaheadDays);
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
                long start = evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.BEGIN));
                long end = evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.END));
                if (end == 0) {
                    LOG.info("no end time, will parse duration string");
                    Time time = new Time(); //FIXME: deprecated FTW
                    time.parse(evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.DURATION)));
                    end = start + time.toMillis(false);
                }

                CalendarEvent calEvent = new CalendarEvent(
                        start,
                        end,
                        evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances._ID)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.TITLE)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.DESCRIPTION)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.EVENT_LOCATION)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_DISPLAY_NAME)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)),
                        evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_COLOR)),
                        !evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.ALL_DAY)).equals("0"),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.ORGANIZER)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_ID)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.RRULE))
                );

                // Query reminders for this event
                final Cursor reminderCursor = mContext.getContentResolver().query(
                        CalendarContract.Reminders.CONTENT_URI,
                        null,
                        CalendarContract.Reminders.EVENT_ID + " = ?",
                        new String[]{String.valueOf(evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.EVENT_ID)))},
                        null
                );

                if (reminderCursor != null && reminderCursor.getCount() > 0) {
                    final List<Long> reminders = new ArrayList<>();
                    while (reminderCursor.moveToNext()) {
                        int minutes = reminderCursor.getInt(reminderCursor.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES));
                        int method = reminderCursor.getInt(reminderCursor.getColumnIndexOrThrow(CalendarContract.Reminders.METHOD));
                        LOG.debug("Reminder Method: {}, Minutes: {}", method, minutes);

                        if (method == 1) //METHOD_ALERT
                            reminders.add(calEvent.getBegin() - minutes * 60 * 1000L);

                    }
                    reminderCursor.close();

                    calEvent.setRemindersAbsoluteTs(reminders);
                }

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

    public List<CalendarEvent> getBirthdays(final int lookaheadDays) {
        final String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.DISPLAY_NAME
        };
        final String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                ContactsContract.CommonDataKinds.Event.TYPE + " = ?";
        final String[] selectionArgs = new String[]{
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                String.valueOf(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
        };
        final List<CalendarEvent> birthdays = new LinkedList<>();
        final LocalDate maxDate = LocalDate.now().plusDays(lookaheadDays);

        try (Cursor birthdayCursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, ContactsContract.CommonDataKinds.Event.START_DATE + " ASC")) {
            if (birthdayCursor == null || birthdayCursor.getCount() == 0) {
                return birthdays;
            }
            while (birthdayCursor.moveToNext()) {
                final String contactId = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.CONTACT_ID));
                final String birthdayStr = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE));
                final String displayName = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME));
                final LocalDate birthday = parseBirthday(birthdayStr);
                if (birthday == null || birthday.isAfter(maxDate)) {
                    continue;
                }

                birthdays.add(new CalendarEvent(
                        DateTimeUtils.dayStart(birthday).getTime(),
                        DateTimeUtils.dayStart(birthday).getTime() + 86400000L - 1L,
                        contactId.hashCode(),
                        mContext.getString(R.string.contact_birthday, displayName),
                        null,
                        null,
                        mContext.getString(R.string.birthdays),
                        mContext.getString(R.string.pref_contacts_title),
                        0,
                        true,
                        null,
                        CalendarContract.ACCOUNT_TYPE_LOCAL,
                        null,
                        null
                ));
            }
        } catch (final Exception e) {
            LOG.error("could not query birthdays, permission denied?", e);
        }
        return birthdays;
    }

    private LocalDate parseBirthday(final String birthdayStr) {
        final LocalDate birthday;
        final LocalDate now = LocalDate.now();

        try {
            if (birthdayStr.startsWith("--")) {
                // MM-DD
                final String monthDay = birthdayStr.substring(2);
                birthday = LocalDate.parse(now.getYear() + "-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else {
                birthday = LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE).withYear(now.getYear());
            }
        } catch (final DateTimeParseException e) {
            LOG.error("Failed to parse birthday {}", birthdayStr, e);
            return null;
        }

        if (birthday.isAfter(now) || birthday.isEqual(now)) {
            return birthday;
        }

        return birthday.plusYears(1);
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
            LOG.info("Blacklisted calendar {}", calendarUniqueName);
            saveCalendarsBlackList();
        } else {
            LOG.warn("Calendar {} already blacklisted!", calendarUniqueName);
        }
    }

    public void removeFromCalendarBlacklist(String calendarUniqueName) {
        calendars_blacklist.remove(calendarUniqueName);
        LOG.info("Unblacklisted calendar {}", calendarUniqueName);
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
