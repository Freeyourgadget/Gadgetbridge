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

public enum AudioCodec {
    UNKNOWN(0x00),
    SBC(0x01),
    AAC(0x02),
    LDAC(0x10),
    APTX(0x20),
    APTX_HD(0x21);

    private final byte code;

    AudioCodec(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

    public static AudioCodec fromCode(final byte code) {
        for (final AudioCodec audioCodec : values()) {
            if (audioCodec.code == code) {
                return audioCodec;
            }
        }

        return null;
    }
}
