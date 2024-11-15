/*  Copyright (C) 2020-2024 Gordon Williams, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class BangleJSSampleProvider extends AbstractSampleProvider<BangleJSActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(BangleJSSampleProvider.class);

    public BangleJSSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<BangleJSActivitySample, ?> getSampleDao() {
        return getSession().getBangleJSActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return BangleJSActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return BangleJSActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return BangleJSActivitySampleDao.Properties.DeviceId;
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
        return rawIntensity / 2048.0f;
    }

    @Override
    public BangleJSActivitySample createActivitySample() {
        return new BangleJSActivitySample();
    }

    /**
     * Upserts a sample in the database, avoiding duplicated samples if a sample already exists in a
     * close timestamp (within 2 minutes);
     */
    public void upsertSample(final BangleJSActivitySample sample) {
        final List<BangleJSActivitySample> nearSamples = getGBActivitySamples(
                sample.getTimestamp() - 60 * 2,
                sample.getTimestamp() + 60 * 2
        );

        if (nearSamples.isEmpty()) {
            // No nearest sample, just insert
            LOG.debug("No duplicate found at {}, inserting", sample.getTimestamp());
            addGBActivitySample(sample);
            return;
        }

        BangleJSActivitySample nearestSample = nearSamples.get(0);

        for (final BangleJSActivitySample s : nearSamples) {
            final int curDist = Math.abs(nearestSample.getTimestamp() - s.getTimestamp());
            final int newDist = Math.abs(sample.getTimestamp() - s.getTimestamp());
            if (newDist < curDist) {
                nearestSample = s;
            }
        }

        LOG.debug("Found {} duplicates for {}, updating nearest sample at {}", nearSamples.size(), sample.getTimestamp(), nearestSample.getTimestamp());

        if (sample.getHeartRate() != 0) {
            nearestSample.setHeartRate(sample.getHeartRate());
        }
        if (sample.getSteps() != 0) {
            nearestSample.setSteps(sample.getSteps());
        }
        if (sample.getRawIntensity() != 0) {
            nearestSample.setRawIntensity(sample.getRawIntensity());
        }

        addGBActivitySample(nearestSample);
    }
}
