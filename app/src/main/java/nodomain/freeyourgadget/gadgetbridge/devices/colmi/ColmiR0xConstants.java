/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.colmi;

import java.util.UUID;

public class ColmiR0xConstants {
    public static final UUID CHARACTERISTIC_SERVICE_V1 = UUID.fromString("6e40fff0-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC_SERVICE_V2 = UUID.fromString("de5bf728-d711-4e47-af26-65e3012a5dc7");
    public static final UUID CHARACTERISTIC_WRITE = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC_COMMAND = UUID.fromString("de5bf72a-d711-4e47-af26-65e3012a5dc7");
    public static final UUID CHARACTERISTIC_NOTIFY_V1 = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC_NOTIFY_V2 = UUID.fromString("de5bf729-d711-4e47-af26-65e3012a5dc7");

    public static final byte CMD_SET_DATE_TIME = 0x01;
    public static final byte CMD_BATTERY = 0x03;
    public static final byte CMD_PHONE_NAME = 0x04;
    public static final byte CMD_POWER_OFF = 0x08;
    public static final byte CMD_PREFERENCES = 0x0a;
    public static final byte CMD_SYNC_HEART_RATE = 0x15;
    public static final byte CMD_AUTO_HR_PREF = 0x16;
    public static final byte CMD_GOALS = 0x21;
    public static final byte CMD_AUTO_SPO2_PREF = 0x2c;
    public static final byte CMD_PACKET_SIZE = 0x2f;
    public static final byte CMD_AUTO_STRESS_PREF = 0x36;
    public static final byte CMD_SYNC_STRESS = 0x37;
    public static final byte CMD_SYNC_ACTIVITY = 0x43;
    public static final byte CMD_FIND_DEVICE = 0x50;
    public static final byte CMD_MANUAL_HEART_RATE = 0x69;
    public static final byte CMD_NOTIFICATION = 0x73;
    public static final byte CMD_BIG_DATA_V2 = (byte) 0xbc;

    public static final byte PREF_READ = 0x01;
    public static final byte PREF_WRITE = 0x02;
    public static final byte PREF_DELETE = 0x03;

    public static final byte NOTIFICATION_NEW_HR_DATA = 0x01;
    public static final byte NOTIFICATION_NEW_SPO2_DATA = 0x03;
    public static final byte NOTIFICATION_NEW_STEPS_DATA = 0x04;
    public static final byte NOTIFICATION_BATTERY_LEVEL = 0x0c;
    public static final byte NOTIFICATION_LIVE_ACTIVITY = 0x12;

    public static final byte BIG_DATA_TYPE_SLEEP = 0x27;
    public static final byte BIG_DATA_TYPE_SPO2 = 0x2a;

    public static final byte SLEEP_TYPE_LIGHT = 0x02;
    public static final byte SLEEP_TYPE_DEEP = 0x03;
    public static final byte SLEEP_TYPE_REM = 0x04;
    public static final byte SLEEP_TYPE_AWAKE = 0x05;
}
