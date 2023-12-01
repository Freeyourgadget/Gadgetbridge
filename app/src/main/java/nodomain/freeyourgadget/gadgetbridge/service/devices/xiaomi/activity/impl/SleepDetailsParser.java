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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSleepTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepTimeSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SleepDetailsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(SleepDetailsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        if (fileId.getVersion() != 2) {
            LOG.warn("Unknown sleep details version {}", fileId.getVersion());
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // header ? 0xF0

        final int isAwake = buf.get() & 0xff; // 0/1
        final int bedTime = buf.getInt();
        final int wakeupTime = buf.getInt();
        LOG.debug("Sleep sample: bedTime: {}, wakeupTime: {}, isAwake: {}", bedTime, wakeupTime, isAwake);

        final XiaomiSleepTimeSample sample = new XiaomiSleepTimeSample();
        sample.setTimestamp(bedTime * 1000L);
        sample.setWakeupTime(wakeupTime * 1000L);
        sample.setIsAwake(isAwake == 1);

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = support.getDevice();

            sample.setDevice(DBHelper.getDevice(gbDevice, session));
            sample.setUser(DBHelper.getUser(session));

            final XiaomiSleepTimeSampleProvider sampleProvider = new XiaomiSleepTimeSampleProvider(gbDevice, session);

            // Check if there is already a later sleep sample - if so, ignore this one
            // Samples for the same sleep will always have the same bedtime (timestamp), but we might get
            // multiple bedtimes until the user wakes up
            final List<XiaomiSleepTimeSample> existingSamples = sampleProvider.getAllSamples(sample.getTimestamp(), sample.getTimestamp());
            if (!existingSamples.isEmpty()) {
                final XiaomiSleepTimeSample existingSample = existingSamples.get(0);
                if (existingSample.getWakeupTime() > sample.getWakeupTime()) {
                    LOG.warn("Ignoring sleep sample - existing sample is more recent ({})", existingSample.getWakeupTime());
                    return true;
                }
            }

            sampleProvider.addSample(sample);

            return true;
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving sleep sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving sleep sample", e);
            return false;
        }
    }
}
