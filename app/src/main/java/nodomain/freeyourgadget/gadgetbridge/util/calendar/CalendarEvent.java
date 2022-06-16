/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import java.util.Objects;

public class CalendarEvent {
    private long begin;
    private long end;
    private long id;
    private String title;
    private String description;
    private String location;
    private String calName;
    private String calAccountName;
    private int color;
    private boolean allDay;

    public CalendarEvent(long begin, long end, long id, String title, String description, String location, String calName, String calAccountName, int color, boolean allDay) {
        this.begin = begin;
        this.end = end;
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.calName = calName;
        this.calAccountName = calAccountName;
        this.color = color;
        this.allDay = allDay;
    }

    public long getBegin() {
        return begin;
    }

    public int getBeginSeconds() {
        return (int) (begin / 1000);
    }

    public long getEnd() {
        return end;
    }

    public long getDuration() {
        return end - begin;
    }

    public int getDurationSeconds() {
        return (int) ((getDuration()) / 1000);
    }

    public short getDurationMinutes() {
        return (short) (getDurationSeconds() / 60);
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

    public String getCalAccountName() {
        return calAccountName;
    }

    public String getUniqueCalName() {
        return getCalAccountName() + '/' + getCalName();
    }

    public int getColor() {
        return color;
    }

    public boolean isAllDay() {
        return allDay;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CalendarEvent) {
            CalendarEvent e = (CalendarEvent) other;
            return (this.getId() == e.getId()) &&
                    Objects.equals(this.getTitle(), e.getTitle()) &&
                    (this.getBegin() == e.getBegin()) &&
                    Objects.equals(this.getLocation(), e.getLocation()) &&
                    Objects.equals(this.getDescription(), e.getDescription()) &&
                    (this.getEnd() == e.getEnd()) &&
                    Objects.equals(this.getCalName(), e.getCalName()) &&
                    Objects.equals(this.getCalAccountName(), e.getCalAccountName()) &&
                    (this.getColor() == e.getColor()) &&
                    (this.isAllDay() == e.isAllDay());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = (int) id;
        result = 31 * result + Objects.hash(title);
        result = 31 * result + Long.valueOf(begin).hashCode();
        result = 31 * result + Objects.hash(location);
        result = 31 * result + Objects.hash(description);
        result = 31 * result + Long.valueOf(end).hashCode();
        result = 31 * result + Objects.hash(calName);
        result = 31 * result + Objects.hash(calAccountName);
        result = 31 * result + Integer.valueOf(color).hashCode();
        result = 31 * result + Boolean.valueOf(allDay).hashCode();
        return result;
    }
}
