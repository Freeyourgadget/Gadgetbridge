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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

/**
 * An implementation of {@link Vo2MaxSampleProvider} that extracts VO2 Max values from
 * workouts, for devices that provide them as part of the workout data.
 */
public class WorkoutVo2MaxSampleProvider implements Vo2MaxSampleProvider<Vo2MaxSample> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutVo2MaxSampleProvider.class);

    private final GBDevice device;
    private final DaoSession session;

    public WorkoutVo2MaxSampleProvider(final GBDevice device, final DaoSession session) {
        this.device = device;
        this.session = session;
    }

    @NonNull
    @Override
    public List<Vo2MaxSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .where(BaseActivitySummaryDao.Properties.StartTime.gt(new Date(timestampFrom)))
                .where(BaseActivitySummaryDao.Properties.StartTime.lt(new Date(timestampTo)))
                .orderAsc(BaseActivitySummaryDao.Properties.StartTime);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();
        fillSummaryData(coordinator, samples);

        return samples.stream()
                .map(GarminVo2maxSample::fromActivitySummary)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addSample(final Vo2MaxSample timeSample) {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Override
    public void addSamples(final List<Vo2MaxSample> timeSamples) {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Override
    public Vo2MaxSample createSample() {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Nullable
    @Override
    public Vo2MaxSample getLatestSample(final Vo2MaxSample.Type type, final long until) {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return null;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();

        addWhereFilter(coordinator, qb, type);

        if (until != 0) {
            qb.where(BaseActivitySummaryDao.Properties.StartTime.le(new Date(until)));
        }

        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .orderDesc(BaseActivitySummaryDao.Properties.StartTime)
                .limit(1);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();
        fillSummaryData(coordinator, samples);

        return !samples.isEmpty() ? GarminVo2maxSample.fromActivitySummary(samples.get(0)) : null;
    }

    private static void addWhereFilter(final DeviceCoordinator coordinator,
                                       final QueryBuilder<BaseActivitySummary> qb,
                                       final Vo2MaxSample.Type type) {
        final List<Integer> codes = new ArrayList<>();

        if (coordinator.supportsVO2MaxRunning()) {
            if (type == Vo2MaxSample.Type.ANY || type == Vo2MaxSample.Type.RUNNING) {
                codes.addAll(Arrays.asList(
                        ActivityKind.INDOOR_RUNNING.getCode(),
                        ActivityKind.OUTDOOR_RUNNING.getCode(),
                        ActivityKind.CROSS_COUNTRY_RUNNING.getCode(),
                        ActivityKind.RUNNING.getCode()
                ));
            }
        }
        if (coordinator.supportsVO2MaxCycling()) {
            if (type == Vo2MaxSample.Type.ANY || type == Vo2MaxSample.Type.CYCLING) {
                codes.addAll(Arrays.asList(
                        ActivityKind.CYCLING.getCode(),
                        ActivityKind.INDOOR_CYCLING.getCode(),
                        ActivityKind.HANDCYCLING.getCode(),
                        ActivityKind.HANDCYCLING_INDOOR.getCode(),
                        ActivityKind.OUTDOOR_CYCLING.getCode()
                ));
            }
        }

        qb.where(BaseActivitySummaryDao.Properties.ActivityKind.in(codes));
    }

    private void fillSummaryData(final DeviceCoordinator coordinator,
                                 final Collection<BaseActivitySummary> summaries) {
        ActivitySummaryParser activitySummaryParser = coordinator.getActivitySummaryParser(device, GBApplication.getContext());
        for (final BaseActivitySummary summary : summaries) {
            if (summary.getSummaryData() == null) {
                activitySummaryParser.parseBinaryData(summary, true);
            }
        }
    }

    @Nullable
    @Override
    public Vo2MaxSample getLatestSample() {
        return getLatestSample(Vo2MaxSample.Type.ANY, 0);
    }

    @Nullable
    @Override
    public Vo2MaxSample getFirstSample() {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return null;
        }

        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .orderAsc(BaseActivitySummaryDao.Properties.StartTime)
                .limit(1);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();

        return !samples.isEmpty() ? GarminVo2maxSample.fromActivitySummary(samples.get(0)) : null;
    }


    public static class GarminVo2maxSample implements Vo2MaxSample {
        private final long timestamp;
        private final Type type;
        private final float value;

        public GarminVo2maxSample(final long timestamp, final Type type, final float value) {
            this.timestamp = timestamp;
            this.type = type;
            this.value = value;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public float getValue() {
            return value;
        }

        @Nullable
        public static GarminVo2maxSample fromActivitySummary(final BaseActivitySummary summary) {
            final String summaryDataJson = summary.getSummaryData();
            if (summaryDataJson == null) {
                return null;
            }

            if (!summaryDataJson.contains(ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE)) {
                return null;
            }

            final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summaryDataJson);
            if (summaryData == null) {
                return null;
            }

            final double value = summaryData.getNumber(ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE, 0).doubleValue();
            if (value == 0) {
                return null;
            }

            final Vo2MaxSample.Type type;
            switch (ActivityKind.fromCode(summary.getActivityKind())) {
                case INDOOR_RUNNING:
                case OUTDOOR_RUNNING:
                case CROSS_COUNTRY_RUNNING:
                case RUNNING:
                    type = Vo2MaxSample.Type.RUNNING;
                    break;
                case CYCLING:
                case INDOOR_CYCLING:
                case HANDCYCLING:
                case HANDCYCLING_INDOOR:
                case MOTORCYCLING:
                case OUTDOOR_CYCLING:
                    type = Vo2MaxSample.Type.CYCLING;
                    break;
                default:
                    type = Vo2MaxSample.Type.ANY;
            }
            return new GarminVo2maxSample(summary.getStartTime().getTime(), type, (float) value);
        }
    }
}
