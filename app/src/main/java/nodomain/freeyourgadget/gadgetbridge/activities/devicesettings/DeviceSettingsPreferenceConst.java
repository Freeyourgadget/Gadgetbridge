/*  Copyright (C) 2019-2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

public class DeviceSettingsPreferenceConst {
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_DATEFORMAT = "dateformat";
    public static final String PREF_TIMEFORMAT = "timeformat";
    public static final String PREF_WEARLOCATION = "wearlocation";
    public static final String PREF_VIBRATION_ENABLE = "vibration_enable";
    public static final String PREF_NOTIFICATION_ENABLE = "notification_enable";
    public static final String PREF_SCREEN_ORIENTATION = "screen_orientation";
    public static final String PREF_RESERVER_ALARMS_CALENDAR = "reserve_alarms_calendar";
    public static final String PREF_RESERVER_REMINDERS_CALENDAR = "reserve_reminders_calendar";
    public static final String PREF_ALLOW_HIGH_MTU = "allow_high_mtu";
    public static final String PREF_SYNC_CALENDAR = "sync_calendar";
    public static final String PREF_USE_CUSTOM_DEVICEICON = "use_custom_deviceicon";
    public static final String PREF_BUTTON_1_FUNCTION_SHORT = "button_1_function_short";
    public static final String PREF_BUTTON_2_FUNCTION_SHORT = "button_2_function_short";
    public static final String PREF_BUTTON_3_FUNCTION_SHORT = "button_3_function_short";
    public static final String PREF_BUTTON_1_FUNCTION_LONG = "button_1_function_long";
    public static final String PREF_BUTTON_2_FUNCTION_LONG = "button_2_function_long";
    public static final String PREF_BUTTON_3_FUNCTION_LONG = "button_3_function_long";
    public static final String PREF_BUTTON_1_FUNCTION_DOUBLE = "button_1_function_double";
    public static final String PREF_BUTTON_2_FUNCTION_DOUBLE = "button_2_function_double";
    public static final String PREF_BUTTON_3_FUNCTION_DOUBLE = "button_3_function_double";
    public static final String PREF_VIBRATION_STRENGH_PERCENTAGE = "vibration_strength";
    public static final String PREF_RELAX_FIRMWARE_CHECKS = "relax_firmware_checks";

    public static final String PREF_DEVICE_INTERNET_ACCESS = "device_internet_access";
    public static final String PREF_DEVICE_INTENTS = "device_intents";

    public static final String PREF_BANGLEJS_TEXT_BITMAP = "banglejs_text_bitmap";
    public static final String PREF_BANGLEJS_WEBVIEW_URL = "banglejs_webview_url";

    public static final String PREF_DISCONNECT_NOTIFICATION = "disconnect_notification";
    public static final String PREF_DISCONNECT_NOTIFICATION_START = "disconnect_notification_start";
    public static final String PREF_DISCONNECT_NOTIFICATION_END = "disconnect_notification_end";

    public static final String PREF_HYBRID_HR_FORCE_WHITE_COLOR = "force_white_color_scheme";
    public static final String PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES = "widget_draw_circles";
    public static final String PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES = "save_raw_activity_files";
    public static final String PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS = "dangerous_external_intents";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING = "activity_recognize_running";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING = "activity_recognize_biking";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING = "activity_recognize_walking";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING = "activity_recognize_rowing";


    public static final String PREF_ACTIVATE_DISPLAY_ON_LIFT = "activate_display_on_lift_wrist";
    public static final String PREF_DISPLAY_ON_LIFT_START = "display_on_lift_start";
    public static final String PREF_DISPLAY_ON_LIFT_END = "display_on_lift_end";
    public static final String PREF_DISPLAY_ON_LIFT_SENSITIVITY = "display_on_lift_sensitivity";

    public static final String PREF_SLEEP_TIME = "prefs_enable_sleep_time";
    public static final String PREF_SLEEP_TIME_START = "prefs_sleep_time_start";
    public static final String PREF_SLEEP_TIME_END = "prefs_sleep_time_end";

    public static final String PREF_LIFTWRIST_NOSHED = "activate_display_on_lift_wrist_noshed";
    public static final String PREF_DISCONNECTNOTIF_NOSHED = "disconnect_notification_noshed";
    public static final String PREF_INACTIVITY_KEY =  "inactivity_warnings_key";
    public static final String PREF_INACTIVITY_ENABLE = "inactivity_warnings_enable";
    public static final String PREF_INACTIVITY_ENABLE_NOSHED = "inactivity_warnings_enable_noshed";
    public static final String PREF_INACTIVITY_START = "inactivity_warnings_start";
    public static final String PREF_INACTIVITY_END = "inactivity_warnings_end";
    public static final String PREF_INACTIVITY_THRESHOLD = "inactivity_warnings_threshold";
    public static final String PREF_INACTIVITY_THRESHOLD_EXTENDED = "inactivity_warnings_threshold_extended";
    public static final String PREF_INACTIVITY_MO = "inactivity_warnings_mo";
    public static final String PREF_INACTIVITY_TU = "inactivity_warnings_tu";
    public static final String PREF_INACTIVITY_WE = "inactivity_warnings_we";
    public static final String PREF_INACTIVITY_TH = "inactivity_warnings_th";
    public static final String PREF_INACTIVITY_FR = "inactivity_warnings_fr";
    public static final String PREF_INACTIVITY_SA = "inactivity_warnings_sa";
    public static final String PREF_INACTIVITY_SU = "inactivity_warnings_su";
    public static final String PREF_INACTIVITY_DND = "inactivity_warnings_dnd";
    public static final String PREF_INACTIVITY_DND_START = "inactivity_warnings_dnd_start";
    public static final String PREF_INACTIVITY_DND_END = "inactivity_warnings_dnd_end";

    public static final String PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION = "heartrate_sleep_detection";
    public static final String PREF_HEARTRATE_MEASUREMENT_INTERVAL = "heartrate_measurement_interval";
    public static final String PREF_HEARTRATE_ACTIVITY_MONITORING = "heartrate_activity_monitoring";
    public static final String PREF_HEARTRATE_ALERT_ENABLED = "heartrate_alert_enabled";
    public static final String PREF_HEARTRATE_ALERT_THRESHOLD = "heartrate_alert_threshold";
    public static final String PREF_HEARTRATE_STRESS_MONITORING = "heartrate_stress_monitoring";

    public static final String PREF_AUTOHEARTRATE_SWITCH = "pref_autoheartrate_switch";
    public static final String PREF_AUTOHEARTRATE_SLEEP = "pref_autoheartrate_sleep";
    public static final String PREF_AUTOHEARTRATE_INTERVAL = "pref_autoheartrate_interval";
    public static final String PREF_AUTOHEARTRATE_START = "pref_autoheartrate_start";
    public static final String PREF_AUTOHEARTRATE_END = "pref_autoheartrate_end";

    public static final String PREF_POWER_MODE = "power_mode";
    public static final String PREF_BUTTON_BP_CALIBRATE = "prefs_sensors_button_bp_calibration";
    public static final String PREF_ALTITUDE_CALIBRATE = "pref_sensors_altitude";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO = "do_not_disturb_no_auto";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO_START = "do_not_disturb_no_auto_start";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO_END = "do_not_disturb_no_auto_end";
    public static final String PREF_DO_NOT_DISTURB = "do_not_disturb";
    public static final String PREF_DO_NOT_DISTURB_START = "do_not_disturb_start";
    public static final String PREF_DO_NOT_DISTURB_END = "do_not_disturb_end";
    public static final String PREF_DO_NOT_DISTURB_LIFT_WRIST = "do_not_disturb_lift_wrist";
    public static final String PREF_DO_NOT_DISTURB_OFF = "off";
    public static final String PREF_DO_NOT_DISTURB_AUTOMATIC = "automatic";
    public static final String PREF_DO_NOT_DISTURB_SCHEDULED = "scheduled";

    public static final String PREF_WORKOUT_START_ON_PHONE = "workout_start_on_phone";
    public static final String PREF_WORKOUT_SEND_GPS_TO_BAND = "workout_send_gps_to_band";

    public static final String PREF_FIND_PHONE = "prefs_find_phone";
    public static final String PREF_FIND_PHONE_DURATION = "prefs_find_phone_duration";
    public static final String PREF_AUTOLIGHT = "autolight";
    public static final String PREF_AUTOREMOVE_MESSAGE = "autoremove_message";
    public static final String PREF_AUTOREMOVE_NOTIFICATIONS = "autoremove_notifications";
    public static final String PREF_OPERATING_SOUNDS = "operating_sounds";
    public static final String PREF_KEY_VIBRATION = "key_vibration";
    public static final String PREF_FAKE_RING_DURATION = "fake_ring_duration";

    public static final String PREF_WORLD_CLOCKS = "pref_world_clocks";

    public static final String PREF_ANTILOST_ENABLED = "pref_antilost_enabled";
    public static final String PREF_HYDRATION_SWITCH = "pref_hydration_switch";
    public static final String PREF_HYDRATION_PERIOD = "pref_hydration_period";
    public static final String PREF_AMPM_ENABLED = "pref_ampm_enabled";

    public static final String PREF_SONYSWR12_LOW_VIBRATION = "vibration_preference";
    public static final String PREF_SONYSWR12_STAMINA = "stamina_preference";
    public static final String PREF_SONYSWR12_SMART_INTERVAL = "smart_alarm_interval_preference";

    public static final String PREF_BT_CONNECTED_ADVERTISEMENT = "bt_connected_advertisement";
    public static final String PREF_TRANSLITERATION_LANGUAGES = "pref_transliteration_languages";

    public static final String PREF_NOTHING_EAR1_INEAR = "pref_nothing_inear_detection";
    public static final String PREF_NOTHING_EAR1_AUDIOMODE = "pref_nothing_audiomode";

    public static final String PREF_GALAXY_BUDS_AMBIENT_MODE = "pref_galaxy_buds_ambient_mode";
    public static final String PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS = "pref_galaxy_buds_ambient_voice_focus";
    public static final String PREF_GALAXY_BUDS_AMBIENT_VOLUME = "pref_galaxy_buds_ambient_volume";
    public static final String PREF_GALAXY_BUDS_LOCK_TOUCH = "pref_galaxy_buds_lock_touch";
    public static final String PREF_GALAXY_BUDS_GAME_MODE = "pref_galaxy_buds_game_mode";
    public static final String PREF_GALAXY_BUDS_EQUALIZER = "pref_galaxy_buds_equalizer";
    public static final String PREF_GALAXY_BUDS_EQUALIZER_DOLBY = "pref_galaxy_buds_equalizer_dolby";
    public static final String PREF_GALAXY_BUDS_EQUALIZER_MODE = "pref_galaxy_buds_equalizer_mode";
    public static final String PREF_GALAXY_BUDS_TOUCH_LEFT = "pref_galaxy_buds_touch_left";
    public static final String PREF_GALAXY_BUDS_TOUCH_RIGHT = "pref_galaxy_buds_touch_right";
    public static final String PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH = "pref_galaxy_buds_touch_right_switch";
    public static final String PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH = "pref_galaxy_buds_touch_left_switch";
    public static final String PREF_GALAXY_BUDS_LIVE_ANC = "pref_galaxy_buds_live_anc";
    public static final String PREF_GALAXY_BUDS_PRESSURE_RELIEF = "pref_galaxy_buds_live_pressure_relief";
    public static final String PREF_GALAXY_BUDS_AMBIENT_SOUND = "pref_galaxy_buds_ambient_sound";
    public static final String PREF_GALAXY_BUDS_PRO_NOISE_CONTROL="pref_galaxy_buds_pro_noise_control";
    public static final String PREF_GALAXY_PRO_DOUBLE_TAP_EDGE ="pref_galaxy_pro_double_tap_edge";
    public static final String PREF_GALAXY_BUDS_PRO_IN_EAR_DETECTION ="pref_galaxy_buds_pro_in_ear_detection";
    public static final String PREF_GALAXY_BUDS_PRO_VOICE_DETECT ="pref_galaxy_buds_pro_voice_detect";
    public static final String PREF_GALAXY_BUDS_PRO_VOICE_DETECT_DURATION ="pref_galaxy_buds_pro_voice_detect_duration";
    public static final String PREF_GALAXY_BUDS_PRO_BALANCE="pref_galaxy_buds_pro_balance";
    public static final String PREF_GALAXY_BUDS_PRO_READ_NOTIFICATIONS_OUTLOUD ="pref_galaxy_buds_pro_read_notifications_outloud";
    public static final String PREF_GALAXY_BUDS_AMBIENT_MODE_DURING_CALL ="pref_galaxy_buds_ambient_mode_during_call";
    public static final String PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_RIGHT ="pref_galaxy_buds_pro_ambient_volume_right";
    public static final String PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_LEFT="pref_galaxy_buds_pro_ambient_volume_left";
    public static final String PREF_GALAXY_BUDS_PRO_AMBIENT_SOUND_TONE ="pref_galaxy_buds_pro_ambient_sound_tone";
    public static final String PREFS_NOISE_CONTROL_WITH_ONE_EARBUD ="pref_galaxy_buds_noise_controls_with_one_earbud";
    public static final String PREF_GALAXY_BUDS_PRO_ANC_LEVEL="pref_galaxy_buds_pro_anc_level";
    public static final String PREFS_GALAXY_BUDS_SEAMLESS_CONNECTION="prefs_galaxy_buds_seamless_connection";

    public static final String PREF_SONY_AUDIO_CODEC = "pref_sony_audio_codec";
    public static final String PREF_SONY_AMBIENT_SOUND_CONTROL = "pref_sony_ambient_sound_control";
    public static final String PREF_SONY_FOCUS_VOICE = "pref_sony_focus_voice";
    public static final String PREF_SONY_AMBIENT_SOUND_LEVEL = "pref_sony_ambient_sound_level";
    public static final String PREF_SONY_NOISE_OPTIMIZER_START = "pref_sony_noise_optimizer_start";
    public static final String PREF_SONY_NOISE_OPTIMIZER_CANCEL = "pref_sony_noise_optimizer_cancel";
    public static final String PREF_SONY_NOISE_OPTIMIZER_STATUS = "pref_sony_noise_optimizer_status";
    public static final String PREF_SONY_NOISE_OPTIMIZER_STATE_PRESSURE = "pref_sony_noise_optimizer_state_pressure";
    public static final String PREF_SONY_SOUND_POSITION = "pref_sony_sound_position";
    public static final String PREF_SONY_SURROUND_MODE = "pref_sony_surround_mode";
    public static final String PREF_SONY_EQUALIZER = "pref_sony_equalizer";
    public static final String PREF_SONY_EQUALIZER_MODE = "pref_sony_equalizer_mode";
    public static final String PREF_SONY_AUDIO_UPSAMPLING = "pref_sony_audio_upsampling";
    public static final String PREF_SONY_EQUALIZER_BAND_400 = "pref_sony_equalizer_band_400";
    public static final String PREF_SONY_EQUALIZER_BAND_1000 = "pref_sony_equalizer_band_1000";
    public static final String PREF_SONY_EQUALIZER_BAND_2500 = "pref_sony_equalizer_band_2500";
    public static final String PREF_SONY_EQUALIZER_BAND_6300 = "pref_sony_equalizer_band_6300";
    public static final String PREF_SONY_EQUALIZER_BAND_16000 = "pref_sony_equalizer_band_16000";
    public static final String PREF_SONY_EQUALIZER_BASS = "pref_sony_equalizer_bass";
    public static final String PREF_SONY_TOUCH_SENSOR = "pref_sony_touch_sensor";
    public static final String PREF_SONY_PAUSE_WHEN_TAKEN_OFF = "sony_pause_when_taken_off";
    public static final String PREF_SONY_BUTTON_MODE_LEFT = "pref_sony_button_mode_left";
    public static final String PREF_SONY_BUTTON_MODE_RIGHT = "pref_sony_button_mode_right";
    public static final String PREF_SONY_AUTOMATIC_POWER_OFF = "pref_sony_automatic_power_off";
    public static final String PREF_SONY_NOTIFICATION_VOICE_GUIDE = "pref_sony_notification_voice_guide";
    public static final String PREF_SONY_SPEAK_TO_CHAT = "pref_sony_speak_to_chat";
    public static final String PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY = "pref_sony_speak_to_chat_sensitivity";
    public static final String PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE = "pref_sony_speak_to_chat_focus_on_voice";
    public static final String PREF_SONY_SPEAK_TO_CHAT_TIMEOUT = "pref_sony_speak_to_chat_timeout";
    public static final String PREF_SONY_CONNECT_TWO_DEVICES = "pref_sony_connect_two_devices";

    public static final String PREF_QC35_NOISE_CANCELLING_LEVEL = "qc35_noise_cancelling_level";

    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD = "prefs_activity_in_device_card";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS = "prefs_activity_in_device_card_steps";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP = "prefs_activity_in_device_card_sleep";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE = "prefs_activity_in_device_card_distance";
    public static final String PREFS_DEVICE_CHARTS_TABS = "charts_tabs";

    public static final String PREF_UM25_SHOW_THRESHOLD_NOTIFICATION = "um25_current_threshold_notify";
    public static final String PREF_UM25_SHOW_THRESHOLD = "um25_current_threshold";

    public static final String PREF_VESC_MINIMUM_VOLTAGE = "vesc_minimum_battery_voltage";
    public static final String PREF_VESC_MAXIMUM_VOLTAGE = "vesc_maximum_battery_voltage";

    public static final String PREF_SOUNDS = "sounds";
    public static final String PREF_AUTH_KEY = "authkey";
    public static final String PREF_USER_FITNESS_GOAL = "fitness_goal";
    public static final String PREF_USER_FITNESS_GOAL_NOTIFICATION = "fitness_goal_notification";
}
