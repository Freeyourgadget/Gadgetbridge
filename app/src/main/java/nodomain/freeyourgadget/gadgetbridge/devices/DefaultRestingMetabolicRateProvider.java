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
package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;

/**
 * Provides a default resting metabolic rate, for devices that do not provide it. Currently it uses the
 * Mifflin St Jeor equation.
 * TODO: use the user data at that timestamp, and make the algorithm configurable.
 * TODO: maybe let the user also configure their own static value
 */
public class DefaultRestingMetabolicRateProvider extends AbstractTimeSampleProvider<RestingMetabolicRateSample> {
    private final DaoSession mSession;
    private final GBDevice mDevice;

    public DefaultRestingMetabolicRateProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
        mDevice = device;
        mSession = session;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @NonNull
    @Override
    public List<RestingMetabolicRateSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        return Collections.singletonList(getLatestSample());
    }

    @Override
    public void addSample(final RestingMetabolicRateSample timeSample) {
        throw new UnsupportedOperationException("addSample not supported");
    }

    @Override
    public void addSamples(final List<RestingMetabolicRateSample> timeSamples) {
        throw new UnsupportedOperationException("addSamples not supported");
    }

    @Override
    public RestingMetabolicRateSample createSample() {
        return new DefaultRestingMetabolicRateSample(System.currentTimeMillis());
    }

    @NonNull
    @Override
    public AbstractDao<RestingMetabolicRateSample, ?> getSampleDao() {
        throw new UnsupportedOperationException("getSampleDao not supported");
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        throw new UnsupportedOperationException("getTimestampSampleProperty not supported");
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        throw new UnsupportedOperationException("getDeviceIdentifierSampleProperty not supported");
    }

    @Nullable
    @Override
    public RestingMetabolicRateSample getLatestSample() {
        return new DefaultRestingMetabolicRateSample(System.currentTimeMillis());
    }

    @Nullable
    public RestingMetabolicRateSample getLastSampleBefore(final long timestampTo) {
        return new DefaultRestingMetabolicRateSample(timestampTo);
    }

    @Nullable
    public RestingMetabolicRateSample getNextSampleAfter(final long timestampFrom) {
        return new DefaultRestingMetabolicRateSample(timestampFrom);
    }

    @Nullable
    @Override
    public RestingMetabolicRateSample getFirstSample() {
        return getLatestSample();
    }

    public static class DefaultRestingMetabolicRateSample extends RestingMetabolicRateSample {
        private long timestamp;
        private final int rate;

        public DefaultRestingMetabolicRateSample(final long timestamp) {
            this.timestamp = timestamp;
            ActivityUser activityUser = new ActivityUser();
            final int weightKg = activityUser.getWeightKg();
            final int heightCm = activityUser.getHeightCm();
            final int age = activityUser.getAge();
            final int s;
            switch (activityUser.getGender()) {
                case ActivityUser.GENDER_FEMALE:
                    s = -161;
                    break;
                case ActivityUser.GENDER_MALE:
                    s = 5;
                    break;
                default:
                    s = (5 - 161) / 2; // average it?
                    break;
            }
            // Mifflin St Jeor equation
            this.rate = (int) Math.round(10.0d * weightKg + 6.25 * heightCm - 5d * age + s);
        }

        @Override
        public int getRestingMetabolicRate() {
            return rate;
        }

        @Override
        public void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getUserId() {
            return 0;
        }

        @Override
        public void setUserId(final long userId) {

        }

        @Override
        public long getDeviceId() {
            return 0;
        }

        @Override
        public void setDeviceId(final long deviceId) {

        }

        @Override
        public void setDevice(final Device device) {

        }

        @Override
        public void setUser(final User user) {

        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
