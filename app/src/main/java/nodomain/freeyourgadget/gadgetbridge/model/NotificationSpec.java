/*  Copyright (C) 2015-2018 Andreas Shimokawa, Frank Slezak

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationSpec {
    public int flags;
    private static final AtomicInteger c = new AtomicInteger((int) (System.currentTimeMillis()/1000));
    private int id;
    public String sender;
    public String phoneNumber;
    public String title;
    public String subject;
    public String body;
    public NotificationType type;
    public String sourceName;
    public String[] cannedReplies;
    /**
     * Wearable actions that were attached to the incoming notifications and will be passed to the gadget (includes the "reply" action)
     */
    public ArrayList<Action> attachedActions;
    /**
     * The application that generated the notification.
     */
    public String sourceAppId;

    /**
     * The color that should be assigned to this notification when displayed on a Pebble
     */
    public byte pebbleColor;

    public NotificationSpec() {
        this.id = c.incrementAndGet();
    }

    public NotificationSpec(int id) {
        if (id != -1)
            this.id = id;
        else
            this.id = c.incrementAndGet();
    }

    public int getId() {
        return id;
    }

    public static class Action implements Serializable {
        static final int TYPE_UNDEFINED = -1;
        public static final int TYPE_WEARABLE_SIMPLE = 0;
        public static final int TYPE_WEARABLE_REPLY = 1;
        public static final int TYPE_SYNTECTIC_REPLY_PHONENR = 2;
        public static final int TYPE_SYNTECTIC_DISMISS = 3;
        public static final int TYPE_SYNTECTIC_DISMISS_ALL = 4;
        public static final int TYPE_SYNTECTIC_MUTE = 5;
        public static final int TYPE_SYNTECTIC_OPEN = 6;

        public int type = TYPE_UNDEFINED;
        public long handle;
        public String title;
    }
}
