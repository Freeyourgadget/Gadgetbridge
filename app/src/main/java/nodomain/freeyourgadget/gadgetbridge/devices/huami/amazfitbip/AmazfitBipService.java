/*  Copyright (C) 2015-2018 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service.DISPLAY_ITEM_BIT_CLOCK;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service.ENDPOINT_DISPLAY;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service.ENDPOINT_DISPLAY_ITEMS;

public class AmazfitBipService {
    public static final UUID UUID_CHARACTERISTIC_WEATHER = UUID.fromString("0000000e-0000-3512-2118-0009af100700");

    // goes to UUID_CHARACTERISTIC_3_CONFIGURATION, TODO: validate this for Mi Band 2, it maybe triggers more than only GPS version...
    public static final byte[] COMMAND_REQUEST_GPS_VERSION = new byte[]{0x0e};

    public static final byte COMMAND_ACTIVITY_DATA_TYPE_DEBUGLOGS = 0x07;

    public static final byte[] COMMAND_SET_LANGUAGE_SIMPLIFIED_CHINESE = new byte[]{ENDPOINT_DISPLAY, 0x13, 0x00, 0x00};
    public static final byte[] COMMAND_SET_LANGUAGE_TRADITIONAL_CHINESE = new byte[]{ENDPOINT_DISPLAY, 0x13, 0x00, 0x01};
    public static final byte[] COMMAND_SET_LANGUAGE_ENGLISH = new byte[]{ENDPOINT_DISPLAY, 0x13, 0x00, 0x02};
    public static final byte[] COMMAND_SET_LANGUAGE_SPANISH = new byte[]{ENDPOINT_DISPLAY, 0x13, 0x00, 0x03};
    public static final byte[] COMMAND_SET_LANGUAGE_NEW_TEMPLATE = new byte[]{ENDPOINT_DISPLAY, 0x17, 0x00, 0, 0, 0, 0, 0};


    public static final byte COMMAND_ACTIVITY_DATA_TYPE_SPORTS_SUMMARIES = 0x05;
    public static final byte COMMAND_ACTIVITY_DATA_TYPE_SPORTS_DETAILS = 0x06;

    public static final byte[] COMMAND_ACK_FIND_PHONE_IN_PROGRESS = new byte[]{ENDPOINT_DISPLAY, 0x14, 0x00, 0x00};

    public static final byte[] COMMAND_CHANGE_SCREENS = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x10, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
}
