package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ASCENT_DISTANCE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DESCENT_DISTANCE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ESTIMATED_SWEAT_LOSS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_EXTREME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_FAT_BURN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_NA;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_WARM_UP;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.LACTATE_THRESHOLD_HR;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.RECOVERY_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_HOURS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_ML;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_ML_KG_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.WORKOUT_LOAD;

import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminBodyEnergySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminEventSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminBodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHrvSummary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHrvValue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoring;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSpo2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitStressLevel;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FitImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FitImporter.class);

    private final Context context;
    private final GBDevice gbDevice;

    private final SortedMap<Integer, List<FitMonitoring>> activitySamplesPerTimestamp = new TreeMap<>();
    private final List<GarminStressSample> stressSamples = new ArrayList<>();
    private final List<GarminBodyEnergySample> bodyEnergySamples = new ArrayList<>();
    private final List<GarminSpo2Sample> spo2samples = new ArrayList<>();
    private final List<GarminEventSample> events = new ArrayList<>();
    private final List<GarminSleepStageSample> sleepStageSamples = new ArrayList<>();
    private final List<GarminHrvSummarySample> hrvSummarySamples = new ArrayList<>();
    private final List<GarminHrvValueSample> hrvValueSamples = new ArrayList<>();
    private final List<FitTimeInZone> timesInZone = new ArrayList<>();
    private final List<ActivityPoint> activityPoints = new ArrayList<>();
    private final Map<Integer, Integer> unknownRecords = new HashMap<>();
    private FitFileId fileId = null;
    private FitSession session = null;
    private FitSport sport = null;
    private FitPhysiologicalMetrics physiologicalMetrics = null;

    public FitImporter(final Context context, final GBDevice gbDevice) {
        this.context = context;
        this.gbDevice = gbDevice;
    }

    public void importFile(final File file) throws IOException {
        reset();

        final FitFile fitFile = FitFile.parseIncoming(file);

        for (final RecordData record : fitFile.getRecords()) {
            final Long ts = record.getComputedTimestamp();

            if (record instanceof FitFileId) {
                final FitFileId newFileId = (FitFileId) record;
                LOG.debug("File ID: {}", newFileId);
                if (fileId != null) {
                    // Should not happen
                    LOG.warn("Already had a file ID: {}", fileId);
                }
                fileId = newFileId;
            } else if (record instanceof FitStressLevel) {
                final FitStressLevel stressRecord = (FitStressLevel) record;
                final Integer stress = stressRecord.getStressLevelValue();
                if (stress != null && stress >= 0) {
                    LOG.trace("Stress at {}: {}", ts, stress);
                    final GarminStressSample sample = new GarminStressSample();
                    sample.setTimestamp(ts * 1000L);
                    sample.setStress(stress);
                    stressSamples.add(sample);
                }

                final Integer energy = stressRecord.getBodyEnergy();
                if (energy != null) {
                    LOG.trace("Body energy at {}: {}", ts, energy);
                    final GarminBodyEnergySample sample = new GarminBodyEnergySample();
                    sample.setTimestamp(ts * 1000L);
                    sample.setEnergy(energy);
                    bodyEnergySamples.add(sample);
                }
            } else if (record instanceof FitSleepStage) {
                final FieldDefinitionSleepStage.SleepStage stage = ((FitSleepStage) record).getSleepStage();
                if (stage == null) {
                    continue;
                }
                LOG.trace("Sleep stage at {}: {}", ts, record);
                final GarminSleepStageSample sample = new GarminSleepStageSample();
                sample.setTimestamp(ts * 1000L);
                sample.setStage(stage.getId());
                sleepStageSamples.add(sample);
            } else if (record instanceof FitMonitoring) {
                LOG.trace("Monitoring at {}: {}", ts, record);
                if (!activitySamplesPerTimestamp.containsKey(ts.intValue())) {
                    activitySamplesPerTimestamp.put(ts.intValue(), new ArrayList<>());
                }
                Objects.requireNonNull(activitySamplesPerTimestamp.get(ts.intValue())).add((FitMonitoring) record);
            } else if (record instanceof FitSpo2) {
                final Integer spo2 = ((FitSpo2) record).getReadingSpo2();
                if (spo2 == null || spo2 <= 0) {
                    continue;
                }
                LOG.trace("SpO2 at {}: {}", ts, spo2);
                final GarminSpo2Sample sample = new GarminSpo2Sample();
                sample.setTimestamp(ts * 1000L);
                sample.setSpo2(spo2);
                spo2samples.add(sample);
            } else if (record instanceof FitEvent) {
                final FitEvent event = (FitEvent) record;
                if (event.getEvent() == null) {
                    LOG.warn("Event in {} is null", event);
                    continue;
                }

                LOG.trace("Event at {}: {}", ts, event);

                final GarminEventSample sample = new GarminEventSample();
                sample.setTimestamp(ts * 1000L);
                sample.setEvent(event.getEvent());
                if (event.getEventType() != null) {
                    sample.setEventType(event.getEventType());
                }
                if (event.getData() != null) {
                    sample.setData(event.getData());
                }
                events.add(sample);
            } else if (record instanceof FitRecord) {
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
            } else if (record instanceof FitHrvSummary) {
                final FitHrvSummary hrvSummary = (FitHrvSummary) record;
                LOG.trace("HRV summary at {}: {}", ts, record);
                final GarminHrvSummarySample sample = new GarminHrvSummarySample();
                sample.setTimestamp(ts * 1000L);
                if (hrvSummary.getWeeklyAverage() != null) {
                    sample.setWeeklyAverage(Math.round(hrvSummary.getWeeklyAverage()));
                }
                if (hrvSummary.getLastNightAverage() != null) {
                    sample.setLastNightAverage(Math.round(hrvSummary.getLastNightAverage()));
                }
                if (hrvSummary.getLastNight5MinHigh() != null) {
                    sample.setLastNight5MinHigh(Math.round(hrvSummary.getLastNight5MinHigh()));
                }
                if (hrvSummary.getBaselineLowUpper() != null) {
                    sample.setBaselineLowUpper(Math.round(hrvSummary.getBaselineLowUpper()));
                }
                if (hrvSummary.getBaselineBalancedLower() != null) {
                    sample.setBaselineBalancedLower(Math.round(hrvSummary.getBaselineBalancedLower()));
                }
                if (hrvSummary.getBaselineBalancedUpper() != null) {
                    sample.setBaselineBalancedUpper(Math.round(hrvSummary.getBaselineBalancedUpper()));
                }
                final FieldDefinitionHrvStatus.HrvStatus status = hrvSummary.getStatus();
                if (status != null) {
                    sample.setStatusNum(status.getId());
                }
                hrvSummarySamples.add(sample);
            } else if (record instanceof FitHrvValue) {
                final FitHrvValue hrvValue = (FitHrvValue) record;
                if (hrvValue.getValue() == null) {
                    LOG.warn("HRV value at {} is null", ts);
                    continue;
                }
                LOG.trace("HRV value at {}: {}", ts, hrvValue.getValue());
                final GarminHrvValueSample sample = new GarminHrvValueSample();
                sample.setTimestamp(ts * 1000L);
                sample.setValue(Math.round(hrvValue.getValue()));
                hrvValueSamples.add(sample);
            } else {
                LOG.trace("Unknown record: {}", record);

                if (!unknownRecords.containsKey(record.getGlobalFITMessage().getNumber())) {
                    unknownRecords.put(record.getGlobalFITMessage().getNumber(), 0);
                }
                unknownRecords.put(
                        record.getGlobalFITMessage().getNumber(),
                        Objects.requireNonNull(unknownRecords.get(record.getGlobalFITMessage().getNumber())) + 1
                );
            }
        }

        if (fileId == null) {
            LOG.error("Got no file ID");
            return;
        }
        if (fileId.getType() == null) {
            LOG.error("File has no type");
            return;
        }

        switch (fileId.getType()) {
            case ACTIVITY:
                persistWorkout(file);
                break;
            case MONITOR:
                persistActivitySamples();
                persistSpo2Samples();
                persistStressSamples();
                persistBodyEnergySamples();
                break;
            case SLEEP:
                persistEvents();
                persistSleepStageSamples();
                break;
            case HRV_STATUS:
                persistHrvSummarySamples();
                persistHrvValueSamples();
                break;
            default:
                LOG.warn("Unable to handle fit file of type {}", fileId.getType());
        }

        for (final Map.Entry<Integer, Integer> e : unknownRecords.entrySet()) {
            LOG.warn("Unknown record of global number {} seen {} times", e.getKey(), e.getValue());
        }
    }

    private void persistWorkout(final File file) {
        if (session == null) {
            LOG.error("Got workout from {}, but no session", fileId);
            return;
        }

        LOG.debug("Persisting workout for {}", fileId);

        final BaseActivitySummary summary;

        // This ensures idempotency when re-processing
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            summary = findOrCreateBaseActivitySummary(session, device, user, Objects.requireNonNull(fileId.getTimeCreated()).intValue());
        } catch (final Exception e) {
            GB.toast(context, "Error finding base summary", Toast.LENGTH_LONG, GB.ERROR, e);
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

        if (session.getTotalElapsedTime() == null) {
            LOG.error("No elapsed time for {}", fileId);
            return;
        }
        summary.setEndTime(new Date(summary.getStartTime().getTime() + session.getTotalElapsedTime().intValue()));

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
            // Find the firt time in zone for the session (assumes single-session)
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
            // TODO lactate_threshold_heart_rate
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
                summaryData.add(RECOVERY_TIME, physiologicalMetrics.getRecoveryTime() / 60f, UNIT_HOURS);
            }
            if (physiologicalMetrics.getLactateThresholdHeartRate() != null) {
                summaryData.add(LACTATE_THRESHOLD_HR, physiologicalMetrics.getLactateThresholdHeartRate(), UNIT_BPM);
            }
        }

        summary.setSummaryData(summaryData.toString());
        if (file != null) {
            summary.setRawDetailsPath(file.getAbsolutePath());
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            summary.setDevice(device);
            summary.setUser(user);

            session.getBaseActivitySummaryDao().insertOrReplace(summary);
        } catch (final Exception e) {
            GB.toast(context, "Error saving workout", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private ActivityKind getActivityKind(final Integer sport, final Integer subsport) {
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

    protected static BaseActivitySummary findOrCreateBaseActivitySummary(final DaoSession session,
                                                                         final Device device,
                                                                         final User user,
                                                                         final int timestampSeconds) {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.StartTime.eq(new Date(timestampSeconds * 1000L)));
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(device.getId()));
        qb.where(BaseActivitySummaryDao.Properties.UserId.eq(user.getId()));
        final List<BaseActivitySummary> summaries = qb.build().list();
        if (summaries.isEmpty()) {
            final BaseActivitySummary summary = new BaseActivitySummary();
            summary.setStartTime(new Date(timestampSeconds * 1000L));
            summary.setDevice(device);
            summary.setUser(user);

            // These will be set later, once we parse the summary
            summary.setEndTime(new Date(timestampSeconds * 1000L));
            summary.setActivityKind(ActivityKind.UNKNOWN.getCode());

            return summary;
        }
        if (summaries.size() > 1) {
            LOG.warn("Found multiple summaries for {}", timestampSeconds);
        }
        return summaries.get(0);
    }

    private void reset() {
        activitySamplesPerTimestamp.clear();
        stressSamples.clear();
        bodyEnergySamples.clear();
        spo2samples.clear();
        events.clear();
        sleepStageSamples.clear();
        hrvSummarySamples.clear();
        hrvValueSamples.clear();
        timesInZone.clear();
        activityPoints.clear();
        unknownRecords.clear();
        fileId = null;
        session = null;
        sport = null;
        physiologicalMetrics = null;
    }

    private void persistActivitySamples() {
        if (activitySamplesPerTimestamp.isEmpty()) {
            return;
        }

        final List<GarminActivitySample> activitySamples = new ArrayList<>(activitySamplesPerTimestamp.size());

        // Garmin reports the cumulative steps per activity, but not always, so we need to keep
        // track of the number of steps for each activity, and set the sum of all on the sample
        final Map<Integer, Long> stepsPerActivity = new HashMap<>();

        final int THRESHOLD_NOT_WORN = 10 * 60; // 10 min gap between samples = not-worn
        int prevActivityKind = ActivityKind.UNKNOWN.getCode();
        int prevTs = -1;

        for (final int ts : activitySamplesPerTimestamp.keySet()) {
            if (prevTs > 0 && ts - prevTs > 60) {
                // Fill gaps between samples
                for (int i = prevTs; i < ts; i += 60) {
                    final GarminActivitySample sample = new GarminActivitySample();
                    sample.setTimestamp(i);
                    sample.setRawKind(ts - prevTs > THRESHOLD_NOT_WORN ? ActivityKind.NOT_WORN.getCode() : prevActivityKind);
                    sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    sample.setSteps(ActivitySample.NOT_MEASURED);
                    activitySamples.add(sample);
                }
            }

            final List<FitMonitoring> records = activitySamplesPerTimestamp.get(ts);

            final GarminActivitySample sample = new GarminActivitySample();
            sample.setTimestamp(ts);
            sample.setRawKind(ActivityKind.ACTIVITY.getCode());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setHeartRate(ActivitySample.NOT_MEASURED);

            boolean hasSteps = false;
            for (final FitMonitoring record : Objects.requireNonNull(records)) {
                final Integer activityType = record.getComputedActivityType().orElse(ActivitySample.NOT_MEASURED);

                final Integer hr = record.getHeartRate();
                if (hr != null) {
                    sample.setHeartRate(hr);
                }

                final Long steps = record.getCycles();
                if (steps != null) {
                    stepsPerActivity.put(activityType, steps);
                    hasSteps = true;
                }

                final Integer intensity = record.getComputedIntensity();
                if (intensity != null) {
                    sample.setRawIntensity(intensity);
                }
            }
            if (hasSteps) {
                int sumSteps = 0;
                for (final Long steps : stepsPerActivity.values()) {
                    sumSteps += steps;
                }
                sample.setSteps(sumSteps);
            }

            activitySamples.add(sample);

            prevActivityKind = sample.getRawKind();
            prevTs = ts;
        }

        LOG.debug("Will persist {} activity samples", activitySamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(gbDevice, session);

            for (final GarminActivitySample sample : activitySamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addGBActivitySamples(activitySamples.toArray(new GarminActivitySample[0]));
        } catch (final Exception e) {
            GB.toast(context, "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistEvents() {
        if (events.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} event samples", events.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminEventSampleProvider sampleProvider = new GarminEventSampleProvider(gbDevice, session);

            for (final GarminEventSample sample : events) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(events);
        } catch (final Exception e) {
            GB.toast(context, "Error saving event samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistSleepStageSamples() {
        if (sleepStageSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} sleep stage samples", sleepStageSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminSleepStageSampleProvider sampleProvider = new GarminSleepStageSampleProvider(gbDevice, session);

            for (final GarminSleepStageSample sample : sleepStageSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(sleepStageSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving sleep stage samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistHrvSummarySamples() {
        if (hrvSummarySamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} HRV summary samples", hrvSummarySamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminHrvSummarySampleProvider sampleProvider = new GarminHrvSummarySampleProvider(gbDevice, session);

            for (final GarminHrvSummarySample sample : hrvSummarySamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(hrvSummarySamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving HRV summary samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistHrvValueSamples() {
        if (hrvValueSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} HRV value samples", hrvValueSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminHrvValueSampleProvider sampleProvider = new GarminHrvValueSampleProvider(gbDevice, session);

            for (final GarminHrvValueSample sample : hrvValueSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(hrvValueSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving HRV value samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistSpo2Samples() {
        if (spo2samples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} spo2 samples", stressSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminSpo2SampleProvider sampleProvider = new GarminSpo2SampleProvider(gbDevice, session);

            for (final GarminSpo2Sample sample : spo2samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(spo2samples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving spo2 samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistStressSamples() {
        if (stressSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} stress samples", stressSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminStressSampleProvider sampleProvider = new GarminStressSampleProvider(gbDevice, session);

            for (final GarminStressSample sample : stressSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(stressSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving stress samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistBodyEnergySamples() {
        if (bodyEnergySamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} body energy samples", bodyEnergySamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminBodyEnergySampleProvider sampleProvider = new GarminBodyEnergySampleProvider(gbDevice, session);

            for (final GarminBodyEnergySample sample : bodyEnergySamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(bodyEnergySamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving body energy samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }
}
