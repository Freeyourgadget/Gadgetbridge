/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import java.util.UUID;

public class SonySWR12Constants {
    //accessory host service
    public static final String BASE_UUID_AHS = "0000%s-37CB-11E3-8682-0002A5D5C51B";
    public static final UUID UUID_SERVICE_AHS = UUID.fromString(String.format(BASE_UUID_AHS, "0200"));
    public static final UUID UUID_CHARACTERISTIC_ALARM = UUID.fromString(String.format(BASE_UUID_AHS, "0204"));
    public static final UUID UUID_CHARACTERISTIC_EVENT = UUID.fromString(String.format(BASE_UUID_AHS, "0205"));
    public static final UUID UUID_CHARACTERISTIC_TIME = UUID.fromString(String.format(BASE_UUID_AHS, "020B"));
    public static final UUID UUID_CHARACTERISTIC_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID_AHS, "0208"));

    public static final int TYPE_ACTIVITY = 0;
    public static final int TYPE_LIGHT = 1;
    public static final int TYPE_DEEP = 2;
    public static final int TYPE_NOT_WORN = 3;
}
