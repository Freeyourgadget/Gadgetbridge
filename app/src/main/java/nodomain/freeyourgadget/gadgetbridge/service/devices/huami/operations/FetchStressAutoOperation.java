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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches auto stress data.
 */
public class FetchStressAutoOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchStressAutoOperation.class);

    public FetchStressAutoOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_STRESS_AUTOMATIC, "auto stress data");
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_stress_data);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        final List<HuamiStressSample> samples = new ArrayList<>();

        for (byte b : bytes) {
            if (b == -1) {
                timestamp.add(Calendar.MINUTE, 1);
                continue;
            }

            // 0-39 = relaxed
            // 40-59 = mild
            // 60-79 = moderate
            // 80-100 = high
            final int stress = b & 0xff;

            LOG.trace("Stress (auto) at {}: {}", timestamp.getTime(), stress);

            final HuamiStressSample sample = new HuamiStressSample();
            sample.setTimestamp(timestamp.getTimeInMillis());
            sample.setType(StressSample.Type.AUTOMATIC);
            sample.setStress(stress);
            samples.add(sample);

            timestamp.add(Calendar.MINUTE, 1);
        }

        timestamp.add(Calendar.MINUTE, -1);

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiStressSample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final HuamiCoordinator coordinator = (HuamiCoordinator) getDevice().getDeviceCoordinator();
            final HuamiStressSampleProvider sampleProvider = coordinator.getStressSampleProvider(getDevice(), session);

            for (final HuamiStressSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} auto stress samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving auto stress samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastStressAutoTimeMillis";
    }
}
