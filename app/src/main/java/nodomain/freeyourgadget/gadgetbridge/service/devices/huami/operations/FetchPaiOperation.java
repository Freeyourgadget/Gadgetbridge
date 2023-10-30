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

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiPaiSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiPaiSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
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
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_pai_data);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        final List<HuamiPaiSample> samples = new ArrayList<>();

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

            LOG.trace(
                    "PAI at {} + {}: paiLow={} paiModerate={} paiHigh={} timeLow={} timeMid={} timeHigh={} paiToday={} paiTotal={} unknown1={} unknown2={}",
                    timestamp.getTime(), utcOffsetInQuarterHours,
                    paiLow, paiModerate, paiHigh,
                    timeLow, timeModerate, timeHigh,
                    paiToday, paiTotal,
                    GB.hexdump(unknown1),
                    GB.hexdump(unknown2)
            );

            final HuamiPaiSample sample = new HuamiPaiSample();
            sample.setTimestamp(timestamp.getTimeInMillis());
            sample.setUtcOffset(utcOffsetInQuarterHours * 900000);
            sample.setPaiLow(paiLow);
            sample.setPaiModerate(paiModerate);
            sample.setPaiHigh(paiHigh);
            sample.setTimeLow(timeLow);
            sample.setTimeModerate(timeModerate);
            sample.setTimeHigh(timeHigh);
            sample.setPaiToday(paiToday);
            sample.setPaiTotal(paiTotal);
            samples.add(sample);
        }

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiPaiSample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final HuamiCoordinator coordinator = (HuamiCoordinator) getDevice().getDeviceCoordinator();
            final HuamiPaiSampleProvider sampleProvider = coordinator.getPaiSampleProvider(getDevice(), session);

            for (final HuamiPaiSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} pai samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving pai samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastPaiTimeMillis";
    }
}
