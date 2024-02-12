/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity;

import androidx.annotation.Nullable;

import java.util.Date;

public class ActivitySyncTimePacketTypeB extends ActivitySyncTimePacketTemplate {
    public ActivitySyncTimePacketTypeB(@Nullable Date stressLastSyncTime, @Nullable Date bodyEnergyLastSyncTime, @Nullable Date caloriesLastSyncTime, @Nullable Date eventsLastSyncTime) {
        super((byte) 0x2, stressLastSyncTime, bodyEnergyLastSyncTime, caloriesLastSyncTime, eventsLastSyncTime);
    }
}
