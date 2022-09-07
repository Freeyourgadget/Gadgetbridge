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

import nodomain.freeyourgadget.gadgetbridge.R;

public class Huami2021MenuType {
    /**
     * These somewhat match the ones in {@link HuamiMenuType}, but not all. The band sends and
     * receives those as 8-digit upper case hex strings.
     */
    public static final Map<String, Integer> displayItemNameLookup = new HashMap<String, Integer>() {{
        put("00000001", R.string.menuitem_personal_activity_intelligence);
        put("00000002", R.string.menuitem_hr);
        put("00000003", R.string.menuitem_workout);
        put("00000004", R.string.menuitem_weather);
        put("00000009", R.string.menuitem_alarm);
        put("0000001A", R.string.menuitem_worldclock);
        put("0000000B", R.string.menuitem_music);
        put("0000000C", R.string.menuitem_stopwatch);
        put("0000000D", R.string.menuitem_countdown);
        put("0000000E", R.string.menuitem_findphone);
        put("0000000F", R.string.menuitem_mutephone);
        put("00000013", R.string.menuitem_settings);
        put("00000014", R.string.menuitem_workout_history);
        put("00000015", R.string.menuitem_eventreminder);
        put("00000019", R.string.menuitem_pai);
        put("0000000A", R.string.menuitem_takephoto);
        put("0000001C", R.string.menuitem_stress);
        put("0000001D", R.string.menuitem_female_health);
        put("0000001E", R.string.menuitem_workout_status);
        put("00000023", R.string.menuitem_sleep);
        put("00000024", R.string.menuitem_spo2);
        put("00000026", R.string.menuitem_events);
        put("00000033", R.string.menuitem_breathing);
        put("00000038", R.string.menuitem_pomodoro);
        put("00000102", R.string.menuitem_flashlight);
    }};

    public static final Map<String, Integer> shortcutsNameLookup = new HashMap<String, Integer>() {{
        put("00000001", R.string.menuitem_hr);
        put("0000000A", R.string.menuitem_workout);
        put("0000000C", R.string.menuitem_workout_status);
        put("00000002", R.string.menuitem_weather);
        put("0000001A", R.string.menuitem_worldclock);
        put("00000016", R.string.menuitem_alarm);
        put("00000004", R.string.menuitem_music);
        put("00000020", R.string.menuitem_activity);
        put("00000021", R.string.menuitem_eventreminder);
        put("00000011", R.string.menuitem_female_health);
        put("00000003", R.string.menuitem_pai);
        put("0000000F", R.string.menuitem_stress);
        put("00000005", R.string.menuitem_sleep);
        put("00000013", R.string.menuitem_spo2);
        put("00000018", R.string.menuitem_events);
        put("00000012", R.string.menuitem_breathing);
    }};
}
