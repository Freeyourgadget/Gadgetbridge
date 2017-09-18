/*  Copyright (C) 2015-2017 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip;

import java.util.UUID;

public class AmazfitBipService {
    public static final UUID UUID_CHARACTERISTIC_WEATHER = UUID.fromString("0000000e-0000-3512-2118-0009af100700");

    // goes to UUID_CHARACTERISTIC_3_CONFIGURATION, TODO: validate this for Mi Band 2, it maybe triggers more than only GPS version...
    public static final byte[] COMMAND_REQUEST_GPS_VERSION = new byte[]{0x0e};

    public static final byte COMMAND_ACTIVITY_DATA_TYPE_DEBUGLOGS = 0x07;
}
