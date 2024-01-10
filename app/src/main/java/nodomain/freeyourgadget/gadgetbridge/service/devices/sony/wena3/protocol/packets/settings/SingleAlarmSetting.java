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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;

public class SingleAlarmSetting implements Wena3Packetable {
    public final boolean enable;
    // Bitmask: See model.Alarm.ALARM_MON, TUE, ...
    public final byte repetition;
    public final int smartAlarmMargin;
    public final int hour;
    public final int minute;

    public SingleAlarmSetting(boolean enable, byte repetition, int smartAlarmMargin, int hour, int minute) {
        this.enable = enable;
        this.repetition = repetition;
        this.smartAlarmMargin = smartAlarmMargin;
        this.hour = hour;
        this.minute = minute;
    }

    // NB: normally this never occurs on the wire
    //     outside of an AlarmListSettings packet!
    @Override
    public byte[] toByteArray() {
        // For some reason their bitmask starts on Sunday!
        // So this brings it in line with what Gadgetbridge expects...
        byte newRepetition = (byte) ((((repetition & ~Alarm.ALARM_SUN) << 1) | ((repetition & Alarm.ALARM_SUN) >> 6)) & 0xFF);
        return ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) (enable ? 0x2 : 0x1)) // 0x0 means no object...
                .put(newRepetition)
                .put((byte) smartAlarmMargin)
                .put((byte) hour)
                .put((byte) minute)
                .array();
    }

    public static byte[] emptyPacket() {
        return ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[] {0, 0, 0, 0, 0})
                .array();
    }
}
