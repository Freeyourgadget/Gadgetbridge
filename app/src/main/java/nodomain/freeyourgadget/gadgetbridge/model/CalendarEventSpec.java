/*  Copyright (C) 2016-2017 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.model;

public class CalendarEventSpec {
    public static final byte TYPE_UNKNOWN = 0;
    public static final byte TYPE_SUNRISE = 1;
    public static final byte TYPE_SUNSET = 2;

    public byte type;
    public long id;
    public int timestamp;
    public int durationInSeconds;
    public String title;
    public String description;
}
