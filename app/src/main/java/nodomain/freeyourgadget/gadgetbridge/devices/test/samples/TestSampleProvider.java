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
package nodomain.freeyourgadget.gadgetbridge.devices.test.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceRand;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class TestSampleProvider extends AbstractSampleProvider<TestSampleProvider.TestActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(TestSampleProvider.class);

    public TestSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<TestActivitySample, ?> getSampleDao() {
        return null;
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return null;
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
    public TestActivitySample createActivitySample() {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Override
    protected List<TestActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to, final int activityType) {
        final List<TestActivitySample> samples = new ArrayList<>();

        int[] sleepStages = new int[] {
                ActivityKind.TYPE_LIGHT_SLEEP,
                ActivityKind.TYPE_DEEP_SLEEP,
        };
        if (getDevice().getDeviceCoordinator().supportsRemSleep()) {
            sleepStages = ArrayUtils.add(sleepStages, ActivityKind.TYPE_REM_SLEEP);
        }
        int sleepStageCurrent = 0;
        int sleepStageDirection = 1;
        int sleepStageDurationRemaining = TestDeviceRand.randInt(timestamp_from * 1000L, 30, 90);

        boolean isActive = false;
        float dayActivityFactor = TestDeviceRand.randFloat(timestamp_from * 1000L, 0f, 1f);
        int steps = (int) (TestDeviceRand.randInt(timestamp_from * 1000L, 0, 100) * dayActivityFactor);
        int intensity = TestDeviceRand.randInt(timestamp_from * 1000L, 0, 100);
        int hr = TestDeviceRand.randInt(timestamp_from * 1000L, 90, 153);

        final long bedtimeHour = TestDeviceRand.randInt(timestamp_from, 21, 22);
        final long bedtimeMinute = TestDeviceRand.randInt(timestamp_from, 0, 59);
        final long wakeHour = TestDeviceRand.randInt(timestamp_from, 6, 9);
        final long wakeMinute = TestDeviceRand.randInt(timestamp_from, 0, 59);

        final Calendar cal = GregorianCalendar.getInstance();

        for (long ts = timestamp_from * 1000L; ts < timestamp_to * 1000L; ts += 60 * 1000L) {
            cal.setTimeInMillis(ts);
            final int h = cal.get(Calendar.HOUR_OF_DAY);
            final int m = cal.get(Calendar.MINUTE);
            boolean isSleep = false;
            if (h == bedtimeHour) {
                isSleep = m >= bedtimeMinute;
            } else if (h > bedtimeHour) {
                isSleep = true;
            } else if (h == wakeHour) {
                isSleep = m <= wakeMinute;
            } else if (h < wakeHour) {
                isSleep = true;
            }
            if (isSleep) {
                isActive = false;
            } else if (isActive) {
                isActive = TestDeviceRand.randBool(ts, 0.8F);
            } else {
                isActive = TestDeviceRand.randBool(ts, 0.05F);
            }

            if (TestDeviceRand.randBool(ts, 0.85f)) {
                samples.add(new TestActivitySample(
                        (int) (ts / 1000),
                        isSleep ? sleepStages[sleepStageCurrent] : ActivityKind.TYPE_UNKNOWN,
                        isActive ? steps : 0,
                        intensity,
                        hr
                ));
            }

            if (isSleep) {
                sleepStageDurationRemaining--;
                if (sleepStageDurationRemaining <= 0) {
                    sleepStageDurationRemaining = TestDeviceRand.randInt(ts * 1000L, 30, 90);
                    sleepStageCurrent += sleepStageDirection;
                    if (sleepStageCurrent == 0 || sleepStageCurrent == sleepStages.length - 1) {
                        sleepStageDirection *= -1;
                    }
                }
            }

            steps += TestDeviceRand.randInt(ts, -steps, 100 - steps) * dayActivityFactor;
            intensity += TestDeviceRand.randInt(ts, -1, 1);
            hr += TestDeviceRand.randInt(ts, -2, 2);
        }

        return samples;
    }

    public class TestActivitySample extends AbstractActivitySample {
        private final int timestamp;
        private final int kind;
        private final int steps;
        private final int intensity;
        private final int hr;

        public TestActivitySample(final int timestamp,
                                  final int kind,
                                  final int steps,
                                  final int intensity,
                                  final int hr) {
            this.timestamp = timestamp;
            this.kind = kind;
            this.steps = steps;
            this.intensity = intensity;
            this.hr = hr;
        }

        @Override
        public SampleProvider getProvider() {
            return TestSampleProvider.this;
        }

        @Override
        public void setTimestamp(final int timestamp) {

        }

        @Override
        public void setUserId(final long userId) {

        }

        @Override
        public void setDeviceId(final long deviceId) {

        }

        @Override
        public long getDeviceId() {
            return 0;
        }

        @Override
        public long getUserId() {
            return 0;
        }

        @Override
        public int getTimestamp() {
            return timestamp;
        }

        @Override
        public int getKind() {
            return kind;
        }

        @Override
        public int getRawKind() {
            return kind;
        }

        @Override
        public float getIntensity() {
            return intensity / 100f;
        }

        @Override
        public int getHeartRate() {
            return hr;
        }

        @Override
        public int getRawIntensity() {
            return intensity;
        }

        @Override
        public int getSteps() {
            return steps;
        }
    }
}
