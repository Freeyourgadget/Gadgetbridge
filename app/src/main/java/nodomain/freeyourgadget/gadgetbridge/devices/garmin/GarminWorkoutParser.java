package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class GarminWorkoutParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(GarminWorkoutParser.class);

    private final Context context;

    private final List<FitTimeInZone> timesInZone = new ArrayList<>();
    private final List<ActivityPoint> activityPoints = new ArrayList<>();
    private FitSession session = null;
    private FitSport sport = null;
    private FitPhysiologicalMetrics physiologicalMetrics = null;
    private final List<FitSet> sets = new ArrayList<>();

    public GarminWorkoutParser(final Context context) {
        this.context = context;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        if (!forDetails) {
            // Our parsing is too slow, especially without a RecyclerView
            return summary;
        }

        final long nanoStart = System.nanoTime();

        reset();

        final String rawDetailsPath = summary.getRawDetailsPath();
        if (rawDetailsPath == null) {
            LOG.warn("No rawDetailsPath");
            return summary;
        }
        final File file = FileUtils.tryFixPath(new File(rawDetailsPath));
        if (file == null || !file.isFile() || !file.canRead()) {
            LOG.warn("Unable to read {}", rawDetailsPath);
            return summary;
        }

        final FitFile fitFile;
        try {
            fitFile = FitFile.parseIncoming(file);
        } catch (final IOException e) {
            LOG.error("Failed to parse fit file", e);
            return summary;
        }

        for (final RecordData record : fitFile.getRecords()) {
            handleRecord(record);
        }

        updateSummary(summary);

        final long nanoEnd = System.nanoTime();
        final long executionTime = (nanoEnd - nanoStart) / 1000000;
        LOG.trace("Updating summary took {}ms", executionTime);

        return summary;
    }

    public void reset() {
        timesInZone.clear();
        activityPoints.clear();
        session = null;
        sport = null;
        physiologicalMetrics = null;
        sets.clear();
    }

    public boolean handleRecord(final RecordData record) {
        if (record instanceof FitRecord) {
            activityPoints.add(((FitRecord) record).toActivityPoint());
        } else if (record instanceof FitSession) {
            LOG.debug("Session: {}", record);
            if (session != null) {
                LOG.warn("Got multiple sessions - NOT SUPPORTED: {}", record);
            } else {
                // We only support 1 session
                session = (FitSession) record;
            }
        } else if (record instanceof FitPhysiologicalMetrics) {
            LOG.debug("Physiological Metrics: {}", record);
            physiologicalMetrics = (FitPhysiologicalMetrics) record;
        } else if (record instanceof FitSport) {
            LOG.debug("Sport: {}", record);
            if (sport != null) {
                LOG.warn("Got multiple sports - NOT SUPPORTED: {}", record);
            } else {
                // We only support 1 sport
                sport = (FitSport) record;
            }
        } else if (record instanceof FitTimeInZone) {
            LOG.trace("Time in zone: {}", record);
            timesInZone.add((FitTimeInZone) record);
        } else if (record instanceof FitSet) {
            LOG.trace("Set: {}", record);
            sets.add((FitSet) record);
        } else {
            return false;
        }

        return true;
    }

    public void updateSummary(final BaseActivitySummary summary) {
        if (session == null) {
            LOG.error("Got workout, but no session");
            return;
        }

        final ActivitySummaryData summaryData = new ActivitySummaryData();

        final ActivityKind activityKind;
        if (sport != null) {
            summary.setName(sport.getName());
            activityKind = getActivityKind(sport.getSport(), sport.getSubSport());
        } else {
            activityKind = getActivityKind(session.getSport(), session.getSubSport());
        }
        summary.setActivityKind(activityKind.getCode());

        if (session.getTotalElapsedTime() != null) {
            summary.setEndTime(new Date(summary.getStartTime().getTime() + session.getTotalElapsedTime().intValue()));
        }

        if (session.getTotalTimerTime() != null) {
            summaryData.add(ACTIVE_SECONDS, session.getTotalTimerTime() / 1000f, UNIT_SECONDS);
        }
        if (session.getTotalDistance() != null) {
            summaryData.add(DISTANCE_METERS, session.getTotalDistance() / 100f, UNIT_METERS);
        }
        if (session.getTotalCalories() != null) {
            summaryData.add(CALORIES_BURNT, session.getTotalCalories(), UNIT_KCAL);
        }
        if (session.getEstimatedSweatLoss() != null) {
            summaryData.add(ESTIMATED_SWEAT_LOSS, session.getEstimatedSweatLoss(), UNIT_ML);
        }
        if (session.getAverageHeartRate() != null) {
            summaryData.add(HR_AVG, session.getAverageHeartRate(), UNIT_BPM);
        }
        if (session.getMaxHeartRate() != null) {
            summaryData.add(HR_MAX, session.getMaxHeartRate(), UNIT_BPM);
        }
        if (session.getTotalAscent() != null) {
            summaryData.add(ASCENT_DISTANCE, session.getTotalAscent(), UNIT_METERS);
        }
        if (session.getTotalDescent() != null) {
            summaryData.add(DESCENT_DISTANCE, session.getTotalDescent(), UNIT_METERS);
        }

        for (final FitTimeInZone fitTimeInZone : timesInZone) {
            // Find the first time in zone for the session (assumes single-session)
            if (fitTimeInZone.getReferenceMessage() != null && fitTimeInZone.getReferenceMessage() == 18) {
                final Double[] timeInZone = fitTimeInZone.getTimeInZone();
                if (timeInZone != null && timeInZone.length == 6) {
                    summaryData.add(HR_ZONE_NA, timeInZone[0].floatValue(), UNIT_SECONDS);
                    summaryData.add(HR_ZONE_WARM_UP, timeInZone[1].floatValue(), UNIT_SECONDS);
                    summaryData.add(HR_ZONE_FAT_BURN, timeInZone[2].floatValue(), UNIT_SECONDS);
                    summaryData.add(HR_ZONE_AEROBIC, timeInZone[3].floatValue(), UNIT_SECONDS);
                    summaryData.add(HR_ZONE_ANAEROBIC, timeInZone[4].floatValue(), UNIT_SECONDS);
                    summaryData.add(HR_ZONE_EXTREME, timeInZone[5].floatValue(), UNIT_SECONDS);
                }
                break;
            }
        }

        if (physiologicalMetrics != null) {
            if (physiologicalMetrics.getAerobicEffect() != null) {
                summaryData.add(TRAINING_EFFECT_AEROBIC, physiologicalMetrics.getAerobicEffect(), UNIT_NONE);
            }
            if (physiologicalMetrics.getAnaerobicEffect() != null) {
                summaryData.add(TRAINING_EFFECT_ANAEROBIC, physiologicalMetrics.getAnaerobicEffect(), UNIT_NONE);
            }
            if (physiologicalMetrics.getMetMax() != null) {
                summaryData.add(MAXIMUM_OXYGEN_UPTAKE, physiologicalMetrics.getMetMax().floatValue() * 3.5f, UNIT_ML_KG_MIN);
            }
            if (physiologicalMetrics.getRecoveryTime() != null) {
                summaryData.add(RECOVERY_TIME, physiologicalMetrics.getRecoveryTime() * 60, UNIT_SECONDS);
            }
            if (physiologicalMetrics.getLactateThresholdHeartRate() != null) {
                summaryData.add(LACTATE_THRESHOLD_HR, physiologicalMetrics.getLactateThresholdHeartRate(), UNIT_BPM);
            }
        }

        if (!sets.isEmpty()) {
            final boolean isMetric = GBApplication.getPrefs().isMetricUnits();

            int i = 1;
            for (final FitSet set : sets) {
                if (set.getSetType() != null && set.getDuration() != null && set.getSetType() == 1) {
                    final StringBuilder sb = new StringBuilder();

                    if (set.getRepetitions() != null) {
                        if (set.getWeight() != null) {
                            if (isMetric) {
                                sb.append(context.getString(R.string.workout_set_repetitions_weight_kg, set.getRepetitions(), set.getWeight()));
                            } else {
                                sb.append(context.getString(R.string.workout_set_repetitions_weight_lbs, set.getRepetitions(), set.getWeight() * 2.2046226f));
                            }
                        } else {
                            sb.append(context.getString(R.string.workout_set_repetitions, set.getRepetitions()));
                        }

                        sb.append(", ");
                    }

                    sb.append(DateTimeUtils.formatDurationHoursMinutes(set.getDuration().longValue(), TimeUnit.SECONDS));

                    summaryData.add(
                            SETS,
                            context.getString(R.string.workout_set_i, i),
                            sb.toString()
                    );
                    i++;
                }
            }
        }

        summaryData.add(
            INTERNAL_HAS_GPS,
            String.valueOf(activityPoints.stream().anyMatch(p -> p.getLocation() != null))
        );

        summary.setSummaryData(summaryData.toString());
    }

    private static ActivityKind getActivityKind(final Integer sport, final Integer subsport) {
        final Optional<GarminSport> garminSport = GarminSport.fromCodes(sport, subsport);
        if (garminSport.isPresent()) {
            return garminSport.get().getActivityKind();
        } else {
            LOG.warn("Unknown garmin sport {}/{}", sport, subsport);

            final Optional<GarminSport> optGarminSportFallback = GarminSport.fromCodes(sport, 0);
            if (!optGarminSportFallback.isEmpty()) {
                return optGarminSportFallback.get().getActivityKind();
            }
        }

        return ActivityKind.UNKNOWN;
    }
}
