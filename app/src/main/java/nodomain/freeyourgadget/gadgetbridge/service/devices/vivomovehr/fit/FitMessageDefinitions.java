/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import java.util.Arrays;
import java.util.List;

public class FitMessageDefinitions {
    public static final int MESSAGE_ID_CONNECTIVITY = 0;
    public static final int MESSAGE_ID_WEATHER_ALERT = 5;
    public static final int MESSAGE_ID_WEATHER_CONDITIONS = 6;
    public static final int MESSAGE_ID_DEVICE_SETTINGS = 7;

    public static final String FIT_MESSAGE_NAME_FILE_ID = "file_id";
    public static final String FIT_MESSAGE_NAME_CAPABILITIES = "capabilities";
    public static final String FIT_MESSAGE_NAME_DEVICE_SETTINGS = "device_settings";
    public static final String FIT_MESSAGE_NAME_USER_PROFILE = "user_profile";
    public static final String FIT_MESSAGE_NAME_EVENT = "event";
    public static final String FIT_MESSAGE_NAME_DEVICE_INFO = "device_info";
    public static final String FIT_MESSAGE_NAME_DEBUG = "debug";
    public static final String FIT_MESSAGE_NAME_SOFTWARE = "software";
    public static final String FIT_MESSAGE_NAME_FILE_CAPABILITIES = "file_capabilities";
    public static final String FIT_MESSAGE_NAME_FILE_CREATOR = "file_creator";
    public static final String FIT_MESSAGE_NAME_MONITORING = "monitoring";
    public static final String FIT_MESSAGE_NAME_MONITORING_INFO = "monitoring_info";
    public static final String FIT_MESSAGE_NAME_CONNECTIVITY = "connectivity";
    public static final String FIT_MESSAGE_NAME_WEATHER_CONDITIONS = "weather_conditions";
    public static final String FIT_MESSAGE_NAME_WEATHER_ALERT = "weather_alert";
    public static final String FIT_MESSAGE_NAME_FILE_DESCRIPTION = "file_description";
    public static final String FIT_MESSAGE_NAME_OHR_SETTINGS = "ohr_settings";
    public static final String FIT_MESSAGE_NAME_EXD_SCREEN_CONFIGURATION = "exd_screen_configuration";
    public static final String FIT_MESSAGE_NAME_EXD_DATA_FIELD_CONFIGURATION = "exd_data_field_configuration";
    public static final String FIT_MESSAGE_NAME_EXD_DATA_CONCEPT_CONFIGURATION = "exd_data_concept_configuration";
    public static final String FIT_MESSAGE_NAME_MONITORING_HR_DATA = "monitoring_hr_data";
    public static final String FIT_MESSAGE_NAME_ALARM_SETTINGS = "alarm_settings";
    public static final String FIT_MESSAGE_NAME_STRESS_LEVEL = "stress_level";
    public static final String FIT_MESSAGE_NAME_MANUAL_STRESS_LEVEL = "manual_stress_level";
    public static final String FIT_MESSAGE_NAME_MAX_MET_DATA = "max_met_data";
    public static final String FIT_MESSAGE_NAME_WHR_DIAG = "whr_diag";
    public static final String FIT_MESSAGE_NAME_METRICS_INFO = "metrics_info";
    public static final String FIT_MESSAGE_NAME_PAGES_MAP = "pages_map";
    public static final String FIT_MESSAGE_NAME_NEURAL_NETWORK_INFO = "neural_network_info";
    public static final String FIT_MESSAGE_NAME_NEURAL_NETWORK_DATA = "neural_network_data";
    public static final String FIT_MESSAGE_NAME_SLEEP_LEVEL = "sleep_level";
    public static final String FIT_MESSAGE_NAME_END_OF_FILE = "end_of_file";

