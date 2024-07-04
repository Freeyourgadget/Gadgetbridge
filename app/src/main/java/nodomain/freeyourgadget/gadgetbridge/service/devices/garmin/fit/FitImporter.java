package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ASCENT_DISTANCE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DESCENT_DISTANCE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;

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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminEventSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitMonitoring;
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
    private final List<GarminSpo2Sample> spo2samples = new ArrayList<>();
    private final List<GarminEventSample> events = new ArrayList<>();
    private final List<GarminSleepStageSample> sleepStageSamples = new ArrayList<>();
    private final List<FitTimeInZone> timesInZone = new ArrayList<>();
    private final List<ActivityPoint> activityPoints = new ArrayList<>();
    private final Map<Integer, Integer> unknownRecords = new HashMap<>();
    private FitFileId fileId = null;
    private FitSession session = null;
    private FitSport sport = null;

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
                final Integer stress = ((FitStressLevel) record).getStressLevelValue();
                if (stress == null || stress < 0) {
                    continue;
                }
                LOG.trace("Stress at {}: {}", ts, stress);
                final GarminStressSample sample = new GarminStressSample();
                sample.setTimestamp(ts * 1000L);
                sample.setStress(stress);
                stressSamples.add(sample);
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
            case activity:
                persistWorkout(file);
                break;
            case monitor:
                persistActivitySamples();
                persistSpo2Samples();
                persistStressSamples();
                break;
            case sleep:
                persistEvents();
                persistSleepStageSamples();
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

        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);

        final ActivitySummaryData summaryData = new ActivitySummaryData();

        final int activityKind;
        if (sport != null) {
            summary.setName(sport.getName());
            activityKind = getActivityKind(sport.getSport(), sport.getSubSport());
        } else {
            activityKind = getActivityKind(session.getSport(), session.getSubSport());
        }
        summary.setActivityKind(activityKind);
        if (session.getStartTime() == null) {
            LOG.error("No session start time for {}", fileId);
            return;
        }
        summary.setStartTime(new Date(GarminTimeUtils.garminTimestampToJavaMillis(session.getStartTime().intValue())));

        if (session.getTotalElapsedTime() == null) {
            LOG.error("No elapsed time for {}", fileId);
            return;
        }
        summary.setEndTime(new Date(GarminTimeUtils.garminTimestampToJavaMillis(session.getStartTime().intValue() + session.getTotalElapsedTime().intValue() / 1000)));

        if (session.getTotalTimerTime() != null) {
            summaryData.add(ACTIVE_SECONDS, session.getTotalTimerTime() / 1000f, UNIT_SECONDS);
        }
        if (session.getTotalDistance() != null) {
            summaryData.add(DISTANCE_METERS, session.getTotalDistance() / 100f, UNIT_METERS);
        }
        if (session.getTotalCalories() != null) {
            summaryData.add(CALORIES_BURNT, session.getTotalCalories(), UNIT_KCAL);
        }
        if (session.getTotalAscent() != null) {
            summaryData.add(ASCENT_DISTANCE, session.getTotalAscent(), UNIT_METERS);
        }
        if (session.getTotalDescent() != null) {
            summaryData.add(DESCENT_DISTANCE, session.getTotalDescent(), UNIT_METERS);
        }

        //FitTimeInZone timeInZone = null;
        //for (final FitTimeInZone fitTimeInZone : timesInZone) {
        //    // Find the firt time in zone for the session (assumes single-session)
        //    if (fitTimeInZone.getReferenceMessage() != null && fitTimeInZone.getReferenceMessage() == 18) {
        //        timeInZone = fitTimeInZone;
        //        break;
        //    }
        //}
        //if (timeInZone != null) {
        //}

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

    private int getActivityKind(final Integer sport, final Integer subsport) {
        final Optional<GarminSport> garminSport = GarminSport.fromCodes(sport, subsport);
        if (garminSport.isEmpty()) {
            LOG.warn("Unknown garmin sport {}/{}", sport, subsport);
            return ActivityKind.TYPE_UNKNOWN;
        }

        switch (garminSport.get()) {
            case RUN:
            case PUSH_RUN_SPEED:
            case INDOOR_PUSH_RUN_SPEED:
            case INDOOR_TRACK:
                return ActivityKind.TYPE_RUNNING;
            case TREADMILL:
                return ActivityKind.TYPE_TREADMILL;
            case E_BIKE:
            case BIKE:
            case BIKE_COMMUTE:
                return ActivityKind.TYPE_CYCLING;
            case BIKE_INDOOR:
                return ActivityKind.TYPE_INDOOR_CYCLING;
            case ELLIPTICAL:
                return ActivityKind.TYPE_ELLIPTICAL_TRAINER;
            case STAIR_STEPPER:
            case PILATES:
            case CARDIO:
                return ActivityKind.TYPE_EXERCISE;
            case POOL_SWIM:
                return ActivityKind.TYPE_SWIMMING;
            case OPEN_WATER:
                return ActivityKind.TYPE_SWIMMING_OPENWATER;
            case SOCCER:
                return ActivityKind.TYPE_SOCCER;
            case STRENGTH:
                return ActivityKind.TYPE_STRENGTH_TRAINING;
            case YOGA:
                return ActivityKind.TYPE_YOGA;
            case WALK:
            case WALK_INDOOR:
            case PUSH_WALK_SPEED:
            case INDOOR_PUSH_WALK_SPEED:
                return ActivityKind.TYPE_WALKING;
            case HIKE:
                return ActivityKind.TYPE_HIKING;
            case CLIMB_INDOOR:
            case BOULDERING:
                return ActivityKind.TYPE_CLIMBING;
        }

        return ActivityKind.TYPE_UNKNOWN;
    }

    private void reset() {
        activitySamplesPerTimestamp.clear();
        stressSamples.clear();
        spo2samples.clear();
        events.clear();
        sleepStageSamples.clear();
        timesInZone.clear();
        activityPoints.clear();
        unknownRecords.clear();
        fileId = null;
        session = null;
        sport = null;
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
        int prevActivityKind = ActivityKind.TYPE_UNKNOWN;
        int prevTs = -1;

        for (final int ts : activitySamplesPerTimestamp.keySet()) {
            if (prevTs > 0 && ts - prevTs > 60) {
                // Fill gaps between samples
                for (int i = prevTs; i < ts; i += 60) {
                    final GarminActivitySample sample = new GarminActivitySample();
                    sample.setTimestamp(i);
                    sample.setRawKind(ts - prevTs > THRESHOLD_NOT_WORN ? ActivityKind.TYPE_NOT_WORN : prevActivityKind);
                    sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    sample.setSteps(ActivitySample.NOT_MEASURED);
                    activitySamples.add(sample);
                }
            }

            final List<FitMonitoring> records = activitySamplesPerTimestamp.get(ts);

            final GarminActivitySample sample = new GarminActivitySample();
            sample.setTimestamp(ts);
            sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
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
}
