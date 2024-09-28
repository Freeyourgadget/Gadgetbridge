package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CalendarEventTest extends TestBase {
    private static final long BEGIN = 1;
    private static final long END = 2;
    private static final long ID_1 = 100;
    private static final long ID_2 = 101;
    private static final String CALNAME_1 = "cal1";
    private static final String CALACCOUNTNAME_1 = "account1";
    private static final int COLOR_1 = 185489;

    @Test
    public void testHashCode() {
        CalendarEvent c1 =
                new CalendarEvent(BEGIN, END, ID_1, "something", null, null, CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, null, null, null, null);
        CalendarEvent c2 =
                new CalendarEvent(BEGIN, END, ID_1, null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, null, null, null, null);
        CalendarEvent c3 =
                new CalendarEvent(BEGIN, END, ID_1, null, null, "something", CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, null, null, null, null);
        CalendarEvent c4 =
                new CalendarEvent(BEGIN, END, ID_1, null, null, "something", CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, "some", null, null, null);

        assertEquals(c1.hashCode(), c1.hashCode());
        assertNotEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c2.hashCode(), c3.hashCode());
        assertNotEquals(c3.hashCode(), c4.hashCode());
    }


    @Test
    public void testSync() {
        List<CalendarEvent> eventList = new ArrayList<>();
        eventList.add(new CalendarEvent(BEGIN, END, ID_1, null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, null, null, null, null));

        GBDevice dummyGBDevice = createDummyGDevice("00:00:01:00:03");
        dummyGBDevice.setState(GBDevice.State.INITIALIZED);
//        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        CalendarReceiver testCR = new CalendarReceiver(getContext(), dummyGBDevice);

        testCR.syncCalendar(eventList);

        eventList.add(new CalendarEvent(BEGIN, END, ID_2, null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_1, false, null, null, null, null));
        testCR.syncCalendar(eventList);

        CalendarSyncStateDao calendarSyncStateDao = daoSession.getCalendarSyncStateDao();
        assertEquals(2, calendarSyncStateDao.count());
    }

}
