/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class MoyoungConstants {
    // (*) - based only on static reverse engineering of the original app code,
    //       not supported by my watch so not implemented
    //       (or at least I didn't manage to get any response out of it)

    // (?) - not checked


    // The device communicates by sending packets by writing to UUID_CHARACTERISTIC_DATA_OUT
    // in MTU-sized chunks. The value of MTU seems to be somehow changeable (?), but the default
    // is 20. Responses are received via notify on UUID_CHARACTERISTIC_DATA_IN in similar format.
    // The write success notification comes AFTER the responses.

    // Packet format:
    // packet[0] = 0xFE;
    // packet[1] = 0xEA;
    // if (MTU == 20) // could be a protocol version check?
    // {
    //     packet[2] = 16;
    //     packet[3] = packet.length;
    // }
    // else
    // {
    //     packet[2] = 32 + (packet.length >> 8) & 0xFF;
    //     packet[3] = packet.length & 0xFF;
    // }
    // packet[4] = packetType;
    // packet[5:] = payload;

    // Protocol version is determined by reading manufacturer name. MOYOUNG for old fixed-size
    // or MOYOUNG-V2 for MTU. The non-MTU version uses packets of size 256
    // for firmware >= 1.6.5, and 64 otherwise.

    // The firmware version is also used to detect availability of some features.

    // Additionally, there seems to be a trace of special packets with cmd 1 and 2, that are sent
    // to UUID_CHARACTERISTIC_DATA_SPECIAL_1 and UUID_CHARACTERISTIC_DATA_SPECIAL_2 instead.
    // They don't appear on my watch though.

    // The response to CMD_ECG is special and is returned using UUID_CHARACTERISTIC_DATA_ECG_OLD
    // or UUID_CHARACTERISTIC_DATA_ECG_NEW. The old version is clearly labeled as old in the
    // unobfuscated part of the code. If both of them exist, old is used (but I presume only one
    // of them is supposed to exist at a time). They also don't appear on my watch as it doesn't
    // support ECG.

    // In addition to the proprietary protocol described above, the following standard BLE services
    // are used:
    // * org.bluetooth.service.generic_access for device name
    // * org.bluetooth.service.device_information for manufacturer, model, serial number and
    //   firmware version
    // * org.bluetooth.service.battery_service for battery level
    // * org.bluetooth.service.heart_rate is exposed, but doesn't seem to work
    // * org.bluetooth.service.human_interface_device is exposed, but not even mentioned
    //   in the official app (?) - needs further research
    // * the custom UUID_CHARACTERISTIC_STEPS is used to sync the pedometer data in real time
    //   via READ or NOTIFY - it's identical to the "sync past data" packet
    //   ({distance:uint24, steps:uint24, calories:uint24})
    // * (?) 0000FEE7-0000-1000-8000-00805F9B34FB another custom service
    //   (NOT UUID_CHARACTERISTIC_DATA_ECG_OLD!!!) not mentioned anywhere in the official app,
    //   containing the following characteristics:
    //   * 0000FEA1-0000-1000-8000-00805F9B34FB - READ, NOTIFY
    //   * 0000FEC9-0000-1000-8000-00805F9B34FB - READ

    // The above standard services are internally handled by the app using the following
    // "packet numbers":
    // * 16 - query steps
    // * 17 - firmware version
    // * 18 - query battery
    // * 19 - DFU status (queries model number, looks for the string DFU and a number == 0 or != 0)
    // * 20 - protocol version (queries manufacturer name, see description above)


    public static final UUID UUID_SERVICE_MOYOUNG = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "feea"));
    public static final UUID UUID_CHARACTERISTIC_STEPS          = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee1"));
    public static final UUID UUID_CHARACTERISTIC_DATA_OUT       = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee2"));
    public static final UUID UUID_CHARACTERISTIC_DATA_IN        = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee3"));
    public static final UUID UUID_CHARACTERISTIC_DATA_SPECIAL_1 = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee5")); // (*)
    public static final UUID UUID_CHARACTERISTIC_DATA_SPECIAL_2 = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee6")); // (*)
    public static final UUID UUID_CHARACTERISTIC_DATA_ECG_OLD   = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee7")); // (*)
    public static final UUID UUID_CHARACTERISTIC_DATA_ECG_NEW   = UUID.fromString(String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fee8")); // (*)


    // Special
    public static final byte CMD_SHUTDOWN = 81;                                     //     {-1}
    public static final byte CMD_FIND_MY_WATCH = 97;                                //     {}
    public static final byte CMD_FIND_MY_PHONE = 98;                                // (*) outgoing {-1} to stop, incoming {0} start, {!=0} stop
    public static final byte CMD_HS_DFU = 99;                                       // (?) {1} - enableHsDfu(), {0} - queryHsDfuAddress()


    // Activity/training tracking

    // CMD_QUERY_LAST_DYNAMIC_RATE is triggered immediately after a training recording is finished on the watch.
    // The watch sends CMD_QUERY_LAST_DYNAMIC_RATE command to the phone with the first part of the data, and then
    // the phone is supposed to respond with empty CMD_QUERY_LAST_DYNAMIC_RATE to retrieve the next part.
    // There seems to be no way to query this data later, or to start communication from phone side.
    // The data format is uint32 date_recorded, uint8 heart_rate[] (where 0 is invalid measurement and
    // data is recorded every 1 minute)

    // CMD_QUERY_MOVEMENT_HEART_RATE returns the summary of last 3 trainings recorded on the watch.
    // This is a cyclic buffer, so the watch will first overwrite entry number 0, then 1, then 2, then 0 again

    // CMD_QUERY_PAST_HEART_RATE_1 and CMD_QUERY_PAST_HEART_RATE_2 don't seem to work at all on my watch.

    // All "date recorded" values are in the hardcoded GMT+8 watch timezone

    public static final byte CMD_QUERY_LAST_DYNAMIC_RATE = 52;                      //     TRANSMISSION TRIGGERED FROM WATCH SIDE AFTER FINISHED TRAINING. Does custom packet splitting. The packet takes no data as input. Send the query repeatedly until you get all the data. THE FIRST PACKET IS SENT BY THE WATCH - THE PHONE QUERIES THIS COMMAND TO GET THE NEXT PART. The response starts with one byte: 0 for first packet, 1 for continuation packet, 2 for end of data. 0,time:uint32,measurement:uint8[] 1,measurement:uint8[] 1,measurement:uint8[] 2
    public static final byte CMD_QUERY_PAST_HEART_RATE_1 = 53;                      // (*) Two arrays built of 4 packets each. See below. todayHeartRate(1) starts at 0 and ends at 3, yesterdayHeartRate() starts at 4 and ends at 7. Sampled every 5 minutes.
    public static final byte CMD_QUERY_PAST_HEART_RATE_2 = 54;                      // (*) An array built of 20 packets. The packet takes the index as input. i.e. {x} -> {data[N*x], data[N*x+1], ..., data[N*x+N-1]} for x in 0-19   -- todayHeartRate(2). Sampled every 1 minute.
    public static final byte CMD_QUERY_MOVEMENT_HEART_RATE = 55;                    //     {} -> One packet with 3 entries of 24 bytes each {startTime:uint32, endTime:uint32, validTime:uint16, entry_number:uint8, type:uint8, steps:uint32, distance:uint32, calories:uint16}, everything little endian

    // first byte for CMD_QUERY_LAST_DYNAMIC_RATE packets
    public static final byte ARG_TRANSMISSION_FIRST = 0;
    public static final byte ARG_TRANSMISSION_NEXT = 1;
    public static final byte ARG_TRANSMISSION_LAST = 2; // note: last packet always empty

    // Health measurements
    public static final byte CMD_QUERY_TIMING_MEASURE_HEART_RATE = 47;              // (*) {} -> ???
    public static final byte CMD_SET_TIMING_MEASURE_HEART_RATE = 31;                // (*) {i}, i >= 0, 0 is disabled
    public static final byte CMD_START_STOP_MEASURE_DYNAMIC_RATE = 104;             // (*) {enabled ? 0 : -1}

    public static final byte HR_INTERVAL_OFF = 0;
    public static final byte HR_INTERVAL_5MIN = 1;
    public static final byte HR_INTERVAL_10MIN = 2;
    public static final byte HR_INTERVAL_20MIN = 4;
    public static final byte HR_INTERVAL_30MIN = 6;

    public static final byte CMD_TRIGGER_MEASURE_BLOOD_PRESSURE = 105;              // (?) {0, 0, 0} to start, {-1, -1, -1} to stop -> {unused?, num1, num2}
    public static final byte CMD_TRIGGER_MEASURE_BLOOD_OXYGEN = 107;                // (?) {start ? 0 : -1} -> {num}
    public static final byte CMD_TRIGGER_MEASURE_HEARTRATE = 109;                   //     {start ? 0 : -1} -> {bpm}
    public static final byte CMD_ECG = 111;                                         // (?) {heart_rate} or {1} to start or {0} to stop or {2} to query
    // ECG data is special and comes from UUID_CHARACTERISTIC_DATA_ECG_OLD or UUID_CHARACTERISTIC_DATA_ECG_NEW


    // Functionality
    public static final byte CMD_SYNC_TIME = 49;                                    //     {time >> 24, time >> 16, time >> 8, time, 8}, time is a timestamp in seconds in GMT+8

    public static final byte CMD_SYNC_SLEEP = 50;                                   //     {} -> {type, start_h, start_m}, repeating, type is SOBER(0),LIGHT(1),RESTFUL(2)
    public static final byte CMD_SYNC_PAST_SLEEP_AND_STEP = 51;                     //     {b (see below)} -> {x<=2, distance:uint24, steps:uint24, calories:uint24} or {x>2, (sleep data like above)} - two functions same CMD

    // NOTE: these names are as specified in the original app. They do NOT match what my watch actually does. See note in FetchDataOperation.
    public static final byte ARG_SYNC_YESTERDAY_STEPS = 1;
    public static final byte ARG_SYNC_DAY_BEFORE_YESTERDAY_STEPS = 2;
    public static final byte ARG_SYNC_YESTERDAY_SLEEP = 3;
    public static final byte ARG_SYNC_DAY_BEFORE_YESTERDAY_SLEEP = 4;

    public static final byte SLEEP_SOBER = 0;
    public static final byte SLEEP_LIGHT = 1;
    public static final byte SLEEP_RESTFUL = 2;

    public static final byte CMD_QUERY_SLEEP_ACTION = 58;                           // (*) {i} -> {hour, x[60]}

    public static final byte CMD_SEND_MESSAGE = 65;                                 //     {type, message[]}, message is encoded with manual splitting by String.valueOf(0x2080)
    //                       CMD_SEND_CALL_OFF_HOOK = 65;                           //     {-1} - the same ID as above, different arguments

    public static final byte CMD_SET_WEATHER_FUTURE = 66;                           //     {weatherId, low_temp, high_temp} * 7
    public static final byte CMD_SET_WEATHER_TODAY = 67;                            //     {have_pm25 ? 1 : 0, weatherId, temp[, pm25 >> 8, pm25], lunar_or_festival[8], city[8]}, names are UTF-16BE encoded (4 characters each!)
    public static final byte CMD_SET_WEATHER_LOCATION = 69;                         //     {string utf8}
    public static final byte CMD_SET_SUNRISE_SUNSET = -75;                          //     {5 bytes unknown, sunrise hour, sunrise min, sunset hour, sunset min, string (location utf8)}

    public static final byte CMD_SET_MUSIC_INFO = 68;                               //     {artist=1/track=0, string}
    public static final byte CMD_SET_MUSIC_STATE = 123;                             //     {is_playing ? 1 : 0}

    public static final byte CMD_GSENSOR_CALIBRATION = 82;                          // (?) {}

    public static final byte CMD_QUERY_STEPS_CATEGORY = 89;                         // (*) {i} -> {0, data:uint16[*]}, {1}, {2, data:uint16[*]}, {3}, query 0+1 together and 2+3 together
    //public static final byte ARG_QUERY_STEPS_CATEGORY_TODAY_STEPS = 0;
    //public static final byte ARG_QUERY_STEPS_CATEGORY_YESTERDAY_STEPS = 2;

    public static final byte CMD_SWITCH_CAMERA_VIEW = 102;                          //     {} -> {}, outgoing open screen, incoming take photo

    public static final byte CMD_NOTIFY_PHONE_OPERATION = 103;                      //     ONLY INCOMING! -> {x}, x -> 0 = play/pause, 1 = prev, 2 = next, 3 = reject incoming call)
    public static final byte CMD_NOTIFY_WEATHER_CHANGE = 100;                       //     ONLY INCOMING! -> {} - when the watch really wants us to retransmit the weather again (it seems to often happen after stopping training - running the training blocks access to main menu so I guess it restarts afterwards or something). Will repeat whenever navigating the menu where the weather should be, and weather won't be visible on watch screen until that happens.

    public static final byte ARG_OPERATION_PLAY_PAUSE = 0;
    public static final byte ARG_OPERATION_PREV_SONG = 1;
    public static final byte ARG_OPERATION_NEXT_SONG = 2;
    public static final byte ARG_OPERATION_DROP_INCOMING_CALL = 3;
    public static final byte ARG_OPERATION_VOLUME_UP = 4;
    public static final byte ARG_OPERATION_VOLUME_DOWN = 5;
    public static final byte ARG_OPERATION_PLAY = 6;
    public static final byte ARG_OPERATION_PAUSE = 7;
    public static final byte ARG_OPERATION_SEND_CURRENT_VOLUME = 12;                //     {0x00-0x10}

    public static final byte CMD_QUERY_ALARM_CLOCK = 33;                            // (?) {} -> a list of entries like below
    public static final byte CMD_SET_ALARM_CLOCK = 17;                              // (?) {id, enable ? 1 : 0, repeat, hour, minute, i >> 8, i, repeatMode}, repeatMode is 0(SINGLE), 127(EVERYDAY), or bitmask of 1,2,4,8,16,32,64(SUNDAY-SATURDAY) is 0,1,2, i is ((year << 12) + (month << 8) + day) where year is 2015-based, month and day start at 1 for repeatMode=SINGLE and 0 otherwise, repeat is 0(SINGLE),1(EVERYDAY),2(OTHER)

    public static final byte CMD_ADVANCED_QUERY = (byte) 0xb9;
    public static final byte CMD_DAGPT = (byte) 0xbb;

    public static final byte ARG_ADVANCED_SET_ALARM = 0x05;
    public static final byte ARG_ADVANCED_SET_CALENDAR = 0x08;
    public static final byte ARG_ADVANCED_QUERY_STOCKS = 0x0e;
    public static final byte ARG_ADVANCED_QUERY_ALARMS = 0x15;

    public static final byte ARG_ALARM_SET = 0x00;
    public static final byte ARG_ALARM_DELETE = 0x02;
    public static final byte ARG_ALARM_FROM_WATCH = 0x04;

    public static final byte ARG_CALENDAR_ADD_ITEM = 0x00;
    public static final byte ARG_CALENDAR_DISABLE = 0x04;
    public static final byte ARG_CALENDAR_FINISHED = 0x05;
    public static final byte ARG_CALENDAR_CLEAR = 0x06;

    public static final int MAX_CALENDAR_ITEMS = 12;  // Tested only on Colmi i28 Ultra, move to coordinator if different on other devices

    // Settings
    public static final byte CMD_SET_USER_INFO = 18;                                // (?) {height, weight, age, gender}, MALE = 0, FEMALE = 1

    public static final byte CMD_QUERY_DOMINANT_HAND = 36;                          // (*) {} -> {value}
    public static final byte CMD_SET_DOMINANT_HAND = 20;                            // (*) {value}

    public static final byte CMD_QUERY_DISPLAY_DEVICE_FUNCTION = 37;                // (*) {} - current, {-1} - list all supported -> {[-1, ], ...} (prefixed with -1 if lists supported, nothing otherwise)
    public static final byte CMD_SET_DISPLAY_DEVICE_FUNCTION = 21;                  // (*) {..., 0} - null terminated list of functions to enable

    public static final byte CMD_QUERY_GOAL_STEP = 38;                              //     {} -> {value, value >> 8, value >> 16, value >> 24}   // this has the endianness swapped between query and set
    public static final byte CMD_SET_GOAL_STEP = 22;                                //     {value >> 24, value >> 16, value >> 8, value}         // yes, really

    public static final byte CMD_QUERY_TIME_SYSTEM = 39;                            //     {} -> {value}
    public static final byte CMD_SET_TIME_SYSTEM = 23;                              //     {value}

    // quick view = enable display when wrist is lifted
    public static final byte CMD_QUERY_QUICK_VIEW = 40;                             //     {} -> {value}
    public static final byte CMD_SET_QUICK_VIEW = 24;                               //     {enabled ? 1 : 0}

    public static final byte CMD_QUERY_DISPLAY_WATCH_FACE = 41;                     //     {} -> {value}
    public static final byte CMD_SET_DISPLAY_WATCH_FACE = 25;                       //     {value}

    public static final byte CMD_QUERY_METRIC_SYSTEM = 42;                          //     {} -> {value}
    public static final byte CMD_SET_METRIC_SYSTEM = 26;                            //    {value}

    public static final byte CMD_QUERY_DEVICE_LANGUAGE = 43;                        //     {} -> {value, bitmask_of_supported_langs:uint32}
    public static final byte CMD_SET_DEVICE_LANGUAGE = 27;                          //     {new_value}

    // enables "other" (as in "not a messaging app") on the notifications configuration screen in the official app
    // seems to be used only in the app, not sure why they even store it on the watch
    public static final byte CMD_QUERY_OTHER_MESSAGE_STATE = 44;                    //     {} -> {value}
    public static final byte CMD_SET_OTHER_MESSAGE_STATE = 28;                      //     {enabled ? 1 : 0}

    public static final byte CMD_QUERY_SEDENTARY_REMINDER = 45;                     //     {} -> {value}
    public static final byte CMD_SET_SEDENTARY_REMINDER = 29;                       //     {enabled ? 1 : 0}

    public static final byte CMD_QUERY_DEVICE_VERSION = 46;                         //     {} -> {value}
    public static final byte CMD_SET_DEVICE_VERSION = 30;                           //     {new_value}

    public static final byte CMD_QUERY_WATCH_FACE_LAYOUT = 57;                      // (*) {} -> {time_position, time_top_content, time_bottom_content, text_color >> 8, text_color, background_picture_md5[32]}
    public static final byte CMD_SET_WATCH_FACE_LAYOUT = 56;                        // (*) {time_position, time_top_content, time_bottom_content, text_color >> 8, text_color, background_picture_md5[32]}, text_color is R5G6B5, background_picture is stored as hex digits (numbers 0-15 not chars '0'-'F' !)

    public static final byte CMD_SET_STEP_LENGTH = 84;                              // (?) {value}

    public static final byte CMD_QUERY_DO_NOT_DISTURB_TIME = -127;                  //     {} -> {start >> 8, start, end >> 8, end} these are 16-bit values (somebody was drunk while writing this or what?)
    public static final byte CMD_SET_DO_NOT_DISTURB_TIME = 113;                     //     {start_hour, start_min, end_hour, end_min}

    public static final byte CMD_QUERY_QUICK_VIEW_TIME = -126;                      //     {} -> {start >> 8, start, end >> 8, end} these are 16-bit values (somebody was drunk while writing this or what?)
    public static final byte CMD_SET_QUICK_VIEW_TIME = 114;                         //     {start_hour, start_min, end_hour, end_min}

    public static final byte CMD_QUERY_REMINDERS_TO_MOVE_PERIOD = -125;             //     {} -> {period, steps, start_hour, end_hour}
    public static final byte CMD_SET_REMINDERS_TO_MOVE_PERIOD = 115;                //     {period, steps, start_hour, end_hour}

    public static final byte CMD_QUERY_SUPPORT_WATCH_FACE = -124;                   // (*) {} -> {count >> 8, count, ...}

    public static final byte CMD_QUERY_PSYCHOLOGICAL_PERIOD = -123;                 // (*) {} -> ??? (too lazy to check, sorry :P)
    public static final byte CMD_SET_PSYCHOLOGICAL_PERIOD = 117;                    // (*) {encodeConfiguredReminders(info), 15, info.getPhysiologcalPeriod(), info.getMenstrualPeriod(), info.startDate.get(Calendar.MONTH), info.startDate.get(Calendar.DATE), info.getReminderHour(), info.getReminderMinute(), info.getReminderHour(), info.getReminderMinute(), info.getReminderHour(), info.getReminderMinute(), info.getReminderHour(), info.getReminderMinute()}
    //    encodeConfiguredReminders(CRPPhysiologcalPeriodInfo info) {
    //        int i = info.isMenstrualReminder() ? 241 : 240;
    //        if (info.isOvulationReminder())
    //            i += 2;
    //        if (info.isOvulationDayReminder())
    //            i += 4;
    //        if (info.isOvulationEndReminder())
    //            i += 8;
    //        return (byte) i;
    //    }

    // no idea what this does
    public static final byte CMD_QUERY_BREATHING_LIGHT = -120;                      //     {} -> {value}
    public static final byte CMD_SET_BREATHING_LIGHT = 120;                         //     {enabled ? 1 : 0}

    public static final byte TRAINING_TYPE_WALK = 0;
    public static final byte TRAINING_TYPE_RUN = 1;
    public static final byte TRAINING_TYPE_BIKING = 2;
    public static final byte TRAINING_TYPE_ROPE = 3;
    public static final byte TRAINING_TYPE_BADMINTON = 4;
    public static final byte TRAINING_TYPE_BASKETBALL = 5;
    public static final byte TRAINING_TYPE_FOOTBALL = 6;
    public static final byte TRAINING_TYPE_SWIM = 7;
    public static final byte TRAINING_TYPE_MOUNTAINEERING = 8;
    public static final byte TRAINING_TYPE_TENNIS = 9;
    public static final byte TRAINING_TYPE_RUGBY = 10;
    public static final byte TRAINING_TYPE_GOLF = 11;

    // The watch stores all dates in GMT+8 time zone with seconds resolution
    // These helper functions convert between the watch time representation and local system representation

    public static int LocalTimeToWatchTime(Date localTime)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        String format = simpleDateFormat.format(localTime);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        try {
            return (int)(simpleDateFormat.parse(format).getTime() / 1000);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Date WatchTimeToLocalTime(int watchTime)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String format = simpleDateFormat.format(new Date((long)watchTime * 1000));
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        try {
            return simpleDateFormat.parse(format);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // The notification types used by CMD_SEND_MESSAGE
    public static final byte NOTIFICATION_TYPE_CALL_OFF_HOOK = -1;
    public static final byte NOTIFICATION_TYPE_CALL = 0;
    public static final byte NOTIFICATION_TYPE_MESSAGE_SMS = 1;
    public static final byte NOTIFICATION_TYPE_MESSAGE_WECHAT = 2;
    public static final byte NOTIFICATION_TYPE_MESSAGE_QQ = 3;
    public static final byte NOTIFICATION_TYPE_MESSAGE_FACEBOOK = 4;
    public static final byte NOTIFICATION_TYPE_MESSAGE_TWITTER = 5;
    public static final byte NOTIFICATION_TYPE_MESSAGE_INSTAGRAM = 6;
    public static final byte NOTIFICATION_TYPE_MESSAGE_SKYPE = 7;
    public static final byte NOTIFICATION_TYPE_MESSAGE_WHATSAPP = 8;
    public static final byte NOTIFICATION_TYPE_MESSAGE_LINE = 9;
    public static final byte NOTIFICATION_TYPE_MESSAGE_KAKAO = 10;
    public static final byte NOTIFICATION_TYPE_MESSAGE_OTHER = 11;

    public static byte notificationType(NotificationType type)
    {
        switch(type)
        {
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                return NOTIFICATION_TYPE_MESSAGE_FACEBOOK;
            case GENERIC_SMS:
                return NOTIFICATION_TYPE_MESSAGE_SMS;
            case INSTAGRAM:
                return NOTIFICATION_TYPE_MESSAGE_INSTAGRAM;
            case KAKAO_TALK:
                return NOTIFICATION_TYPE_MESSAGE_KAKAO;
            case LINE:
                return NOTIFICATION_TYPE_MESSAGE_LINE;
            case SKYPE:
                return NOTIFICATION_TYPE_MESSAGE_SKYPE;
            case TWITTER:
                return NOTIFICATION_TYPE_MESSAGE_TWITTER;
            case WECHAT:
                return NOTIFICATION_TYPE_MESSAGE_WECHAT;
            case WHATSAPP:
                return NOTIFICATION_TYPE_MESSAGE_WHATSAPP;
            default:
                return NOTIFICATION_TYPE_MESSAGE_OTHER;
        }
    }


    // Weather types
    public static final byte WEATHER_CLOUDY = 0;
    public static final byte WEATHER_FOGGY = 1;
    public static final byte WEATHER_OVERCAST = 2;
    public static final byte WEATHER_RAINY = 3;
    public static final byte WEATHER_SNOWY = 4;
    public static final byte WEATHER_SUNNY = 5;
    public static final byte WEATHER_SANDSTORM = 6; // aka "wind", according to the image
    public static final byte WEATHER_HAZE = 7; // it's basically very big fog :P
    // NOTE: values > 7 give random glitchy crap as images :D

    public static byte openWeatherConditionToMoyoungConditionId(int openWeatherMapCondition) {
        int openWeatherMapGroup = openWeatherMapCondition / 100;
        switch (openWeatherMapGroup) {
            case 2: // thunderstorm
            case 3: // drizzle
            case 5: // rain
                return MoyoungConstants.WEATHER_RAINY;
            case 6: // snow
                return MoyoungConstants.WEATHER_SNOWY;
            case 7: // fog
                return MoyoungConstants.WEATHER_FOGGY;
            case 8: // clear / clouds
                if (openWeatherMapCondition <= 801) // few clouds
                    return MoyoungConstants.WEATHER_SUNNY;
                if (openWeatherMapCondition >= 804) // overcast clouds
                    return MoyoungConstants.WEATHER_CLOUDY;
                return MoyoungConstants.WEATHER_OVERCAST;
            case 9: // extreme
            default:
                if (openWeatherMapCondition == 905) // windy
                    return MoyoungConstants.WEATHER_SANDSTORM;
                return MoyoungConstants.WEATHER_HAZE;
        }
    }


    public static final String PREF_MOYOUNG_WATCH_FACE = "moyoung_watch_face";
    public static final String PREF_LANGUAGE = "moyoung_language";
    public static final String PREF_LANGUAGE_SUPPORT = "moyoung_language_supported";
    public static final String PREF_MOYOUNG_DEVICE_VERSION = "moyoung_device_version";
    public static final String PREF_SEDENTARY_REMINDER = "sedentary_reminder";
    public static final String PREF_SEDENTARY_REMINDER_PERIOD = "sedentary_reminder_period";
    public static final String PREF_SEDENTARY_REMINDER_STEPS = "sedentary_reminder_steps";
    public static final String PREF_SEDENTARY_REMINDER_START = "sedentary_reminder_start";
    public static final String PREF_SEDENTARY_REMINDER_END = "sedentary_reminder_end";
}
