/*  Copyright (C) 2019-2024 115ek, akasaka / Genjitsu Labs, Alicia Hormann,
    Andreas Böhler, Andreas Shimokawa, Arjan Schrijver, Damien Gaignon, Daniel
    Dakhno, Daniele Gobbetti, Davis Mosenkovs, foxstidious, Gordon Williams,
    José Rebelo, Lukas, LukasEdl, mamucho, narektor, NekoBox, opavlov, Petr
    Vaněk, Yoran Vulker, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

public class DeviceSettingsPreferenceConst {
    public static final String PREF_HEADER_APPS = "pref_header_apps";
    public static final String PREF_HEADER_TIME = "pref_header_time";
    public static final String PREF_HEADER_DISPLAY = "pref_header_display";
    public static final String PREF_HEADER_HEALTH = "pref_header_health";
    public static final String PREF_HEADER_WORKOUT = "pref_header_workout";
    public static final String PREF_HEADER_WORKOUT_DETECTION = "pref_header_workout_detection";
    public static final String PREF_HEADER_GPS = "pref_header_gps";
    public static final String PREF_HEADER_AGPS = "pref_header_agps";
    public static final String PREF_HEADER_WIFI_HOTSPOT_CONFIGURATION = "pref_header_wifi_hotspot_configuration";
    public static final String PREF_HEADER_WIFI_HOTSPOT_STATUS = "pref_header_wifi_hotspot_status";
    public static final String PREF_HEADER_FTP_SERVER_STATUS = "pref_header_ftp_server_status";
    public static final String PREF_HEADER_FTP_SERVER_CONFIGURATION = "pref_header_ftp_server_configuration";

    public static final String PREF_BATTERY_SHOW_IN_NOTIFICATION = "battery_show_in_notification_";
    public static final String PREF_BATTERY_NOTIFY_LOW_ENABLED = "battery_notify_low_enabled_";
    public static final String PREF_BATTERY_NOTIFY_LOW_THRESHOLD = "battery_notify_low_threshold_";
    public static final String PREF_BATTERY_NOTIFY_FULL_ENABLED = "battery_notify_full_enabled_";
    public static final String PREF_BATTERY_NOTIFY_FULL_THRESHOLD = "battery_notify_full_threshold_";

    public static final String PREF_BATTERY_POLLING_ENABLE = "pref_battery_polling_enabled";
    public static final String PREF_BATTERY_POLLING_INTERVAL = "pref_battery_polling_interval";

    public static final String PREF_SCREEN_NIGHT_MODE = "pref_screen_night_mode";
    public static final String PREF_SCREEN_SLEEP_MODE = "pref_screen_sleep_mode";
    public static final String PREF_SCREEN_LIFT_WRIST = "pref_screen_lift_wrist";
    public static final String PREF_SCREEN_PASSWORD = "pref_screen_password";
    public static final String PREF_SCREEN_ALWAYS_ON_DISPLAY = "pref_screen_always_on_display";
    public static final String PREF_SCREEN_HEARTRATE_MONITORING = "pref_screen_heartrate_monitoring";
    public static final String PREF_SCREEN_INACTIVITY_EXTENDED = "pref_screen_inactivity_extended";
    public static final String PREF_SCREEN_SOUND_AND_VIBRATION = "pref_screen_sound_and_vibration";
    public static final String PREF_SCREEN_DO_NOT_DISTURB = "pref_screen_do_not_disturb";
    public static final String PREF_SCREEN_OFFLINE_VOICE = "pref_screen_offline_voice";
    public static final String PREF_SCREEN_WIFI_HOTSPOT = "pref_screen_wifi_hotspot";
    public static final String PREF_SCREEN_FTP_SERVER = "pref_screen_ftp_server";
    public static final String PREF_SCREEN_MORNING_UPDATES = "pref_morning_updates";

    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_LANGUAGE_AUTO = "auto";
    public static final String PREF_DEVICE_REGION = "device_region";
    public static final String PREF_DEVICE_NAME = "pref_device_name";
    public static final String PREF_DATEFORMAT = "dateformat";
    public static final String PREF_DATEFORMAT_AUTO = "auto";
    public static final String PREF_DATEFORMAT_DAY_MONTH = "day_month";
    public static final String PREF_DATEFORMAT_MONTH_DAY = "month_day";
    public static final String PREF_TIMEFORMAT = "timeformat";
    public static final String PREF_TIMEFORMAT_24H = "24h";
    public static final String PREF_TIMEFORMAT_12H = "am/pm";
    public static final String PREF_TIMEFORMAT_AUTO = "auto";
    public static final String PREF_WEARLOCATION = "wearlocation";
    public static final String PREF_WEARDIRECTION = "weardirection";
    public static final String PREF_WEARMODE = "wearmode";
    public static final String PREF_VIBRATION_ENABLE = "vibration_enable";
    public static final String PREF_NOTIFICATION_ENABLE = "notification_enable";
    public static final String PREF_SCREEN_BRIGHTNESS = "screen_brightness";
    public static final String PREF_SCREEN_AUTO_BRIGHTNESS = "screen_auto_brightness";
    public static final String PREF_SCREEN_ORIENTATION = "screen_orientation";
    public static final String PREF_SCREEN_TIMEOUT = "screen_timeout";
    public static final String PREF_RESERVER_ALARMS_CALENDAR = "reserve_alarms_calendar";
    public static final String PREF_RESERVE_REMINDERS_CALENDAR = "reserve_reminders_calendar";
    public static final String PREF_ALLOW_HIGH_MTU = "allow_high_mtu";
    public static final String PREF_SYNC_CALENDAR = "sync_calendar";
    public static final String PREF_TIME_SYNC = "time_sync";
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
    public static final String PREF_UPPER_BUTTON_LONG_PRESS = "pref_button_action_upper_long";
    public static final String PREF_LOWER_BUTTON_SHORT_PRESS = "pref_button_action_lower_short";
    public static final String PREF_VIBRATION_STRENGH_PERCENTAGE = "vibration_strength";
    public static final String PREF_RELAX_FIRMWARE_CHECKS = "relax_firmware_checks";

    public static final String PREF_DEVICE_GPS_UPDATE = "banglejs_gps_update";
    public static final String PREF_DEVICE_GPS_UPDATE_INTERVAL = "banglejs_gps_update_interval";
    public static final String PREF_DEVICE_GPS_USE_NETWORK_ONLY = "banglejs_gps_use_network_only";

    public static final String PREF_DEVICE_INTERNET_ACCESS = "device_internet_access";
    public static final String PREF_DEVICE_INTENTS = "device_intents";

    public static final String PREF_BANGLEJS_TEXT_BITMAP = "banglejs_text_bitmap";
    public static final String PREF_BANGLEJS_TEXT_BITMAP_SIZE = "banglejs_txt_bitmap_size";
    public static final String PREF_BANGLEJS_WEBVIEW_URL = "banglejs_webview_url";

    public static final String PREF_DISCONNECT_NOTIFICATION = "disconnect_notification";
    public static final String PREF_DISCONNECT_NOTIFICATION_START = "disconnect_notification_start";
    public static final String PREF_DISCONNECT_NOTIFICATION_END = "disconnect_notification_end";

    public static final String PREF_HYBRID_HR_FORCE_WHITE_COLOR = "force_white_color_scheme";
    public static final String PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES = "widget_draw_circles";
    public static final String PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES = "save_raw_activity_files";
    public static final String PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS = "dangerous_external_intents";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ENABLED = "activity_recognize_running_enabled";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ASK_FIRST = "activity_recognize_running_ask_first";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_MINUTES = "activity_recognize_running_minutes";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ENABLED = "activity_recognize_biking_enabled";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ASK_FIRST = "activity_recognize_biking_ask_first";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_MINUTES = "activity_recognize_biking_minutes";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ENABLED = "activity_recognize_walking_enabled";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ASK_FIRST = "activity_recognize_walking_ask_first";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_MINUTES = "activity_recognize_walking_minutes";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ENABLED = "activity_recognize_rowing_enabled";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ASK_FIRST = "activity_recognize_rowing_ask_first";
    public static final String PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_MINUTES = "activity_recognize_rowing_minutes";

    public static final String PREF_ACTIVATE_DISPLAY_ON_LIFT = "activate_display_on_lift_wrist";
    public static final String PREF_DISPLAY_ON_LIFT_START = "display_on_lift_start";
    public static final String PREF_DISPLAY_ON_LIFT_END = "display_on_lift_end";
    public static final String PREF_DISPLAY_ON_LIFT_SENSITIVITY = "display_on_lift_sensitivity";

    public static final String PREF_ALWAYS_ON_DISPLAY_MODE = "always_on_display_mode";
    public static final String PREF_ALWAYS_ON_DISPLAY_START = "always_on_display_start";
    public static final String PREF_ALWAYS_ON_DISPLAY_END = "always_on_display_end";
    public static final String PREF_ALWAYS_ON_DISPLAY_OFF = "off";
    public static final String PREF_ALWAYS_ON_DISPLAY_AUTOMATIC = "automatic";
    public static final String PREF_ALWAYS_ON_DISPLAY_ALWAYS = "always";
    public static final String PREF_ALWAYS_ON_DISPLAY_SCHEDULED = "scheduled";
    public static final String PREF_ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE = "always_on_display_follow_watchface";
    public static final String PREF_ALWAYS_ON_DISPLAY_STYLE = "always_on_display_style";

    public static final String PREF_VOLUME = "volume";
    public static final String PREF_CROWN_VIBRATION = "crown_vibration";
    public static final String PREF_ALERT_TONE = "alert_tone";
    public static final String PREF_COVER_TO_MUTE = "cover_to_mute";
    public static final String PREF_VIBRATE_FOR_ALERT = "vibrate_for_alert";
    public static final String PREF_TEXT_TO_SPEECH = "text_to_speech";

    public static final String PREF_OFFLINE_VOICE_RESPOND_TURN_WRIST = "offline_voice_respond_turn_wrist";
    public static final String PREF_OFFLINE_VOICE_RESPOND_SCREEN_ON = "offline_voice_respond_screen_on";
    public static final String PREF_OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING = "offline_voice_response_during_screen_lighting";
    public static final String PREF_OFFLINE_VOICE_LANGUAGE = "offline_voice_language";

    public static final String PREF_SLEEP_TIME = "prefs_enable_sleep_time";
    public static final String PREF_SLEEP_TIME_START = "prefs_sleep_time_start";
    public static final String PREF_SLEEP_TIME_END = "prefs_sleep_time_end";
    public static final String PREF_SLEEP_MODE_SCHEDULE_ENABLED = "sleep_mode_schedule_enabled";
    public static final String PREF_SLEEP_MODE_SCHEDULE_START = "sleep_mode_schedule_start";
    public static final String PREF_SLEEP_MODE_SCHEDULE_END = "sleep_mode_schedule_end";
    public static final String PREF_SLEEP_MODE_SLEEP_SCREEN = "pref_sleep_mode_sleep_screen";
    public static final String PREF_SLEEP_MODE_SMART_ENABLE = "pref_sleep_mode_smart_enable";

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
    public static final String PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD = "heartrate_alert_active_high_threshold";
    public static final String PREF_HEARTRATE_ALERT_HIGH_THRESHOLD = "heartrate_alert_threshold";
    public static final String PREF_HEARTRATE_ALERT_LOW_THRESHOLD = "heartrate_alert_low_threshold";
    public static final String PREF_HEARTRATE_STRESS_MONITORING = "heartrate_stress_monitoring";
    public static final String PREF_HEARTRATE_STRESS_RELAXATION_REMINDER = "heartrate_stress_relaxation_reminder";
    public static final String PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING = "heartrate_sleep_breathing_quality_monitoring";
    public static final String PREF_SPO2_ALL_DAY_MONITORING = "spo2_all_day_monitoring_enabled";
    public static final String PREF_SPO2_LOW_ALERT_THRESHOLD = "spo2_low_alert_threshold";

    public static final String PREF_AUTOHEARTRATE_SWITCH = "pref_autoheartrate_switch";
    public static final String PREF_AUTOHEARTRATE_SLEEP = "pref_autoheartrate_sleep";
    public static final String PREF_AUTOHEARTRATE_INTERVAL = "pref_autoheartrate_interval";
    public static final String PREF_AUTOHEARTRATE_START = "pref_autoheartrate_start";
    public static final String PREF_AUTOHEARTRATE_END = "pref_autoheartrate_end";

    public static final String PREF_POWER_MODE = "power_mode";
    public static final String PREF_CONNECTION_DURATION = "connection_duration";
    public static final String PREF_BUTTON_BP_CALIBRATE = "prefs_sensors_button_bp_calibration";
    public static final String PREF_ALTITUDE_CALIBRATE = "pref_sensors_altitude";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO = "do_not_disturb_no_auto";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO_START = "do_not_disturb_no_auto_start";
    public static final String PREF_DO_NOT_DISTURB_NOAUTO_END = "do_not_disturb_no_auto_end";
    public static final String PREF_DO_NOT_DISTURB = "do_not_disturb";
    public static final String PREF_DO_NOT_DISTURB_START = "do_not_disturb_start";
    public static final String PREF_DO_NOT_DISTURB_END = "do_not_disturb_end";
    public static final String PREF_DO_NOT_DISTURB_LIFT_WRIST = "do_not_disturb_lift_wrist";
    public static final String PREF_DO_NOT_DISTURB_NOT_WEAR = "do_not_disturb_not_wear";
    public static final String PREF_DO_NOT_DISTURB_OFF = "off";
    public static final String PREF_DO_NOT_DISTURB_AUTOMATIC = "automatic";
    public static final String PREF_DO_NOT_DISTURB_ALWAYS = "always";
    public static final String PREF_DO_NOT_DISTURB_SCHEDULED = "scheduled";
    public static final String PREF_DO_NOT_DISTURB_MO = "pref_do_not_disturb_mo";
    public static final String PREF_DO_NOT_DISTURB_TU = "pref_do_not_disturb_tu";
    public static final String PREF_DO_NOT_DISTURB_WE = "pref_do_not_disturb_we";
    public static final String PREF_DO_NOT_DISTURB_TH = "pref_do_not_disturb_th";
    public static final String PREF_DO_NOT_DISTURB_FR = "pref_do_not_disturb_fr";
    public static final String PREF_DO_NOT_DISTURB_SA = "pref_do_not_disturb_sa";
    public static final String PREF_DO_NOT_DISTURB_SU = "pref_do_not_disturb_su";

    public static final String PREF_CAMERA_REMOTE = "camera_remote";

    public static final String PREF_WORKOUT_START_ON_PHONE = "workout_start_on_phone";
    public static final String PREF_WORKOUT_SEND_GPS_TO_BAND = "workout_send_gps_to_band";
    public static final String PREF_WORKOUT_DETECTION_CATEGORIES = "workout_detection_categories";
    public static final String PREF_WORKOUT_DETECTION_ALERT = "workout_detection_alert";
    public static final String PREF_WORKOUT_DETECTION_SENSITIVITY = "workout_detection_sensitivity";

    public static final String PREF_GPS_MODE_PRESET = "pref_gps_mode_preset";
    public static final String PREF_GPS_BAND = "pref_gps_band";
    public static final String PREF_GPS_COMBINATION = "pref_gps_combination";
    public static final String PREF_GPS_SATELLITE_SEARCH = "pref_gps_satellite_search";
    public static final String PREF_AGPS_EXPIRY_REMINDER_ENABLED = "pref_agps_expiry_reminder_enabled";
    public static final String PREF_AGPS_EXPIRY_REMINDER_TIME = "pref_agps_expiry_reminder_time";
    public static final String PREF_AGPS_UPDATE_TIME = "pref_agps_update_time";
    public static final String PREF_AGPS_EXPIRE_TIME = "pref_agps_expire_time";
    public static final String PREF_AGPS_STATUS = "pref_agps_status";

    public static final String PREF_FIND_PHONE = "prefs_find_phone";
    public static final String PREF_FIND_PHONE_DURATION = "prefs_find_phone_duration";
    public static final String PREF_AUTOLIGHT = "autolight";

    public static final String PREF_PREVIEW_MESSAGE_IN_TITLE = "preview_message_in_title";

    public static final String PREF_CASIO_ALERT_CALENDAR = "casio_alert_calendar";

    public static final String PREF_CASIO_ALERT_CALL = "casio_alert_call";

    public static final String PREF_CASIO_ALERT_EMAIL = "casio_alert_email";

    public static final String PREF_CASIO_ALERT_OTHER = "casio_alert_other";

    public static final String PREF_CASIO_ALERT_SMS = "casio_alert_sms";
    public static final String PREF_LIGHT_DURATION_LONGER = "light_duration_longer";
    public static final String PREF_AUTOREMOVE_MESSAGE = "autoremove_message";
    public static final String PREF_SEND_APP_NOTIFICATIONS = "send_app_notifications";
    public static final String PREF_NOTIFICATION_WAKE_ON_OPEN = "notification_wake_on_open";
    public static final String PREF_AUTOREMOVE_NOTIFICATIONS = "autoremove_notifications";
    public static final String PREF_SCREEN_ON_ON_NOTIFICATIONS = "screen_on_on_notifications";
    public static final String PREF_WORKOUT_KEEP_SCREEN_ON = "workout_keep_screen_on";
    public static final String PREF_OPERATING_SOUNDS = "operating_sounds";
    public static final String PREF_KEY_VIBRATION = "key_vibration";
    public static final String PREF_FAKE_RING_DURATION = "fake_ring_duration";

    public static final String PREF_WORLD_CLOCKS = "pref_world_clocks";
    public static final String PREF_CONTACTS = "pref_contacts";
    public static final String PREF_WIDGETS = "pref_widgets";

    public static final String PREF_ANTILOST_ENABLED = "pref_antilost_enabled";
    public static final String PREF_HYDRATION_SWITCH = "pref_hydration_switch";
    public static final String PREF_HYDRATION_PERIOD = "pref_hydration_period";
    public static final String PREF_HYDRATION_DND = "pref_hydration_dnd";
    public static final String PREF_HYDRATION_DND_START = "pref_hydration_dnd_start";
    public static final String PREF_HYDRATION_DND_END = "pref_hydration_dnd_end";
    public static final String PREF_AMPM_ENABLED = "pref_ampm_enabled";

    public static final String PREF_SONYSWR12_LOW_VIBRATION = "vibration_preference";
    public static final String PREF_SONYSWR12_STAMINA = "stamina_preference";
    public static final String PREF_SONYSWR12_SMART_INTERVAL = "smart_alarm_interval_preference";

    public static final String PREF_BT_CONNECTED_ADVERTISEMENT = "bt_connected_advertisement";
    public static final String PREF_TRANSLITERATION_LANGUAGES = "pref_transliteration_languages";

    public static final String PREF_BLUETOOTH_CALLS_PAIR = "bluetooth_calls_pair";
    public static final String PREF_BLUETOOTH_CALLS_ENABLED = "bluetooth_calls_enabled";
    public static final String PREF_DISPLAY_CALLER = "display_caller";
    public static final String PREF_NOTIFICATION_DELAY_CALLS = "notification_delay_calls";
    public static final String PREF_CALL_REJECT_METHOD = "call_reject_method";

    public static final String WIFI_HOTSPOT_SSID = "wifi_hotspot_ssid";
    public static final String WIFI_HOTSPOT_PASSWORD = "wifi_hotspot_password";
    public static final String WIFI_HOTSPOT_START = "wifi_hotspot_start";
    public static final String WIFI_HOTSPOT_STOP = "wifi_hotspot_stop";
    public static final String WIFI_HOTSPOT_STATUS = "wifi_hotspot_status";

    public static final String PREF_APP_LOGS_START = "pref_app_logs_start";
    public static final String PREF_APP_LOGS_STOP = "pref_app_logs_stop";

    public static final String MORNING_UPDATES_ENABLED = "morning_updates_enabled";
    public static final String MORNING_UPDATES_CATEGORIES_SORTABLE = "morning_updates_categories";

    public static final String SHORTCUT_CARDS_SORTABLE = "shortcut_cards_sortable";

    public static final String PREF_WATCHFACE = "watchface";

    public static final String FTP_SERVER_ROOT_DIR = "ftp_server_root_dir";
    public static final String FTP_SERVER_ADDRESS = "ftp_server_address";
    public static final String FTP_SERVER_USERNAME = "ftp_server_username";
    public static final String FTP_SERVER_START = "ftp_server_start";
    public static final String FTP_SERVER_STOP = "ftp_server_stop";
    public static final String FTP_SERVER_STATUS = "ftp_server_status";

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
    public static final String PREF_GALAXY_BUDS_2_NOISE_CONTROL="pref_galaxy_buds_2_noise_control";
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
    public static final String PREF_SONY_PROTOCOL_VERSION = "pref_protocol_version";
    public static final String PREF_SONY_ACTUAL_PROTOCOL_VERSION = "pref_actual_protocol_version";
    public static final String PREF_SONY_AMBIENT_SOUND_CONTROL = "pref_sony_ambient_sound_control";
    public static final String PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL = "pref_soundcore_ambient_sound_control";
    public static final String PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING = "pref_adaptive_noise_cancelling";
    public static final String PREF_SOUNDCORE_WIND_NOISE_REDUCTION= "pref_soundcore_wind_noise_reduction";
    public static final String PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE = "pref_soundcore_transparency_vocal_mode";
    public static final String PREF_SOUNDCORE_WEARING_DETECTION = "pref_soundcore_wearing_detection";
    public static final String PREF_SOUNDCORE_WEARING_TONE = "pref_soundcore_wearing_tone";
    public static final String PREF_SOUNDCORE_TOUCH_TONE = "pref_soundcore_touch_tone";
    public static final String PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED = "pref_soundcore_control_single_tap_disabled";
    public static final String PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED = "pref_soundcore_control_double_tap_disabled";
    public static final String PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED = "pref_soundcore_control_triple_tap_disabled";
    public static final String PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED = "pref_soundcore_control_long_press_disabled";
    public static final String PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT = "pref_soundcore_control_single_tap_action_left";
    public static final String PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT = "pref_soundcore_control_double_tap_action_left";
    public static final String PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT = "pref_soundcore_control_triple_tap_action_left";
    public static final String PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT = "pref_soundcore_control_long_press_action_left";
    public static final String PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT = "pref_soundcore_control_single_tap_action_right";
    public static final String PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT = "pref_soundcore_control_double_tap_action_right";
    public static final String PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT = "pref_soundcore_control_triple_tap_action_right";
    public static final String PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT = "pref_soundcore_control_long_press_action_right";
    public static final String PREF_SOUNDCORE_VOICE_PROMPTS = "pref_soundcore_voice_prompts";
    public static final String PREF_SOUNDCORE_BUTTON_BRIGHTNESS = "pref_soundcore_button_brightness";
    public static final String PREF_SOUNDCORE_AUTO_POWER_OFF = "pref_soundcore_auto_power_off";
    public static final String PREF_SOUNDCORE_LDAC_MODE = "pref_soundcore_ldac_mode";
    public static final String PREF_SOUNDCORE_ADAPTIVE_DIRECTION = "pref_soundcore_adaptive_direction";
    public static final String PREF_SOUNDCORE_EQUALIZER_PRESET = "pref_soundcore_equalizer_preset";
    public static final String PREF_SOUNDCORE_EQUALIZER_CUSTOM = "pref_soundcore_equalizer_custom";
    public static final String PREF_SOUNDCORE_EQUALIZER_DIRECTION = "pref_soundcore_equalizer_direction";
    public static final String PREF_SOUNDCORE_EQUALIZER_RESET = "pref_soundcore_equalizer_reset";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND1_FREQ = "pref_soundcore_equalizer_band1_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND1_VALUE = "pref_soundcore_equalizer_band1_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND2_FREQ = "pref_soundcore_equalizer_band2_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND2_VALUE = "pref_soundcore_equalizer_band2_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND3_FREQ = "pref_soundcore_equalizer_band3_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND3_VALUE = "pref_soundcore_equalizer_band3_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND4_FREQ = "pref_soundcore_equalizer_band4_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND4_VALUE = "pref_soundcore_equalizer_band4_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND5_FREQ = "pref_soundcore_equalizer_band5_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND5_VALUE = "pref_soundcore_equalizer_band5_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND6_FREQ = "pref_soundcore_equalizer_band6_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND6_VALUE = "pref_soundcore_equalizer_band6_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND7_FREQ = "pref_soundcore_equalizer_band7_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND7_VALUE = "pref_soundcore_equalizer_band7_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND8_FREQ = "pref_soundcore_equalizer_band8_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND8_VALUE = "pref_soundcore_equalizer_band8_value";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND9_FREQ = "pref_soundcore_equalizer_band9_freq";
    public static final String PREF_SOUNDCORE_EQUALIZER_BAND9_VALUE = "pref_soundcore_equalizer_band9_value";
    public static final String PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE = "pref_sony_ambient_sound_control_button_mode";
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
    public static final String PREF_SONY_PAUSE_WHEN_TAKEN_OFF = "pref_sony_pause_when_taken_off";
    public static final String PREF_SONY_BUTTON_MODE_LEFT = "pref_sony_button_mode_left";
    public static final String PREF_SONY_BUTTON_MODE_RIGHT = "pref_sony_button_mode_right";
    public static final String PREF_SONY_QUICK_ACCESS_DOUBLE_TAP = "pref_sony_quick_access_double_tap";
    public static final String PREF_SONY_QUICK_ACCESS_TRIPLE_TAP = "pref_sony_quick_access_triple_tap";
    public static final String PREF_SONY_AUTOMATIC_POWER_OFF = "pref_sony_automatic_power_off";
    public static final String PREF_SONY_NOTIFICATION_VOICE_GUIDE = "pref_sony_notification_voice_guide";
    public static final String PREF_SONY_SPEAK_TO_CHAT = "pref_sony_speak_to_chat";
    public static final String PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY = "pref_sony_speak_to_chat_sensitivity";
    public static final String PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE = "pref_sony_speak_to_chat_focus_on_voice";
    public static final String PREF_SONY_SPEAK_TO_CHAT_TIMEOUT = "pref_sony_speak_to_chat_timeout";
    public static final String PREF_SONY_CONNECT_TWO_DEVICES = "pref_sony_connect_two_devices";
    public static final String PREF_SONY_ADAPTIVE_VOLUME_CONTROL = "pref_adaptive_volume_control";
    public static final String PREF_SONY_WIDE_AREA_TAP = "pref_wide_area_tap";

    public static final String PREF_MOONDROP_EQUALIZER_PRESET = "pref_moondrop_equalizer_preset";
    public static final String PREF_MOONDROP_TOUCH_PLAY_PAUSE_EARBUD = "pref_moondrop_touch_play_pause_earbud";
    public static final String PREF_MOONDROP_TOUCH_PLAY_PAUSE_TRIGGER = "pref_moondrop_touch_play_pause_trigger";
    public static final String PREF_MOONDROP_TOUCH_MEDIA_PREV_EARBUD = "pref_moondrop_touch_media_prev_earbud";
    public static final String PREF_MOONDROP_TOUCH_MEDIA_PREV_TRIGGER = "pref_moondrop_touch_media_prev_trigger";
    public static final String PREF_MOONDROP_TOUCH_MEDIA_NEXT_EARBUD = "pref_moondrop_touch_media_next_earbud";
    public static final String PREF_MOONDROP_TOUCH_MEDIA_NEXT_TRIGGER = "pref_moondrop_touch_media_next_trigger";
    public static final String PREF_MOONDROP_TOUCH_CALL_PICK_HANG_EARBUD = "pref_moondrop_touch_call_pick_hang_earbud";
    public static final String PREF_MOONDROP_TOUCH_CALL_PICK_HANG_TRIGGER = "pref_moondrop_touch_call_pick_hang_trigger";
    public static final String PREF_MOONDROP_TOUCH_CALL_START_EARBUD = "pref_moondrop_touch_call_start_earbud";
    public static final String PREF_MOONDROP_TOUCH_CALL_START_TRIGGER = "pref_moondrop_touch_call_start_trigger";
    public static final String PREF_MOONDROP_TOUCH_ASSISTANT_EARBUD = "pref_moondrop_touch_assistant_earbud";
    public static final String PREF_MOONDROP_TOUCH_ASSISTANT_TRIGGER = "pref_moondrop_touch_assistant_trigger";
    public static final String PREF_MOONDROP_TOUCH_ANC_MODE_EARBUD = "pref_moondrop_touch_anc_mode_earbud";
    public static final String PREF_MOONDROP_TOUCH_ANC_MODE_TRIGGER = "pref_moondrop_touch_anc_mode_trigger";

    public static final String PREF_MISCALE_WEIGHT_UNIT = "pref_miscale_weight_unit";
    public static final String PREF_MISCALE_SMALL_OBJECTS = "pref_miscale_small_objects";

    public static final String PREF_QC35_NOISE_CANCELLING_LEVEL = "qc35_noise_cancelling_level";

    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD = "prefs_activity_in_device_card";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS = "prefs_activity_in_device_card_steps";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP = "prefs_activity_in_device_card_sleep";
    public static final String PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE = "prefs_activity_in_device_card_distance";
    public static final String PREFS_DEVICE_CHARTS_TABS = "charts_tabs";
    public static final String PREFS_PER_APP_NOTIFICATION_SETTINGS = "pref_per_app_notification_settings";

    public static final String PREF_UM25_SHOW_THRESHOLD_NOTIFICATION = "um25_current_threshold_notify";
    public static final String PREF_UM25_SHOW_THRESHOLD = "um25_current_threshold";

    public static final String PREF_VESC_MINIMUM_VOLTAGE = "vesc_minimum_battery_voltage";
    public static final String PREF_VESC_MAXIMUM_VOLTAGE = "vesc_maximum_battery_voltage";

    public static final String PREF_SOUNDS = "sounds";
    public static final String PREF_AUTH_KEY = "authkey";
    public static final String PREF_USER_FITNESS_GOAL = "fitness_goal";
    public static final String PREF_USER_FITNESS_GOAL_NOTIFICATION = "fitness_goal_notification";
    public static final String PREF_USER_FITNESS_GOAL_SECONDARY = "fitness_goal_secondary";
    public static final String PREF_VITALITY_SCORE_7_DAY = "pref_vitality_score_7_day";
    public static final String PREF_VITALITY_SCORE_DAILY = "pref_vitality_score_daily";

    public static final String PREF_PHONE_SILENT_MODE = "phone_silent_mode";

    public static final String PREF_HOURLY_CHIME_ENABLE = "hourly_chime_enable";
    public static final String PREF_HOURLY_CHIME_START = "hourly_chime_start";
    public static final String PREF_HOURLY_CHIME_END = "hourly_chime_end";

    public static final String PREF_VOICE_SERVICE_LANGUAGE = "voice_service_language";

    public static final String PREF_TEMPERATURE_SCALE_CF = "temperature_scale_cf";

    public static final String PREF_FAKE_ANDROID_ID = "fake_android_id";

    public static final String PREF_HEARTRATE_AUTOMATIC_ENABLE = "heartrate_automatic_enable";
    public static final String PREF_SPO_AUTOMATIC_ENABLE = "spo_automatic_enable";

    public static final String PREF_FORCE_OPTIONS = "pref_force_options";
    public static final String PREF_FORCE_ENABLE_SMART_ALARM = "pref_force_enable_smart_alarm";
    public static final String PREF_FORCE_ENABLE_WEAR_LOCATION = "pref_force_enable_wear_location";
    public static final String PREF_FORCE_DND_SUPPORT = "pref_force_dnd_support";
    public static final String PREF_FORCE_ENABLE_HEARTRATE_SUPPORT = "pref_force_enable_heartrate_support";
    public static final String PREF_FORCE_ENABLE_SPO2_SUPPORT = "pref_force_enable_spo2_support";
    public static final String PREF_IGNORE_WAKEUP_STATUS_START = "pref_force_ignore_wakeup_status_start";
    public static final String PREF_IGNORE_WAKEUP_STATUS_END = "pref_force_ignore_wakeup_status_end";

    public static final String PREF_FEMOMETER_MEASUREMENT_MODE = "femometer_measurement_mode";

    public static final String PREF_PREFIX_NOTIFICATION_WITH_APP = "pref_prefix_notification_with_app";
    public static final String PREF_DEVICE_ACTION_SELECTION_BROADCAST = "BROADCAST";
    public static final String PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS = "events_forwarding_fellsleep_action_selections";
    public static final String PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST = "prefs_events_forwarding_fellsleep_broadcast";
    public static final String PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS = "events_forwarding_wokeup_action_selections";
    public static final String PREF_DEVICE_ACTION_WOKE_UP_BROADCAST = "prefs_events_forwarding_wokeup_broadcast";
    public static final String PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS = "events_forwarding_startnonwear_action_selections";
    public static final String PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST = "prefs_events_forwarding_startnonwear_broadcast";
    public static final String PREF_CLAP_HANDS_TO_WAKEUP_DEVICE = "pref_key_clap_hands_to_wakeup_device";
    public static final String PREF_POWER_SAVING = "pref_key_power_saving";
    public static final String PREF_FORCE_CONNECTION_TYPE = "pref_force_connection_type";

    public static final String PREF_AUTO_REPLY_INCOMING_CALL = "pref_auto_reply_phonecall";
    public static final String PREF_AUTO_REPLY_INCOMING_CALL_DELAY = "pref_auto_reply_phonecall_delay";
    public static final String PREF_SPEAK_NOTIFICATIONS_ALOUD = "pref_speak_notifications_aloud";
    public static final String PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE = "pref_speak_notifications_focus_exclusive";

    public static final String PREF_CYCLING_SENSOR_PERSISTENCE_INTERVAL = "pref_cycling_persistence_interval";
    public static final String PREF_CYCLING_SENSOR_WHEEL_DIAMETER = "pref_cycling_wheel_diameter";
}
