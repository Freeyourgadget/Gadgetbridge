/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, ivanovlev,
    JohnnySun, Julien Pivotto, Kasha, Steffen Liebergeld

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

import android.support.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

/**
 *
 */
public interface DeviceService extends EventHandler {
    String PREFIX = "nodomain.freeyourgadget.gadgetbridge.devices";

    String ACTION_MIBAND2_AUTH = PREFIX + ".action.miban2_auth";
    String ACTION_START = PREFIX + ".action.start";
    String ACTION_CONNECT = PREFIX + ".action.connect";
    String ACTION_NOTIFICATION = PREFIX + ".action.notification";
    String ACTION_DELETE_NOTIFICATION = PREFIX + ".action.delete_notification";
    String ACTION_CALLSTATE = PREFIX + ".action.callstate";
    String ACTION_SETCANNEDMESSAGES = PREFIX + ".action.setcannedmessages";
    String ACTION_SETTIME = PREFIX + ".action.settime";
    String ACTION_SETMUSICINFO = PREFIX + ".action.setmusicinfo";
    String ACTION_SETMUSICSTATE = PREFIX + ".action.setmusicstate";
    String ACTION_REQUEST_DEVICEINFO = PREFIX + ".action.request_deviceinfo";
    String ACTION_REQUEST_APPINFO = PREFIX + ".action.request_appinfo";
    String ACTION_REQUEST_SCREENSHOT = PREFIX + ".action.request_screenshot";
    String ACTION_STARTAPP = PREFIX + ".action.startapp";
    String ACTION_DELETEAPP = PREFIX + ".action.deleteapp";
    String ACTION_APP_CONFIGURE = PREFIX + ".action.app_configure";
    String ACTION_APP_REORDER = PREFIX + ".action.app_reorder";
    String ACTION_INSTALL = PREFIX + ".action.install";
    String ACTION_REBOOT = PREFIX + ".action.reboot";
    String ACTION_HEARTRATE_TEST = PREFIX + ".action.heartrate_test";
    String ACTION_FETCH_ACTIVITY_DATA = PREFIX + ".action.fetch_activity_data";
    String ACTION_DISCONNECT = PREFIX + ".action.disconnect";
    String ACTION_FIND_DEVICE = PREFIX + ".action.find_device";
    String ACTION_SET_CONSTANT_VIBRATION = PREFIX + ".action.set_constant_vibration";
    String ACTION_SET_ALARMS = PREFIX + ".action.set_alarms";
    String ACTION_ENABLE_REALTIME_STEPS = PREFIX + ".action.enable_realtime_steps";
    String ACTION_REALTIME_SAMPLES = PREFIX + ".action.realtime_samples";
    String ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT = PREFIX + ".action.realtime_hr_measurement";
    String ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT = PREFIX + ".action.enable_heartrate_sleep_support";
    String ACTION_HEARTRATE_MEASUREMENT = PREFIX + ".action.hr_measurement";
    String ACTION_ADD_CALENDAREVENT = PREFIX + ".action.add_calendarevent";
    String ACTION_DELETE_CALENDAREVENT = PREFIX + ".action.delete_calendarevent";
    String ACTION_SEND_CONFIGURATION = PREFIX + ".action.send_configuration";
    String ACTION_SEND_WEATHER = PREFIX + ".action.send_weather";
    String ACTION_TEST_NEW_FUNCTION = PREFIX + ".action.test_new_function";
    String EXTRA_NOTIFICATION_BODY = "notification_body";
    String EXTRA_NOTIFICATION_FLAGS = "notification_flags";
    String EXTRA_NOTIFICATION_ID = "notification_id";
    String EXTRA_NOTIFICATION_PHONENUMBER = "notification_phonenumber";
    String EXTRA_NOTIFICATION_SENDER = "notification_sender";
    String EXTRA_NOTIFICATION_SOURCENAME = "notification_sourcename";
    String EXTRA_NOTIFICATION_SUBJECT = "notification_subject";
    String EXTRA_NOTIFICATION_TITLE = "notification_title";
    String EXTRA_NOTIFICATION_TYPE = "notification_type";
    String EXTRA_FIND_START = "find_start";
    String EXTRA_VIBRATION_INTENSITY = "vibration_intensity";
    String EXTRA_CALL_COMMAND = "call_command";
    String EXTRA_CALL_PHONENUMBER = "call_phonenumber";
    String EXTRA_CALL_DISPLAYNAME = "call_displayname";
    String EXTRA_CANNEDMESSAGES = "cannedmessages";
    String EXTRA_CANNEDMESSAGES_TYPE = "cannedmessages_type";
    String EXTRA_MUSIC_ARTIST = "music_artist";
    String EXTRA_MUSIC_ALBUM = "music_album";
    String EXTRA_MUSIC_TRACK = "music_track";
    String EXTRA_MUSIC_DURATION = "music_duration";
    String EXTRA_MUSIC_TRACKNR = "music_tracknr";
    String EXTRA_MUSIC_TRACKCOUNT = "music_trackcount";
    String EXTRA_MUSIC_STATE = "music_state";
    String EXTRA_MUSIC_SHUFFLE = "music_shuffle";
    String EXTRA_MUSIC_REPEAT = "music_repeat";
    String EXTRA_MUSIC_POSITION = "music_position";
    String EXTRA_MUSIC_RATE = "music_rate";
    String EXTRA_APP_UUID = "app_uuid";
    String EXTRA_APP_START = "app_start";
    String EXTRA_APP_CONFIG = "app_config";
    String EXTRA_APP_CONFIG_ID = "app_config_id";
    String EXTRA_URI = "uri";
    String EXTRA_CONFIG = "config";
    String EXTRA_ALARMS = "alarms";
    String EXTRA_CONNECT_FIRST_TIME = "connect_first_time";
    String EXTRA_BOOLEAN_ENABLE = "enable_realtime_steps";

