/*  Copyright (C) 2023-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSleepTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepTimeSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SleepStagesParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(SleepStagesParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        if (fileId.getVersion() != 2) {
            LOG.warn("Unknown sleep stages version {}", fileId.getVersion());
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

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
        final int wakeupTime = buf.getInt();

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

        LOG.debug("Sleep stages sample: bedTime: {}, wakeupTime: {}, sleepDuration: {}", bedTime, wakeupTime, sleepDuration);

        if (bedTime == 0 || wakeupTime == 0 || sleepDuration == 0) {
            LOG.warn("Ignoring sleep stages sample with no data");
            return true;
        }

        final XiaomiSleepTimeSample sample = new XiaomiSleepTimeSample();
        sample.setTimestamp(bedTime * 1000L);
        sample.setWakeupTime(wakeupTime * 1000L);
        sample.setIsAwake(false);
        sample.setTotalDuration((int) sleepDuration);
        sample.setDeepSleepDuration((int) deepSleepDuration);
        sample.setLightSleepDuration((int) lightSleepDuration);
        sample.setRemSleepDuration((int) REMDuration);
        sample.setAwakeDuration((int) wakeDuration);

        final List<XiaomiSleepStageSample> stages = new ArrayList<>();

        // byte 11 small-medium
        final byte unk3 = buf.get();
        while (buf.position() < buf.limit()) {
            // when the change to the phase occurs
            final int time = buf.getInt();
            // what phase state changed to
            final int sleepPhase = buf.get() & 0xff;

            final XiaomiSleepStageSample stageSample = new XiaomiSleepStageSample();
            stageSample.setTimestamp(time * 1000L);
            stageSample.setStage(sleepPhase);
            stages.add(stageSample);
        }

        // Save the sleep time sample
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
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving sleep sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving sleep sample", e);
            return false;
        }

        // Save the sleep stage samples
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = support.getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final XiaomiSleepStageSampleProvider sampleProvider = new XiaomiSleepStageSampleProvider(gbDevice, session);

            for (final XiaomiSleepStageSample stageSample : stages) {
                stageSample.setDevice(device);
                stageSample.setUser(user);
            }

            sampleProvider.addSamples(stages);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving sleep stage samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving sleep stage samples", e);
            return false;
        }

        return true;
    }
}
