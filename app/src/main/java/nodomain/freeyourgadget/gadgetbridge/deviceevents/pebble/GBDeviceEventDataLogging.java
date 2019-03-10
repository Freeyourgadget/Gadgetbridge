/*  Copyright (C) 2017-2019 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;

public class GBDeviceEventDataLogging extends GBDeviceEvent {
    public static final int COMMAND_RECEIVE_DATA = 1;
    public static final int COMMAND_FINISH_SESSION = 2;

    public int command;
    public UUID appUUID;
    public long timestamp;
    public long tag;
    public byte pebbleDataType;
    public Object[] data;
}