    String EXTRA_WEATHER_TIMESTAMP = "weather_timestamp";
    String EXTRA_WEATHER_LOCATION = "weather_location";
    String EXTRA_WEATHER_CURRENTTEMP = "weather_currenttemp";
    String EXTRA_WEATHER_CURRENTCONDITIONCODE = "weather_currentconditioncode";
    String EXTRA_WEATHER_CURRENTCONDITION = "currentcondition";
    String EXTRA_WEATHER_TODAYMAXTEMP = "weather_todaymaxtemp";
    String EXTRA_WEATHER_TODAYMINTEMP = "weather_todaymintemp";
    String EXTRA_WEATHER_TOMORROWMAXTEMP = "weather_tomorrowmaxtemp";
    String EXTRA_WEATHER_TOMORROWMINTEMP = "weather_tomorrowmintemp";
    String EXTRA_WEATHER_TOMORROWCONDITIONCODE = "weather_tomorrowconditioncode";

    /**
     * Use EXTRA_REALTIME_SAMPLE instead
     */
    @Deprecated
    String EXTRA_REALTIME_STEPS = "realtime_steps";
    String EXTRA_REALTIME_SAMPLE = "realtime_sample";
    String EXTRA_TIMESTAMP = "timestamp";
    /**
     * Use EXTRA_REALTIME_SAMPLE instead
     */
    @Deprecated
    String EXTRA_HEART_RATE_VALUE = "hr_value";
    String EXTRA_CALENDAREVENT_ID = "calendarevent_id";
    String EXTRA_CALENDAREVENT_TYPE = "calendarevent_type";
    String EXTRA_CALENDAREVENT_TIMESTAMP = "calendarevent_timestamp";
    String EXTRA_CALENDAREVENT_DURATION = "calendarevent_duration";
    String EXTRA_CALENDAREVENT_TITLE = "calendarevent_title";
    String EXTRA_CALENDAREVENT_DESCRIPTION = "calendarevent_description";
    String EXTRA_CALENDAREVENT_LOCATION = "calendarevent_location";

    void start();

    void connect();

    void connect(@Nullable GBDevice device);

    void connect(@Nullable GBDevice device, boolean performPair);

    void disconnect();

    void quit();

    /**
     * Requests information from the {@link DeviceCommunicationService} about the connection state,
     * firmware info, etc.
     * <p/>
     * Note that this will not need a connection to the device -- only the cached information
     * from the service will be reported.
     */
    void requestDeviceInfo();
}
