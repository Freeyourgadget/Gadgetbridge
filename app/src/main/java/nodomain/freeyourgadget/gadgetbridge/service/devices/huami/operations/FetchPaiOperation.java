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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches PAI data.
 */
public class FetchPaiOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchPaiOperation.class);

    public FetchPaiOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_PAI, "pai data");
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        while (buf.position() < bytes.length) {
            final int type = buf.get() & 0xff;

            if (type != 5) {
                LOG.error("Unsupported PAI type {}", type);
                return false;
            }

            final long timestampSeconds = buf.getInt();
            timestamp.setTimeInMillis(timestampSeconds * 1000L);
            final byte utcOffsetInQuarterHours = buf.get();

            byte[] unknown1 = new byte[31];
            buf.get(unknown1);

            final float paiLow = buf.getFloat();
            final float paiModerate = buf.getFloat();
            final float paiHigh = buf.getFloat();
            final short timeLow = buf.getShort(); // minutes
            final short timeModerate = buf.getShort(); // minutes
            final short timeHigh = buf.getShort(); // minutes
            final float paiToday = buf.getFloat();
            final float paiTotal = buf.getFloat();

            byte[] unknown2 = new byte[39];
            buf.get(unknown2);

            LOG.debug(
                    "PAI at {} + {}: paiLow={} paiModerate={} paiHigh={} timeLow={} timeMid={} timeHigh={} paiToday={} paiTotal={} unknown1={} unknown2={}",
                    timestamp.getTime(), utcOffsetInQuarterHours,
                    paiLow, paiModerate, paiHigh,
                    timeLow, timeModerate, timeHigh,
                    paiToday, paiTotal,
                    GB.hexdump(unknown1),
                    GB.hexdump(unknown2)
            );

            // TODO save
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastPaiTimeMillis";
    }
}
