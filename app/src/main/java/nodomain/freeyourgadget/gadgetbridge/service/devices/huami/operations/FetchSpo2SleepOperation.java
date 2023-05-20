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
 * An operation that fetches SPO2 data for sleep measurements (this requires sleep breathing quality enabled).
 */
public class FetchSpo2SleepOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSpo2SleepOperation.class);

    public FetchSpo2SleepOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_SPO2_SLEEP, "spo2 sleep data");
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if ((bytes.length - 1) % 30 != 0) {
            LOG.error("Unexpected length for sleep spo2 data {}, not divisible by 30", bytes.length);
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final int version = buf.get() & 0xff;
        if (version != 2) {
            LOG.error("Unknown sleep spo2 data version {}", version);
            return false;
        }

        while (buf.position() < bytes.length) {
            final long timestampSeconds = buf.getInt();
            // this doesn't match the spo2 value returned by FetchSpo2NormalOperation.. it's often 100 when the other is 99, but not always
            final int spo2 = buf.get() & 0xff;

            // Not sure what the nextg 25 bytes mean:
            // 40646464646464636363636363000000000000400000000000
            //   an unknown byte, always 0x40 (64)
            //   6 bytes with max values?
            //   6 bytes with min values?
            //   12 unknown bytes, always ending with 4 zeroes?
            final byte[] unknown = new byte[25];

            buf.get(unknown);

            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            LOG.debug("SPO2 (sleep) at {}: {} unknown={}", timestamp.getTime(), spo2, GB.hexdump(unknown));
            // TODO save
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSpo2sleepTimeMillis";
    }
}
