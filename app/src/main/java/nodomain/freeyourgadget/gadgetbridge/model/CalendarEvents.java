package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class CalendarEvents {

    // needed for pebble: time, duration, layout, reminders, actions
    // layout: type, title, subtitle, body (max 512), tinyIcon, smallIcon, largeIcon
    //further: primaryColor, secondaryColor, backgroundColor, headings, paragraphs, lastUpdated
    // taken from: https://developer.getpebble.com/guides/timeline/pin-structure/

    // needed for miband:
    // time

    private static final String[] EVENT_INSTANCE_PROJECTION = new String[] {
            Instances._ID,
            Instances.BEGIN,
            Instances.END,
            Instances.EVENT_ID,
            Instances.TITLE,
            Instances.DESCRIPTION,
            Instances.EVENT_LOCATION,
            Instances.CALENDAR_DISPLAY_NAME
    };

    private static final int lookahead_days = 7;

    private List<CalendarEvent> calendarEventList = new ArrayList<CalendarEvent>();

    public List<CalendarEvent> getCalendarEventList(Context mContext) {
        fetchSystemEvents(mContext);
        return calendarEventList;
    }

    private boolean fetchSystemEvents(Context mContext) {

        Calendar cal = GregorianCalendar.getInstance();
        Long dtStart = cal.getTime().getTime();
        cal.add(Calendar.DATE, lookahead_days);
        Long dtEnd = cal.getTime().getTime();

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, dtStart);
        ContentUris.appendId(eventsUriBuilder, dtEnd);
        Uri eventsUri = eventsUriBuilder.build();

        Cursor evtCursor = null;
        evtCursor = mContext.getContentResolver().query(eventsUri, EVENT_INSTANCE_PROJECTION, null, null, CalendarContract.Instances.DTSTART + " ASC");

        if (evtCursor.moveToFirst()) {
            do {
                CalendarEvent calEvent = new CalendarEvent(
                        evtCursor.getLong(1),
                        evtCursor.getLong(2),
                        evtCursor.getLong(3),
                        evtCursor.getString(4),
                        evtCursor.getString(5),
                        evtCursor.getString(6),
                        evtCursor.getString(7)
                        );
                calendarEventList.add(calEvent);
            } while(evtCursor.moveToNext());

            return true;
        }
        return false;
    }

    public class CalendarEvent {
        private long begin;
        private long end;
        private long id;
        private String title;
        private String description;
        private String location;
        private String calName;

        public CalendarEvent(long begin, long end, long id, String title, String description, String location, String calName) {
            this.begin = begin;
            this.end = end;
            this.id = id;
            this.title = title;
            this.description = description;
            this.location = location;
            this.calName = calName;
        }

        public long getBegin() {
            return begin;
        }

        public long getEnd() {
            return end;
        }

        public long getDuration() {
            return end - begin;
        }


        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        public String getCalName() {
            return calName;
        }

    }
}
