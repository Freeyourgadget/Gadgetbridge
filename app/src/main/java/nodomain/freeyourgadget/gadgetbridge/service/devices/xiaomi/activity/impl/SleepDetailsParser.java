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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.HeartPulseSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSleepTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HeartPulseSample;
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
        // Seems to come both as DetailType.DETAILS (version 2) and DetailType.SUMMARY (version 4)
        if (fileId.getVersion() > 4) {
            LOG.warn("Unknown sleep details version {}", fileId.getVersion());
            return false;
        }

        // Stores number of fields which are only present in certain versions of the message
        // this is required for correct header offset calculation
        int versionDependentFields = 0;

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        final byte header = buf.get();

        final int isAwake = buf.get() & 0xff; // 0/1 - more correctly this would be !isSleepFinish
        final int bedTime = buf.getInt();
        final int wakeupTime = buf.getInt();
        int sleepQuality = -1;
        if (fileId.getVersion() >= 4) {
            versionDependentFields += 1;
            sleepQuality = buf.get() & 0xff;
        }

        // note on RR-intervals (msg type 1) on Xiaomi band 9, FW 2.3.93, HW M2345B1:
        //
        // the band collects RR interval in groups of ~10 minutes, which are then sent as msg type 1
        // along with an associated RTC timestamp. RTC timestamp jitter and drift of up to
        // +/-several seconds was observed, compared to the sum of intervals in a given msg.
        //
        // In order to preserve RR-interval continuity throughout the entire duration of sleep, as
        // is necessary for breath-detection algorithm, and to avoid negative or impossibly large
        // (false) RR intervals, we maintain an running timestamp of last heart pulse.
        // While processing msgs, in case the detected jitter is too large, the running timestamp
        // is reset to the current msg timestamp.
        long lastHeartPulseTimestamp = 0;  // tracks the true timestamp of last heart pulse across the jittery 10-minute segments

        LOG.debug("Sleep sample: bedTime: {}, wakeupTime: {}, isAwake: {}", bedTime, wakeupTime, isAwake);

        final List<XiaomiSleepTimeSample> summaries = new ArrayList<>();

        XiaomiSleepTimeSample sample = new XiaomiSleepTimeSample();
        sample.setTimestamp(bedTime * 1000L);
        sample.setWakeupTime(wakeupTime * 1000L);
        sample.setIsAwake(isAwake == 1);

        // Heart rate samples
        if ((header & (1 << (5 - versionDependentFields))) != 0) {
            LOG.debug("Heart rate samples from offset {}", Integer.toHexString(buf.position()));
            final int unit = buf.getShort(); // Time unit (i.e sample rate)
            final int count = buf.getShort();

            if (count > 0) {
                // If version is less than 2 firstRecordTime is bedTime
                if (fileId.getVersion() >= 2) {
                    final int firstRecordTime = buf.getInt();
                }

                // Skip count samples - each sample is a u8
                //   timestamp of each sample is firstRecordTime + (unit * index)
                buf.position(buf.position() + count);
            }
        }

        // SpO2 samples
        if ((header & (1 << (4 - versionDependentFields))) != 0) {
            LOG.debug("SpO₂ samples from offset {}", Integer.toHexString(buf.position()));
            final int unit = buf.getShort(); // Time unit (i.e sample rate)
            final int count = buf.getShort();

            if (count > 0) {
                // If version is less than 2 firstRecordTime is bedTime
                if (fileId.getVersion() >= 2) {
                    final int firstRecordTime = buf.getInt();
                }

                // Skip count samples - each sample is a u8
                //   timestamp of each sample is firstRecordTime + (unit * index)
                buf.position(buf.position() + count);
            }
        }

        // snore samples
        if (fileId.getVersion() >= 3 && (header & (1 << (3 - versionDependentFields))) != 0) {
            LOG.debug("Snore level samples from offset {}", Integer.toHexString(buf.position()));
            final int unit = buf.getShort(); // Time unit (i.e sample rate)
            final int count = buf.getShort();

            if (count > 0) {
                // If version is less than 2 firstRecordTime is bedTime
                if (fileId.getVersion() >= 2) {
                    final int firstRecordTime = buf.getInt();
                }

                // Skip count samples - each sample is a float
                //   timestamp of each sample is firstRecordTime + (unit * index)
                buf.position(buf.position() + count * 4);
            }
        }

        final List<XiaomiSleepStageSample> stages = new ArrayList<>();
        final List<HeartPulseSample> heartPulseSamples = new ArrayList<>();
        LOG.debug("Sleep stage packets from offset {}", Integer.toHexString(buf.position()));

        // Do not crash if we face a buffer underflow, as the next parsing is not 100% fool-proof,
        // and we still want to persist whatever we got so far
        boolean stagesParseFailed = false;
        try {
            while (buf.remaining() >= 17) {
                if (!readStagePacketHeader(buf)) {
                    break;
                }

                final int headerLen = buf.get() & 0xFF; // this seems to always be 17

                // This timestamp is kind of weird, is seems to sometimes be in seconds
                // and other times in nanoseconds. Message types 16 and 17 are in seconds
                final long ts = buf.getLong();
                final int parity = buf.get() & 0xFF; // sum of stage bit count should be uneven
                final int type = buf.get() & 0xFF;
                final int dataLen = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);

                // Known types:
                //  - acc_unk = 0,
                //  - ppg_unk = 1,
                //  - fall_asleep = 2,
                //  - wake_up = 3,
                //  - switch_ts_unk1 = 12,
                //  - switch_ts_unk2 = 13,
                //  - Summary = 16,
                //  - Stages = 17

                if (type == 0x2 || type == 0x3 || type == 0x9 || type == 0xc || type == 0xd || type == 0xe || type == 0xf) {
                    // the bytes reserved for the data length are believed to be flags, as they
                    // do not actually have any data following the headers
                    continue;
                }

                final byte[] data = new byte[dataLen];
                buf.get(data);

                final ByteBuffer dataBuf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

                if (type == 1) {
                    // RR intervals: https://en.wikipedia.org/wiki/RR_interval
                    // add first heartbeat before intervals, if necessary
                    if (Math.abs(lastHeartPulseTimestamp - ts) > 30000) {  // 30 seconds
                        // we drifted too far from RTC timestamp, or this is the very first sample:
                        lastHeartPulseTimestamp = ts;  // resync
                        final HeartPulseSample heartPulseSample = new HeartPulseSample();
                        heartPulseSample.setTimestamp(lastHeartPulseTimestamp);
                        heartPulseSamples.add(heartPulseSample);
                    }
                    // add heartbeats after intervals
                    while (dataBuf.position() < dataBuf.limit()) {
                        final int delta = dataBuf.get() & 0xff;  // delta is in 10msec units
                        lastHeartPulseTimestamp += 10 * delta;  // convert to 1msec units for timestamps
                        final HeartPulseSample heartPulseSample = new HeartPulseSample();
                        heartPulseSample.setTimestamp(lastHeartPulseTimestamp);
                        heartPulseSamples.add(heartPulseSample);
                    }
                } else if (type == 16) {
                    final int data_0 = dataBuf.get() & 0xFF;
                    final int sleep_index = data_0 >> 4;
                    final int wake_count = data_0 & 0x0F;

                    final int sleep_duration = dataBuf.getShort() & 0xFFFF;
                    final int wake_duration  = dataBuf.getShort() & 0xFFFF;
                    final int light_duration = dataBuf.getShort() & 0xFFFF;
                    final int rem_duration   = dataBuf.getShort() & 0xFFFF;
                    final int deep_duration  = dataBuf.getShort() & 0xFFFF;

                    final int data_1 = dataBuf.get() & 0xFF;
                    final boolean has_rem = (data_1 >> 4) == 1;
                    final boolean has_stage = (data_1 >> 2) == 1;

                    // Could probably be an "awake" duration after sleep
                    final int unk_duration_minutes = dataBuf.get() & 0xFF;

                    if (sample == null) {
                        sample = new XiaomiSleepTimeSample();
                    }

                    sample.setTimestamp(bedTime * 1000L);
                    sample.setWakeupTime(wakeupTime * 1000L);
                    sample.setTotalDuration(sleep_duration);
                    sample.setDeepSleepDuration(deep_duration);
                    sample.setLightSleepDuration(light_duration);
                    sample.setRemSleepDuration(rem_duration);
                    sample.setAwakeDuration(wake_duration);

                    // FIXME: This is an array, but we end up persisting only the last sample, since
                    //        the timestamp is the primary key
                    summaries.add(sample);
                    sample = null;
                } else if (type == 17) { // Stages
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
        } catch (final BufferUnderflowException e) {
            LOG.warn("Buffer underflow while parsing sleep stages...", e);
            stagesParseFailed = true;
        }

        if (summaries.isEmpty()) {
            // We did not manage to find sleep stage samples - ensure we at least persist the base one
            summaries.add(sample);
        }

        boolean persistSuccess = !stagesParseFailed;

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = support.getDevice();

            final XiaomiSleepTimeSampleProvider sampleProvider = new XiaomiSleepTimeSampleProvider(gbDevice, session);

            for (final XiaomiSleepTimeSample summary : summaries) {
                summary.setDevice(DBHelper.getDevice(gbDevice, session));
                summary.setUser(DBHelper.getUser(session));

                // Check if there is already a later sleep sample - if so, ignore this one
                // Samples for the same sleep will always have the same bedtime (timestamp), but we might get
                // multiple bedtimes until the user wakes up
                final List<XiaomiSleepTimeSample> existingSamples = sampleProvider.getAllSamples(summary.getTimestamp(), summary.getTimestamp());
                if (!existingSamples.isEmpty()) {
                    final XiaomiSleepTimeSample existingSample = existingSamples.get(0);
                    if (existingSample.getWakeupTime() > summary.getWakeupTime()) {
                        LOG.warn("Ignoring sleep sample - existing sample is more recent ({})", existingSample.getWakeupTime());
                        continue;
                    }
                }

                sampleProvider.addSample(summary);
            }
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving sleep sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving sleep sample", e);
            persistSuccess = false;
        }

        if (!stagesParseFailed && !stages.isEmpty()) {
            LOG.debug("Persisting {} sleep stage samples", stages.size());

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
                persistSuccess = false;
            }
        }

        // Save the heart pulse samples
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = support.getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final HeartPulseSampleProvider sampleProvider = new HeartPulseSampleProvider(gbDevice, session);

            for (final HeartPulseSample stageSample : heartPulseSamples) {
                stageSample.setDevice(device);
                stageSample.setUser(user);
            }

            sampleProvider.addSamples(heartPulseSamples);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving heart pulse samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving heart pulse samples", e);
            persistSuccess = false;
        }

        return persistSuccess;
    }

    private static boolean readStagePacketHeader(final ByteBuffer buffer) {
        while (buffer.remaining() >= 17) {
            if (buffer.getInt() != 0xfffcfafb) {
                // rollback to second byte of header
                buffer.position(buffer.position() - 3);
                continue;
            }

            return true;
        }
        return false;
    }

    private static int decodeStage(int rawStage) {
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
