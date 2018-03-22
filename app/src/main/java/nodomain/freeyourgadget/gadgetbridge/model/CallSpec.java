/*  Copyright (C) 2016-2018 Andreas Shimokawa

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

public class CallSpec {
    public static final int CALL_UNDEFINED = 1;
    public static final int CALL_ACCEPT = 1;
    public static final int CALL_INCOMING = 2;
    public static final int CALL_OUTGOING = 3;
    public static final int CALL_REJECT = 4;
    public static final int CALL_START = 5;
    public static final int CALL_END = 6;

    public String number;
    public String name;
    public int command;
}
