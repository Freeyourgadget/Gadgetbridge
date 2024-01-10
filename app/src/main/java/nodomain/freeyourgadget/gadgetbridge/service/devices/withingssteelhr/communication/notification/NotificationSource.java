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
import java.util.Locale;
import java.util.Random;

public class NotificationSource {
    private byte eventID;
    private byte eventFlags;
    private byte categoryId;
    private byte categoryCount;
    private int notificationUID;

    public NotificationSource(int notificationUID, byte eventID, byte eventFlags, byte categoryId, byte categoryCount) {
        this.eventID = eventID;
        this.eventFlags = eventFlags;
        this.categoryId = categoryId;
        this.categoryCount = categoryCount;
        this.notificationUID = Integer.valueOf(new Random().nextInt());
    }

    public int getNotificationUID() {
        return notificationUID;
    }

    void setNotificationUID(int notificationUID) {
        this.notificationUID = notificationUID;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(eventID);
        buffer.put(eventFlags);
        buffer.put(categoryId);
        buffer.put(categoryCount);
        buffer.putInt(notificationUID);
        return buffer.array();
    }
}