    public static final int FIT_MESSAGE_NUMBER_FILE_ID = 0;
    public static final int FIT_MESSAGE_NUMBER_CAPABILITIES = 1;
    public static final int FIT_MESSAGE_NUMBER_DEVICE_SETTINGS = 2;
    public static final int FIT_MESSAGE_NUMBER_USER_PROFILE = 3;
    public static final int FIT_MESSAGE_NUMBER_EVENT = 21;
    public static final int FIT_MESSAGE_NUMBER_DEVICE_INFO = 23;
    public static final int FIT_MESSAGE_NUMBER_DEBUG = 24;
    public static final int FIT_MESSAGE_NUMBER_SOFTWARE = 35;
    public static final int FIT_MESSAGE_NUMBER_FILE_CAPABILITIES = 37;
    public static final int FIT_MESSAGE_NUMBER_FILE_CREATOR = 49;
    public static final int FIT_MESSAGE_NUMBER_MONITORING = 55;
    public static final int FIT_MESSAGE_NUMBER_MONITORING_INFO = 103;
    public static final int FIT_MESSAGE_NUMBER_CONNECTIVITY = 127;
    public static final int FIT_MESSAGE_NUMBER_WEATHER_CONDITIONS = 128;
    public static final int FIT_MESSAGE_NUMBER_WEATHER_ALERT = 129;
    public static final int FIT_MESSAGE_NUMBER_FILE_DESCRIPTION = 138;
    public static final int FIT_MESSAGE_NUMBER_OHR_SETTINGS = 188;
    public static final int FIT_MESSAGE_NUMBER_EXD_SCREEN_CONFIGURATION = 200;
    public static final int FIT_MESSAGE_NUMBER_EXD_DATA_FIELD_CONFIGURATION = 201;
    public static final int FIT_MESSAGE_NUMBER_EXD_DATA_CONCEPT_CONFIGURATION = 202;
    public static final int FIT_MESSAGE_NUMBER_MONITORING_HR_DATA = 211;
    public static final int FIT_MESSAGE_NUMBER_ALARM_SETTINGS = 222;
    public static final int FIT_MESSAGE_NUMBER_STRESS_LEVEL = 227;
    public static final int FIT_MESSAGE_NUMBER_MANUAL_STRESS_LEVEL = 228;
    public static final int FIT_MESSAGE_NUMBER_MAX_MET_DATA = 229;
    public static final int FIT_MESSAGE_NUMBER_WHR_DIAG = 233;
    public static final int FIT_MESSAGE_NUMBER_METRICS_INFO = 241;
    public static final int FIT_MESSAGE_NUMBER_PAGES_MAP = 254;
    public static final int FIT_MESSAGE_NUMBER_NEURAL_NETWORK_INFO = 273;
    public static final int FIT_MESSAGE_NUMBER_NEURAL_NETWORK_DATA = 274;
    public static final int FIT_MESSAGE_NUMBER_SLEEP_LEVEL = 275;
    public static final int FIT_MESSAGE_NUMBER_END_OF_FILE = 276;

