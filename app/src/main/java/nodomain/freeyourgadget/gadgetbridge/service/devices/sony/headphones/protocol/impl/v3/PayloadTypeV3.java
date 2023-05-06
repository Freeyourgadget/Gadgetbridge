/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v3;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;

public enum PayloadTypeV3 {
    AMBIENT_SOUND_CONTROL_BUTTON_MODE_GET(MessageType.COMMAND_1, 0xfa),
    AMBIENT_SOUND_CONTROL_BUTTON_MODE_RET(MessageType.COMMAND_1, 0xfb),
    AMBIENT_SOUND_CONTROL_BUTTON_MODE_SET(MessageType.COMMAND_1, 0xfc),
    AMBIENT_SOUND_CONTROL_BUTTON_MODE_NOTIFY(MessageType.COMMAND_1, 0xfd),

    UNKNOWN(MessageType.UNKNOWN, 0xff);

    private final MessageType messageType;
    private final byte code;

    PayloadTypeV3(final MessageType messageType, final int code) {
        this.messageType = messageType;
        this.code = (byte) code;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public byte getCode() {
        return this.code;
    }

    public static PayloadTypeV3 fromCode(final MessageType messageType, final byte code) {
        for (final PayloadTypeV3 payloadType : values()) {
            if (messageType.equals(payloadType.messageType) && payloadType.code == code) {
                return payloadType;
            }
        }

        return PayloadTypeV3.UNKNOWN;
    }
}
