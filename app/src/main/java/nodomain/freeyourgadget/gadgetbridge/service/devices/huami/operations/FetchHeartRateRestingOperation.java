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
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiHeartRateRestingSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiHeartRateRestingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches resting heart rate data.
 */
public class FetchHeartRateRestingOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchHeartRateRestingOperation.class);

    public FetchHeartRateRestingOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_RESTING_HEART_RATE, "resting hr data");
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_hr_data);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if (bytes.length % 6 != 0) {
            LOG.warn("Unexpected buffered rest heart rate data size {} is not a multiple of 6", bytes.length);
            return false;
        }

        final List<HuamiHeartRateRestingSample> samples = new ArrayList<>();

        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        while (buffer.position() < bytes.length) {
            final long currentTimestamp = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;
            timestamp.setTimeInMillis(currentTimestamp);

            // Official app only shows this at day-level
            final byte utcOffsetInQuarterHours = buffer.get();
            final int hr = buffer.get() & 0xff;

            LOG.trace("Resting HR at {} + {}: {}", timestamp.getTime(), utcOffsetInQuarterHours, hr);

            final HuamiHeartRateRestingSample sample = new HuamiHeartRateRestingSample();
            sample.setTimestamp(timestamp.getTimeInMillis());
            sample.setHeartRate(hr);
            sample.setUtcOffset(utcOffsetInQuarterHours * 900000);
            samples.add(sample);
        }

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiHeartRateRestingSample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final HuamiCoordinator coordinator = (HuamiCoordinator) getDevice().getDeviceCoordinator();
            final HuamiHeartRateRestingSampleProvider sampleProvider = coordinator.getHeartRateRestingSampleProvider(getDevice(), session);

            for (final HuamiHeartRateRestingSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} heart rate resting samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving heart rate resting samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastHeartRateRestingTimeMillis";
    }
}
