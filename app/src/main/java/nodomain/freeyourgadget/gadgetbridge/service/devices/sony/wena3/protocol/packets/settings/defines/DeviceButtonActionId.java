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

public enum DeviceButtonActionId {
    NONE(0),
    ALEXA(1),
    QRIO(2),
    QRIO_UNLOCK(258),
    QRIO_LOCK(514),
    FIND_PHONE(3),
    TOGGLE_NFC(4),
    TOGGLE_SILENT(5),
    START_TIMER(262),
    TIMER(6),
    WEATHER(7),
    WENA_PAY(8),
    SCHEDULE(9),
    ACTIVITY_SCREEN(10),
    NOTIFICATION_SCREEN(11),
    SUICA(12),
    EDY(13),
    ALARM(14),
    RIIIVER(15),
    MUSIC(16),
    TOGGLE_MUSIC(272),
    MUSIC_NEXT(528),
    MUSIC_PREV(784);


    public final short value;

    DeviceButtonActionId(int val) {
        this.value = (short) val;
    }
}
