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
package nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class VivomoveConstants {
    public static final UUID UUID_SERVICE_GARMIN_GFDI = UUID.fromString("6A4E2401-667B-11E3-949A-0800200C9A66");
    public static final UUID UUID_SERVICE_GARMIN_REALTIME = UUID.fromString("6A4E2500-667B-11E3-949A-0800200C9A66");

    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_SEND = UUID.fromString("6a4e4c80-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_GFDI_RECEIVE = UUID.fromString("6a4ecd28-667b-11e3-949a-0800200c9a66");

    public static final UUID UUID_CHARACTERISTIC_GARMIN_HEART_RATE = UUID.fromString("6a4e2501-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_STEPS = UUID.fromString("6a4e2502-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_CALORIES = UUID.fromString("6a4e2503-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_STAIRS = UUID.fromString("6a4e2504-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_INTENSITY = UUID.fromString("6a4e2505-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_HEART_RATE_VARIATION = UUID.fromString("6a4e2507-667b-11e3-949a-0800200c9a66");
    // public static final UUID UUID_CHARACTERISTIC_GARMIN_STRESS = UUID.fromString("6a4e2508-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_2_9 = UUID.fromString("6a4e2509-667b-11e3-949a-0800200c9a66");
    // public static final UUID UUID_CHARACTERISTIC_GARMIN_SPO2 = UUID.fromString("6a4e250c-667b-11e3-949a-0800200c9a66");
    // public static final UUID UUID_CHARACTERISTIC_GARMIN_RESPIRATION = UUID.fromString("6a4e250e-667b-11e3-949a-0800200c9a66");

    // public static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int STATUS_ACK = 0;
    public static final int STATUS_NAK = 1;
    public static final int STATUS_UNSUPPORTED = 2;
    public static final int STATUS_DECODE_ERROR = 3;
    public static final int STATUS_CRC_ERROR = 4;
    public static final int STATUS_LENGTH_ERROR = 5;

    public static final int GADGETBRIDGE_UNIT_NUMBER = 22222;

    public static final int GARMIN_DEVICE_XML_FILE_INDEX = 65533;

    // TODO: Better capability management/configuration
    // public static final Set<GarminCapability> OUR_CAPABILITIES = new HashSet<>(Arrays.asList(GarminCapability.SYNC, GarminCapability.GNCS, GarminCapability.ADVANCED_MUSIC_CONTROLS, GarminCapability.FIND_MY_PHONE, GarminCapability.WEATHER_CONDITIONS, GarminCapability.WEATHER_ALERTS, GarminCapability.DEVICE_MESSAGES, GarminCapability.SMS_NOTIFICATIONS, GarminCapability.SYNC, GarminCapability.DEVICE_INITIATES_SYNC, GarminCapability.HOST_INITIATED_SYNC_REQUESTS, GarminCapability.CALENDAR, GarminCapability.CURRENT_TIME_REQUEST_SUPPORT));
    public static final Set<GarminCapability> OUR_CAPABILITIES = GarminCapability.ALL_CAPABILITIES;

    public static final int MAX_WRITE_SIZE = 20;

    /**
     * Garmin zero time in seconds since Epoch: 1989-12-31T00:00:00Z
     */
    public static final int GARMIN_TIME_EPOCH = 631065600;

    public static final SimpleDateFormat ANCS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);

    public static final int MESSAGE_RESPONSE = 5000;
    public static final int MESSAGE_REQUEST = 5001;
    public static final int MESSAGE_DOWNLOAD_REQUEST = 5002;
    public static final int MESSAGE_UPLOAD_REQUEST = 5003;
    public static final int MESSAGE_FILE_TRANSFER_DATA = 5004;
    public static final int MESSAGE_CREATE_FILE_REQUEST = 5005;
    public static final int MESSAGE_DIRECTORY_FILE_FILTER_REQUEST = 5007;
    public static final int MESSAGE_FILE_READY = 5009;
    public static final int MESSAGE_FIT_DEFINITION = 5011;
    public static final int MESSAGE_FIT_DATA = 5012;
    public static final int MESSAGE_WEATHER_REQUEST = 5014;
    public static final int MESSAGE_BATTERY_STATUS = 5023;
    public static final int MESSAGE_DEVICE_INFORMATION = 5024;
    public static final int MESSAGE_DEVICE_SETTINGS = 5026;
    public static final int MESSAGE_SYSTEM_EVENT = 5030;
    public static final int MESSAGE_SUPPORTED_FILE_TYPES_REQUEST = 5031;
    public static final int MESSAGE_NOTIFICATION_SOURCE = 5033;
    public static final int MESSAGE_GNCS_CONTROL_POINT_REQUEST = 5034;
    public static final int MESSAGE_GNCS_DATA_SOURCE = 5035;
    public static final int MESSAGE_NOTIFICATION_SERVICE_SUBSCRIPTION = 5036;
    public static final int MESSAGE_SYNC_REQUEST = 5037;
    public static final int MESSAGE_FIND_MY_PHONE = 5039;
    public static final int MESSAGE_CANCEL_FIND_MY_PHONE = 5040;
    public static final int MESSAGE_MUSIC_CONTROL_CAPABILITIES = 5042;
    public static final int MESSAGE_PROTOBUF_REQUEST = 5043;
    public static final int MESSAGE_PROTOBUF_RESPONSE = 5044;
    public static final int MESSAGE_MUSIC_CONTROL_ENTITY_UPDATE = 5049;
    public static final int MESSAGE_CONFIGURATION = 5050;
    public static final int MESSAGE_CURRENT_TIME_REQUEST = 5052;
    public static final int MESSAGE_AUTH_NEGOTIATION = 5101;
}
