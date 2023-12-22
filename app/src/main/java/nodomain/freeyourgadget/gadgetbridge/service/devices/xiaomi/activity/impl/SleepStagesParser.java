/*  Copyright (C) 2023 Alice, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;

public class SleepStagesParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(SleepStagesParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        if (fileId.getVersion() != 2) {
            LOG.warn("Unknown sleep stages version {}", fileId.getVersion());
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        // over 4 days
        // first 2 bytes: always FF FF
        // bytes 3,4 small-medium
        // byte 5 ???
        // byte 6,7 small
        final byte[] unk1 = new byte[7];
        buf.get(unk1);

        // total sleep duration in minutes
        final short sleepDuration = buf.getShort();
        // timestamp when watch counts "real" sleep start, might be later than first phase change
        final int bedTime = buf.getInt();
        // timestamp when sleep ended (have not observed, but may also be earlier than last phase?)
        final int wakeUpTime = buf.getInt();

        // byte 8 medium
        // bytes 9,10 look like a short
        final byte[] unk2 = new byte[3];
        buf.get(unk2);

        // sum of all "real" deep sleep durations
        final short deepSleepDuration = buf.getShort();
        // sum of all "real" light sleep durations
        final short lightSleepDuration = buf.getShort();
        // sum of all "real" REM durations
        final short REMDuration = buf.getShort();
        // sum of all "real" awake durations
        final short wakeDuration = buf.getShort();

        // byte 11 small-medium
        final byte unk3 = buf.get();
        while (buf.position() < buf.limit()) {
            // when the change to the phase occurs
            final int time = buf.getInt();
            // what phase state changed to
            final byte sleepPhase = buf.get();
        }
        return true;
    }
}
