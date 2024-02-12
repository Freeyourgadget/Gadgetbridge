/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class GetNotificationAttributesResponse {
    private byte commandID = 0;
    private int notificationUID;
    private List<NotificationAttribute> attributes = new ArrayList<>();

    public GetNotificationAttributesResponse(int notificationUID) {
        this.notificationUID = notificationUID;
    }

    public void addAttribute(NotificationAttribute attribute) {
        attributes.add(attribute);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(getLength());
        buffer.put(commandID);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(notificationUID);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (NotificationAttribute attribute : attributes) {
            buffer.put(attribute.serialize());
        }
        return buffer.array();
    }

    private int getLength() {
        int length = 5;
        for (NotificationAttribute attribute : attributes) {
            length += attribute.getAttributeLength() + 3;
        }

        return length;
    }
}
