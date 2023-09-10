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

import nodomain.freeyourgadget.gadgetbridge.R;
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
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_spo2_data);
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

            final int duration = buf.get() & 0xff;
            final byte[] spo2High = new byte[6];
            final byte[] spo2Low = new byte[6];
            final byte[] signalQuality = new byte[8];
            final byte[] extend = new byte[4];
            buf.get(spo2High);
            buf.get(spo2Low);
            buf.get(signalQuality);
            buf.get(extend);

            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            LOG.debug(
                    "SPO2 (sleep) at {}: {} duration={} high={} low={} signalQuality={}, extend={}",
                    timestamp.getTime(),
                    spo2,
                    duration,
                    spo2High,
                    spo2Low,
                    signalQuality,
                    extend
            );
            // TODO save
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSpo2sleepTimeMillis";
    }
}
