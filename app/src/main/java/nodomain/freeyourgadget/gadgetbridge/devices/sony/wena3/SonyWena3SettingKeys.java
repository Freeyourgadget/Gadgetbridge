/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

public class SonyWena3SettingKeys {
    // ------ Booleans
    public static final String RICH_DESIGN_MODE = "pref_wena3_rich_design_mode";
    public static final String LARGE_FONT_SIZE = "pref_wena3_large_font_size";
    public static final String WEATHER_IN_STATUSBAR = "pref_wena3_weather_in_statusbar";
    public static final String SMART_VIBRATION = "pref_wena3_vibration_smart";
    public static final String RECEIVE_CALLS = "pref_wena3_receive_calls";
    public static final String BACKGROUND_SYNC = "pref_wena3_background_sync";

    public static final String AUTO_POWER_SCHEDULE_KIND = "pref_wena3_power_schedule_kind";
    public static final String AUTO_POWER_SCHEDULE_START_HHMM = "pref_wena3_power_schedule_start";
    public static final String AUTO_POWER_SCHEDULE_END_HHMM = "pref_wena3_power_schedule_end";

    public static final String LEFT_HOME_ICON = "pref_wena3_home_icon_left";
    public static final String CENTER_HOME_ICON = "pref_wena3_home_icon_center";
    public static final String RIGHT_HOME_ICON = "pref_wena3_home_icon_right";
    public static final String MENU_ICON_CSV_KEY = "pref_wena3_menu_icon_csv";
    public static final String STATUS_PAGE_CSV_KEY = "pref_wena3_status_pages_csv";
    public static final int MAX_STATUS_PAGES = 7;

    public static final String DEFAULT_CALL_LED_COLOR = "pref_wena3_default_call_led_color";
    public static final String DEFAULT_LED_COLOR = "pref_wena3_default_led_color";
    public static final String DEFAULT_CALL_VIBRATION_PATTERN = "pref_wena3_call_default_vibration";
    public static final String DEFAULT_VIBRATION_PATTERN = "pref_wena3_default_vibration";
    public static final String BUTTON_LONG_PRESS_ACTION = "pref_wena3_button_long_action";
    public static final String BUTTON_DOUBLE_PRESS_ACTION = "pref_wena3_button_double_action";
    public static final String VIBRATION_STRENGTH = "pref_wena3_vibration_strength";
    // ------ Ints
    public static final String SMART_WAKEUP_MARGIN_MINUTES = "pref_wena3_smart_wakeup_margin";
    public static final String DAY_START_HOUR = "pref_wena3_day_start_hour";
    public static final String DEFAULT_VIBRATION_REPETITION = "pref_wena3_default_vibration_repetition";
    public static final String DEFAULT_CALL_VIBRATION_REPETITION = "pref_wena3_default_call_vibration_repetition";
}
