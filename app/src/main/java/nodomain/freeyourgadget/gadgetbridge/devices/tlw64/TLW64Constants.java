/*  Copyright (C) 2020 Erik Bloß

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

package nodomain.freeyourgadget.gadgetbridge.devices.tlw64;

import java.util.UUID;

public final class TLW64Constants {

    public static final UUID UUID_SERVICE_NO1            = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb");

    // Command bytes
    public static final byte CMD_DATETIME = (byte) 0xa3;
    public static final byte CMD_ALARM = (byte) 0xab;

}
