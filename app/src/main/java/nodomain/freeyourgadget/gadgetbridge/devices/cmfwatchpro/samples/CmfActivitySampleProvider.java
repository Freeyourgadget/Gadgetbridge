/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class CmfActivitySampleProvider extends AbstractSampleProvider<CmfActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(CmfActivitySampleProvider.class);

    public CmfActivitySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<CmfActivitySample, ?> getSampleDao() {
        return getSession().getCmfActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return CmfActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CmfActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CmfActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(final int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(final ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity / 100f;
    }

    @Override
    public CmfActivitySample createActivitySample() {
        return new CmfActivitySample();
    }

    @Override
    protected List<CmfActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        LOG.trace(
                "Getting cmf activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );

        final long nanoStart = System.nanoTime();

        final List<CmfActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to);

        if (!samples.isEmpty()) {
            convertCumulativeSteps(samples, CmfActivitySampleDao.Properties.Steps);
        }

        final Map<Integer, CmfActivitySample> sampleByTs = new HashMap<>();
        for (final CmfActivitySample sample : samples) {
            sampleByTs.put(sample.getTimestamp(), sample);
        }

        overlayHeartRate(sampleByTs, timestamp_from, timestamp_to);
        overlaySleep(sampleByTs, timestamp_from, timestamp_to);

        final List<CmfActivitySample> finalSamples = new ArrayList<>(sampleByTs.values());
        Collections.sort(finalSamples, (a, b) -> Integer.compare(a.getTimestamp(), b.getTimestamp()));

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        LOG.trace("Getting cmf samples took {}ms", executionTime);

        return finalSamples;
    }

    private void overlayHeartRate(final Map<Integer, CmfActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final CmfHeartRateSampleProvider heartRateSampleProvider = new CmfHeartRateSampleProvider(getDevice(), getSession());
        final List<CmfHeartRateSample> hrSamples = heartRateSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        for (final CmfHeartRateSample hrSample : hrSamples) {
            // round to the nearest minute, we don't need per-second granularity
            final int tsSeconds = (int) ((hrSample.getTimestamp() / 1000) / 60) * 60;
            CmfActivitySample sample = sampleByTs.get(tsSeconds);
            if (sample == null) {
                //LOG.debug("Adding dummy sample at {} for hr", tsSeconds);
                sample = new CmfActivitySample();
                sample.setTimestamp(tsSeconds);
                sample.setProvider(this);
                sampleByTs.put(tsSeconds, sample);
            }

            sample.setHeartRate(hrSample.getHeartRate());
        }
    }

    private void overlaySleep(final Map<Integer, CmfActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final CmfSleepStageSampleProvider sleepStageSampleProvider = new CmfSleepStageSampleProvider(getDevice(), getSession());
        final List<CmfSleepStageSample> sleepStageSamples = sleepStageSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        for (final CmfSleepStageSample sleepStageSample : sleepStageSamples) {
            // round to the nearest minute, we don't need per-second granularity
            final int tsSeconds = (int) ((sleepStageSample.getTimestamp() / 1000) / 60) * 60;
            for (int i = tsSeconds; i < tsSeconds + sleepStageSample.getDuration(); i += 60) {
                CmfActivitySample sample = sampleByTs.get(i);
                if (sample == null) {
                    //LOG.debug("Adding dummy sample at {} for sleep", i);
                    sample = new CmfActivitySample();
                    sample.setTimestamp(i);
                    sample.setProvider(this);
                    sampleByTs.put(i, sample);
                }

                final ActivityKind sleepRawKind = sleepStageToActivityKind(sleepStageSample.getStage());
                sample.setRawKind(sleepRawKind.getCode());
                sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            }
        }
    }

    final ActivityKind sleepStageToActivityKind(final int sleepStage) {
        switch (sleepStage) {
            case 1:
                return ActivityKind.DEEP_SLEEP;
            case 2:
                return ActivityKind.LIGHT_SLEEP;
            case 3:
                return ActivityKind.REM_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }
}
