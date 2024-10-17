package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminBodyEnergySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminEventSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminHeartRateRestingSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminRespiratoryRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminRestingMetabolicRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminWorkoutParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminBodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHeartRateRestingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminRestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHrvSummary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitHrvValue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoring;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoringHrData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoringInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitPhysiologicalMetrics;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRespirationRate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSleepDataInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSleepDataRaw;
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

    private final SortedMap<Long, List<FitMonitoring>> activitySamplesPerTimestamp = new TreeMap<>();
    private final List<GarminStressSample> stressSamples = new ArrayList<>();
    private final List<GarminBodyEnergySample> bodyEnergySamples = new ArrayList<>();
    private final List<GarminSpo2Sample> spo2samples = new ArrayList<>();
    private final List<GarminRespiratoryRateSample> respiratoryRateSamples = new ArrayList<>();
    private final List<GarminHeartRateRestingSample> restingHrSamples = new ArrayList<>();
    private final List<GarminEventSample> events = new ArrayList<>();
    private final List<GarminSleepStageSample> sleepStageSamples = new ArrayList<>();
    private final List<GarminHrvSummarySample> hrvSummarySamples = new ArrayList<>();
    private final List<GarminHrvValueSample> hrvValueSamples = new ArrayList<>();
    private final List<GarminRestingMetabolicRateSample> restingMetabolicRateSamples = new ArrayList<>();
    private final Map<Integer, Integer> unknownRecords = new HashMap<>();
    private FitSleepDataInfo fitSleepDataInfo = null;
    private final List<FitSleepDataRaw> fitSleepDataRawSamples = new ArrayList<>();
    private FitFileId fileId = null;

    private final GarminWorkoutParser workoutParser;

    public FitImporter(final Context context, final GBDevice gbDevice) {
        this.context = context;
        this.gbDevice = gbDevice;
        this.workoutParser = new GarminWorkoutParser(context);
    }

    /**
     * @noinspection StatementWithEmptyBody
     */
    public void importFile(final File file) throws IOException {
        reset();

        final FitFile fitFile = FitFile.parseIncoming(file);

        Long lastMonitoringTimestamp = null;

        for (final RecordData record : fitFile.getRecords()) {
            if (fileId != null && fileId.getType() == FileType.FILETYPE.ACTIVITY) {
                if (workoutParser.handleRecord(record)) {
                    continue;
                }
            }

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
            } else if (record instanceof FitSleepDataInfo) {
                final FitSleepDataInfo newFitSleepDataInfo = (FitSleepDataInfo) record;
                LOG.debug("Sleep Data Info: {}", newFitSleepDataInfo);
                if (fitSleepDataInfo != null) {
                    // Should not happen
                    LOG.warn("Already had sleep data info: {}", fitSleepDataInfo);
                }
                fitSleepDataInfo = newFitSleepDataInfo;
            } else if (record instanceof FitSleepDataRaw) {
                final FitSleepDataRaw fitSleepDataRaw = (FitSleepDataRaw) record;
                //LOG.debug("Sleep Data Raw: {}", fitSleepDataRaw);
                fitSleepDataRawSamples.add(fitSleepDataRaw);
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
                final FitMonitoring monitoringRecord = (FitMonitoring) record;
                final Long currentMonitoringTimestamp = monitoringRecord.computeTimestamp(lastMonitoringTimestamp);
                if (!activitySamplesPerTimestamp.containsKey(currentMonitoringTimestamp)) {
                    activitySamplesPerTimestamp.put(currentMonitoringTimestamp, new ArrayList<>());
                }
                Objects.requireNonNull(activitySamplesPerTimestamp.get(currentMonitoringTimestamp)).add(monitoringRecord);
                lastMonitoringTimestamp = currentMonitoringTimestamp;
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
            } else if (record instanceof FitRespirationRate) {
                final Float respiratoryRate = ((FitRespirationRate) record).getRespirationRate();
                if (respiratoryRate == null || respiratoryRate <= 0) {
                    continue;
                }
                LOG.trace("Respiratory rate at {}: {}", ts, respiratoryRate);
                final GarminRespiratoryRateSample sample = new GarminRespiratoryRateSample();
                sample.setTimestamp(ts * 1000L);
                sample.setRespiratoryRate(respiratoryRate);
                respiratoryRateSamples.add(sample);
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
                // handled in workout parser
            } else if (record instanceof FitSession) {
                // handled in workout parser
            } else if (record instanceof FitPhysiologicalMetrics) {
                // handled in workout parser
            } else if (record instanceof FitSport) {
                // handled in workout parser
            } else if (record instanceof FitTimeInZone) {
                // handled in workout parser
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
            } else if (record instanceof FitMonitoringInfo) {
                final FitMonitoringInfo monitoringInfo = (FitMonitoringInfo) record;
                if (monitoringInfo.getRestingMetabolicRate() == null) {
                    continue;
                }
                LOG.trace("Monitoring info at {}: {}", ts, record);
                final GarminRestingMetabolicRateSample sample = new GarminRestingMetabolicRateSample();
                sample.setTimestamp(ts * 1000L);
                sample.setRestingMetabolicRate(monitoringInfo.getRestingMetabolicRate());
                restingMetabolicRateSamples.add(sample);
            } else if (record instanceof FitMonitoringHrData) {
                final FitMonitoringHrData monitoringHrData = (FitMonitoringHrData) record;
                if (monitoringHrData.getRestingHeartRate() == null) {
                    LOG.warn("Resting HR at {} is null", ts);
                    continue;
                }
                LOG.trace("Resting HR at {}: {}", ts, monitoringHrData.getRestingHeartRate());
                final GarminHeartRateRestingSample sample = new GarminHeartRateRestingSample();
                sample.setTimestamp(ts * 1000L);
                sample.setHeartRate(monitoringHrData.getRestingHeartRate());
                restingHrSamples.add(sample);
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
                persistRespiratoryRateSamples();
                persistRestingHrSamples();
                persistStressSamples();
                persistBodyEnergySamples();
                persistRestingMetabolicRateSamples();
                break;
            case SLEEP:
                persistEvents();
                persistSleepStageSamples();
                processRawSleepSamples();
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
        LOG.debug("Persisting workout for {}", fileId);

        final BaseActivitySummary summary;

        // This ensures idempotency when re-processing
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            summary = ActivitySummaryParser.findOrCreateBaseActivitySummary(
                    session,
                    gbDevice,
                    Objects.requireNonNull(fileId.getTimeCreated()).intValue()
            );
        } catch (final Exception e) {
            GB.toast(context, "Error finding base summary", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        workoutParser.updateSummary(summary);

        summary.setRawDetailsPath(file.getAbsolutePath());

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

    private void reset() {
        activitySamplesPerTimestamp.clear();
        stressSamples.clear();
        bodyEnergySamples.clear();
        spo2samples.clear();
        respiratoryRateSamples.clear();
        restingHrSamples.clear();
        events.clear();
        sleepStageSamples.clear();
        hrvSummarySamples.clear();
        hrvValueSamples.clear();
        restingMetabolicRateSamples.clear();
        unknownRecords.clear();
        fitSleepDataInfo = null;
        fitSleepDataRawSamples.clear();
        fileId = null;
        workoutParser.reset();
    }

    private void persistActivitySamples() {
        if (activitySamplesPerTimestamp.isEmpty()) {
            return;
        }

        final List<GarminActivitySample> activitySamples = new ArrayList<>(activitySamplesPerTimestamp.size());

        // Garmin reports the cumulative data per activity, but not always, so we need to keep
        // track of the amounts for each activity, and set the sum of all on the sample
        final Map<Integer, Long> stepsPerActivity = new HashMap<>();
        final Map<Integer, Long> distancePerActivity = new HashMap<>();
        final Map<Integer, Integer> caloriesPerActivity = new HashMap<>();

        final int THRESHOLD_NOT_WORN = 10 * 60; // 10 min gap between samples = not-worn
        int prevActivityKind = ActivityKind.UNKNOWN.getCode();
        int prevTs = -1;

        for (final long ts : activitySamplesPerTimestamp.keySet()) {
            if (prevTs > 0 && ts - prevTs > 60) {
                // Fill gaps between samples
                LOG.debug("Filling gap between {} and {}", prevTs, ts);
                for (int i = prevTs + 60; i < ts; i += 60) {
                    final GarminActivitySample sample = new GarminActivitySample();
                    sample.setTimestamp(i);
                    sample.setRawKind(ts - prevTs > THRESHOLD_NOT_WORN ? ActivityKind.NOT_WORN.getCode() : prevActivityKind);
                    sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    sample.setSteps(ActivitySample.NOT_MEASURED);
                    sample.setHeartRate(ActivitySample.NOT_MEASURED);
                    sample.setDistanceCm(ActivitySample.NOT_MEASURED);
                    sample.setActiveCalories(ActivitySample.NOT_MEASURED);
                    activitySamples.add(sample);
                }
            }

            final List<FitMonitoring> records = activitySamplesPerTimestamp.get(ts);

            final GarminActivitySample sample = new GarminActivitySample();
            sample.setTimestamp((int) ts);
            sample.setRawKind(ActivityKind.ACTIVITY.getCode());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setDistanceCm(ActivitySample.NOT_MEASURED);
            sample.setActiveCalories(ActivitySample.NOT_MEASURED);

            boolean hasSteps = false;
            boolean hasDistance = false;
            boolean hasCalories = false;
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

                final Long distance = record.getDistance();
                if (distance != null) {
                    distancePerActivity.put(activityType, distance);
                    hasDistance = true;
                }

                final Integer calories = record.getActiveCalories();
                if (calories != null) {
                    caloriesPerActivity.put(activityType, calories);
                    hasCalories = true;
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
            if (hasDistance) {
                int sumDistance = 0;
                for (final Long distance : distancePerActivity.values()) {
                    sumDistance += distance;
                }
                sample.setDistanceCm(sumDistance);
            }
            if (hasCalories) {
                int sumCalories = 0;
                for (final Integer calories : caloriesPerActivity.values()) {
                    sumCalories += calories;
                }
                sample.setActiveCalories(sumCalories);
            }

            activitySamples.add(sample);

            prevActivityKind = sample.getRawKind();
            prevTs = (int) ts;
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
        // We may have samples, but not sleep samples - #4048
        // 0 unmeasurable, 1 awake
        final boolean anySleepSample = sleepStageSamples.stream()
                .anyMatch(s -> s.getStage() != 0 && s.getStage() != 1);
        if (!anySleepSample) {
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

    /**
     * As per #4048, devices that do not have a sleep widget send raw sleep samples, which we do not
     * know how to parse. Therefore, we don't persist the sleep stages they report (they're all awake),
     * but we fake light sleep for the duration of the raw sleep samples, in order to have some data
     * at all.
     */
    private void processRawSleepSamples() {
        if (fitSleepDataRawSamples.isEmpty()) {
            return;
        }

        final boolean anySleepSample = sleepStageSamples.stream()
                .anyMatch(s -> s.getStage() != 0 && s.getStage() != 1);
        if (anySleepSample) {
            // We have at least one real sleep sample - do nothing
            return;
        }

        final long asleepTimeMillis = Objects.requireNonNull(fileId.getTimeCreated()).intValue() * 1000L;
        final long wakeTimeMillis = asleepTimeMillis + fitSleepDataRawSamples.size() * 60 * 1000L;

        LOG.debug("Got {} raw sleep samples - faking sleep events from {} to {}", fitSleepDataRawSamples.size(), asleepTimeMillis, wakeTimeMillis);

        // We only need to fake sleep start and end times, the sample provider will take care of the rest
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminEventSampleProvider sampleProvider = new GarminEventSampleProvider(gbDevice, session);

            final GarminEventSample sampleFallAsleep = new GarminEventSample();
            sampleFallAsleep.setTimestamp(asleepTimeMillis);
            sampleFallAsleep.setEvent(74); // sleep
            sampleFallAsleep.setEventType(0); // sleep start
            sampleFallAsleep.setData(-1L); // in actual samples they're a garmin epoch, this way we can identify them
            sampleFallAsleep.setDevice(device);
            sampleFallAsleep.setUser(user);

            final GarminEventSample sampleWakeUp = new GarminEventSample();
            sampleWakeUp.setTimestamp(wakeTimeMillis);
            sampleWakeUp.setEvent(74); // sleep
            sampleWakeUp.setEventType(1); // sleep end
            sampleWakeUp.setData(-1L); // in actual samples they're a garmin epoch, this way we can identify them
            sampleWakeUp.setDevice(device);
            sampleWakeUp.setUser(user);

            sampleProvider.addSample(sampleFallAsleep);
            sampleProvider.addSample(sampleWakeUp);
        } catch (final Exception e) {
            GB.toast(context, "Error faking event samples", Toast.LENGTH_LONG, GB.ERROR, e);
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

        LOG.debug("Will persist {} spo2 samples", spo2samples.size());

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

    private void persistRespiratoryRateSamples() {
        if (respiratoryRateSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} respiratory rate samples", stressSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminRespiratoryRateSampleProvider sampleProvider = new GarminRespiratoryRateSampleProvider(gbDevice, session);

            for (final GarminRespiratoryRateSample sample : respiratoryRateSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(respiratoryRateSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving respiratory rate samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void persistRestingHrSamples() {
        if (restingHrSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} resting heart rate samples", restingHrSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminHeartRateRestingSampleProvider sampleProvider = new GarminHeartRateRestingSampleProvider(gbDevice, session);

            for (final GarminHeartRateRestingSample sample : restingHrSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(restingHrSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving resting heart rate samples", Toast.LENGTH_LONG, GB.ERROR, e);
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

    private void persistRestingMetabolicRateSamples() {
        if (restingMetabolicRateSamples.isEmpty()) {
            return;
        }

        LOG.debug("Will persist {} resting metabolic rate samples", restingMetabolicRateSamples.size());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final GarminRestingMetabolicRateSampleProvider sampleProvider = new GarminRestingMetabolicRateSampleProvider(gbDevice, session);

            for (final GarminRestingMetabolicRateSample sample : restingMetabolicRateSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            sampleProvider.addSamples(restingMetabolicRateSamples);
        } catch (final Exception e) {
            GB.toast(context, "Error saving body energy samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }
}
