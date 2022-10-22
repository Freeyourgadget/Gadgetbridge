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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v2;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;

public enum PayloadTypeV2 {
    AUDIO_CODEC_REQUEST(MessageType.COMMAND_1, 0x13), // TODO confirm
    AUDIO_CODEC_REPLY(MessageType.COMMAND_1, 0x14), // TODO confirm
    AUDIO_CODEC_NOTIFY(MessageType.COMMAND_1, 0x15),

    BATTERY_LEVEL_REQUEST(MessageType.COMMAND_1, 0x22),
    BATTERY_LEVEL_REPLY(MessageType.COMMAND_1, 0x23),
    BATTERY_LEVEL_NOTIFY(MessageType.COMMAND_1, 0x24), // TODO confirm

    AUTOMATIC_POWER_OFF_GET(MessageType.COMMAND_1, 0x26),
    AUTOMATIC_POWER_OFF_RET(MessageType.COMMAND_1, 0x27),
    AUTOMATIC_POWER_OFF_SET(MessageType.COMMAND_1, 0x28),
    AUTOMATIC_POWER_OFF_NOTIFY(MessageType.COMMAND_1, 0x29),

    UNKNOWN(MessageType.UNKNOWN, 0xff);

    private final MessageType messageType;
    private final byte code;

    PayloadTypeV2(final MessageType messageType, final int code) {
        this.messageType = messageType;
        this.code = (byte) code;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public byte getCode() {
        return this.code;
    }

    public static PayloadTypeV2 fromCode(final MessageType messageType, final byte code) {
        for (final PayloadTypeV2 payloadType : values()) {
            if (messageType.equals(payloadType.messageType) && payloadType.code == code) {
                return payloadType;
            }
        }

        return PayloadTypeV2.UNKNOWN;
    }
}
