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

// This is done via a class to be able to init the value from an arbitrary int (e.g. from prefs)
// Whether listing all cases is needed is a good concern, they are essentially unused because
// the settings dialog uses a string-array instead...
public enum HomeIconId {
    TIMER(256),
    ALARM(512),
    CLOCK(768),
    ALEXA(1024),
    WENA_PAY(1280),
    QRIO_LOCK(1536),
    EDY(1792),
    NOTIFICATION_COUNT(2048),
    SCHEDULE(2304),
    PEDOMETER(2560),
    SLEEP(2816),
    HEART_RATE(3072),
    VO2MAX(3328),
    STRESS(3584),
    ENERGY(3840),

    SUICA(4096),
    CALORIES(4352),
    RIIIVER(4608),
    MUSIC(4864),
    CAMERA(5120);

    public final short value;

    HomeIconId(final int value) {
        this.value = (short) value;
    }
}
