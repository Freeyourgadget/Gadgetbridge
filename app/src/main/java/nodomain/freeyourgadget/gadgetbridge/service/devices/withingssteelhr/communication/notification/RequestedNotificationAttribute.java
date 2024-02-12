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

public class RequestedNotificationAttribute {
    private byte attributeID;
    private short attributeMaxLength;

    public byte getAttributeID() {
        return attributeID;
    }

    public void setAttributeID(byte attributeID) {
        this.attributeID = attributeID;
    }

    public short getAttributeMaxLength() {
        return attributeMaxLength;
    }

    public void setAttributeMaxLength(short attributeMaxLength) {
        this.attributeMaxLength = attributeMaxLength;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(attributeID);
        buffer.putShort(attributeMaxLength);
        return buffer.array();
    }

    public void deserialize(byte[] rawData) {
        ByteBuffer buffer = ByteBuffer.wrap(rawData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        attributeID = buffer.get();
        if (buffer.capacity() >= 3) {
            attributeMaxLength = buffer.getShort();
        }
    }
}
