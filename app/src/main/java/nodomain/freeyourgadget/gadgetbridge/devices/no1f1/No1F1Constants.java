/*  Copyright (C) 2017-2019 protomors

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
package nodomain.freeyourgadget.gadgetbridge.devices.no1f1;

import java.util.UUID;

public final class No1F1Constants {

    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_NO1 = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb");

    public static final byte CMD_DISPLAY_SETTINGS = (byte) 0xa0;
    public static final byte CMD_FIRMWARE_VERSION = (byte) 0xa1;
    public static final byte CMD_BATTERY = (byte) 0xa2;
    public static final byte CMD_DATETIME = (byte) 0xa3;
    public static final byte CMD_USER_DATA = (byte) 0xa9;
    public static final byte CMD_ALARM = (byte) 0xab;
    public static final byte CMD_FACTORY_RESET = (byte) 0xad;
    public static final byte CMD_REALTIME_STEPS = (byte) 0xb1;
    public static final byte CMD_FETCH_STEPS = (byte) 0xb2;
    public static final byte CMD_FETCH_SLEEP = (byte) 0xb3;
    public static final byte CMD_REALTIME_HEARTRATE = (byte) 0xe5;
    public static final byte CMD_FETCH_HEARTRATE = (byte) 0xe6;
    public static final byte CMD_NOTIFICATION = (byte) 0xc1;
    public static final byte CMD_ICON = (byte) 0xc3;
    public static final byte CMD_DEVICE_SETTINGS = (byte) 0xd3;
    public static final byte CMD_HEARTRATE_SETTINGS = (byte) 0xd6;

    public static final byte NOTIFICATION_HEADER = (byte) 0x01;
    public static final byte NOTIFICATION_CALL = (byte) 0x02;
    public static final byte NOTIFICATION_SMS = (byte) 0x03;
    public static final byte NOTIFICATION_STOP = (byte) 0x04; // to stop showing incoming call

    public static final byte ICON_QQ = (byte) 0x01;
    public static final byte ICON_WECHAT = (byte) 0x02;
    public static final byte ICON_ALARM = (byte) 0x04;
}
