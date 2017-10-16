/*  Copyright (C) 2015-2017 Andreas Shimokawa, Frank Slezak

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
package nodomain.freeyourgadget.gadgetbridge.model;

public class NotificationSpec {
    public static final int FLAG_WEARABLE_REPLY = 0x00000001;

    public int flags;
    public int id;
    public String sender;
    public String phoneNumber;
    public String title;
    public String subject;
    public String body;
    public NotificationType type;
    public String sourceName;
    public String[] cannedReplies;

    /**
     * The application that generated the notification.
     */
    public String sourceAppId;

    /**
     * The color that should be assigned to this notification when displayed on a Pebble
     */
    public byte pebbleColor;
}
