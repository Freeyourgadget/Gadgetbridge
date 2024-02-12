/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params;

public enum VirtualSoundParam {
    SURROUND_MODE(0x01),
    SOUND_POSITION(0x02),
    ;

    private final byte code;

    VirtualSoundParam(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

    public static VirtualSoundParam fromCode(final byte code) {
        for (final VirtualSoundParam virtualSound : values()) {
            if (virtualSound.code == code) {
                return virtualSound;
            }
        }

        return null;
    }
}
