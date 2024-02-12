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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol;

public enum MessageType {
    ACK(0x01),
    COMMAND_1(0x0c),
    COMMAND_2(0x0e),

    UNKNOWN(0xff);

    private final byte code;

    MessageType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

    public static MessageType fromCode(final byte code) {
        for (final MessageType messageType : values()) {
            if (messageType.code == code) {
                return messageType;
            }
        }

        return MessageType.UNKNOWN;
    }
}
