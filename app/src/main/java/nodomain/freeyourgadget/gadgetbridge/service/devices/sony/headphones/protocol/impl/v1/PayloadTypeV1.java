/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;

public enum PayloadTypeV1 {
    INIT_REQUEST(MessageType.COMMAND_1, 0x00),
    INIT_REPLY(MessageType.COMMAND_1, 0x01),

    FW_VERSION_REQUEST(MessageType.COMMAND_1, 0x04),
    FW_VERSION_REPLY(MessageType.COMMAND_1, 0x05),

    INIT_2_REQUEST(MessageType.COMMAND_1, 0x06),
    INIT_2_REPLY(MessageType.COMMAND_1, 0x07),

    BATTERY_LEVEL_REQUEST(MessageType.COMMAND_1, 0x10),
    BATTERY_LEVEL_REPLY(MessageType.COMMAND_1, 0x11),
    BATTERY_LEVEL_NOTIFY(MessageType.COMMAND_1, 0x13),

    AUDIO_CODEC_REQUEST(MessageType.COMMAND_1, 0x18),
    AUDIO_CODEC_REPLY(MessageType.COMMAND_1, 0x19),
    AUDIO_CODEC_NOTIFY(MessageType.COMMAND_1, 0x1b),

    POWER_OFF(MessageType.COMMAND_1, 0x22),

    SOUND_POSITION_OR_MODE_GET(MessageType.COMMAND_1, 0x46),
    SOUND_POSITION_OR_MODE_RET(MessageType.COMMAND_1, 0x47),
    SOUND_POSITION_OR_MODE_SET(MessageType.COMMAND_1, 0x48),
    SOUND_POSITION_OR_MODE_NOTIFY(MessageType.COMMAND_1, 0x49),

    EQUALIZER_GET(MessageType.COMMAND_1, 0x56),
    EQUALIZER_RET(MessageType.COMMAND_1, 0x57),
    EQUALIZER_SET(MessageType.COMMAND_1, 0x58),
    EQUALIZER_NOTIFY(MessageType.COMMAND_1, 0x59),

    AMBIENT_SOUND_CONTROL_GET(MessageType.COMMAND_1, 0x66),
    AMBIENT_SOUND_CONTROL_RET(MessageType.COMMAND_1, 0x67),
    AMBIENT_SOUND_CONTROL_SET(MessageType.COMMAND_1, 0x68),
    AMBIENT_SOUND_CONTROL_NOTIFY(MessageType.COMMAND_1, 0x69),

    VOLUME_GET(MessageType.COMMAND_1, 0xa6),
    VOLUME_RET(MessageType.COMMAND_1, 0xa7),
    VOLUME_SET(MessageType.COMMAND_1, 0xa8),
    VOLUME_NOTIFY(MessageType.COMMAND_1, 0xa9),

    NOISE_CANCELLING_OPTIMIZER_START(MessageType.COMMAND_1, 0x84),
    NOISE_CANCELLING_OPTIMIZER_STATUS(MessageType.COMMAND_1, 0x85),

    NOISE_CANCELLING_OPTIMIZER_STATE_GET(MessageType.COMMAND_1, 0x86),
    NOISE_CANCELLING_OPTIMIZER_STATE_RET(MessageType.COMMAND_1, 0x87),
    NOISE_CANCELLING_OPTIMIZER_STATE_NOTIFY(MessageType.COMMAND_1, 0x89),

    TOUCH_SENSOR_GET(MessageType.COMMAND_1, 0xd6),
    TOUCH_SENSOR_RET(MessageType.COMMAND_1, 0xd7),
    TOUCH_SENSOR_SET(MessageType.COMMAND_1, 0xd8),
    TOUCH_SENSOR_NOTIFY(MessageType.COMMAND_1, 0xd9),

    AUDIO_UPSAMPLING_GET(MessageType.COMMAND_1, 0xe6),
    AUDIO_UPSAMPLING_RET(MessageType.COMMAND_1, 0xe7),
    AUDIO_UPSAMPLING_SET(MessageType.COMMAND_1, 0xe8),
    AUDIO_UPSAMPLING_NOTIFY(MessageType.COMMAND_1, 0xe9),

    AUTOMATIC_POWER_OFF_BUTTON_MODE_GET(MessageType.COMMAND_1, 0xf6),
    AUTOMATIC_POWER_OFF_BUTTON_MODE_RET(MessageType.COMMAND_1, 0xf7),
    AUTOMATIC_POWER_OFF_BUTTON_MODE_SET(MessageType.COMMAND_1, 0xf8),
    AUTOMATIC_POWER_OFF_BUTTON_MODE_NOTIFY(MessageType.COMMAND_1, 0xf9),

    SPEAK_TO_CHAT_CONFIG_GET(MessageType.COMMAND_1, 0xfa),
    SPEAK_TO_CHAT_CONFIG_RET(MessageType.COMMAND_1, 0xfb),
    SPEAK_TO_CHAT_CONFIG_SET(MessageType.COMMAND_1, 0xfc),
    SPEAK_TO_CHAT_CONFIG_NOTIFY(MessageType.COMMAND_1, 0xfd),

    // TODO: The headphones spit out a lot of json, analyze it
    JSON_GET(MessageType.COMMAND_1, 0xc4),
    JSON_RET(MessageType.COMMAND_1, 0xc9),

    // TODO: App sends those sometimes
    SOMETHING_GET(MessageType.COMMAND_1, 0x90),
    SOMETHING_RET(MessageType.COMMAND_1, 0x91),

    VOICE_NOTIFICATIONS_GET(MessageType.COMMAND_2, 0x46),
    VOICE_NOTIFICATIONS_RET(MessageType.COMMAND_2, 0x47),
    VOICE_NOTIFICATIONS_SET(MessageType.COMMAND_2, 0x48),
    VOICE_NOTIFICATIONS_NOTIFY(MessageType.COMMAND_2, 0x49),

    UNKNOWN(MessageType.UNKNOWN, 0xff);

    private final MessageType messageType;
    private final byte code;

    PayloadTypeV1(final MessageType messageType, final int code) {
        this.messageType = messageType;
        this.code = (byte) code;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public byte getCode() {
        return this.code;
    }

    public static PayloadTypeV1 fromCode(final MessageType messageType, final byte code) {
        for (final PayloadTypeV1 payloadType : values()) {
            if (messageType.equals(payloadType.messageType) && payloadType.code == code) {
                return payloadType;
            }
        }

        return PayloadTypeV1.UNKNOWN;
    }
}
