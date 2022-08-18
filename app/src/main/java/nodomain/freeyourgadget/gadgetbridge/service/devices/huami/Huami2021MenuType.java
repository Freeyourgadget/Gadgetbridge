/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.util.HashMap;
import java.util.Map;

public class Huami2021MenuType {
    /**
     * These somewhat match the ones in {@link HuamiMenuType}, but not all. The band sends and
     * receives those as 8-digit upper case hex strings.
     */
    public static final Map<String, Integer> displayItemIdLookup = new HashMap<String, Integer>() {{
        put("personal_activity_intelligence", 0x01);
        put("hr", 0x02);
        put("workout", 0x03);
        put("weather", 0x04);
        put("alarm", 0x09);
        put("worldclock", 0x1A);
        put("music", 0x0B);
        put("stopwatch", 0x0C);
        put("countdown", 0x0D);
        put("findphone", 0x0E);
        put("mutephone", 0x0F);
        put("settings", 0x13);
        put("workout_history", 0x14);
        put("eventreminder", 0x15);
        put("pai", 0x19);
        put("takephoto", 0x0A);
        put("stress", 0x1C);
        put("female_health", 0x1D);
        put("workout_status", 0x1E);
        put("sleep", 0x23);
        put("spo2", 0x24);
        put("events", 0x26);
        put("breathing", 0x33);
        put("pomodoro", 0x38);
        put("flashlight", 0x0102);
    }};

    public static final Map<String, Integer> shortcutsIdLookup = new HashMap<String, Integer>() {{
        put("hr", 0x01);
        put("workout", 0x0A);
        put("workout_status", 0x0C);
        put("weather", 0x02);
        put("worldclock", 0x1A);
        put("alarm", 0x16);
        put("music", 0x04);
        put("activity", 0x20);
        put("eventreminder", 0x21);
        put("female_health", 0x11);
        put("pai", 0x03);
        put("stress", 0x0F);
        put("sleep", 0x05);
        put("spo2", 0x13);
        put("events", 0x18);
        put("breathing", 0x12);
    }};
}
