/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status;

public enum StatusRequestType {
    BACKGROUND_SYNC_REQUEST(0), // timer?
    GET_CALENDAR(1),
    LOCATE_PHONE(2),
    GET_WEATHER(3),
    RIIIVER_SIDE_BUTTON_ENGAGED(4), // -> 0x0 double click, 0x1 long click
    RIIIVER_HOME_ICON_CLICKED(5), // -> 0x00 0x12, 0x01 0x12, 0x02 0x12 : button no.?
    UNKNOWN1(6), // -> payment related?
    NOTIFICATION_REMOVE_REQUEST(10), // -> followed by int notification id
    MUSIC_INFO_FETCH(11);

    public final int value;
    StatusRequestType(int val) {
        this.value = val;
    }
}
