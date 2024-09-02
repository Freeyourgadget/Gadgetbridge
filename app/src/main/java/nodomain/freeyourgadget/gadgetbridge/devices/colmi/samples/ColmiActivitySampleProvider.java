/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.ColmiR0xConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class ColmiActivitySampleProvider extends AbstractSampleProvider<ColmiActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(ColmiActivitySampleProvider.class);

    public ColmiActivitySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<ColmiActivitySample, ?> getSampleDao() {
        return getSession().getColmiActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ColmiActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ColmiActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return Math.min(rawIntensity / 7000f, 1f);
    }

    @Override
    public ColmiActivitySample createActivitySample() {
        return new ColmiActivitySample();
    }

    @Override
    protected List<ColmiActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        LOG.trace(
                "Getting Colmi activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );
        final long nanoStart = System.nanoTime();

        final List<ColmiActivitySample> samples = fillGaps(
                super.getGBActivitySamples(timestamp_from, timestamp_to),
                timestamp_from,
                timestamp_to
        );

        final Map<Integer, ColmiActivitySample> sampleByTs = new HashMap<>();
        for (final ColmiActivitySample sample : samples) {
            sampleByTs.put(sample.getTimestamp(), sample);
        }

        overlayHeartRate(sampleByTs, timestamp_from, timestamp_to);
        overlaySleep(sampleByTs, timestamp_from, timestamp_to);

        final List<ColmiActivitySample> finalSamples = new ArrayList<>(sampleByTs.values());
        Collections.sort(finalSamples, (a, b) -> Integer.compare(a.getTimestamp(), b.getTimestamp()));

        final long nanoEnd = System.nanoTime();
        final long executionTime = (nanoEnd - nanoStart) / 1000000;
        LOG.trace("Getting Colmi samples took {}ms", executionTime);

        return finalSamples;
    }

    private void overlayHeartRate(final Map<Integer, ColmiActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final ColmiHeartRateSampleProvider heartRateSampleProvider = new ColmiHeartRateSampleProvider(getDevice(), getSession());
        final List<ColmiHeartRateSample> hrSamples = heartRateSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        for (final ColmiHeartRateSample hrSample : hrSamples) {
            // round to the nearest minute, we don't need per-second granularity
            final int tsSeconds = (int) ((hrSample.getTimestamp() / 1000) / 60) * 60;
            ColmiActivitySample sample = sampleByTs.get(tsSeconds);
            if (sample == null) {
                sample = new ColmiActivitySample();
                sample.setTimestamp(tsSeconds);
                sample.setProvider(this);
                sampleByTs.put(tsSeconds, sample);
            }

            sample.setHeartRate(hrSample.getHeartRate());
        }
    }

    private void overlaySleep(final Map<Integer, ColmiActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final ColmiSleepStageSampleProvider sleepStageSampleProvider = new ColmiSleepStageSampleProvider(getDevice(), getSession());
        final List<ColmiSleepStageSample> sleepStageSamples = sleepStageSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        for (final ColmiSleepStageSample sleepStageSample : sleepStageSamples) {
            final ActivityKind sleepRawKind = sleepStageToActivityKind(sleepStageSample.getStage());
            if (sleepRawKind == ActivityKind.AWAKE_SLEEP) continue;
            // round to the nearest minute, we don't need per-second granularity
            final int tsSeconds = (int) ((sleepStageSample.getTimestamp() / 1000) / 60) * 60;
            for (int i = tsSeconds; i < tsSeconds + sleepStageSample.getDuration() * 60; i += 60) {
                ColmiActivitySample sample = sampleByTs.get(i);
                if (sample == null) {
                    sample = new ColmiActivitySample();
                    sample.setTimestamp(i);
                    sample.setProvider(this);
                    sampleByTs.put(i, sample);
                }
                sample.setRawKind(sleepRawKind.getCode());

                switch (sleepRawKind) {
                    case LIGHT_SLEEP:
                        sample.setRawIntensity(1400);
                        break;
                    case DEEP_SLEEP:
                        sample.setRawIntensity(700);
                        break;
                }
            }
        }
    }

    final ActivityKind sleepStageToActivityKind(final int sleepStage) {
        switch (sleepStage) {
            case ColmiR0xConstants.SLEEP_TYPE_LIGHT:
                return ActivityKind.LIGHT_SLEEP;
            case ColmiR0xConstants.SLEEP_TYPE_DEEP:
                return ActivityKind.DEEP_SLEEP;
            case ColmiR0xConstants.SLEEP_TYPE_AWAKE:
                return ActivityKind.AWAKE_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }
}
