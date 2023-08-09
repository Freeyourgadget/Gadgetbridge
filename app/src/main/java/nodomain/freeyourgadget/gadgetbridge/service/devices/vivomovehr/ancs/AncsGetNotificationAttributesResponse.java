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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AncsGetNotificationAttributesResponse {
    public final byte[] packet;

    public AncsGetNotificationAttributesResponse(int notificationUID, Map<AncsAttribute, String> attributes) {
        final MessageWriter messageWriter = new MessageWriter();
        messageWriter.writeByte(AncsCommand.GET_NOTIFICATION_ATTRIBUTES.code);
        messageWriter.writeInt(notificationUID);
        for(Map.Entry<AncsAttribute, String> attribute : attributes.entrySet()) {
            messageWriter.writeByte(attribute.getKey().code);
            final byte[] bytes = attribute.getValue().getBytes(StandardCharsets.UTF_8);
            messageWriter.writeShort(bytes.length);
            messageWriter.writeBytes(bytes);
        }
        this.packet = messageWriter.getBytes();
    }
}
