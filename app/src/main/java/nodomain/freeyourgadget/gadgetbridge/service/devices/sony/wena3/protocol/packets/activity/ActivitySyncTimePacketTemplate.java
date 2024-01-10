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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;

abstract class ActivitySyncTimePacketTemplate implements Wena3Packetable {
    public final byte header;

    @Nullable
    public final Date date1;
    @Nullable
    public final Date date2;
    @Nullable
    public final Date date3;
    @Nullable
    public final Date date4;

    public ActivitySyncTimePacketTemplate(byte header, @Nullable Date date1, @Nullable Date date2, @Nullable Date date3, @Nullable Date date4) {
        this.header = header;
        this.date1 = date1;
        this.date2 = date2;
        this.date3 = date3;
        this.date4 = date4;
    }

    @Override
    public byte[] toByteArray() {
        ByteBuffer buf = ByteBuffer.allocate(17)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(header);
        if(date1 == null) {
            buf.putInt(0);
        } else {
            buf.putInt(TimeUtil.dateToWenaTime(date1));
        }
        if(date2 == null) {
            buf.putInt(0);
        } else {
            buf.putInt(TimeUtil.dateToWenaTime(date2));
        }
        if(date3 == null) {
            buf.putInt(0);
        } else {
            buf.putInt(TimeUtil.dateToWenaTime(date3));
        }
        if(date4 == null) {
            buf.putInt(0);
        } else {
            buf.putInt(TimeUtil.dateToWenaTime(date4));
        }
        return buf.array();
    }
}

