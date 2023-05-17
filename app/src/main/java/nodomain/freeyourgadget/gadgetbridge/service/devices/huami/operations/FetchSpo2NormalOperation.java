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
 * An operation that fetches SPO2 data for normal (manual and automatic) measurements.
 */
public class FetchSpo2NormalOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSpo2NormalOperation.class);

    public FetchSpo2NormalOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_SPO2_NORMAL, "spo2 normal data");
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if ((bytes.length - 1) % 65 != 0) {
            LOG.error("Unexpected length for spo2 data {}, not divisible by 65", bytes.length);
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final int version = buf.get() & 0xff;
        if (version != 2) {
            LOG.error("Unknown normal spo2 data version {}", version);
            return false;
        }

        while (buf.position() < bytes.length) {
            final long timestampSeconds = buf.getInt();
            final byte spo2raw = buf.get();
            final boolean autoMeasurement = (spo2raw < 0);
            final byte spo2 = (byte) (autoMeasurement ? (spo2raw + 128) : spo2raw);

            final byte[] unknown = new byte[60]; // starts with a few spo2 values, but mostly zeroes after?
            buf.get(unknown);

            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            LOG.info("SPO2 at {}: {} auto={} unknown={}", timestamp.getTime(), spo2, autoMeasurement, GB.hexdump(unknown));
            // TODO save
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSpo2normalTimeMillis";
    }
}
