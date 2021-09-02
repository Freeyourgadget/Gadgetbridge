/*  Copyright (C) 2020-2021 Andreas Shimokawa, TinfoilSubmarine

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

class HuamiMenuType {
    public static final Map<String, Integer> idLookup = new HashMap<String, Integer>() {{
        put("status", 0x01);
        put("hr", 0x02);
        put("workout", 0x03);
        put("weather", 0x04);
        put("notifications", 0x06);
        put("more", 0x07);
        put("dnd", 0x08);
        put("alarm", 0x09);
        put("takephoto", 0x0a);
        put("music", 0x0b);
        put("stopwatch", 0x0c);
        put("timer", 0x0d);
        put("findphone", 0x0e);
        put("mutephone", 0x0f);
        put("nfc", 0x10);
        put("alipay", 0x11);
        put("watchface", 0x12);
        put("settings", 0x13);
        put("activity", 0x14);
        put("eventreminder", 0x15);
        put("compass", 0x16);
        put("pai", 0x19);
        put("worldclock", 0x1a);
        put("timer_stopwatch", 0x1b);
        put("stress", 0x1c);
        put("period", 0x1d);
        put("goal", 0x21);
        put("sleep", 0x23);
        put("spo2", 0x24);
        put("events", 0x26);
        put("widgets", 0x28);
        put("breathing",0x33);
        put("steps",0x34);
        put("distance",0x35);
        put("calories",0x36);
        put("pomodoro", 0x38);
        put("alexa", 0x39);
        put("battery", 0x3a);
        put("temperature", 0x40);
        put("barometer", 0x41);
        put("flashlight", 0x43);
    }};
}
