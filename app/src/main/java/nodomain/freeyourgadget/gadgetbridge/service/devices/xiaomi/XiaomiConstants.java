/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import java.util.UUID;

public class XiaomiConstants {
    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    public static final UUID UUID_SERVICE_XIAOMI_FE95 = UUID.fromString((String.format(BASE_UUID, "fe95")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0050 = UUID.fromString((String.format(BASE_UUID, "0050")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ = UUID.fromString((String.format(BASE_UUID, "0051")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE = UUID.fromString((String.format(BASE_UUID, "0052")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA = UUID.fromString((String.format(BASE_UUID, "0053")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0054 = UUID.fromString((String.format(BASE_UUID, "0054")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_WATCHFACE = UUID.fromString((String.format(BASE_UUID, "0055")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0056 = UUID.fromString((String.format(BASE_UUID, "0056")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0057 = UUID.fromString((String.format(BASE_UUID, "0057")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0058 = UUID.fromString((String.format(BASE_UUID, "0058")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0059 = UUID.fromString((String.format(BASE_UUID, "0059")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_005A = UUID.fromString((String.format(BASE_UUID, "005a")));

    public static final UUID UUID_SERVICE_XIAOMI_FDAB = UUID.fromString((String.format(BASE_UUID, "fdab")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0001 = UUID.fromString((String.format(BASE_UUID, "0001")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0002 = UUID.fromString((String.format(BASE_UUID, "0002")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0003 = UUID.fromString((String.format(BASE_UUID, "0003")));

    // TODO not like this
    public static final byte[] PAYLOAD_CHUNKED_START = new byte[]{0, 0, 0, 1};
    public static final byte[] PAYLOAD_CHUNKED_START_ACK = new byte[]{0, 0, 1, 1};
    public static final byte[] PAYLOAD_CHUNKED_END_ACK = new byte[]{0, 0, 1, 0};
    public static final byte[] PAYLOAD_HEADER_AUTH = new byte[]{0, 0, 2, 2};
    public static final byte[] PAYLOAD_HEADER_CMD = new byte[]{0, 0, 2, 1};
    public static final byte[] PAYLOAD_ACK = new byte[]{0, 0, 3, 0};
}
