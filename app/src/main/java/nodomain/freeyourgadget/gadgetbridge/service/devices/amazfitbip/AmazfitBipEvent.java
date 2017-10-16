/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip;


public class AmazfitBipEvent {
    public static final byte FELL_ASLEEP = 0x01;
    public static final byte WOKE_UP = 0x02;
    public static final byte STEPSGOAL_REACHED = 0x03;
    public static final byte BUTTON_PRESSED = 0x04;
    public static final byte START_NONWEAR = 0x06;
    public static final byte CALL_REJECT = 0x07;
    public static final byte CALL_ACCEPT = 0x09;
    public static final byte ALARM_TOGGLED = 0x0a;
    public static final byte BUTTON_PRESSED_LONG = 0x0b;
    public static final byte TICK_30MIN = 0x0e; // unsure
}
