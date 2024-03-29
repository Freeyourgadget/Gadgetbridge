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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepTimeSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.RangeMap;

public class XiaomiSampleProvider extends AbstractSampleProvider<XiaomiActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSampleProvider.class);

    public XiaomiSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<XiaomiActivitySample, ?> getSampleDao() {
        return getSession().getXiaomiActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return XiaomiActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XiaomiActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XiaomiActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(final int rawType) {
        // TODO
        return rawType;
    }

    @Override
    public int toRawActivityKind(final int activityKind) {
        // TODO
        return activityKind;
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity / 100f;
    }

    @Override
    public XiaomiActivitySample createActivitySample() {
        return new XiaomiActivitySample();
    }

    @Override
    protected List<XiaomiActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to, final int activityType) {
        final List<XiaomiActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, activityType);

        overlaySleep(samples, timestamp_from, timestamp_to);

        return samples;
    }

    /**
     * Overlay sleep states on activity samples, since they are stored on a separate table.
     *
     * @implNote This currently needs to look back a further 24h, so that we are sure that we
     * got the sleep start of a sleep session at the start of the samples, if any. This is especially
     * noticeable if the charts are configured in a noon-to-noon setting. FIXME: This is not ideal,
     * and we may need to rethink the way sleep samples are persisted in the database for Xiaomi devices.
     */
    public void overlaySleep(final List<XiaomiActivitySample> samples, final int timestamp_from, final int timestamp_to) {
        final RangeMap<Long, Integer> stagesMap = new RangeMap<>();

        final XiaomiSleepStageSampleProvider sleepStagesSampleProvider = new XiaomiSleepStageSampleProvider(getDevice(), getSession());
        final List<XiaomiSleepStageSample> stageSamples = sleepStagesSampleProvider.getAllSamples(
                timestamp_from * 1000L - 86400000L,
                timestamp_to * 1000L
        );
        if (!stageSamples.isEmpty()) {
            // We got actual sleep stages
            LOG.debug("Found {} sleep stage samples between {} and {}", stageSamples.size(), timestamp_from, timestamp_to);

            for (final XiaomiSleepStageSample stageSample : stageSamples) {
                final int activityKind;

                switch (stageSample.getStage()) {
                    case 2: // deep
                        activityKind = ActivityKind.TYPE_DEEP_SLEEP;
                        break;
                    case 3: // light
                        activityKind = ActivityKind.TYPE_LIGHT_SLEEP;
                        break;
                    case 4: // rem
                        activityKind = ActivityKind.TYPE_REM_SLEEP;
                        break;
                    case 0: // final awake
                    case 1: // ?
                    case 5: // awake during the night
                    default:
                        activityKind = ActivityKind.TYPE_UNKNOWN;
                        break;
                }
                stagesMap.put(stageSample.getTimestamp(), activityKind);
            }
        }

        // Fetch bed and wakeup times as well.
        final XiaomiSleepTimeSampleProvider sleepTimeSampleProvider = new XiaomiSleepTimeSampleProvider(getDevice(), getSession());
        final List<XiaomiSleepTimeSample> sleepTimeSamples = sleepTimeSampleProvider.getAllSamples(
                timestamp_from * 1000L - 86400000L,
                timestamp_to * 1000L
        );
        if (!sleepTimeSamples.isEmpty()) {
            LOG.debug("Found {} sleep samples between {} and {}", sleepTimeSamples.size(), timestamp_from, timestamp_to);
            for (final XiaomiSleepTimeSample stageSample : sleepTimeSamples) {
                if (stageSamples.isEmpty()) {
                    // Only overlay them as light sleep if we don't have actual sleep stages
                    stagesMap.put(stageSample.getTimestamp(), ActivityKind.TYPE_LIGHT_SLEEP);
                }

                // We need to set the wakeup times, because some bands don't report them in the stage samples (see #3502)
                stagesMap.put(stageSample.getWakeupTime(), ActivityKind.TYPE_UNKNOWN);
            }
        }

        if (!stagesMap.isEmpty()) {
            LOG.debug("Found {} sleep samples between {} and {}", stagesMap.size(), timestamp_from, timestamp_to);

            for (final XiaomiActivitySample sample : samples) {
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
