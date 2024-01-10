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

public final class AncsConstants {

    public static final byte EVENT_ID_NOTIFICATION_ADDED = 0;
    public static final byte EVENT_ID_NOTIFICATION_MODIFIED = 1;
    public static final byte EVENT_ID_NOTIFICATION_REMOVED = 2;

    public static final byte EVENT_FLAGS_SILENT = (1 << 0);
    public static final byte EVENT_FLAGS_IMPORTANT = (1 << 1);
    public static final byte EVENT_FLAGS_PREEXISTING = (1 << 2);
    public static final byte EVENT_FLAGS_POSITIVE_ACTION = (1 << 3);
    public static final byte EVENT_FLAGS_NEGATIVE_ACTION = (1 << 4);

    public static final byte CATEGORY_ID_OTHER = 0;
    public static final byte CATEGORY_ID_INCOMING_CALL = 1;
    public static final byte CATEGORY_ID_MISSED_CALL = 2;
    public static final byte CATEGORY_ID_VOICEMAIL = 3;
    public static final byte CATEGORY_ID_SOCIAL = 4;
    public static final byte CATEGORY_ID_SCHEDULE = 5;
    public static final byte CATEGORY_ID_EMAIL = 6;
    public static final byte CATEGORY_ID_NEWS = 7;
    public static final byte CATEGORY_ID_HEALTHANDFITNESS = 8;
    public static final byte CATEGORY_ID_BUSINESSANDFINANCE = 9;
    public static final byte CATEGORY_ID_LOCATION = 10;
    public static final byte CATEGORY_ID_ENTERTAINMENT = 11;

    private AncsConstants(){}
}
