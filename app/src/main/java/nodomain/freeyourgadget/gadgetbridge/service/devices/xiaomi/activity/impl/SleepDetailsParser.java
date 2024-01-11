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

import android.util.Log;
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

        // SleepAssistItemInfo 2x
        //  - 0: Heart rate samples
        //  - 1: Sp02 samples
        for (int i = 0; i < 2; i++) {
            final int unit = buf.getShort(); // Time unit (i.e sample rate)
            final int count = buf.getShort();
            final int firstRecordTime = buf.getInt();

            // Skip count samples - each sample is a u8
            //   timestamp of each sample is firstRecordTime + (unit * index)
            buf.position(buf.position() + count);
        }

        final List<XiaomiSleepStageSample> stages = new ArrayList<>();


        while (buf.remaining() >= 17 && buf.getInt() == 0xFFFCFAFB) {
            final int headerLen = buf.get() & 0xFF; // this seems to always be 17

            // This timestamp is kind of weird, is seems to sometimes be in seconds
            // and other times in nanoseconds. Message types 16 and 17 are in seconds
            final long ts = buf.getLong();
            final int unk = buf.get() & 0xFF;
            final int type = buf.get() & 0xFF;

            final int dataLen = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);

            final byte[] data = new byte[dataLen];
            buf.get(data);

            final ByteBuffer dataBuf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

//            Known types:
//             - acc_unk = 0,
//             - ppg_unk = 1,
//             - fall_asleep = 2,
//             - wake_up = 3,
//             - switch_ts_unk1 = 12,
//             - switch_ts_unk2 = 13,
//             - Summary = 16,
//             - Stages = 17

            if (type == 17) { // Stages
                long currentTime = ts * 1000;
                for (int i = 0; i < dataLen / 2; i++) {
                    // when the change to the phase occurs
                    final int val = dataBuf.getShort() & 0xFFFF;

                    final int stage = val >> 12;
                    final int offsetMinutes = val & 0xFFF;

                    final XiaomiSleepStageSample stageSample = new XiaomiSleepStageSample();
                    stageSample.setTimestamp(currentTime);
                    stageSample.setStage(decodeStage(stage));
                    stages.add(stageSample);

                    currentTime += offsetMinutes * 60000;
                }
            }
        }


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

    static private int decodeStage(int rawStage) {
        switch (rawStage) {
            case 0:
                return 5; // AWAKE
            case 1:
                return 3; // LIGHT_SLEEP
            case 2:
                return 2; // DEEP_SLEEP
            case 3:
                return 4; // REM_SLEEP
            case 4:
                return 0; // NOT_SLEEP
            default:
                return 1; // N/A
        }
    }
}
