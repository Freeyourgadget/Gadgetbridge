/*  Copyright (C) 2018 ladbsoft

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
package nodomain.freeyourgadget.gadgetbridge.devices.xwatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XWatchService {
    public static final UUID UUID_NOTIFY = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_WRITE = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");

    public static final byte COMMAND_CONNECTED = 0x01;
    public static final byte COMMAND_ACTION_BUTTON = 0x4c;
    public static final byte COMMAND_ACTIVITY_DATA = 0x43;
    public static final byte COMMAND_ACTIVITY_TOTALS = 0x46;

    private static final Map<UUID, String> XWATCH_DEBUG;

    static {
        XWATCH_DEBUG = new HashMap<>();

        XWATCH_DEBUG.put(UUID_NOTIFY, "Read data");
        XWATCH_DEBUG.put(UUID_WRITE, "Write data");
        XWATCH_DEBUG.put(UUID_SERVICE, "Get service");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = XWATCH_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }
}
