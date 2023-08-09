/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ams;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

import java.nio.charset.StandardCharsets;

public class AmsEntityAttribute {
    public static final int PLAYER_ATTRIBUTE_NAME = 0;
    public static final int PLAYER_ATTRIBUTE_PLAYBACK_INFO = 1;
    public static final int PLAYER_ATTRIBUTE_VOLUME = 2;

    public static final int QUEUE_ATTRIBUTE_INDEX = 0;
    public static final int QUEUE_ATTRIBUTE_COUNT = 1;
    public static final int QUEUE_ATTRIBUTE_SHUFFLE_MODE = 2;
    public static final int QUEUE_ATTRIBUTE_REPEAT_MODE = 3;

    public static final int TRACK_ATTRIBUTE_ARTIST = 0;
    public static final int TRACK_ATTRIBUTE_ALBUM = 1;
    public static final int TRACK_ATTRIBUTE_TITLE = 2;
    public static final int TRACK_ATTRIBUTE_DURATION = 3;

    public final AmsEntity entity;
    public final int attributeID;
    public final int updateFlags;
    public final byte[] value;

    public AmsEntityAttribute(AmsEntity entity, int attributeID, int updateFlags, String value) {
        this.entity = entity;
        this.attributeID = attributeID;
        this.updateFlags = updateFlags;
        this.value = value.getBytes(StandardCharsets.UTF_8);
        if (this.value.length > 255) throw new IllegalArgumentException("Too long value");
    }

    public void writeToMessage(MessageWriter writer) {
        writer.writeByte(entity.ordinal());
        writer.writeByte(attributeID);
        writer.writeByte(updateFlags);
        writer.writeByte(value.length);
        writer.writeBytes(value);
    }
}
