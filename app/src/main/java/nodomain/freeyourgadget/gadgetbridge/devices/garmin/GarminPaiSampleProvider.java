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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminIntensityMinutesSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;

public class GarminPaiSampleProvider implements TimeSampleProvider<PaiSample> {
    private final GarminIntensityMinutesSampleProvider intensityMinutesSampleProvider;

    public GarminPaiSampleProvider(final GBDevice device, final DaoSession session) {
        this.intensityMinutesSampleProvider = new GarminIntensityMinutesSampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<PaiSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        // Intensity minutes reset every monday, so we need to go to the previous monday if we
        // are not there yet
        final ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampFrom), tz);
        final DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();
        if (dayOfWeek != DayOfWeek.MONDAY) {
            zonedDateTime = zonedDateTime.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        }
        zonedDateTime = zonedDateTime.truncatedTo(ChronoUnit.DAYS);
        final List<GarminIntensityMinutesSample> allSamples = intensityMinutesSampleProvider.getAllSamples(
                zonedDateTime.toEpochSecond() * 1000L,
                timestampTo
        );
        if (allSamples.isEmpty()) {
            return Collections.emptyList();
        }

        int totalModerate = 0;
        int totalVigorous = 0;
        int currentDay = 0;

        LocalDate lastSampleDate = LocalDate.ofInstant(Instant.ofEpochMilli(allSamples.get(0).getTimestamp()), tz);
        final List<PaiSample> ret = new ArrayList<>(allSamples.size());
        for (final GarminIntensityMinutesSample sample : allSamples) {
            final LocalDate sampleDate = LocalDate.ofInstant(Instant.ofEpochMilli(sample.getTimestamp()), tz);

            // Since we only persist minute samples for days where there was activity, we need to fill any gaps
            for (LocalDate d = lastSampleDate.plusDays(1); d.isBefore(sampleDate); d = d.plusDays(1)) {
                if (lastSampleDate.getDayOfWeek() != d.getDayOfWeek()) {
                    currentDay = 0;
                    if (d.getDayOfWeek() == DayOfWeek.MONDAY) {
                        totalModerate = 0;
                        totalVigorous = 0;
                    }
                }

                ret.add(new GarminPaiSample(
                        d.atStartOfDay(tz).toInstant().toEpochMilli(),
                        totalModerate,
                        totalVigorous,
                        currentDay
                ));

                lastSampleDate = d;
            }

            if (sampleDate.getDayOfWeek() != lastSampleDate.getDayOfWeek()) {
                currentDay = 0;
                if (sampleDate.getDayOfWeek() == DayOfWeek.MONDAY) {
                    totalModerate = 0;
                    totalVigorous = 0;
                }
            }

            totalModerate += sample.getModerate();
            totalVigorous += sample.getVigorous();
            currentDay += sample.getModerate() + sample.getVigorous() * 2;

            if (sample.getTimestamp() >= timestampFrom && sample.getTimestamp() <= timestampTo) {
                ret.add(new GarminPaiSample(
                        sample.getTimestamp(),
                        totalModerate,
                        totalVigorous,
                        currentDay
                ));
            }

            lastSampleDate = sampleDate;
        }

        // Finally, fill out from the last sample to the end of the timestampTo
        final LocalDate endDate = LocalDate.ofInstant(Instant.ofEpochMilli(timestampTo), tz);
        for (LocalDate d = lastSampleDate.plusDays(1); !d.isAfter(endDate); d = d.plusDays(1)) {
            if (lastSampleDate.getDayOfWeek() != d.getDayOfWeek()) {
                currentDay = 0;
                if (d.getDayOfWeek() == DayOfWeek.MONDAY) {
                    totalModerate = 0;
                    totalVigorous = 0;
                }
            }

            ret.add(new GarminPaiSample(
                    d.atStartOfDay(tz).toInstant().toEpochMilli(),
                    totalModerate,
                    totalVigorous,
                    currentDay
            ));

            lastSampleDate = d;
        }

        return ret;
    }

    @Override
    public void addSample(final PaiSample timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(final List<PaiSample> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public PaiSample createSample() {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Nullable
    @Override
    public PaiSample getLatestSample() {
        // TODO
        return null;
    }

    @Nullable
    @Override
    public PaiSample getLatestSample(long until) {
        // TODO
        return null;
    }

    @Nullable
    @Override
    public PaiSample getFirstSample() {
        // TODO
        return null;
    }


    public static class GarminPaiSample implements PaiSample {
        private final long timestamp;
        private final int minutesModerate;
        private final int minutesVigorous;
        private final int today;

        public GarminPaiSample(final long timestamp,
                               final int moderate,
                               final int vigorous,
                               final int today) {
            this.timestamp = timestamp;
            this.minutesModerate = moderate;
            this.minutesVigorous = vigorous;
            this.today = today;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public float getPaiLow() {
            return 0;
        }

        @Override
        public float getPaiModerate() {
            return minutesModerate;
        }

        @Override
        public float getPaiHigh() {
            return minutesVigorous * 2;
        }

        @Override
        public int getTimeLow() {
            return 0; // not supported
        }

        @Override
        public int getTimeModerate() {
            return minutesModerate;
        }

        @Override
        public int getTimeHigh() {
            return minutesVigorous;
        }

        @Override
        public float getPaiToday() {
            return today;
        }

        @Override
        public float getPaiTotal() {
            return minutesModerate + 2 * minutesVigorous;
        }
    }
}
