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
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
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
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_spo2_data);
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

        final List<HuamiSpo2Sample> samples = new ArrayList<>();

        while (buf.position() < bytes.length) {
            final long timestampSeconds = buf.getInt();
            final byte spo2raw = buf.get();
            final boolean autoMeasurement = (spo2raw < 0);
            final byte spo2 = (byte) (spo2raw < 0 ? spo2raw + 128 : spo2raw);

            final byte[] unknown = new byte[60]; // starts with a few spo2 values, but mostly zeroes after?
            buf.get(unknown);

            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            LOG.trace("SPO2 at {}: {} auto={}", timestamp.getTime(), spo2, autoMeasurement);

            final HuamiSpo2Sample sample = new HuamiSpo2Sample();
            sample.setTimestamp(timestamp.getTimeInMillis());
            sample.setType(autoMeasurement ? Spo2Sample.Type.AUTOMATIC : Spo2Sample.Type.MANUAL);
            sample.setSpo2(spo2);
            samples.add(sample);
        }

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiSpo2Sample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final HuamiCoordinator coordinator = (HuamiCoordinator) getDevice().getDeviceCoordinator();
            final HuamiSpo2SampleProvider sampleProvider = coordinator.getSpo2SampleProvider(getDevice(), session);

            for (final HuamiSpo2Sample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} normal spo2 samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving normal spo2 samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSpo2normalTimeMillis";
    }
}
