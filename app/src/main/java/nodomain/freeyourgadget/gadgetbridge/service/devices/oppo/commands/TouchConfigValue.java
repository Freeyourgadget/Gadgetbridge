/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands;

import androidx.annotation.Nullable;

public enum TouchConfigValue {
    OFF(0x00),
    PLAY_PAUSE(0x01),
    VOICE_ASSISTANT(0x03), // oppo
    VOICE_ASSISTANT_REALME(0x04),
    PREVIOUS(0x05),
    NEXT(0x06),
    GAME_MODE(0x11),
    VOLUME_UP(0x0B),
    VOLUME_DOWN(0x0C),
    ;

    private final int code;

    TouchConfigValue(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static TouchConfigValue fromCode(final int code) {
        for (final TouchConfigValue param : TouchConfigValue.values()) {
            if (param.code == code) {
                return param;
            }
        }

        return null;
    }
}
