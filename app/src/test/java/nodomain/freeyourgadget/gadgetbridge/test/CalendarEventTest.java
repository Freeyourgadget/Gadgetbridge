package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.model.CalendarEvents;

import static org.junit.Assert.assertNotEquals;

public class CalendarEventTest extends TestBase {
    private static final long BEGIN = 1;
    private static final long END = 2;
    private static final long ID_1 = 100;

    @Test
    public void testHashCode()  {
        CalendarEvents.CalendarEvent c1 = new CalendarEvents.CalendarEvent(BEGIN, END, ID_1, "something", null, null, null,  false);
        CalendarEvents.CalendarEvent c2 = new CalendarEvents.CalendarEvent(BEGIN, END, ID_1, "something", null, null, null,  false);
        CalendarEvents.CalendarEvent c3 = new CalendarEvents.CalendarEvent(BEGIN, END, ID_1, "something", null, null, null,  false);

        assertNotEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c2.hashCode(), c3.hashCode());
    }
}
