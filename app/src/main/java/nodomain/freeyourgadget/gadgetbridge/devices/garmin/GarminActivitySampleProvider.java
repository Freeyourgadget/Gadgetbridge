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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.util.RangeMap;

public class GarminActivitySampleProvider extends AbstractSampleProvider<GarminActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(GarminActivitySampleProvider.class);

    public GarminActivitySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<GarminActivitySample, ?> getSampleDao() {
        return getSession().getGarminActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return GarminActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GarminActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GarminActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(final int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(final int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity / 100f;
    }

    @Override
    public GarminActivitySample createActivitySample() {
        return new GarminActivitySample();
    }

    @Override
    protected List<GarminActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to, final int activityType) {
        LOG.trace(
                "Getting garmin activity samples for {} between {} and {}",
                String.format("0x%08x", activityType),
                timestamp_from,
                timestamp_to
        );

        final long nanoStart = System.nanoTime();

        final List<GarminActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, activityType);

        if (!samples.isEmpty()) {
            convertCumulativeSteps(samples, GarminActivitySampleDao.Properties.Steps);
        }

        overlaySleep(samples, timestamp_from, timestamp_to);

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        LOG.trace("Getting Garmin samples took {}ms", executionTime);

        return samples;
    }

    public void overlaySleep(final List<GarminActivitySample> samples, final int timestamp_from, final int timestamp_to) {
        // The samples provided by Garmin are upper-bound timestamps of the sleep stage
        final RangeMap<Long, Integer> stagesMap = new RangeMap<>(RangeMap.Mode.UPPER_BOUND);

        final GarminEventSampleProvider eventSampleProvider = new GarminEventSampleProvider(getDevice(), getSession());
        final List<GarminEventSample> sleepEventSamples = eventSampleProvider.getSleepEvents(
                timestamp_from * 1000L - 86400000L,
                timestamp_to * 1000L
        );
        if (!sleepEventSamples.isEmpty()) {
            LOG.debug("Found {} sleep event samples between {} and {}", sleepEventSamples.size(), timestamp_from, timestamp_to);
            for (final GarminEventSample event : sleepEventSamples) {
                switch (event.getEventType()) {
                    case 0: // start
                        // We only need the start event as an upper-bound timestamp (anything before it is unknown)
                        stagesMap.put(event.getTimestamp(), ActivityKind.TYPE_UNKNOWN);
                    case 1: // stop
                    default:
                }
            }
        }

        final GarminSleepStageSampleProvider sleepStagesSampleProvider = new GarminSleepStageSampleProvider(getDevice(), getSession());
        final List<GarminSleepStageSample> stageSamples = sleepStagesSampleProvider.getAllSamples(
                timestamp_from * 1000L - 86400000L,
                timestamp_to * 1000L
        );

        if (!stageSamples.isEmpty()) {
            // We got actual sleep stages
            LOG.debug("Found {} sleep stage samples between {} and {}", stageSamples.size(), timestamp_from, timestamp_to);

            for (final GarminSleepStageSample stageSample : stageSamples) {
                final int activityKind;

                final FieldDefinitionSleepStage.SleepStage sleepStage = FieldDefinitionSleepStage.SleepStage.fromId(stageSample.getStage());
                if (sleepStage == null) {
                    LOG.error("Unknown sleep stage for {}", stageSample.getStage());
                    continue;
                }

                switch (sleepStage) {
                    case LIGHT:
                        activityKind = ActivityKind.TYPE_LIGHT_SLEEP;
                        break;
                    case DEEP:
                        activityKind = ActivityKind.TYPE_DEEP_SLEEP;
                        break;
                    case REM:
                        activityKind = ActivityKind.TYPE_REM_SLEEP;
                        break;
                    default:
                        activityKind = ActivityKind.TYPE_UNKNOWN;
                        break;
                }
                stagesMap.put(stageSample.getTimestamp(), activityKind);
            }
        }

        if (!stagesMap.isEmpty()) {
            for (final GarminActivitySample sample : samples) {
                final long ts = sample.getTimestamp() * 1000L;
                final Integer sleepType = stagesMap.get(ts);
                if (sleepType != null && !sleepType.equals(ActivityKind.TYPE_UNKNOWN)) {
                    sample.setRawKind(sleepType);

                    switch (sleepType) {
                        case ActivityKind.TYPE_DEEP_SLEEP:
                            sample.setRawIntensity(20);
                            break;
                        case ActivityKind.TYPE_LIGHT_SLEEP:
                            sample.setRawIntensity(30);
                            break;
                        case ActivityKind.TYPE_REM_SLEEP:
                            sample.setRawIntensity(40);
                            break;
                    }
                }
            }
        }
    }
}