    public static final FitMessageDefinition DEFINITION_FILE_ID = new FitMessageDefinition(FIT_MESSAGE_NAME_FILE_ID, FIT_MESSAGE_NUMBER_FILE_ID, -1,
            new FitMessageFieldDefinition("type", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("manufacturer", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("product", 2, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("serial_number", 3, 4, FitFieldBaseType.UINT32Z, null),
            new FitMessageFieldDefinition("time_created", 4, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("number", 5, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("manufacturer_partner", 6, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("product_name", 8, 20, FitFieldBaseType.STRING, null)
    );

    public static final FitMessageDefinition DEFINITION_CAPABILITIES = new FitMessageDefinition(FIT_MESSAGE_NAME_CAPABILITIES, FIT_MESSAGE_NUMBER_CAPABILITIES, -1,
            new FitMessageFieldDefinition("languages", 0, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("sports", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("workouts_supported", 21, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("activity_profile_supported", 22, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("connectivity_supported", 23, 4, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("wifi_supported", 24, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("segments_supported", 25, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("audio_prompts_supported", 26, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_DEVICE_SETTINGS = new FitMessageDefinition(FIT_MESSAGE_NAME_DEVICE_SETTINGS, FIT_MESSAGE_NUMBER_DEVICE_SETTINGS, MESSAGE_ID_DEVICE_SETTINGS,
            new FitMessageFieldDefinition("active_time_zone", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("utc_offset", 1, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("time_offset", 2, 4, FitFieldBaseType.UINT32, 1, 0, "s", null),
            new FitMessageFieldDefinition("time_daylight_savings", 3, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("time_mode", 4, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("time_zone_offset", 5, 1, FitFieldBaseType.SINT8, 1, 0, "hr", null),
            new FitMessageFieldDefinition("alarm_time", 8, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("alarm_mode", 9, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("key_tones_enabled", 10, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("message_tones_enabled", 11, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_mode", 12, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_timeout", 13, 1, FitFieldBaseType.UINT8, 1, 0, "s", null),
            new FitMessageFieldDefinition("backlight_brightness", 14, 1, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("display_contrast", 15, 1, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("computer_beacon", 16, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("computer_pairing", 17, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("fitness_equipment_pairing", 18, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("bezel_sensitivity", 19, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("gps_enabled", 21, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("weight_scale_enabled", 22, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_orientation", 23, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("map_show", 24, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_show_locations", 25, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("time_zone", 26, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_shutdown", 27, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("alarm_tone", 28, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("data_storage", 29, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("map_auto_zoom", 30, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_guidance", 31, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("current_map_profile", 32, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("current_routing_profile", 33, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("display_mode", 34, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("first_day_of_week", 35, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("activity_tracker_enabled", 36, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("sleep_enabled", 37, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("wifi_auto_upload_enabled", 38, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("clock_time", 39, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("pages_enabled", 40, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("recovery_advisor_enabled", 41, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("auto_max_hr_enabled", 42, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("clock_profile_color_enabled", 43, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("clock_background_inverted", 44, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("auto_goal_enabled", 45, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("move_alert_enabled", 46, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("date_mode", 47, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("data_recording_interval", 48, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("data_recording_value", 49, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("vivohub_settings", 50, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("display_steps_goal_enabled", 51, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("course_navigation_enabled", 52, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("course_off_course_warnings_enabled", 53, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("segment_navigation_enabled", 54, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("display_orientation", 55, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("mounting_side", 56, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("default_page", 57, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("autosync_min_steps", 58, 2, FitFieldBaseType.UINT16, 1, 0, "steps", null),
            new FitMessageFieldDefinition("autosync_min_time", 59, 2, FitFieldBaseType.UINT16, 1, 0, "minutes", null),
            new FitMessageFieldDefinition("smart_sleep_window", 60, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("gesture_detection_mode", 61, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("glonass_enabled", 62, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("display_pace", 63, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("display_activity_tracker_enabled", 64, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("phone_notification_enabled", 65, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("phone_notification_tone", 66, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("phone_notification_default_filter", 67, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("phone_notification_activity_filter", 68, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("phone_notification_activity_tone", 69, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("user_notices_enabled", 70, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("lap_key_enabled", 71, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("features", 72, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("features_mask", 73, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("course_points_enabled", 74, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("course_segments_enabled", 75, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_show_track", 76, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_track_color", 77, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("next_dst_change", 78, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("dst_change_value", 79, 1, FitFieldBaseType.SINT8, 1, 0, "hours", null),
            new FitMessageFieldDefinition("lactate_threshold_autodetect_enabled", 80, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("backlight_keys", 81, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_alerts", 82, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_gesture", 83, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("bluetooth_connection_alerts_enabled", 84, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("ftp_auto_calc_enabled", 85, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("ble_auto_upload_enabled", 86, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("sleep_do_not_disturb_enabled", 87, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("backlight_smart_notifications", 88, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_sync_frequency", 89, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_activity_detect", 90, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("phone_notification_filters", 91, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("alarm_days", 92, 1, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("auto_update_app_enabled", 93, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("number_of_screens", 94, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("smart_notification_display_orientation", 95, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_lock_enabled", 96, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("grouptrack_activity_type", 97, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("wifi_enabled", 98, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("smart_notification_enabled", 99, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("beeper_enabled", 100, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("goal_notification", 101, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("product_category", 102, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("touch_sensitivity", 103, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("power_controls_items", 104, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("selected_watchface_index", 105, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("livetrack_message_notification_enabled", 106, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("alert_tones_app_only", 107, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("auto_detect_max_hr", 108, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("perf_cond_ntfctn_enabled", 109, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("new_vo2_ntfctn_enabled", 110, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("training_effect_ntfctn_enabled", 111, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("recovery_time_ntfctn_enabled", 112, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("auto_activity_start_enabled", 113, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("move_bar_enabled", 114, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("vibration_intensity", 115, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("lock_on_road", 116, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("map_detail", 117, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("screen_timeout", 119, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("display_theme", 120, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("popularity_routing_enabled", 121, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("glance_mode_layout", 122, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("user_text", 123, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_brightness_current_activity", 124, 1, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("backlight_timeout_current_activity", 125, 1, FitFieldBaseType.UINT8, 1, 0, "s", null),
            new FitMessageFieldDefinition("backlight_keys_current_activity", 126, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_alerts_current_activity", 127, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight_gesture_current_activity", 128, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("marine_chart_mode", 129, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("spot_soundings", 130, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("light_sectors", 131, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("marine_symbol_set", 132, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_update_software_enabled", 133, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("tap_interface", 134, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("auto_lock_mode", 135, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("simplified_backlight_timeout", 136, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("draw_segments", 137, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("hourly_alert", 138, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("turn_guidance_popup", 139, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("stress_alert_enabled", 140, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("spo2_mode", 141, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("low_spo2_threshold", 142, 1, FitFieldBaseType.UINT8, 1, 0, "percent", null),
            new FitMessageFieldDefinition("sedentary_hr_alert_threshold", 143, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("activity_physio_true_up_enabled", 144, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("smart_notification_timeout", 145, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("sideswipe_enabled", 146, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("sideswipe_direction_inverted", 147, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("draw_contour_lines", 148, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("sedentary_hr_alert_state", 149, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("autosync_max_steps", 150, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("low_spo2_alert_enabled", 151, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("cda_auto_calc_enabled", 152, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("hydration_system_units", 153, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("hydration_containers", 154, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("hydration_alert_enabled", 155, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("hydration_alert_frequency", 156, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("hydration_containers_units", 157, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("hydration_auto_goal_enabled", 158, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("user_phone_verified", 159, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("primary_tracker_enabled", 160, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("phone_notification_default_privacy", 161, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("phone_notification_activity_privacy", 162, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("abnormal_low_hr_alert_state", 163, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("abnormal_low_hr_alert_threshold", 164, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null)
    );

    public static final FitMessageDefinition DEFINITION_USER_PROFILE = new FitMessageDefinition(FIT_MESSAGE_NAME_USER_PROFILE, FIT_MESSAGE_NUMBER_USER_PROFILE, -1,
            new FitMessageFieldDefinition("friendly_name", 0, 16, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("gender", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("age", 2, 1, FitFieldBaseType.UINT8, 1, 0, "years", null),
            new FitMessageFieldDefinition("height", 3, 1, FitFieldBaseType.UINT8, 1, 0, "cm", null),
            new FitMessageFieldDefinition("weight", 4, 2, FitFieldBaseType.UINT16, 10, 0, "kg", null),
            new FitMessageFieldDefinition("language", 5, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("elev_setting", 6, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("weight_setting", 7, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("resting_heart_rate", 8, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("default_max_running_heart_rate", 9, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("default_max_biking_heart_rate", 10, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("default_max_heart_rate", 11, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("hr_setting", 12, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("speed_setting", 13, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("dist_setting", 14, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("power_setting", 16, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("activity_class", 17, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("position_setting", 18, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("rmr", 19, 2, FitFieldBaseType.UINT16, 1, 0, "kcal/d", null),
            new FitMessageFieldDefinition("active_time", 20, 1, FitFieldBaseType.UINT8, 1, 0, "min", null),
            new FitMessageFieldDefinition("temperature_setting", 21, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("local_id", 22, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("global_id", 23, 6, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("birth_year", 24, 1, FitFieldBaseType.UINT8, 1, 1900, "", null),
            new FitMessageFieldDefinition("avg_cycle_length", 25, 2, FitFieldBaseType.UINT16, 10000, 0, "m", null),
            new FitMessageFieldDefinition("pressure_setting", 26, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("handedness", 27, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("wake_time", 28, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("sleep_time", 29, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("height_setting", 30, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("user_running_step_length", 31, 2, FitFieldBaseType.UINT16, 1, 0, "mm", null),
            new FitMessageFieldDefinition("user_walking_step_length", 32, 2, FitFieldBaseType.UINT16, 1, 0, "mm", null),
            new FitMessageFieldDefinition("firstbeat_monthly_load", 33, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("firstbeat_recovery_time", 34, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("firstbeat_recovery_time_start", 35, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("firstbeat_max_stress_score", 36, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("firstbeat_running_lt_kmh", 37, 2, FitFieldBaseType.UINT16, 10, 0, "km/h", null),
            new FitMessageFieldDefinition("firstbeat_cycling_lt_watts", 38, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("firstbeat_running_maxMET", 39, 4, FitFieldBaseType.FLOAT32, null),
            new FitMessageFieldDefinition("firstbeat_cycling_maxMET", 40, 4, FitFieldBaseType.FLOAT32, null),
            new FitMessageFieldDefinition("firstbeat_running_lt_timestamp", 41, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("firstbeat_cycling_lt_timestamp", 42, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("resting_hr_auto_update_enabled", 43, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("birth_day", 44, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("birth_month", 45, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("avatar", 46, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("depth_setting", 47, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("dive_count", 49, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("phone_number", 50, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("keep_user_name_private", 51, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("active_minutes_calc_method", 52, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("active_minutes_moderate_zone", 53, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("active_minutes_vigorous_zone", 54, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("swim_skill_level", 55, 1, FitFieldBaseType.UINT8, null)
    );

    public static final FitMessageDefinition DEFINITION_EVENT = new FitMessageDefinition(FIT_MESSAGE_NAME_EVENT, FIT_MESSAGE_NUMBER_EVENT, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("event", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("event_type", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("data16", 2, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("data", 3, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("event_group", 4, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("device_index", 13, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("activity_type", 14, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("start_timestamp", 15, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("activity_subtype", 16, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_DEVICE_INFO = new FitMessageDefinition(FIT_MESSAGE_NAME_DEVICE_INFO, FIT_MESSAGE_NUMBER_DEVICE_INFO, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("device_index", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("device_type", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("manufacturer", 2, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("serial_number", 3, 4, FitFieldBaseType.UINT32Z, null),
            new FitMessageFieldDefinition("product", 4, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("software_version", 5, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("hardware_version", 6, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("cum_operating_time", 7, 4, FitFieldBaseType.UINT32, 1, 0, "s", null),
            new FitMessageFieldDefinition("cum_training_time", 8, 4, FitFieldBaseType.UINT32, 1, 0, "s", null),
            new FitMessageFieldDefinition("reception", 9, 4, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("battery_voltage", 10, 2, FitFieldBaseType.UINT16, 256, 0, "V", null),
            new FitMessageFieldDefinition("battery_status", 11, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("rx_pass_count", 15, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("rx_fail_count", 16, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("software_version_string", 17, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("sensor_position", 18, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("descriptor", 19, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("ant_transmission_type", 20, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("ant_device_number", 21, 2, FitFieldBaseType.UINT16Z, null),
            new FitMessageFieldDefinition("ant_network", 22, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("source_type", 25, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("product_name", 27, 20, FitFieldBaseType.STRING, null)
    );

    public static final FitMessageDefinition DEFINITION_DEBUG = new FitMessageDefinition(FIT_MESSAGE_NAME_DEBUG, FIT_MESSAGE_NUMBER_DEBUG, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("id", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("string", 1, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("data", 2, 20, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("time256", 3, 20, FitFieldBaseType.UINT8, 256, 0, "s", null),
            new FitMessageFieldDefinition("fractional_timestamp", 4, 2, FitFieldBaseType.UINT16, 32768.0, 0, "s", null)
    );

    public static final FitMessageDefinition DEFINITION_SOFTWARE = new FitMessageDefinition(FIT_MESSAGE_NAME_SOFTWARE, FIT_MESSAGE_NUMBER_SOFTWARE, -1,
            new FitMessageFieldDefinition("message_index", 254, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("version", 3, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("part_number", 5, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("version_string", 6, 20, FitFieldBaseType.STRING, null)
    );

    public static final FitMessageDefinition DEFINITION_FILE_CAPABILITIES = new FitMessageDefinition(FIT_MESSAGE_NAME_FILE_CAPABILITIES, FIT_MESSAGE_NUMBER_FILE_CAPABILITIES, -1,
            new FitMessageFieldDefinition("type", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("flags", 1, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("directory", 2, 16, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("max_count", 3, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("max_size", 4, 4, FitFieldBaseType.UINT32, null)
    );

    public static final FitMessageDefinition DEFINITION_FILE_CREATOR = new FitMessageDefinition(FIT_MESSAGE_NAME_FILE_CREATOR, FIT_MESSAGE_NUMBER_FILE_CREATOR, -1,
            new FitMessageFieldDefinition("software_version", 0, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("hardware_version", 1, 2, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("creator_name", 2, 2, FitFieldBaseType.STRING, null)
    );

    public static final FitMessageDefinition DEFINITION_MONITORING = new FitMessageDefinition(FIT_MESSAGE_NAME_MONITORING, FIT_MESSAGE_NUMBER_MONITORING, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("device_index", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("calories", 1, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("distance", 2, 4, FitFieldBaseType.UINT32, 100, 0, "m", null),
            // TODO: Scale depends on activity type
            new FitMessageFieldDefinition("cycles", 3, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("active_time", 4, 1, FitFieldBaseType.UINT32, 1000, 0, "s", null),
            new FitMessageFieldDefinition("activity_type", 5, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("activity_subtype", 6, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("activity_level", 7, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("distance_16", 8, 2, FitFieldBaseType.UINT16, 0.01, 0, "m", null),
            new FitMessageFieldDefinition("cycles_16", 9, 2, FitFieldBaseType.UINT16, 0.5, 0, "cycles", null),
            new FitMessageFieldDefinition("active_time_16", 10, 2, FitFieldBaseType.UINT16, 1, 0, "s", null),
            new FitMessageFieldDefinition("local_timestamp", 11, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("temperature", 12, 2, FitFieldBaseType.SINT16, 100, 0, "°C", null),
            new FitMessageFieldDefinition("temperature_min", 14, 2, FitFieldBaseType.SINT16, 100, 0, "°C", null),
            new FitMessageFieldDefinition("temperature_max", 15, 2, FitFieldBaseType.SINT16, 100, 0, "°C", null),
            // TODO: Array
            new FitMessageFieldDefinition("activity_time", 16, 2, FitFieldBaseType.UINT16, 1, 0, "min", null),
            new FitMessageFieldDefinition("active_calories", 19, 2, FitFieldBaseType.UINT16, 1, 0, "kcal", null),
            new FitMessageFieldDefinition("current_activity_type_intensity", 24, 1, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("timestamp_min_8", 25, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("timestamp_16", 26, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("heart_rate", 27, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("intensity", 28, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("duration_min", 29, 2, FitFieldBaseType.UINT16, 1, 0, "min", null),
            new FitMessageFieldDefinition("duration", 30, 4, FitFieldBaseType.UINT32, 1, 0, "s", null),
            new FitMessageFieldDefinition("ascent", 31, 4, FitFieldBaseType.UINT32, 1000, 0, "m", null),
            new FitMessageFieldDefinition("descent", 32, 4, FitFieldBaseType.UINT32, 1000, 0, "m", null),
            new FitMessageFieldDefinition("moderate_activity_minutes", 33, 2, FitFieldBaseType.UINT16, 1, 0, "min", null),
            new FitMessageFieldDefinition("vigorous_activity_minutes", 34, 2, FitFieldBaseType.UINT16, 1, 0, "min", null),
            new FitMessageFieldDefinition("ascent_total", 35, 4, FitFieldBaseType.UINT32, 1000, 0, "m", null),
            new FitMessageFieldDefinition("descent_total", 36, 4, FitFieldBaseType.UINT32, 1000, 0, "m", null),
            new FitMessageFieldDefinition("moderate_activity_minutes_total", 37, 2, FitFieldBaseType.UINT16, 1, 0, "min", null),
            new FitMessageFieldDefinition("vigorous_activity_minutes_total", 38, 2, FitFieldBaseType.UINT16, 1, 0, "min", null)
    );

    public static final FitMessageDefinition DEFINITION_MONITORING_INFO = new FitMessageDefinition(FIT_MESSAGE_NAME_MONITORING_INFO, FIT_MESSAGE_NUMBER_MONITORING_INFO, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("local_timestamp", 0, 4, FitFieldBaseType.UINT32, null),
            // TODO: Arrays
            new FitMessageFieldDefinition("activity_type", 1, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("cycles_to_distance", 3, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("cycles_to_calories", 4, 2, FitFieldBaseType.UINT16, null),

            new FitMessageFieldDefinition("resting_metabolic_rate", 5, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("cycles_goal", 7, 4, FitFieldBaseType.UINT32, 2, 0, "cycles", null),
            new FitMessageFieldDefinition("monitoring_time_source", 8, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_CONNECTIVITY = new FitMessageDefinition(FIT_MESSAGE_NAME_CONNECTIVITY, FIT_MESSAGE_NUMBER_CONNECTIVITY, MESSAGE_ID_CONNECTIVITY,
            new FitMessageFieldDefinition("bluetooth_enabled", 0, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("bluetooth_le_enabled", 1, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("ant_enabled", 2, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("name", 3, 16, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("live_tracking_enabled", 4, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("weather_conditions_enabled", 5, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("weather_alerts_enabled", 6, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("auto_activity_upload_enabled", 7, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("course_download_enabled", 8, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("workout_download_enabled", 9, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("gps_ephemeris_download_enabled", 10, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("live_track_auto_start_enabled", 13, 1, FitFieldBaseType.ENUM, FitBool.FALSE)
    );

    public static final FitMessageDefinition DEFINITION_WEATHER_CONDITIONS = new FitMessageDefinition(FIT_MESSAGE_NAME_WEATHER_CONDITIONS, FIT_MESSAGE_NUMBER_WEATHER_CONDITIONS, MESSAGE_ID_WEATHER_CONDITIONS,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("weather_report", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("temperature", 1, 1, FitFieldBaseType.SINT8, 1, 0, "°C", null),
            new FitMessageFieldDefinition("condition", 2, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("wind_direction", 3, 2, FitFieldBaseType.UINT16, 1, 0, "°", null),
            new FitMessageFieldDefinition("wind_speed", 4, 2, FitFieldBaseType.UINT16, 1000, 0, "m/s", null),
            new FitMessageFieldDefinition("precipitation_probability", 5, 1, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("temperature_feels_like", 6, 1, FitFieldBaseType.SINT8, 1, 0, "°C", null),
            new FitMessageFieldDefinition("relative_humidity", 7, 1, FitFieldBaseType.UINT8, 1, 0, "%", null),
            new FitMessageFieldDefinition("location", 8, 16, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("observed_at_time", 9, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("observed_location_lat", 10, 4, FitFieldBaseType.SINT32, 1, 0, "semicircles", null),
            new FitMessageFieldDefinition("observed_location_long", 11, 4, FitFieldBaseType.SINT32, 1, 0, "semicircles", null),
            new FitMessageFieldDefinition("day_of_week", 12, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("high_temperature", 13, 1, FitFieldBaseType.SINT8, 1, 0, "°C", null),
            new FitMessageFieldDefinition("low_temperature", 14, 1, FitFieldBaseType.SINT8, 1, 0, "°C", null)
    );

    public static final FitMessageDefinition DEFINITION_WEATHER_ALERT = new FitMessageDefinition(FIT_MESSAGE_NAME_WEATHER_ALERT, FIT_MESSAGE_NUMBER_WEATHER_ALERT, MESSAGE_ID_WEATHER_ALERT,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("report_id", 0, 10, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("issue_time", 1, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("expire_time", 2, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("severity", 3, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("type", 4, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_FILE_DESCRIPTION = new FitMessageDefinition(FIT_MESSAGE_NAME_FILE_DESCRIPTION, FIT_MESSAGE_NUMBER_FILE_DESCRIPTION, -1,
            new FitMessageFieldDefinition("message_index", 254, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("manufacturer", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("directory", 1, 16, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("name", 2, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("flags", 3, 1, FitFieldBaseType.UINT8Z, null),
            new FitMessageFieldDefinition("purpose", 4, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("garmin_file_purpose", 5, 1, FitFieldBaseType.UINT8, null)
    );

    public static final FitMessageDefinition DEFINITION_OHR_SETTINGS = new FitMessageDefinition(FIT_MESSAGE_NAME_OHR_SETTINGS, FIT_MESSAGE_NUMBER_OHR_SETTINGS, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("enabled", 0, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("sample_rate", 1, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("transmit_hr_enabled", 2, 1, FitFieldBaseType.ENUM, FitBool.FALSE)
    );

    public static final FitMessageDefinition DEFINITION_EXD_SCREEN_CONFIGURATION = new FitMessageDefinition(FIT_MESSAGE_NAME_EXD_SCREEN_CONFIGURATION, FIT_MESSAGE_NUMBER_EXD_SCREEN_CONFIGURATION, -1,
            new FitMessageFieldDefinition("screen_index", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("field_count", 1, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("layout", 2, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("screen_enabled", 3, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_EXD_DATA_FIELD_CONFIGURATION = new FitMessageDefinition(FIT_MESSAGE_NAME_EXD_DATA_FIELD_CONFIGURATION, FIT_MESSAGE_NUMBER_EXD_DATA_FIELD_CONFIGURATION, -1,
            new FitMessageFieldDefinition("screen_index", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("concept_field", 1, 1, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("field_id", 2, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("concept_count", 3, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("display_type", 4, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("title", 5, 32, FitFieldBaseType.STRING, null)
    );

    public static final FitMessageDefinition DEFINITION_EXD_DATA_CONCEPT_CONFIGURATION = new FitMessageDefinition(FIT_MESSAGE_NAME_EXD_DATA_CONCEPT_CONFIGURATION, FIT_MESSAGE_NUMBER_EXD_DATA_CONCEPT_CONFIGURATION, -1,
            new FitMessageFieldDefinition("screen_index", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("concept_field", 1, 1, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("field_id", 2, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("concept_index", 3, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("data_page", 4, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("concept_key", 5, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("scaling", 6, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("offset", 7, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("data_units", 8, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("qualifier", 9, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("descriptor", 10, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("is_signed", 11, 1, FitFieldBaseType.ENUM, FitBool.FALSE)
    );

    public static final FitMessageDefinition DEFINITION_MONITORING_HR_DATA = new FitMessageDefinition(FIT_MESSAGE_NAME_MONITORING_HR_DATA, FIT_MESSAGE_NUMBER_MONITORING_HR_DATA, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("resting_heart_rate", 0, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null),
            new FitMessageFieldDefinition("current_day_resting_heart_rate", 1, 1, FitFieldBaseType.UINT8, 1, 0, "bpm", null)
    );

    public static final FitMessageDefinition DEFINITION_ALARM_SETTINGS = new FitMessageDefinition(FIT_MESSAGE_NAME_ALARM_SETTINGS, FIT_MESSAGE_NUMBER_ALARM_SETTINGS, -1,
            new FitMessageFieldDefinition("message_index", 254, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("time", 0, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("days", 1, 1, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("enabled", 2, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("sound", 3, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("backlight", 4, 1, FitFieldBaseType.ENUM, FitBool.FALSE),
            new FitMessageFieldDefinition("id", 5, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("alarm_mesg", 6, 20, FitFieldBaseType.STRING, null),
            new FitMessageFieldDefinition("snooze_count", 7, 1, FitFieldBaseType.UINT8, null)
    );

    public static final FitMessageDefinition DEFINITION_STRESS_LEVEL = new FitMessageDefinition(FIT_MESSAGE_NAME_STRESS_LEVEL, FIT_MESSAGE_NUMBER_STRESS_LEVEL, -1,
            new FitMessageFieldDefinition("stress_level_value", 0, 2, FitFieldBaseType.SINT16, null),
            new FitMessageFieldDefinition("stress_level_time", 1, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("average_stress_intensity", 2, 1, FitFieldBaseType.SINT8, null)
    );

    public static final FitMessageDefinition DEFINITION_MANUAL_STRESS_LEVEL = new FitMessageDefinition(FIT_MESSAGE_NAME_MANUAL_STRESS_LEVEL, FIT_MESSAGE_NUMBER_MANUAL_STRESS_LEVEL, -1,
            new FitMessageFieldDefinition("stress_level_value", 0, 2, FitFieldBaseType.SINT16, null),
            new FitMessageFieldDefinition("stress_level_time", 1, 4, FitFieldBaseType.UINT32, null)
    );

    public static final FitMessageDefinition DEFINITION_MAX_MET_DATA = new FitMessageDefinition(FIT_MESSAGE_NAME_MAX_MET_DATA, FIT_MESSAGE_NUMBER_MAX_MET_DATA, -1,
            new FitMessageFieldDefinition("update_time", 0, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("max_met", 1, 4, FitFieldBaseType.SINT32, null),
            new FitMessageFieldDefinition("vo2_max", 2, 2, FitFieldBaseType.UINT16, 10, 0, "mL/kg/min", null),
            new FitMessageFieldDefinition("fitness_age", 3, 1, FitFieldBaseType.SINT8, null),
            new FitMessageFieldDefinition("fitness_age_desc", 4, 1, FitFieldBaseType.SINT8, null),
            new FitMessageFieldDefinition("sport", 5, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("sub_sport", 6, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("analyzer_method", 7, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("max_met_category", 8, 1, FitFieldBaseType.ENUM, null),
            new FitMessageFieldDefinition("calibrated_data", 9, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_WHR_DIAG = new FitMessageDefinition(FIT_MESSAGE_NAME_WHR_DIAG, FIT_MESSAGE_NUMBER_WHR_DIAG, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("fractional_timestamp", 1, 2, FitFieldBaseType.UINT16, 32768.0, 0, "s", null),
            new FitMessageFieldDefinition("page_data", 2, 1, FitFieldBaseType.BYTE, null)
    );

    public static final FitMessageDefinition DEFINITION_METRICS_INFO = new FitMessageDefinition(FIT_MESSAGE_NAME_METRICS_INFO, FIT_MESSAGE_NUMBER_METRICS_INFO, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("local_timestamp", 0, 4, FitFieldBaseType.UINT32, null)
    );

    public static final FitMessageDefinition DEFINITION_PAGES_MAP = new FitMessageDefinition(FIT_MESSAGE_NAME_PAGES_MAP, FIT_MESSAGE_NUMBER_PAGES_MAP, -1,
            new FitMessageFieldDefinition("message_index", 254, 2, FitFieldBaseType.UINT16, null),
            new FitMessageFieldDefinition("map", 0, 10, FitFieldBaseType.BYTE, null),
            new FitMessageFieldDefinition("default_to_last", 1, 1, FitFieldBaseType.ENUM, FitBool.FALSE)
    );

    public static final FitMessageDefinition DEFINITION_NEURAL_NETWORK_INFO = new FitMessageDefinition(FIT_MESSAGE_NAME_NEURAL_NETWORK_INFO, FIT_MESSAGE_NUMBER_NEURAL_NETWORK_INFO, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("network_version", 0, 1, FitFieldBaseType.UINT8, null),
            new FitMessageFieldDefinition("implicit_message_duration", 1, 2, FitFieldBaseType.UINT16, 1, 0, "s", null),
            new FitMessageFieldDefinition("local_timestamp", 2, 4, FitFieldBaseType.UINT32, null)
    );

    public static final FitMessageDefinition DEFINITION_NEURAL_NETWORK_DATA = new FitMessageDefinition(FIT_MESSAGE_NAME_NEURAL_NETWORK_DATA, FIT_MESSAGE_NUMBER_NEURAL_NETWORK_DATA, -1,
            new FitMessageFieldDefinition("network_data", 0, 20, FitFieldBaseType.BYTE, null)
    );

    public static final FitMessageDefinition DEFINITION_SLEEP_LEVEL = new FitMessageDefinition(FIT_MESSAGE_NAME_SLEEP_LEVEL, FIT_MESSAGE_NUMBER_SLEEP_LEVEL, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null),
            new FitMessageFieldDefinition("sleep_level", 0, 1, FitFieldBaseType.ENUM, null)
    );

    public static final FitMessageDefinition DEFINITION_END_OF_FILE = new FitMessageDefinition(FIT_MESSAGE_NAME_END_OF_FILE, FIT_MESSAGE_NUMBER_END_OF_FILE, -1,
            new FitMessageFieldDefinition("timestamp", 253, 4, FitFieldBaseType.UINT32, null)
    );


    public static final List<FitMessageDefinition> ALL_DEFINITIONS = Arrays.asList(
            DEFINITION_FILE_ID,
            DEFINITION_CAPABILITIES,
            DEFINITION_DEVICE_SETTINGS,
            DEFINITION_USER_PROFILE,
            DEFINITION_EVENT,
            DEFINITION_DEVICE_INFO,
            DEFINITION_DEBUG,
            DEFINITION_SOFTWARE,
            DEFINITION_FILE_CAPABILITIES,
            DEFINITION_FILE_CREATOR,
            DEFINITION_MONITORING,
            DEFINITION_MONITORING_INFO,
            DEFINITION_CONNECTIVITY,
            DEFINITION_WEATHER_CONDITIONS,
            DEFINITION_WEATHER_ALERT,
            DEFINITION_FILE_DESCRIPTION,
            DEFINITION_EXD_SCREEN_CONFIGURATION,
            DEFINITION_EXD_DATA_FIELD_CONFIGURATION,
            DEFINITION_EXD_DATA_CONCEPT_CONFIGURATION,
            DEFINITION_OHR_SETTINGS,
            DEFINITION_MONITORING_HR_DATA,
            DEFINITION_ALARM_SETTINGS,
            DEFINITION_STRESS_LEVEL,
            DEFINITION_MANUAL_STRESS_LEVEL,
            DEFINITION_MAX_MET_DATA,
            DEFINITION_WHR_DIAG,
            DEFINITION_METRICS_INFO,
            DEFINITION_PAGES_MAP,
            DEFINITION_NEURAL_NETWORK_INFO,
            DEFINITION_NEURAL_NETWORK_DATA,
            DEFINITION_SLEEP_LEVEL,
            DEFINITION_END_OF_FILE
    );
}
