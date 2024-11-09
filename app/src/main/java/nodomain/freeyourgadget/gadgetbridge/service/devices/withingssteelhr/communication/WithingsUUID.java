/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication;

import java.util.UUID;

public final class WithingsUUID {

    public static final UUID WITHINGS_SERVICE_UUID = UUID.fromString("00000020-5749-5448-0037-000000000000");
    public static final UUID WITHINGS_WRITE_CHARACTERISTIC_UUID = UUID.fromString("00000024-5749-5448-0037-000000000000");
    public static final UUID WITHINGS_APP_CHARACTERISTIC_UUID = UUID.fromString("10000059-5749-5448-0037-000000000000");
    public static final UUID WITHINGS_APP_CHARACTERISTIC2_UUID = UUID.fromString("10000028-5749-5448-0037-000000000000");
    public static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID WITHINGS_ANCS_SERVICE_UUID = UUID.fromString("10000057-5749-5448-0037-000000000000");
    public static final UUID NOTIFICATION_SOURCE_CHARACTERISTIC_UUID = UUID.fromString("10000059-5749-5448-0037-000000000000");
    public static final UUID CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("10000058-5749-5448-0037-000000000000");
    public static final UUID DATA_SOURCE_CHARACTERISTIC_UUID = UUID.fromString("1000005a-5749-5448-0037-000000000000");

    private WithingsUUID() {}
}
