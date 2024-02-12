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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines;

public enum MenuIconId {
    NONE(0),
    TIMER(1),
    ALARM(2),
    FIND_PHONE(3),
    ALEXA(4),
    PAYMENT(5),
    QRIO(6),
    WEATHER(7),
    MUSIC(8),
    CAMERA(9);

    public final byte value;

    MenuIconId(int value) {
        this.value = (byte) value;
    }
}
